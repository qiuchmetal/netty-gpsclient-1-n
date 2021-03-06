package com.test.nettytest.client.channelhandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.NettyClientConnetion;
import com.test.nettytest.client.pojo.ChannelThreadInfo;
import com.test.nettytest.client.pojo.NettyClientCommand;
import com.test.nettytest.client.util.ChannelThreadInfoFile;
import com.test.nettytest.client.util.NettyClientUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;

public class LoginHandler extends ChannelInboundHandlerAdapter
{
	private volatile ScheduledFuture<?> logInTask; // 注册任务
	private volatile ScheduledFuture<?> disconnectWithoutHeartBeatTask; // 没有收到心跳的断开任务
	private volatile ScheduledFuture<?> disconnectInRandomTimeTask; // 模拟设备的不稳定情况
	private volatile ScheduledFuture<?> timingAndFixedDistanceTask; // 定时定距任务
	private volatile ScheduledFuture<?> abnormalTask; // 异常任务（开始）
	private volatile ScheduledFuture<?> abnormalEndTask; // 异常任务（结束）

	private volatile ScheduledFuture<?> threadInfoOutputTask; // 记录线程的一些信息任务
	/**
	 * 管道线程集合
	 */
	private ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque;
	/**
	 * 记录当前线程的一些信息
	 */
	private ChannelThreadInfo channelThreadInfo;
	/**
	 * 指令生成类
	 */
	private NettyClientCommand clientCommand;
	/**
	 * 断开重连需要用到的
	 */
	private NettyClientConnetion client;
	/**
	 * 是否已经注册成功
	 */
	private boolean isLogin = false;
	/**
	 * 记录因为没有及时收到异常应答而断开的信息
	 */
	private String abnormalMsg;
	/**
	 * 当前 channel 激活时间
	 */
	private long startTime = 0;
	/**
	 * 当前 channel 关闭时间
	 */
	private long endTime;
	/**
	 * 发起异常了之后，就要把这段时间内接收到的数据包记录下来
	 */
	private boolean isAbnomalTime = false;
	/**
	 * 专门用来记录异常信息包的列表
	 */
	private List<String> abnomalPackageList = new ArrayList<String>();

	public LoginHandler(ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque, NettyClientConnetion client)
	{
		this.channelThreadInfodDeque = channelThreadInfodDeque;
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
			try
			{
				ctx.writeAndFlush(Unpooled.copiedBuffer(clientCommand.getLoginBytes()));
				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送注册信息！-->> "
						+ clientCommand.getSendString());
				// 发送的注册包个数
				channelThreadInfo.incrementLoginPackageSendCount();
			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送注册信息失败。");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 没有收到心跳的断开任务
	 */
	private class DisconnectWithoutHeartBeatTask implements Runnable
	{
		private final ChannelHandlerContext ctx;

		public DisconnectWithoutHeartBeatTask(final ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
			// System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 创建了心跳检测。");
		}

		@Override
		public void run()
		{
			ctx.disconnect();
			// 因为没有接收到心跳而断开次数
			channelThreadInfo.setDisconnectionOfHeartBeatCount(channelThreadInfo.getDisconnectionOfHeartBeatCount() + 1);
			System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] [" + clientCommand.getBusId()
					+ "] 因为没有及时收到心跳而断开连接！！运行时长："
					+ (startTime > 0 ? NettyClientUtil.getFormatTime(System.currentTimeMillis() - startTime) : "未知"));
		}
	}

	/**
	 * 模拟设备的不稳定情况
	 */
	private class DisconnectInRandomTimeTask implements Runnable
	{
		private final ChannelHandlerContext ctx;

		public DisconnectInRandomTimeTask(final ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			ctx.disconnect();
			channelThreadInfo.setDisconnectInRandomTimeCount(channelThreadInfo.getDisconnectInRandomTimeCount() + 1);
			System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] [" + clientCommand.getBusId()
					+ "] 模拟设备不稳定而断开连接！！运行时长："
					+ (startTime > 0 ? NettyClientUtil.getFormatTime(System.currentTimeMillis() - startTime) : "未知"));

