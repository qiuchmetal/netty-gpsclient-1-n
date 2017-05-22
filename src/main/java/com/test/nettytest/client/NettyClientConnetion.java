package com.test.nettytest.client;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.channelhandler.RealDataHandler;
import com.test.nettytest.client.pojo.ChannelThreadInfo;
import com.test.nettytest.client.pojo.ConnectionThreadInfo;
import com.test.nettytest.client.pojo.GPSDataLineFromAFile;
import com.test.nettytest.client.util.NettyClientUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class NettyClientConnetion
{
	/**
	 * 当前连接线程信息
	 */
	private ConnectionThreadInfo connectionThreadInfo;
	/**
	 * 管道线程集合
	 */
	private ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque;
	/**
	 * 从目录下加载到的车号队列
	 */
//	public ConcurrentLinkedDeque<Queue<GPSDataLineFromAFile>> busDeque;
	/**
	 * 一共多少辆车，每辆车一个连接
	 */
	private int busCount;
	/**
	 * 使用 busId 与 将要发送的车号队列组成 map，在模拟断开重连时需要用上
	 */
	public ConcurrentHashMap<String, ArrayDeque<GPSDataLineFromAFile>> busMap;
	/**
	 * 记录每个车号发送了多少指令
	 */
	public ConcurrentHashMap<String, Integer> busSendCountMap = new ConcurrentHashMap<String, Integer>();	
	/**
	 * 当前连接线程组，在子线程模拟断开重连时，需要用它来启用调度
	 */
	public EventLoopGroup group = new NioEventLoopGroup();
	/**
	 * 已连接的车号集合，用于检查数据包是否已经发完
	 */
	public Set<String> busSet = new ConcurrentSkipListSet<String>();

	private volatile ScheduledFuture<?> checkBusSetTask; // 检查车号集合是否为空

	private long startTimestamp; // 开始运行

	private static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	private Bootstrap bootstrap;

//	private final int CONNECTION_COUNT = NettyClientUtil.CONNETION_COUNT; //需要保持的连接数

	// 执行 IO 之外的业务线程
	public ScheduledExecutorService taskService;

//	public NettyClientConnetion(ConnectionThreadInfo connectionThreadInfo, ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque)
//	{
//		this.connectionThreadInfo = connectionThreadInfo;
//		this.channelThreadInfodDeque = channelThreadInfodDeque;
//
//		// 连接线程开始时间
//		this.connectionThreadInfo.setStartTime(System.currentTimeMillis());
//
//		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
//		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 即将开启的连接数：["
//				+ NettyClientUtil.CONNETION_COUNT + "]");
//
//		taskService = Executors.newScheduledThreadPool(NettyClientUtil.THREAD_POOL_SIZE + 1);
//	}

	public NettyClientConnetion(ConnectionThreadInfo connectionThreadInfo
			, ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque
			, ConcurrentHashMap<String, ArrayDeque<GPSDataLineFromAFile>> busMap, int busCount)
	{
		this.connectionThreadInfo = connectionThreadInfo;
		this.channelThreadInfodDeque = channelThreadInfodDeque;
		this.busMap = busMap;
		this.busCount = busCount;

		// 连接线程开始时间
		this.connectionThreadInfo.setStartTime(System.currentTimeMillis());

		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 即将开启的连接数：[" + busCount + "]");

		taskService = Executors.newScheduledThreadPool(busCount > 2 ? busCount / 2 : 1);
//		taskService = Executors.newScheduledThreadPool(busCount);
	}

	public void start()
	{
		// EventLoopGroup group = new NioEventLoopGroup();
		bootstrap = new Bootstrap();
		bootstrap.group(group);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.handler(new ChannelInitializer<SocketChannel>()
		{
			@Override
			protected void initChannel(SocketChannel ch) throws Exception
			{
				ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(65535, 4, 2, 2, 0));
//				ch.pipeline().addLast(new LoginHandler(channelThreadInfodDeque, NettyClientConnetion.this));
//				ch.pipeline().addLast(new RealDataHandler(channelThreadInfodDeque, NettyClientConnetion.this, null));
			}
		});

		// //记录线程开始时间
		// threadInfo.setStartTime(System.currentTimeMillis());
		// 开始时间
		startTimestamp = System.currentTimeMillis();

		// for (int i = 0; i < CONNECTION_COUNT; i++)
		// doConnect();

		// 使用调度线程进行连接，每个车号一个连接
//		for (int i = 0; i < busCount; i++)
		for(String busId:busMap.keySet())
			group.schedule(() -> doConnect(busId, false), (long) (NettyClientUtil.LOGIN_TIMEOUT * 60 * (Math.random() * 0.9 + 0.1)),
					TimeUnit.SECONDS);
//			group.schedule(() -> doConnect(busId, false), 0, TimeUnit.SECONDS);

//		for(Map.Entry<UUID, Queue<GPSDataLineFromAFile>> e:busMap.entrySet())
//		{
//			group.schedule(() -> doConnect(e.getKey()), 0,TimeUnit.SECONDS);
//		}

		// ChannelFuture f = null;
		//
		// while (true)
		// {
		// try
		// {
		// f = b.connect(host, port).sync();
		// //连接次数
		// threadInfo.setConnectionCount(threadInfo.getConnectionCount() + 1);
		// if (null!=f && f.isSuccess())
		// {
		// System.out.println("[" + Thread.currentThread().getName() + "]
		// 已连接至Netty Server --> " + host + ":" + port);
		// f.channel().closeFuture().sync();
		// System.out.println("已断开连接");
		// //断开次数
		// threadInfo.setDisconnectionCount(threadInfo.getDisconnectionCount() +
		// 1);
		// }
		//
		// //断开10秒后进行重连
		// TimeUnit.SECONDS.sleep(10);
		// System.out.println("现尝试重连。");
		// }
		// catch (InterruptedException e)
		// {
		// e.printStackTrace();
		// }
		// }

		// finally
		// {
		// group.shutdownGracefully();
		// System.out.println("已优雅退出。");
		// }
	}

	public void doConnect(String busId, boolean isDisconnectByManual)
	{
		String host = NettyClientUtil.SERVER_IP;
		int port = NettyClientUtil.SERVER_PORT;

//		System.out.println("当前连接线程：" + Thread.currentThread().getName() + "  " + Thread.currentThread().getId());

		// 尝试连接次数
		connectionThreadInfo.setAndGetTryToConnectCount();

		// System.out.println("[" + Thread.currentThread().getName() + "] [" +
		// df.format(new Date()) + "] 准备连接 Netty Server --> " + host + ":" +
		// port);

		ChannelFuture future = bootstrap.connect(host, port);

		future.addListener(new ChannelFutureListener()
		{
			public void operationComplete(ChannelFuture futureListener) throws Exception
			{
				if (futureListener.isSuccess())
				{
					// 成功连接次数
					connectionThreadInfo.setAndGetConnectionCount();

					// 根据 busId 的不同，动态增加 ChannelHandler
					if (busId != null && !busId.isEmpty())
					{
//						futureListener.channel().pipeline().removeLast();
						futureListener.channel().pipeline()
							.addLast(new RealDataHandler(channelThreadInfodDeque, NettyClientConnetion.this, busId, isDisconnectByManual));
					}

					// 有一个车号连接上后，就开启检查车号集合任务，每分钟检查一次
					if (checkBusSetTask == null)
					{
//						checkBusSetTask = taskService.scheduleAtFixedRate(() ->
						checkBusSetTask = group.scheduleAtFixedRate(() ->
						{
							if (busSet.isEmpty())
							{
								checkBusSetTask.cancel(true);
								group.shutdownGracefully();
								System.out.println("所有的数据包已发送完毕！运行时长：" + (startTimestamp > 0
										? NettyClientUtil.getFormatTime(System.currentTimeMillis() - startTimestamp) : "未知"));
//								ClientMain.countDownLatch.countDown();
							}
						} , 1, 1, TimeUnit.MINUTES);
					}

//					channel = futureListener.channel();

					// 保存当前连接
					// threadInfo.setChannel(channel);

					// System.out.println(
					// "[" + Thread.currentThread().getName() + "] [" +
					// df.format(new Date()) + "] 已连接至 Netty Server --> " + host
					// + ":" + port);
				}
				else
				{
					System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 连接失败，10秒后尝试重连。");

					// 连接失败次数
					connectionThreadInfo.setAndGetFailToConnectCount();
					// 断开10秒后进行重连
					futureListener.channel().eventLoop().schedule(new Runnable()
					{
						@Override
						public void run()
						{
							doConnect(busId, isDisconnectByManual);
						}
					}, 10, TimeUnit.SECONDS);
				}
			}
		});
		// }

	}

	// @Override
	// public void run()
	// {
	// //记录线程ID
	// threadInfo.setThreadID(Long.toString(Thread.currentThread().getId()));
	//
	// //记录线程开始时间
	// threadInfo.setStartTime(System.currentTimeMillis());
	// start();
	// }
}
