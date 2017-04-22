package com.test.nettytest.client;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.channelhandler.LoginHandler;
import com.test.nettytest.client.pojo.ThreadInfo;
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
	 * 当前连接线程的一些信息
	 */
	private ThreadInfo threadInfo;
	/**
	 * 线程统计信息列表
	 */
	//private List<ThreadInfo> threadInfoList;
	/**
	 * 指令生成类
	 */
	//	private NettyClientCommand clientCommand;

	//	private Channel channel;
	private Bootstrap bootstrap;

	private final int CONNECTION_COUNT = NettyClientUtil.PER_THREAD_CONNETIONS; //需要保持的连接数
	//	public int disconnectionCount; //未连接的连接数
	//	public int connecting; //正在进行连接的数量

	public NettyClientConnetion(List<ThreadInfo> threadInfoList)
	{
		//		this.disconnectionCount = this.CONNECTION_COUNT;
		//		this.connecting = 0;
		this.threadInfo = new ThreadInfo();
		//记录线程开始时间
		this.threadInfo.setStartTime(System.currentTimeMillis());
		//		this.clientCommand = new NettyClientCommand();
		//把当前创建的线程加入列表
		threadInfoList.add(threadInfo);
	}

	public void start()
	{
		EventLoopGroup group = new NioEventLoopGroup(1);
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
				//				ch.pipeline().addLast(new LoginHandler(threadInfo, clientCommand, NettyClientInSingleConnetion.this));
				ch.pipeline().addLast(new LoginHandler(threadInfo, NettyClientConnetion.this));
			}
		});

		//记录线程ID
		threadInfo.setThreadID(Thread.currentThread().getName());

		//		//记录线程开始时间
		//		threadInfo.setStartTime(System.currentTimeMillis());

		for (int i = 0; i < CONNECTION_COUNT; i++)
			doConnect();

		//		ChannelFuture f = null;
		//
		//		while (true)
		//		{
		//			try
		//			{
		//				f = b.connect(host, port).sync();
		//				//连接次数
		//				threadInfo.setConnectionCount(threadInfo.getConnectionCount() + 1);
		//				if (null!=f && f.isSuccess())
		//				{
		//					System.out.println("[" + Thread.currentThread().getName() + "] 已连接至Netty Server --> " + host + ":" + port);
		//					f.channel().closeFuture().sync();
		//					System.out.println("已断开连接");
		//					//断开次数
		//					threadInfo.setDisconnectionCount(threadInfo.getDisconnectionCount() + 1);
		//				}
		//				
		//				//断开10秒后进行重连
		//				TimeUnit.SECONDS.sleep(10);
		//				System.out.println("现尝试重连。");
		//			}
		//			catch (InterruptedException e)
		//			{
		//				e.printStackTrace();
		//			}
		//		}

		//		finally
		//		{
		//			group.shutdownGracefully();
		//			System.out.println("已优雅退出。");
		//		}
	}

	public void doConnect()
	{
		String host = NettyClientUtil.SERVER_IP;
		int port = NettyClientUtil.SERVER_PORT;

		//尝试连接次数
		threadInfo.setTryToConnectCount(threadInfo.getTryToConnectCount() + 1);

		System.out.println("[" + Thread.currentThread().getName() + "] 准备连接 Netty Server --> " + host + ":" + port);

		ChannelFuture future = bootstrap.connect(host, port);

		future.addListener(new ChannelFutureListener()
		{
			public void operationComplete(ChannelFuture futureListener) throws Exception
			{
				if (futureListener.isSuccess())
				{
					//成功连接次数
					threadInfo.setConnectionCount(threadInfo.getConnectionCount() + 1);

					//					channel = futureListener.channel();

					//					if(disconnectionCount>1) 
					//						disconnectionCount--;

					//保存当前连接
					//					threadInfo.setChannel(channel);

					System.out.println(
							"[" + Thread.currentThread().getName() + "] 已连接至 Netty Server --> " + host + ":" + port);

					//					System.out.println("已断开连接");
					//					//断开次数
					//					threadInfo.setDisconnectionCount(threadInfo.getDisconnectionCount() + 1);
				}
				else
				{
					System.out.println("连接失败，10秒后尝试重连。");

					//连接失败次数
					threadInfo.setFailToConnectCount(threadInfo.getFailToConnectCount() + 1);
					//断开10秒后进行重连
					futureListener.channel().eventLoop().schedule(new Runnable()
					{
						@Override
						public void run()
						{
							doConnect();
						}
					}, 10, TimeUnit.SECONDS);
				}
			}
		});
		//		}

	}

	//	@Override
	//	public void run()
	//	{
	//		//记录线程ID
	//		threadInfo.setThreadID(Long.toString(Thread.currentThread().getId()));
	//		
	//		//记录线程开始时间
	//		threadInfo.setStartTime(System.currentTimeMillis());
	//		start();
	//	}
}
