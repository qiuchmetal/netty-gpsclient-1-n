package com.test.nettytest.client.channelhandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.NettyClientCommand;
import com.test.nettytest.client.NettyClientInSingleConnetion;
import com.test.nettytest.client.NettyClientUtil;
import com.test.nettytest.client.ThreadInfoFile;
import com.test.nettytest.client.pojo.ThreadInfo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;

public class LoginHandler extends ChannelInboundHandlerAdapter
{
	private volatile ScheduledFuture<?> logInTask; //注册任务
	private volatile ScheduledFuture<?> disconnectTask; //断开任务
	private volatile ScheduledFuture<?> timingAndFixedDistanceTask; //定时定距任务
	private volatile ScheduledFuture<?> abnormalTask; //异常任务

	private volatile ScheduledFuture<?> threadInfoOutputTask; //记录线程的一些信息任务
	/**
	 * 记录当前线程的一些信息
	 */
	private ThreadInfo threadInfo;
	/**
	 * 指令生成类
	 */
	private NettyClientCommand clientCommand;
	/**
	 * 断开重连需要用到的
	 */
	private NettyClientInSingleConnetion client;

	public LoginHandler(ThreadInfo threadInfo, NettyClientCommand clientCommand, NettyClientInSingleConnetion client)
	{
		this.threadInfo = threadInfo;
		this.clientCommand = clientCommand;
		this.client = client;
	}
	
	public LoginHandler(ThreadInfo threadInfo, NettyClientInSingleConnetion client)
	{
		this.threadInfo = threadInfo;
		this.client = client;
	}

	private SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	/**
	 * 发送注册信息
	 */
	private class LogInTask implements Runnable
	{
		private final ChannelHandlerContext ctx;

