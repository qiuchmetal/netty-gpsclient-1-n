package com.test.nettytest.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
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

import org.omg.CORBA.PRIVATE_MEMBER;

import com.test.nettytest.client.channelhandler.RealDataFromFileStreamHandler;
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
import io.netty.util.AttributeKey;

public class RealDataFromFileConnetion
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
	 * 保存每个车号之前发送的一个数据包，在断开重连时会用上
	 */
	public ConcurrentHashMap<String, GPSDataLineFromAFile> busSendGpsDataLineMap = new ConcurrentHashMap<String, GPSDataLineFromAFile>();
	/**
	 * 当前连接线程组，在子线程模拟断开重连时，需要用它来启用调度
	 */
	public EventLoopGroup group = new NioEventLoopGroup();
	/**
	 * 已连接的车号集合，用于检查数据包是否已经发完
	 */
	public Set<String> busSet = new ConcurrentSkipListSet<String>();
	/**
	 * 根据加载文件的目录，获取 File
	 */
	private File fileDir;

	private volatile ScheduledFuture<?> checkBusSetTask; // 检查车号集合是否为空

	private long startTimestamp; // 开始运行

	private static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	private Bootstrap bootstrap;

	// 执行 IO 之外的业务线程
	public ScheduledExecutorService taskService;
	
	/**
	 * 设置每个 channel 的属性的键
	 */
//	public AttributeKey<String> busIdKey = AttributeKey.valueOf("busId");
	public AttributeKey<Boolean> isDisconnectByManualKey = AttributeKey.valueOf("isDisconnectByManual");
	public AttributeKey<File> busFileKey = AttributeKey.valueOf("busFile");
	public AttributeKey<InputStreamReader> busReaderKey = AttributeKey.valueOf("busReader");
	public AttributeKey<BufferedReader> busBufferedReaderKey = AttributeKey.valueOf("busBufferedReader");
	
//	public AttributeKey<InputStreamReader> busReaderKey = AttributeKey.valueOf("busReader");
	
	
	
	public RealDataFromFileConnetion(ConnectionThreadInfo connectionThreadInfo
			, ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque
			, String filePath)
	{
		this.connectionThreadInfo = connectionThreadInfo;
		this.channelThreadInfodDeque = channelThreadInfodDeque;
		
		this.fileDir = new File(filePath);
		if (fileDir.isDirectory())
			this.busCount = fileDir.listFiles().length;
		
		// 连接线程开始时间
		this.connectionThreadInfo.setStartTime(System.currentTimeMillis());
		
//		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 即将开启的连接数：[" + busCount + "]");
		
		taskService = Executors.newScheduledThreadPool(busCount > 2 ? busCount / 2 : 1);
//		taskService = Executors.newScheduledThreadPool(busCount);
	}

	public void start()
	{
		if (busCount < 1)
		{
			System.out.println("无车号文件可加载。");
			return;
		}
		
		
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
				ch.pipeline().addLast(new RealDataFromFileStreamHandler(channelThreadInfodDeque, RealDataFromFileConnetion.this));
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
		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 即将开启的连接数：[" + busCount + "]");
		
		
		// 循环，对单个文件进行操作
		File[] busFiles = fileDir.listFiles();
		for (File oneBusFile : busFiles)
		{
//			if (busId != null && !busId.isEmpty())
//			taskService.schedule(() -> doConnect(busId, false), (long) (NettyClientUtil.LOGIN_TIMEOUT * 60 * (Math.random() * 0.9 + 0.1)),
//					TimeUnit.SECONDS);
//		group.schedule(() -> doConnect(busId, false), (long) (NettyClientUtil.LOGIN_TIMEOUT * 60 * (Math.random() * 0.9 + 0.1)),
//				TimeUnit.SECONDS);
			try
			{
				InputStreamReader reader = new InputStreamReader(new FileInputStream(oneBusFile)); // 使用默认字符集
				BufferedReader bufferedReader = new BufferedReader(reader);
				taskService.schedule(() -> doConnect(oneBusFile,reader,bufferedReader,false), 0, TimeUnit.SECONDS);
							
				//不 sleep 的话，加载老是出现一些空加载，很不稳定。
				try
				{
					TimeUnit.MILLISECONDS.sleep(1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
//				doConnect(busId, false);
		}

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

	public void doConnect(File oneBusFile,InputStreamReader reader,BufferedReader bufferedReader, boolean isDisconnectByManual)
	{
		class ConnectionFutureListener implements ChannelFutureListener
		{
			private File futureBusFile;
			private InputStreamReader futureReader;
			private BufferedReader futureBufferedReader;
			private boolean futureIsDisconnectByManual;
			
			public ConnectionFutureListener(File futureBusFile,InputStreamReader futureReader,BufferedReader futureBufferedReader, boolean futureIsDisconnectByManual)
			{
				this.futureBusFile = futureBusFile;
				this.futureReader = futureReader;
				this.futureBufferedReader = futureBufferedReader;
				this.futureIsDisconnectByManual = futureIsDisconnectByManual;
			}

			@Override
			public void operationComplete(ChannelFuture futureListener) throws Exception
			{
				if (futureListener.isSuccess())
				{
					// 成功连接次数
					connectionThreadInfo.setAndGetConnectionCount();

					if (futureBusFile != null)
					{
//						System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] " + busId + " 在主线程已连接成功。");
						
						//给一个已经连接上的 channel 赋予一个属性，让每个 channel 可以对应一个独有的车号						
						futureListener.channel().attr(busFileKey).setIfAbsent(futureBusFile);
						futureListener.channel().attr(busReaderKey).setIfAbsent(futureReader);
						futureListener.channel().attr(busBufferedReaderKey).setIfAbsent(futureBufferedReader);
						futureListener.channel().attr(isDisconnectByManualKey).setIfAbsent(futureIsDisconnectByManual);
					}

					// 有一个车号连接上后，就开启检查车号集合任务，每分钟检查一次
//					if (checkBusSetTask == null)
//					{
////						checkBusSetTask = taskService.scheduleAtFixedRate(() ->
//						checkBusSetTask = group.scheduleAtFixedRate(() ->
//						{
//							if (busSet.isEmpty())
//							{
//								checkBusSetTask.cancel(true);
//								group.shutdownGracefully();
//								System.out.println("所有的数据包已发送完毕！运行时长：" + (startTimestamp > 0
//										? NettyClientUtil.getFormatTime(System.currentTimeMillis() - startTimestamp) : "未知"));
////								ClientMain.countDownLatch.countDown();
//							}
//						} , 1, 1, TimeUnit.MINUTES);
//					}
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
							doConnect(futureBusFile,futureReader,futureBufferedReader, futureIsDisconnectByManual);
						}
					}, 10, TimeUnit.SECONDS);
				}
				
			}
			
		}
		
		String host = NettyClientUtil.SERVER_IP;
		int port = NettyClientUtil.SERVER_PORT;

//		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] " + busId + " 准备进行连接。");
		
//		try
//		{
//			TimeUnit.HOURS.sleep(1);
//		}
//		catch (InterruptedException e)
//		{
//			e.printStackTrace();
//		}

		// 尝试连接次数
		connectionThreadInfo.setAndGetTryToConnectCount();
		
		ChannelFuture future = bootstrap.connect(host, port);

		future.addListener(new ConnectionFutureListener(oneBusFile,reader,bufferedReader, isDisconnectByManual));
	}
}
