package com.test.nettytest.client.channelhandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.NettyClientConnetion;
import com.test.nettytest.client.pojo.ChannelThreadInfo;
import com.test.nettytest.client.pojo.GPSDataLineFromAFile;
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

public class RealDataHandler extends ChannelInboundHandlerAdapter
{
	private volatile ScheduledFuture<?> logInTask; // 注册任务
	private volatile ScheduledFuture<?> disconnectWithoutHeartBeatTask; // 没有收到心跳的断开任务
	private volatile ScheduledFuture<?> disconnectInRandomTimeTask; // 模拟设备的不稳定情况
	private volatile ScheduledFuture<?> timingAndFixedDistanceTask; // 定时定距任务
	private volatile ScheduledFuture<?> abnormalTask; // 异常任务

	private volatile ScheduledFuture<?> sendGpsDataTask; // 发送 GPS 数据任务

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
	/**
	 * 当前管道加载的车号信息
	 */
	private Queue<GPSDataLineFromAFile> gpsDataQueue;
	/**
	 * 保留当前最近的一个已发送的 GPS 数据包
	 */
	private GPSDataLineFromAFile lastSendGpsDataLine;

	public RealDataHandler(ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque, NettyClientConnetion client)
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
		private GPSDataLineFromAFile gpsDataLine;

		public LogInTask(final ChannelHandlerContext ctx, GPSDataLineFromAFile gpsDataLine)
		{
			this.ctx = ctx;
			this.gpsDataLine = gpsDataLine;
		}