		public LogInTask(final ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			//			System.out.println("[" + df.format(new Date()) + "] 发送注册信息。。。。。。。");
			//			String sendMsg = "faf50010000c0001ff020000000000000000011e";
			try
			{
				ctx.writeAndFlush(Unpooled.copiedBuffer(clientCommand.getLoginBytes()));
				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送注册信息！-->>");
				threadInfo.setLoginPackageCount(threadInfo.getLoginPackageCount() + 1);
			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送注册信息失败。");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 断开连接
	 */
	private class DisconnectTask implements Runnable
	{
		private final ChannelHandlerContext ctx;

		public DisconnectTask(final ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			ctx.disconnect();
			System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] ！！已断开连接！！");
		}
	}

	/**
	 * 发送定时定距信息
	 */
	private class TimingAndFixedDistanceTask implements Runnable
	{
		private final ChannelHandlerContext ctx;

		public TimingAndFixedDistanceTask(final ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			try
			{
				ctx.writeAndFlush(Unpooled.copiedBuffer(clientCommand.getTimingAndFixedDistanceBytes()));
				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送定时定距信息！-->>");
				threadInfo.setTimingPackageCount(threadInfo.getTimingPackageCount() + 1);
			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送定时定距信息失败。");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 发送异常信息
	 */
	private class AbnormalTask implements Runnable
	{
		private int count;
		private final ChannelHandlerContext ctx;

		public AbnormalTask(final ChannelHandlerContext ctx)
		{
			this.count = 1;
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			try
			{
				if (count > 3)
				{
					ctx.disconnect();
					return;
				}

				ctx.writeAndFlush(Unpooled.copiedBuffer(clientCommand.getAbnormalBytes()));
				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送异常信息！-->> " + count);
				count++;
				threadInfo.setAbnormalPackageCount(threadInfo.getAbnormalPackageCount() + 1);
			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送异常信息失败。");
				e.printStackTrace();
			}
		}

	}

	/**
	 * 记录线程的一些信息
	 */
	private class ThreadInfoOutputTask implements Runnable
	{
		@Override
		public void run()
		{
			//线程当前统计时间
			threadInfo.setEndTime(System.currentTimeMillis());
			ThreadInfoFile.writeToTxtFile(threadInfo.toString());
			System.out.println("线程信息已写入文件。");
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		System.out.println("失去连接。");
		//断开次数
		threadInfo.setDisconnectionCount(threadInfo.getDisconnectionCount() + 1);
		
		if (disconnectTask != null)
		{
			disconnectTask.cancel(true);
			disconnectTask = null;
		}
		if (logInTask != null)
		{
			logInTask.cancel(true);
			logInTask = null;
		}
		if (timingAndFixedDistanceTask != null)
		{
			timingAndFixedDistanceTask.cancel(true);
			timingAndFixedDistanceTask = null;
		}
		if (abnormalTask != null)
		{
			abnormalTask.cancel(true);
			abnormalTask = null;
		}
		
//		client.disconnectionCount++;
		//断开重连
		client.doConnect();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		this.clientCommand = new NettyClientCommand();
		threadInfo.setChannel(ctx.channel());
		
		logInTask = ctx.executor().scheduleWithFixedDelay(new LogInTask(ctx), 0, NettyClientUtil.LOGIN_INTERVAL, TimeUnit.SECONDS);

		//开启记录线程的一些信息的调度任务
//		if (threadInfoOutputTask == null)
//			threadInfoOutputTask = ctx.executor().scheduleWithFixedDelay(new ThreadInfoOutputTask(), 30, 30, TimeUnit.SECONDS);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		ByteBuf in = (ByteBuf) msg;
		byte[] bytes = new byte[in.readableBytes()];
		in.readBytes(in.readableBytes()).readBytes(bytes);

		//		System.out.println(Thread.currentThread().getName() + "接收到的信息：");
		//		for (byte b : bytes)
		//			System.out.print(b + ",");

		//		System.out.println(ByteUtil.bin2HexStr(bytes));

		//接收到了注册应答信息
		if (bytes.length > 8 && bytes[7] == -96 && logInTask != null)
		{
			logInTask.cancel(true);
			System.out.println("已注册成功。");
			//一段时间后没有收到心跳，就断开连接
			disconnectTask = ctx.executor().scheduleWithFixedDelay(new DisconnectTask(ctx), NettyClientUtil.HEARTBEAT_TIMEOUT,
					NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);

			//发送异常信息
			abnormalTask = ctx.executor().scheduleWithFixedDelay(new AbnormalTask(ctx), NettyClientUtil.ABNORMAL_INTERVAL,
					NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);

			//发送定时定距消息
			timingAndFixedDistanceTask = ctx.executor().scheduleAtFixedRate(new TimingAndFixedDistanceTask(ctx),
					NettyClientUtil.TIMING_INTERVAL, NettyClientUtil.TIMING_INTERVAL, TimeUnit.SECONDS);
		}
		//收到了心跳指令
		else if (bytes.length > 8 && bytes[7] == 1)
		{
			System.out.println(Thread.currentThread().getName() + "<<-- 接收到了心跳消息！！！");
			threadInfo.setHeartBeatPackageCount(threadInfo.getHeartBeatPackageCount() + 1);
			//把之前的断开任务取消掉
			if (disconnectTask != null)
			{
				disconnectTask.cancel(true);
				disconnectTask = null;
			}
			//重新开启一个断开任务
			disconnectTask = ctx.executor().scheduleWithFixedDelay(new DisconnectTask(ctx), NettyClientUtil.HEARTBEAT_TIMEOUT,
					NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
		}
		//收到了异常应答
		else if (bytes.length > 8 && bytes[7] == -63)
		{
			System.out.println(Thread.currentThread().getName() + "<<-- 收到了异常应答！！！");
			threadInfo.setAbnormalResponsePackageCount(threadInfo.getAbnormalResponsePackageCount() + 1);
			//把之前的异常任务取消掉
			if (abnormalTask != null)
			{
				abnormalTask.cancel(true);
				abnormalTask = null;
			}
			//重新开启一个断开任务
			abnormalTask = ctx.executor().scheduleWithFixedDelay(new AbnormalTask(ctx), NettyClientUtil.ABNORMAL_INTERVAL,
					NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		ctx.close();
	}

}
