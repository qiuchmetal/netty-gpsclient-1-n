package com.test.nettytest.client.channelhandler;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.NettyClientConnetion;
import com.test.nettytest.client.pojo.ChannelThreadInfo;
import com.test.nettytest.client.pojo.GPSDataLineFromAFile;
import com.test.nettytest.client.pojo.NettyClientCommand;
import com.test.nettytest.client.util.NettyClientUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;

public class RealDataFromRAMHandler extends ChannelInboundHandlerAdapter
{
	private volatile ScheduledFuture<?> logInTask; // 注册任务
	private volatile ScheduledFuture<?> sendGpsDataTask; // 发送 GPS 数据任务

	/**
	 * 管道线程集合
	 */
	private ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque;
	/**
	 * 记录当前线程的一些信息
	 */
	private ChannelThreadInfo channelThreadInfo;
	/**
	 * 断开重连需要用到的
	 */
	private NettyClientConnetion client;
	/**
	 * 是否已经注册成功
	 */
	private boolean isLogin = false;
	/**
	 * 当前 channel 激活时间
	 */
	private long startTime = 0;
	/**
	 * 当前管道加载的车号信息
	 */
	private ArrayDeque<GPSDataLineFromAFile> gpsDataQueue;
	/**
	 * 保留当前最近的一个已发送的 GPS 数据包
	 */
	private GPSDataLineFromAFile lastSendGpsDataLine;
	/**
	 * 当前的车号
	 */
	private String busId;
	/**
	 * 是否是模拟断开
	 */
	private boolean isDisconnectByManual;