			// 取消之前的任务
			if (disconnectInRandomTimeTask != null)
			{
				disconnectInRandomTimeTask.cancel(true);
				disconnectInRandomTimeTask = null;
			}
			// 开启一个新任务
			// disconnectInRandomTimeTask = ctx.executor().scheduleWithFixedDelay(new DisconnectInRandomTimeTask(ctx),
			// (long) (NettyClientUtil.DISCONNECT_RANGE * 60 * (Math.random() * 0.9 + 0.1)), NettyClientUtil.DISCONNECT_RANGE,
			// TimeUnit.SECONDS);
			disconnectInRandomTimeTask = client.taskService.schedule(new DisconnectInRandomTimeTask(ctx),
					(long) (NettyClientUtil.DISCONNECT_RANGE * 60 * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);
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

				channelThreadInfo.incrementTimingPackageCount();
				;

				if (NettyClientUtil.TIMING_INTERVAL_FIXED != 1) // 发包时间不固定
				{
//					System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date())
//							+ "] 不固定时间间隔发送定时定距信息！-->>");

					// 取消之前的任务
					if (timingAndFixedDistanceTask != null)
					{
						timingAndFixedDistanceTask.cancel(true);
						timingAndFixedDistanceTask = null;
					}
					// 开启一个新任务
					// timingAndFixedDistanceTask = ctx.executor().scheduleWithFixedDelay(new TimingAndFixedDistanceTask(ctx),
					// (long) (NettyClientUtil.TIMING_INTERVAL * (Math.random() * 0.9 + 0.1)), NettyClientUtil.TIMING_INTERVAL,
					// TimeUnit.SECONDS);
					timingAndFixedDistanceTask = client.taskService.schedule(new TimingAndFixedDistanceTask(ctx),
							(long) (NettyClientUtil.TIMING_INTERVAL * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);
				}
				else
				{
//					System.out.println(
//							"[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送定时定距信息！-->>");
				}

			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送定时定距信息失败。");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 发送异常开始信息
	 */
	private class AbnormalTask implements Runnable
	{
		private int count;
		private final ChannelHandlerContext ctx;

		private final long initialDelay; // 主要用于记录
		private final long delay; // 主要用于记录
		private final Date date = new Date();

		public AbnormalTask(final ChannelHandlerContext ctx, long initialDelay, long delay)
		{
			this.count = 1;
			this.ctx = ctx;
			this.initialDelay = initialDelay;
			this.delay = delay;
		}

		@Override
		public void run()
		{
			try
			{
				if (count == 1)
				{
					isAbnomalTime = true;
					abnomalPackageList.clear();
				}

				if (count > 3)
				{
					channelThreadInfo.setDisconnectionOfAbnormalCount(channelThreadInfo.getDisconnectionOfAbnormalCount() + 1);

					abnormalMsg = "异常开启时间: [" + df.format(date.getTime() + (long) (initialDelay * 1000)) + "] 超时时间： ["
							+ df.format(date.getTime() + (long) ((initialDelay + delay * 3) * 1000)) + "]";
					System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] ["
							+ clientCommand.getBusId() + "] " + abnormalMsg + " ！！因为没有及时收到异常应答而断开连接！！运行时长："
							+ (startTime > 0 ? NettyClientUtil.getFormatTime(System.currentTimeMillis() - startTime) : "未知") + "。期间接收到的包："
							+ abnomalPackageList.toString());
					abnomalPackageList.clear();
					isAbnomalTime = false;
					ctx.disconnect();
					return;
				}

				// 发送前，随机的 sleep 100ms 以内的一个时间
				// try
				// {
				// TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 100));
				// }
				// catch (InterruptedException e)
				// {
				// e.printStackTrace();
				// }

				ctx.writeAndFlush(Unpooled.copiedBuffer(clientCommand.getAbnormalBytes()));
//				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送异常信息！-->> " + count);
				System.out.println("[" + clientCommand.getBusId() + "] [" + df.format(new Date()) + "] 发送异常开始信息！-->> "
						+ clientCommand.getSendString());
				count++;
				channelThreadInfo.incrementAbnormalPackageCount();

				// 过2秒发送异常结束信息，一次性任务
				abnormalEndTask = client.taskService.schedule(new AbnormalEndTask(ctx), 2, TimeUnit.SECONDS);
			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送异常开始信息失败。");
				e.printStackTrace();
			}
		}

	}

	/**
	 * 发送异常信息
	 */
	private class AbnormalEndTask implements Runnable
	{
		private final ChannelHandlerContext ctx;

		public AbnormalEndTask(final ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			try
			{
				ctx.writeAndFlush(Unpooled.copiedBuffer(clientCommand.getAbnormalEndBytes()));
//				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送异常信息！-->> " + count);
				System.out.println("[" + clientCommand.getBusId() + "] [" + df.format(new Date()) + "] 发送异常结束信息！-->> "
						+ clientCommand.getSendString());
				channelThreadInfo.incrementAbnormalPackageCount();
			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送异常结束信息失败。");
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
			// 线程当前统计时间
			channelThreadInfo.setEndTime(System.currentTimeMillis());
			ChannelThreadInfoFile.writeToTxtFile(channelThreadInfo.toString());
			// System.out.println("线程信息已写入文件。");
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		long endTime = System.currentTimeMillis();
		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] [" + clientCommand.getBusId()
				+ "] 失去连接。运行时长：" + (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知"));
		// 断开次数
		channelThreadInfo.setDisconnectionCount(channelThreadInfo.getDisconnectionCount() + 1);
		// 断开时间
		channelThreadInfo.setEndTime(endTime);

		if (disconnectWithoutHeartBeatTask != null)
		{
			disconnectWithoutHeartBeatTask.cancel(true);
			disconnectWithoutHeartBeatTask = null;
		}
		if (disconnectInRandomTimeTask != null)
		{
			disconnectInRandomTimeTask.cancel(true);
			disconnectInRandomTimeTask = null;
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
		if (abnormalEndTask != null)
		{
			abnormalEndTask.cancel(true);
			abnormalEndTask = null;
		}

		// client.disconnectionCount++;
		// 断开重连
		client.doConnect();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		this.clientCommand = new NettyClientCommand();
		this.channelThreadInfo = new ChannelThreadInfo();
		channelThreadInfodDeque.add(channelThreadInfo);

		this.startTime = System.currentTimeMillis();

//		String currentThreadName = Thread.currentThread().getName();

//		System.out.println("当前管道连接线程：" + currentThreadName);
		// 记录端口号，
		channelThreadInfo.setThreadID(((SocketChannel) ctx.channel()).localAddress().getPort() + "");
		// 记录当前通道
		channelThreadInfo.setChannel(ctx.channel());
		// 记录连接时间
		channelThreadInfo.setStartTime(startTime);

		// 立即注册
		// logInTask = ctx.executor().scheduleWithFixedDelay(new LogInTask(ctx),0, NettyClientUtil.LOGIN_INTERVAL,TimeUnit.SECONDS);
		logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, NettyClientUtil.LOGIN_INTERVAL, TimeUnit.SECONDS);

		// 在设定的一个时间段内的一个随机时间点进行注册
		// logInTask = ctx.executor().scheduleWithFixedDelay(new LogInTask(ctx),
		// (long) (NettyClientUtil.LOGIN_TIMEOUT * 60 * (Math.random() * 0.9 + 0.1)), NettyClientUtil.LOGIN_INTERVAL,
		// TimeUnit.SECONDS);

		// 模拟设备因为不稳定而断开连接（一次性任务）
		if (NettyClientUtil.DISCONNECT_RANGE > 0)
			// disconnectInRandomTimeTask = ctx.executor().scheduleWithFixedDelay(new DisconnectInRandomTimeTask(ctx),
			// (long) (NettyClientUtil.DISCONNECT_RANGE * 60 * (Math.random() * 0.9 + 0.1)), NettyClientUtil.DISCONNECT_RANGE,
			// TimeUnit.SECONDS);
			disconnectInRandomTimeTask = client.taskService.schedule(new DisconnectInRandomTimeTask(ctx),
					(long) (NettyClientUtil.DISCONNECT_RANGE * 60 * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);

		// 开启记录线程的一些信息的调度任务
		// if (threadInfoOutputTask == null)
		// threadInfoOutputTask = ctx.executor().scheduleWithFixedDelay(new ThreadInfoOutputTask(), 30, 30, TimeUnit.SECONDS);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		// ByteBuf in = (ByteBuf) msg;
		// ByteBuf in = Unpooled.copiedBuffer((ByteBuf) msg);
		// byte[] bytes = new byte[in.readableBytes()];
		// in.readBytes(in.readableBytes()).readBytes(bytes);
		// ReferenceCountUtil.release(in);

		byte[] bytes = ByteBufUtil.getBytes((ByteBuf) msg);
		ReferenceCountUtil.release(msg);

		// 发送了异常包后，需要在超时前，记录所有接收到的包
		if (isAbnomalTime)
			abnomalPackageList.add(ByteBufUtil.hexDump(bytes));

		// System.out.println(Thread.currentThread().getName() + "接收到的信息：");
		// for (byte b : bytes)
		// System.out.print(b + ",");

		// System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 接收到的信息："+ByteBufUtil.hexDump(bytes));

		// 接收到了注册应答信息
		if (bytes.length > 8 && bytes[7] == -96 && logInTask != null)
		{
			logInTask.cancel(true);
			isLogin = true;
			// 收到的注册应答包个数
			channelThreadInfo.incrementLoginPackageReceivedCount();
//			System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 已注册成功。");
			System.out.println("[" + clientCommand.getBusId() + "] [" + df.format(new Date()) + "] 已注册成功。");

			// 一段时间后没有收到心跳，就断开连接
			// disconnectWithoutHeartBeatTask = ctx.executor().scheduleWithFixedDelay(new DisconnectWithoutHeartBeatTask(ctx),
			// NettyClientUtil.HEARTBEAT_TIMEOUT, NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
			disconnectWithoutHeartBeatTask = client.taskService.schedule(new DisconnectWithoutHeartBeatTask(ctx),
					NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);

			// 发送异常信息
			// abnormalTask = ctx.executor().scheduleWithFixedDelay(new AbnormalTask(ctx), NettyClientUtil.ABNORMAL_INTERVAL,
			// NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
			long initialDelay = (long) (NettyClientUtil.ABNORMAL_INTERVAL * (Math.random() * 0.9 + 0.1));
//			abnormalTask = client.taskService.scheduleWithFixedDelay(new AbnormalTask(ctx, initialDelay, NettyClientUtil.ABNORMAL_INTERVAL),
//					initialDelay, NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
			// 为了发送上一条的异常结束指令，需要再延后2秒再发送新的异常开始指令
			abnormalTask = client.taskService.scheduleWithFixedDelay(
					new AbnormalTask(ctx, initialDelay + 2, NettyClientUtil.ABNORMAL_INTERVAL), initialDelay + 2,
					NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);

			// 发送定时定距消息
//			if (NettyClientUtil.TIMING_INTERVAL_FIXED == 1) //按固定的时间间隔进行发包
//				timingAndFixedDistanceTask = client.taskService.scheduleAtFixedRate(new TimingAndFixedDistanceTask(ctx),
//						NettyClientUtil.TIMING_INTERVAL, NettyClientUtil.TIMING_INTERVAL, TimeUnit.SECONDS);
//			else //发包时间间隔不固定
//					//				timingAndFixedDistanceTask = ctx.executor().scheduleWithFixedDelay(new TimingAndFixedDistanceTask(ctx),
//					//						(long) (NettyClientUtil.TIMING_INTERVAL * (Math.random() * 0.9 + 0.1)),
//					//						(long) (NettyClientUtil.TIMING_INTERVAL * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);
//				timingAndFixedDistanceTask = client.taskService.schedule(new TimingAndFixedDistanceTask(ctx),
//						(long) (NettyClientUtil.TIMING_INTERVAL * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);

		}
		// 收到了心跳指令
		else if (isLogin && bytes.length > 8 && bytes[7] == 1)
		{
			// System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] <<-- 接收到了心跳消息！！！");
			channelThreadInfo.incrementHeartBeatPackageCount();
			// 把之前的断开任务取消掉
			if (disconnectWithoutHeartBeatTask != null)
			{
				disconnectWithoutHeartBeatTask.cancel(true);
				disconnectWithoutHeartBeatTask = null;
			}
			// 重新开启一个断开任务
			// disconnectWithoutHeartBeatTask = ctx.executor().scheduleWithFixedDelay(new DisconnectWithoutHeartBeatTask(ctx),
			// NettyClientUtil.HEARTBEAT_TIMEOUT, NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
			disconnectWithoutHeartBeatTask = client.taskService.schedule(new DisconnectWithoutHeartBeatTask(ctx),
					NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
		}
		// 收到了异常应答
		else if (isLogin && bytes.length > 8 && bytes[7] == -63)
		{
//			System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] <<-- 收到了异常应答 <<--");
			System.out.println("[" + clientCommand.getBusId() + "] [" + df.format(new Date()) + "] <<-- 收到了异常应答 <<--");
			channelThreadInfo.incrementAbnormalResponsePackageCount();
			// 把之前的异常任务取消掉
			if (abnormalTask != null)
			{
				abnormalTask.cancel(true);
				abnormalTask = null;
			}
			// 重新开启一个异常任务
			// abnormalTask = ctx.executor().scheduleWithFixedDelay(new AbnormalTask(ctx), NettyClientUtil.ABNORMAL_INTERVAL,
			// NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
			long initialDelay = (long) (NettyClientUtil.ABNORMAL_INTERVAL * (Math.random() * 0.9 + 0.1));
//			abnormalTask = client.taskService.scheduleWithFixedDelay(new AbnormalTask(ctx, initialDelay, NettyClientUtil.ABNORMAL_INTERVAL),
//					initialDelay, NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
			// 为了发送上一条的异常结束指令，需要再延后2秒再发送新的异常开始指令
			abnormalTask = client.taskService.scheduleWithFixedDelay(
					new AbnormalTask(ctx, initialDelay + 2, NettyClientUtil.ABNORMAL_INTERVAL), initialDelay + 2,
					NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		super.exceptionCaught(ctx, cause);

		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] [" + clientCommand.getBusId()
				+ "] 因为异常而关闭连接。运行时长：" + (startTime > 0 ? NettyClientUtil.getFormatTime(System.currentTimeMillis() - startTime) : "未知"));

		if (ctx.channel().isActive())
			ctx.close();
	}
}