		@Override
		public void run()
		{
			try
			{
//				ctx.writeAndFlush(Unpooled.copiedBuffer(clientCommand.getLoginBytes()));
				ctx.writeAndFlush(Unpooled.copiedBuffer(gpsDataLine.getGpsDataBytesByCurrentTime(System.currentTimeMillis())));
				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送注册信息！-->>");
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
			// System.out.println("[" + Thread.currentThread().getName() + "] ["
			// + df.format(new Date()) + "] 创建了心跳检测。");
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
			// disconnectInRandomTimeTask =
			// ctx.executor().scheduleWithFixedDelay(new
			// DisconnectInRandomTimeTask(ctx),
			// (long) (NettyClientUtil.DISCONNECT_RANGE * 60 * (Math.random() *
			// 0.9 + 0.1)), NettyClientUtil.DISCONNECT_RANGE,
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
					// timingAndFixedDistanceTask =
					// ctx.executor().scheduleWithFixedDelay(new
					// TimingAndFixedDistanceTask(ctx),
					// (long) (NettyClientUtil.TIMING_INTERVAL * (Math.random()
					// * 0.9 + 0.1)), NettyClientUtil.TIMING_INTERVAL,
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
	 * 发送异常信息
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
				// System.out.println("[" + Thread.currentThread().getName() +
				// "] [" + df.format(new Date()) + "] 发送异常信息！-->> " + count);
				count++;
				channelThreadInfo.incrementAbnormalPackageCount();
				;
			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送异常信息失败。");
				e.printStackTrace();
			}
		}

	}

	/**
	 * 发送GPS 数据任务
	 */
	private class SendGpsDataTask implements Runnable
	{
		private final ChannelHandlerContext ctx;
		private byte[] gpsDataLineBytes;

		public SendGpsDataTask(final ChannelHandlerContext ctx, byte[] gpsDataLineBytes)
		{
			this.ctx = ctx;
			this.gpsDataLineBytes = gpsDataLineBytes;
		}

		@Override
		public void run()
		{
			try
			{
				ctx.writeAndFlush(Unpooled.copiedBuffer(gpsDataLineBytes));
				// System.out.println("[" + Thread.currentThread().getName() +
				// "] [" + df.format(new Date()) + "] 发送异常信息！-->> " + count);

				// FIXME
				// 准备发送下一个包
				GPSDataLineFromAFile gpsDataLine = gpsDataQueue.poll();
				// 1、计算两包之间的 GPS 时间间隔
				long gpsInterval = gpsDataLine.getGpsOriginalTimestamp() - lastSendGpsDataLine.getGpsOriginalTimestamp();
				gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
				// 2、计算两包之间的发送间隔
				long sendInterval = (gpsDataLine.getReceiveTimestamp() - lastSendGpsDataLine.getReceiveTimestamp()) / 1000;
				sendInterval = sendInterval < 0 ? 0 : sendInterval;
				// 3、以上一包的 GPS 时间，加上 GPS 时间间隔，作为将要发送的包的 GPS 时间，并在 sendInterval
				// 时间间隔之后发送出去
				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 即将发送 -->> ");
				sendGpsDataTask = client.taskService
						.schedule(
								new SendGpsDataTask(ctx,
										gpsDataLine
												.getGpsDataBytesByCurrentTime(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval)),
								sendInterval, TimeUnit.SECONDS);
				//再保留当前已发送的包
				lastSendGpsDataLine = gpsDataLine;
			}
			catch (Exception e)
			{
				System.out.println("[" + df.format(new Date()) + "] 发送 GPS 数据失败。");
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
		// 从所有的车号队列里抽取一条出来，一个通道只负责一个车号
		gpsDataQueue = client.busDeque.poll();
		// 取出第一条指令，注册指令，修改里面的GPS时间
		GPSDataLineFromAFile gpsDataLine = gpsDataQueue.poll();

		// 立即注册
		// logInTask = ctx.executor().scheduleWithFixedDelay(new
		// LogInTask(ctx),0, NettyClientUtil.LOGIN_INTERVAL,TimeUnit.SECONDS);
		logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx, gpsDataLine), 0, NettyClientUtil.LOGIN_INTERVAL,
				TimeUnit.SECONDS);

		// 在设定的一个时间段内的一个随机时间点进行注册
		// logInTask = ctx.executor().scheduleWithFixedDelay(new LogInTask(ctx),
		// (long) (NettyClientUtil.LOGIN_TIMEOUT * 60 * (Math.random() * 0.9 +
		// 0.1)), NettyClientUtil.LOGIN_INTERVAL,
		// TimeUnit.SECONDS);

		// 模拟设备因为不稳定而断开连接（一次性任务）
		if (NettyClientUtil.DISCONNECT_RANGE > 0)
			// disconnectInRandomTimeTask =
			// ctx.executor().scheduleWithFixedDelay(new
			// DisconnectInRandomTimeTask(ctx),
			// (long) (NettyClientUtil.DISCONNECT_RANGE * 60 * (Math.random() *
			// 0.9 + 0.1)), NettyClientUtil.DISCONNECT_RANGE,
			// TimeUnit.SECONDS);
			disconnectInRandomTimeTask = client.taskService.schedule(new DisconnectInRandomTimeTask(ctx),
					(long) (NettyClientUtil.DISCONNECT_RANGE * 60 * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);

		// 开启记录线程的一些信息的调度任务
		// if (threadInfoOutputTask == null)
		// threadInfoOutputTask = ctx.executor().scheduleWithFixedDelay(new
		// ThreadInfoOutputTask(), 30, 30, TimeUnit.SECONDS);
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

//				System.out.println(Thread.currentThread().getName() + "接收到的信息：");
		// for (byte b : bytes)
		// System.out.print(b + ",");

//						System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 接收到的信息："+ByteBufUtil.hexDump(bytes));

		// 接收到了注册应答信息
		if (bytes.length > 8 && bytes[7] == -96 && logInTask != null)
		{
			logInTask.cancel(true);
			isLogin = true;
			// 收到的注册应答包个数
			channelThreadInfo.incrementLoginPackageReceivedCount();
			System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 已注册成功。");

			// 一段时间后没有收到心跳，就断开连接
			disconnectWithoutHeartBeatTask = client.taskService.schedule(new DisconnectWithoutHeartBeatTask(ctx),
					NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);

			// FIXME
			// 收到注册应答后，取出第二条要发送的数据
			GPSDataLineFromAFile gpsDataLine = gpsDataQueue.poll();
			// 保留当前已经 poll 出来的包
			lastSendGpsDataLine = gpsDataLine;
			// 计算发送与采集时间的间隔（秒）
			long interval = (gpsDataLine.getReceiveTimestamp() - gpsDataLine.getGpsOriginalTimestamp()) / 1000;
			interval = interval < 0 ? 0 : interval;
			// 以当前时间更改第二条 GPS 数据
			sendGpsDataTask = client.taskService.schedule(
					new SendGpsDataTask(ctx, gpsDataLine.getGpsDataBytesByCurrentTime(System.currentTimeMillis())), interval,
					TimeUnit.SECONDS);

//			//发送异常信息
//			long initialDelay = (long) (NettyClientUtil.ABNORMAL_INTERVAL * (Math.random() * 0.9 + 0.1));
//			abnormalTask = client.taskService.scheduleWithFixedDelay(
//					new AbnormalTask(ctx, initialDelay, NettyClientUtil.ABNORMAL_INTERVAL), initialDelay,
//					NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
//
//			//发送定时定距消息
//			if (NettyClientUtil.TIMING_INTERVAL_FIXED == 1) //按固定的时间间隔进行发包
//				timingAndFixedDistanceTask = client.taskService.scheduleAtFixedRate(new TimingAndFixedDistanceTask(ctx),
//						NettyClientUtil.TIMING_INTERVAL, NettyClientUtil.TIMING_INTERVAL, TimeUnit.SECONDS);
//			else //发包时间间隔不固定
//				timingAndFixedDistanceTask = client.taskService.schedule(new TimingAndFixedDistanceTask(ctx),
//						(long) (NettyClientUtil.TIMING_INTERVAL * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);

		}
		// 收到了心跳指令
		else if (isLogin && bytes.length > 8 && bytes[7] == 1)
		{
			// System.out.println("[" + Thread.currentThread().getName() + "] ["
			// + df.format(new Date()) + "] <<-- 接收到了心跳消息！！！");
			channelThreadInfo.incrementHeartBeatPackageCount();
			// 把之前的断开任务取消掉
			if (disconnectWithoutHeartBeatTask != null)
			{
				disconnectWithoutHeartBeatTask.cancel(true);
				disconnectWithoutHeartBeatTask = null;
			}
			// 重新开启一个断开任务
			disconnectWithoutHeartBeatTask = client.taskService.schedule(new DisconnectWithoutHeartBeatTask(ctx),
					NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
		}
		// 收到了异常应答
//		else if (isLogin && bytes.length > 8 && bytes[7] == -63)
//		{
//			//System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] <<-- 收到了异常应答 <<--");
//			channelThreadInfo.incrementAbnormalResponsePackageCount();
//			//把之前的异常任务取消掉
//			if (abnormalTask != null)
//			{
//				abnormalTask.cancel(true);
//				abnormalTask = null;
//			}
//			//重新开启一个异常任务
//			//			abnormalTask = ctx.executor().scheduleWithFixedDelay(new AbnormalTask(ctx), NettyClientUtil.ABNORMAL_INTERVAL,
//			//					NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
//			long initialDelay = (long) (NettyClientUtil.ABNORMAL_INTERVAL * (Math.random() * 0.9 + 0.1));
//			abnormalTask = client.taskService.scheduleWithFixedDelay(
//					new AbnormalTask(ctx, initialDelay, NettyClientUtil.ABNORMAL_INTERVAL), initialDelay,
//					NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
//		}
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