	private String diconnectionMessage;

//	public RealDataHandler(ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque, NettyClientConnetion client)
//	{
//		this.channelThreadInfodDeque = channelThreadInfodDeque;
//		this.client = client;
//	}

//	public RealDataHandler(ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque, NettyClientConnetion client, String busId,
//			boolean isDisconnectByManual)
//	{
//		this.channelThreadInfodDeque = channelThreadInfodDeque;
//		this.client = client;
//		this.busId = busId;
//		this.gpsDataQueue = client.busMap.get(busId);
//		this.isDisconnectByManual = isDisconnectByManual;
//	}
	public RealDataFromRAMHandler(ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque, NettyClientConnetion client)
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
//		private GPSDataLineFromAFile gpsDataLine; // 要发送的注册指令

//		public LogInTask(final ChannelHandlerContext ctx, GPSDataLineFromAFile gpsDataLine)
//		{
//			this.ctx = ctx;
//			this.gpsDataLine = gpsDataLine;
//		}
		public LogInTask(final ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			GPSDataLineFromAFile gpsDataLine = null;
			try
			{
				// FIXME
				gpsDataLine = gpsDataQueue.peekFirst();
				// 发送的注册包是指令包里的第一条指令，需要使用当前时间来初始化注册包里的 GPS 时间
				if (!isDisconnectByManual)
					gpsDataLine.updateGpsDataByTimestamp(System.currentTimeMillis());

				ctx.writeAndFlush(Unpooled.copiedBuffer(NettyClientUtil.hexStringToByteArray(gpsDataLine.getGpsData())));
//				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送注册信息：[" + gpsDataLine.getGpsData() + "]");
				gpsDataQueue.pollFirst();
				// 保存已发送的包
				lastSendGpsDataLine = gpsDataLine;
//				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送注册信息！-->>");

				// 发送的注册包个数
				channelThreadInfo.setPackageCount(gpsDataLine.getCommand());

				// 记录此车号发送指令的数量
				if (client.busSendCountMap.containsKey(busId))
					client.busSendCountMap.put(busId, client.busSendCountMap.get(busId) + 1);
				else
					client.busSendCountMap.put(busId, 1);
			}
			catch (Exception e)
			{
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送注册信息失败。" + gpsDataLine.toString());
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

		public SendGpsDataTask(final ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			GPSDataLineFromAFile gpsDataLine = null;
			try
			{
				// 确实要进行发送时，再拉取
				/*
				 * 现有逻辑：
				 * Queue先读顶部元素，发送指令，然后弹出，再预读下一个顶部元素，不弹出，但会修改gps时间的数据
				 * 换用buffer后的逻辑：
				 * 0. 增加一个线程变量（ lastSendGPSDataLine ） 记录前一个已发送的数据对象实体
				 * 1. buffer.readLine() 读取当前最新数据行
				 * 2. 做时间处理逻辑，原来在发送完成的预读后，提前到这里
				 * 2.5 发送GPS数据
				 * 3. buffer.mark() 标记预读起始位置
				 * 4. buffer.readLine() 预读一行，进行原来的时间差计算等逻辑
				 * 5. buffer.reset() 重置文件读取游标到预读前位置，等于回退了一行
				 * 
				 */
				gpsDataLine = gpsDataQueue.peekFirst();
				ctx.writeAndFlush(Unpooled.copiedBuffer(NettyClientUtil.hexStringToByteArray(gpsDataLine.getGpsData())));
//				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 现在发送 ：[" + lastSendGpsDataLine.getCommand() + "]["
//						+ lastSendGpsDataLine.getGpsData() + "]");
				gpsDataQueue.pollFirst();
//				if ("3".equals(gpsDataLine.getCommand()))
//					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送了校时请求 ：[" + gpsDataLine.getGpsData() + "]");

				// 保留当前已发送的包
				lastSendGpsDataLine = gpsDataLine;
				// System.out.println("[" + Thread.currentThread().getName() +
				// "] [" + df.format(new Date()) + "] 发送异常信息！-->> " + count);

				// 发送包的个数
				channelThreadInfo.setPackageCount(gpsDataLine.getCommand());

				// 记录此车号发送指令的数量
				if (client.busSendCountMap.containsKey(busId))
					client.busSendCountMap.put(busId, client.busSendCountMap.get(busId) + 1);
				else
					client.busSendCountMap.put(busId, 1);

				if (gpsDataQueue.isEmpty())
				{
//					System.out.println(
//							"[" + busId + "] [" + df.format(new Date()) + "] 所有的包已发完。发送了 " + client.busSendCountMap.get(busId) + " 条指令。");
					// 从已连接的车号集合里，把本车号移除
					client.busSet.remove(busId);
					// 假如发送的包不需要应答，则可以中断连接
//					ctx.disconnect();
					return;
				}

				// 预读下一个包
//				GPSDataLineFromAFile nextSendGpsDataLine = gpsDataQueue.poll();
				GPSDataLineFromAFile nextSendGpsDataLine = gpsDataQueue.peekFirst();

				// 1、计算两包之间的 GPS 时间间隔
				long gpsInterval = (nextSendGpsDataLine.getGpsOriginalTimestamp() - lastSendGpsDataLine.getGpsOriginalTimestamp());
				gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
				// 2、计算两包之间的发送间隔
				long sendInterval = (nextSendGpsDataLine.getReceiveTimestamp() - lastSendGpsDataLine.getReceiveTimestamp()) / 1000;
				sendInterval = sendInterval < 0 ? 0 : sendInterval;
				// 3、利用两包之间的 GPS 时间间隔，更新将要发包的 GPS 时间
				nextSendGpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);
				// FIXME
				// 这个包不是注册包
				if (!"32".equals(nextSendGpsDataLine.getCommand()))
				{
					sendGpsDataTask = client.taskService.schedule(new SendGpsDataTask(ctx), sendInterval, TimeUnit.SECONDS);
				}
				// 这个包是注册包
				else
				{
					// 把要发的包，重新加载到队列里
//					Queue<GPSDataLineFromAFile> gpsDataNewQueue = new LinkedList<GPSDataLineFromAFile>();
//					gpsDataNewQueue.add(nextSendGpsDataLine); // 首先加入注册包
//					gpsDataNewQueue.addAll(gpsDataQueue); // 再把剩下的包，都往后加
//					if (client.busMap == null)
//						client.busMap = new ConcurrentHashMap<String, Queue<GPSDataLineFromAFile>>();
//					client.busMap.put(busId, gpsDataNewQueue);// 把这个车号的队列，增加到断开重连专用的
					// map里，等待重连后的提取
//					client.busDeque.add(gpsDataNewQueue); // 把这个车号的队列，再重新增加到全车队列里，等待重连后的提取
//					gpsDataQueue.clear();// 这个队列已全部转移，进行清空

					// 记录模拟断开次数
					channelThreadInfo.incrementDisconnectInRandomTimeCount();

					// 启用主线程的调度任务，通过 busId 告诉主线程，在重连后，需要提取哪个车号队列继续进行发送数据
					client.group.schedule(() -> client.doConnect(busId, true), sendInterval, TimeUnit.SECONDS);

//					System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 遇到了注册包，已启用调度重连。 ");
//					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 遇到了注册包，已启用调度重连。 ");
					diconnectionMessage = "遇到了注册包，已启用调度重连。 ";

					// 需要断开重连
					ctx.disconnect();
				}
			}
			catch (Exception e)
			{
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送 GPS 数据失败。" + gpsDataLine.toString());
				e.printStackTrace();
			}
		}

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		// 断开前，先检查任务列表里的任务是否都已经结束
//		System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 等待发送任务的完成。");
//		while (!sendGpsDataTaskList.isEmpty())
//		{
//			if (sendGpsDataTaskList.get(0).isDone())
//				sendGpsDataTaskList.remove(0);
//		}
//		System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送任务已全部完成。");

		long endTime = System.currentTimeMillis();
//		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] [" + busId + "] 失去连接。运行时长："
//				+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知"));
//		System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 连接断开。[断开原因："
//				+ ((diconnectionMessage == null || diconnectionMessage.isEmpty()) ? "不明" : diconnectionMessage) + "] [运行时长："
//				+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知") + "]");

		if (logInTask != null)
		{
			logInTask.cancel(true);
			logInTask = null;
		}

		if (sendGpsDataTask != null)
		{
			sendGpsDataTask.cancel(true);
			sendGpsDataTask = null;
		}

		// client.disconnectionCount++;
		// FIXME
		// 意外断开重连
		if (!isDisconnectByManual)
		{
			// 断开次数
			channelThreadInfo.incrementDisconnectionCount();
			// 断开时间
			channelThreadInfo.setEndTime(endTime);

//			System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 不明原因的断开连接。 [运行时长："
//					+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知") + "]");

			if (busId != null && !busId.isEmpty())
			{
				if (!gpsDataQueue.isEmpty())
				{
					GPSDataLineFromAFile nextSendGpsDataLine = gpsDataQueue.peekFirst();
					// 下一个包不是注册包，就要编造一个注册包
					if (!"32".equals(nextSendGpsDataLine.getCommand()))
					{
						long currentTimestamp = System.currentTimeMillis();
						GPSDataLineFromAFile newGpsLoginLine = new GPSDataLineFromAFile();
						// 车号
						newGpsLoginLine.setBusId(busId);
						// 接收时间
//						gpsDataLine.setReceiveTime(currentTimestamp);
						// gps时间
//						gpsDataLine.setGpsTime(currentTimestamp);
						// 命令字
						newGpsLoginLine.setCommand("32");
						// gps数据串
						newGpsLoginLine.setGpsData(NettyClientCommand.getLoginHexString(busId, currentTimestamp));

						// 把要发的包，加载到队列最前端
						gpsDataQueue.addFirst(newGpsLoginLine);
//						ArrayDeque<GPSDataLineFromAFile> gpsDataNewQueue = new ArrayDeque<GPSDataLineFromAFile>();
//						gpsDataNewQueue.add(newGpsLoginLine); // 首先加入注册包
//						gpsDataNewQueue.addAll(gpsDataQueue); // 再把剩下的包，都往后加
//						client.busMap.put(busId, gpsDataNewQueue);// 把这个车号的队列，增加到断开重连专用的map里，等待重连后的提取
//						gpsDataQueue.clear();// 当前这个队列已全部转移，进行清空

						client.doConnect(busId, false);
					}
					// 假如是注册包，就当做是调度重连
					else
					{
						// 一个包都没发，就断开了
						if (lastSendGpsDataLine == null)
						{
							client.doConnect(busId, false);
							return;
						}

						// 1、计算两包之间的 GPS 时间间隔
						long gpsInterval = (nextSendGpsDataLine.getGpsOriginalTimestamp() - lastSendGpsDataLine.getGpsOriginalTimestamp());
						gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
						// 2、计算两包之间的发送间隔
						long sendInterval = (nextSendGpsDataLine.getReceiveTimestamp() - lastSendGpsDataLine.getReceiveTimestamp()) / 1000;
						sendInterval = sendInterval < 0 ? 0 : sendInterval;
						// 3、利用两包之间的 GPS 时间间隔，更新将要发包的 GPS 时间
						nextSendGpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);

						// 把要发的包，重新加载到队列里
//						Queue<GPSDataLineFromAFile> gpsDataNewQueue = new LinkedList<GPSDataLineFromAFile>();
//						gpsDataNewQueue.add(nextSendGpsDataLine); // 首先加入注册包
//						gpsDataNewQueue.addAll(gpsDataQueue); // 再把剩下的包，都往后加
//						client.busMap.put(busId, gpsDataNewQueue);// 把这个车号的队列，增加到断开重连专用的map里，等待重连后的提取
//						gpsDataQueue.clear();// 当前这个队列已全部转移，进行清空

						// 启用主线程的调度任务，通过 busId 告诉主线程，在重连后，需要提取哪个车号队列继续进行发送数据
						client.group.schedule(() -> client.doConnect(busId, true), sendInterval, TimeUnit.SECONDS);
					}
				}
				else
				{
					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 所有的包已发完，连接断开。发送了 " 
							+ client.busSendCountMap.get(busId) + " 条指令。[运行时长："
							+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知") + "]");
					// 从已连接的车号集合里，把本车号移除
					client.busSet.remove(busId);
				}
			}
			else
			{
				System.out.println("[未知车号] [" + df.format(new Date()) + "] "+diconnectionMessage+"而断开连接。 [运行时长："
						+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知") + "]");
			}
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		this.channelThreadInfo = new ChannelThreadInfo();
		channelThreadInfodDeque.add(channelThreadInfo);

		this.startTime = System.currentTimeMillis();

		// 记录端口号，
		channelThreadInfo.setThreadID(((SocketChannel) ctx.channel()).localAddress().getPort() + "");
		// 记录当前通道
		channelThreadInfo.setChannel(ctx.channel());
		// 记录连接时间
		channelThreadInfo.setStartTime(startTime);
		
		
		
		busId = ctx.channel().attr(client.busIdKey).get();
		if (busId == null || busId.isEmpty())
		{
//			System.out.println("取车号出错！");
//			diconnectionMessage = "取车号出错";
			ctx.disconnect();
			return;
		}
		else
		{
			gpsDataQueue = client.busMap.get(busId);
			isDisconnectByManual = ctx.channel().attr(client.isDisconnectByManualKey).get();
			System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 已连接！");
		}
		
		
		
		// 取出第一条指令，注册指令
//		this.gpsDataQueue = client.busMap.get(busId);
//		GPSDataLineFromAFile gpsDataLine = gpsDataQueue.poll();
		// 记录当前车号
//		busId = gpsDataLine.getBusId();
		// 连接上后，往车号集合里添加本车号，主要用于后面判断此车号的指令是否已经发送完毕
		client.busSet.add(busId);
		// FIXME
		// 第一次连接，调度任务的固定间隔可以自定义
		if (!isDisconnectByManual)
			// 立即注册
			logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, NettyClientUtil.LOGIN_INTERVAL, TimeUnit.SECONDS);
//			logInTask = ctx.executor().scheduleWithFixedDelay(new LogInTask(ctx, gpsDataLine), 1, NettyClientUtil.LOGIN_INTERVAL,
//					TimeUnit.SECONDS);
		// 手动断开重连，调度任务的固定间隔要缩短
		else
			logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, 2, TimeUnit.SECONDS);
		
		
		

		// 在设定的一个时间段内的一个随机时间点进行注册
		// logInTask = ctx.executor().scheduleWithFixedDelay(new LogInTask(ctx),
		// (long) (NettyClientUtil.LOGIN_TIMEOUT * 60 * (Math.random() * 0.9 +
		// 0.1)), NettyClientUtil.LOGIN_INTERVAL,
		// TimeUnit.SECONDS);

		// 模拟设备因为不稳定而断开连接（一次性任务）
//		if (NettyClientUtil.DISCONNECT_RANGE > 0)
//			disconnectInRandomTimeTask = client.taskService.schedule(new DisconnectInRandomTimeTask(ctx),
//					(long) (NettyClientUtil.DISCONNECT_RANGE * 60 * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);

		// 开启记录线程的一些信息的调度任务
		// if (threadInfoOutputTask == null)
		// threadInfoOutputTask = ctx.executor().scheduleWithFixedDelay(new
		// ThreadInfoOutputTask(), 30, 30, TimeUnit.SECONDS);

//		ctx.fireChannelActive();
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

//				System.out.println(Thread.currentThread().getName() + "接收到的信息：");
		// for (byte b : bytes)
		// System.out.print(b + ",");

//		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 接收到的信息："+ByteBufUtil.hexDump(bytes));
//		System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 接收到的信息：[" + ByteBufUtil.hexDump(bytes) + "]");

		// 接收到了注册应答信息
		if (bytes.length > 8 && bytes[7] == -96 && logInTask != null)
		{
			logInTask.cancel(true);
			isLogin = true;
			// 收到的注册应答包个数
			channelThreadInfo.setPackageCount("-96");
			// System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 已注册成功。");
			// System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 已注册成功。");
			
			// 一段时间后没有收到心跳，就断开连接
			// disconnectWithoutHeartBeatTask = client.taskService.schedule(new DisconnectWithoutHeartBeatTask(ctx),
			// NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
			
			// 只有一个注册包的情况下发生
			if (gpsDataQueue.isEmpty())
			{
			System.out.println(
			"[" + busId + "] [" + df.format(new Date()) + "] 所有的包已发完。发送了 " + client.busSendCountMap.get(busId) + " 条指令。");
			// 从已连接的车号集合里，把本车号移除
			client.busSet.remove(busId);
			// ctx.disconnect();
			return;
			}
			
			// 收到注册应答后，预读出第二条要发送的数据
			GPSDataLineFromAFile gpsDataLine = gpsDataQueue.peek();
			
			// FIXME
			
			// 第一次连接，第二个包的 GPS 时间使用当前的系统时间进行替换
			if (!isDisconnectByManual)
			{
			// 1、计算发送与采集时间的间隔（秒）
			long interval = (gpsDataLine.getReceiveTimestamp() - gpsDataLine.getGpsOriginalTimestamp()) / 1000;
			interval = interval < 0 ? 0 : interval;
			// 2、以当前时间更改第二条 GPS 数据
			gpsDataLine.updateGpsDataByTimestamp(System.currentTimeMillis());
			
			sendGpsDataTask = client.taskService.schedule(new SendGpsDataTask(ctx), interval, TimeUnit.SECONDS);
			}
			// 断开重连并注册成功后，第二包的 GPS 时间要参照注册包的 GPS 时间进行计算
			else
			{
			// 1、计算两包之间的 GPS 时间间隔
			long gpsInterval = (gpsDataLine.getGpsOriginalTimestamp() - lastSendGpsDataLine.getGpsOriginalTimestamp());
			gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
			// 2、计算两包之间的发送间隔
			long sendInterval = (gpsDataLine.getReceiveTimestamp() - lastSendGpsDataLine.getReceiveTimestamp()) / 1000;
			sendInterval = sendInterval < 0 ? 0 : sendInterval;
			// 3、以上一包的 GPS 时间，加上 GPS 时间间隔，作为将要发送的包的 GPS 时间，并在 sendInterval 时间间隔之后发送出去
			gpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);
			
			sendGpsDataTask = client.taskService.schedule(new SendGpsDataTask(ctx), sendInterval, TimeUnit.SECONDS);
			}
			
			// //发送异常信息
			// long initialDelay = (long) (NettyClientUtil.ABNORMAL_INTERVAL * (Math.random()0.9 + 0.1));
			// abnormalTask = client.taskService.scheduleWithFixedDelay(
			// new AbnormalTask(ctx, initialDelay, NettyClientUtil.ABNORMAL_INTERVAL), initialDelay,
			// NettyClientUtil.ABNORMAL_INTERVAL, TimeUnit.SECONDS);
			//
			// //发送定时定距消息
			// if (NettyClientUtil.TIMING_INTERVAL_FIXED == 1) //按固定的时间间隔进行发包
			// timingAndFixedDistanceTask = client.taskService.scheduleAtFixedRate(new TimingAndFixedDistanceTask(ctx),
			// NettyClientUtil.TIMING_INTERVAL, NettyClientUtil.TIMING_INTERVAL, TimeUnit.SECONDS);
			// else //发包时间间隔不固定
			// timingAndFixedDistanceTask = client.taskService.schedule(new TimingAndFixedDistanceTask(ctx),
			// (long) (NettyClientUtil.TIMING_INTERVAL * (Math.random() * 0.9 + 0.1)), TimeUnit.SECONDS);
			 

		}
		// 收到了心跳指令
		else if (isLogin && bytes.length > 8 && bytes[7] == 1)
		{
			// System.out.println("[" + Thread.currentThread().getName() + "] ["
			// + df.format(new Date()) + "] <<-- 接收到了心跳消息！！！");
			channelThreadInfo.setPackageCount("1");
			// 把之前的断开任务取消掉
//			if (disconnectWithoutHeartBeatTask != null)
//			{
//				disconnectWithoutHeartBeatTask.cancel(true);
//				disconnectWithoutHeartBeatTask = null;
//			}
			// 重新开启一个断开任务
//			disconnectWithoutHeartBeatTask = client.taskService.schedule(new DisconnectWithoutHeartBeatTask(ctx),
//					NettyClientUtil.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
		}
		// 收到了关键数据应答
		else if (isLogin && bytes.length > 8 && bytes[7] == -63)
		{
//			System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] <<-- 收到了异常应答 <<--");
//			System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 接收到关键数据应答");
			channelThreadInfo.setPackageCount("-63");
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
		}
		// 收到了校时应答
		else if (isLogin && bytes.length > 8 && bytes[7] == -125)
		{
//			System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 接收到校时应答");
			channelThreadInfo.setPackageCount("-125");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		super.exceptionCaught(ctx, cause);

		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] [" + busId + "] 因为异常而关闭连接。运行时长："
				+ (startTime > 0 ? NettyClientUtil.getFormatTime(System.currentTimeMillis() - startTime) : "未知"));

		if (ctx.channel().isActive())
			ctx.close();
	}
}
