package com.test.nettytest.client.channelhandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.RealDataFromFileConnetion;
import com.test.nettytest.client.pojo.ChannelThreadInfo;
import com.test.nettytest.client.pojo.GPSDataLineFromAFile;
import com.test.nettytest.client.pojo.NettyClientCommand;
import com.test.nettytest.client.util.NettyClientUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;

public class RealDataFromFileStreamHandler extends ChannelInboundHandlerAdapter
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
	private RealDataFromFileConnetion client;
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
//	private ArrayDeque<GPSDataLineFromAFile> gpsDataQueue;
	/**
	 * 即将发送的 GPS 数据包
	 */
	private GPSDataLineFromAFile nextSendGpsDataLine;
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
	private volatile boolean isDisconnectByManual;
	/**
	 * 当前 channel 加载的车号文件
	 */
	private File busFile;
	/**
	 * 当前 channel 加载的车号文件的 Reader
	 */
	private InputStreamReader reader;
	/**
	 * 当前 channel 加载的车号文件的缓冲
	 */
	private BufferedReader busFileBufReader;
	/**
	 * 判断将要发送的注册包，是编造的，还是实际读取的
	 */
	private boolean isLoginFromFile;
	/**
	 * 是否是第一次进行连接
	 */
	private boolean isFirstActive;
	/**
	 * 是否使用当前时间更新 GPS 数据里的时间
	 */
	private boolean updateGpsTimeByCurrentTime;
	/**
	 * 断开信息
	 */
	private volatile String diconnectionMessage;
	/**
	 * 在断开前，检测发送数据包的任务是否已经全部结束，然后再断开，否则会抛异常
	 */
	private volatile int sendingCount = 0;
	/**
	 * 在调度重连的断开前，再进行重连任务
	 */
	private volatile long reConnectInterval;

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
	public RealDataFromFileStreamHandler(ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque, RealDataFromFileConnetion client)
	{
		this.channelThreadInfodDeque = channelThreadInfodDeque;
		this.client = client;
//		this.isDisconnectByManual = false;
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
//			GPSDataLineFromAFile gpsDataLine = null;
			try
			{
				// FIXME
//				gpsDataLine = gpsDataQueue.peekFirst();
				// 发送的注册包是指令包里的第一条指令，需要使用当前时间来初始化注册包里的 GPS 时间
//				if (!isDisconnectByManual)
//					nextSendGpsDataLine.updateGpsDataByTimestamp(System.currentTimeMillis());
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送注册信息：[" + nextSendGpsDataLine.getGpsData() + "]");
				sendingCount++;
				ctx.writeAndFlush(Unpooled.copiedBuffer(NettyClientUtil.hexStringToByteArray(nextSendGpsDataLine.getGpsData())))
						.addListener(new ChannelFutureListener()
						{
							@Override
							public void operationComplete(ChannelFuture future) throws Exception
							{
								if (future.isSuccess())
								{
									sendingCount--;
									// 保存已发送的包
									lastSendGpsDataLine = nextSendGpsDataLine; // 线程级别
									client.busSendGpsDataLineMap.put(busId, lastSendGpsDataLine); // 进程级别，在断开重连时使用
									// 发送的注册包个数
									channelThreadInfo.setPackageCount(nextSendGpsDataLine.getCommand());
								}

							}
						});

				
//				gpsDataQueue.pollFirst();

//				System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 发送注册信息！-->>");

			}
			catch (Exception e)
			{
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送注册信息失败。" + nextSendGpsDataLine.toString());
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
			try
			{				
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

				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送了 ：[" + nextSendGpsDataLine.getCommand() + "]["
						+ nextSendGpsDataLine.getGpsData() + "]");
				
				sendingCount++;
				ctx.writeAndFlush(Unpooled.copiedBuffer(NettyClientUtil.hexStringToByteArray(nextSendGpsDataLine.getGpsData())))
						.addListener(new ChannelFutureListener()
						{
							@Override
							public void operationComplete(ChannelFuture future) throws Exception
							{
								if (future.isSuccess())
								{
									sendingCount--;
									
									try
									{
										// 发送成功了，才真正的移动文件游标
										busFileBufReader.readLine();
									}
									catch (Exception e)
									{
										System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 缓冲区已关闭，等待连接断开。 ");
										
										return;
									}

		//									if ("3".equals(gpsDataLine.getCommand()))
		//										System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送了校时请求 ：[" + gpsDataLine.getGpsData() + "]");

									// 保留当前已发送的包
									lastSendGpsDataLine = nextSendGpsDataLine;
									client.busSendGpsDataLineMap.put(busId, lastSendGpsDataLine); // 进程级别，在断开重连时使用
									// System.out.println("[" + Thread.currentThread().getName() +
									// "] [" + df.format(new Date()) + "] 发送异常信息！-->> " + count);

									// 发送包的个数
									channelThreadInfo.setPackageCount(nextSendGpsDataLine.getCommand());

									// 记录此车号发送指令的数量
									if (client.busSendCountMap.containsKey(busId))
										client.busSendCountMap.put(busId, client.busSendCountMap.get(busId) + 1);
									else
										client.busSendCountMap.put(busId, 1);

									String lineTxt;

									// 预读下一行数据
									busFileBufReader.mark((int) (busFile.length() + 1));
									lineTxt = busFileBufReader.readLine();
									busFileBufReader.reset();

		//									try
		//									{
		//										// 预读下一行数据
		//										busFileBufReader.mark((int) (busFile.length() + 1));
		//										lineTxt = busFileBufReader.readLine();
		//										busFileBufReader.reset();
		//									}
		//									catch (Exception e)
		//									{
		//										client.busSet.remove(busId);
		//										System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 所有的包已发完，缓冲区已关闭。发送了 "
		//												+ client.busSendCountMap.get(busId) + " 条指令。还剩 "+client.busSet.size()+" 个车号。");
		//										
		//										return;
		//									}

									if (lineTxt == null)
									{
										int i = client.busSendCount.incrementAndGet();
										//查看是否仍有数据在发，确定没有再发的数据了，再关闭缓冲区
										client.taskService.schedule(() ->
										{
											while (true)
											{
												if (sendingCount == 0)
												{
													try
													{
														reader.close();
														break;
													}
													catch (Exception e)
													{
														e.printStackTrace();
													}
												}
											}
										} , 0, TimeUnit.SECONDS);
										
										System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 所有的包已发完，缓冲区即将关闭(SendGpsDataTask)。发送了 "
												+ client.busSendCountMap.get(busId) + " 条指令。已完成 " + i + " 个车号。");
//										client.busSendCountDownLatch.countDown();
										return;
									}

									String[] strings = lineTxt.split(",");
									nextSendGpsDataLine = new GPSDataLineFromAFile(busId, strings);

									// 1、计算两包之间的 GPS 时间间隔
									long gpsInterval = (nextSendGpsDataLine.getGpsOriginalTimestamp()
											- lastSendGpsDataLine.getGpsOriginalTimestamp());
									gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
									// 2、计算两包之间的发送间隔
									long sendInterval = (nextSendGpsDataLine.getReceiveTimestamp()
											- lastSendGpsDataLine.getReceiveTimestamp()) / 1000;
									sendInterval = sendInterval < 0 ? 0 : sendInterval;
									// 3、利用两包之间的 GPS 时间间隔，更新将要发包的 GPS 时间
									nextSendGpsDataLine
											.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);
									// FIXME
									// 这个包不是注册包
									if (!"32".equals(nextSendGpsDataLine.getCommand()))
									{
										sendGpsDataTask = client.taskService.schedule(new SendGpsDataTask(ctx), sendInterval,
												TimeUnit.SECONDS);
									}
									// 这个包是注册包
									else
									{
										// 记录模拟断开次数
										channelThreadInfo.incrementDisconnectInRandomTimeCount();

		//										System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 遇到了注册包，已启用调度重连。 ");
		//										System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 遇到了注册包，已启用调度重连。 ");
										diconnectionMessage = "遇到了注册包，已启用调度重连。 ";
										isDisconnectByManual = true;
										reConnectInterval = sendInterval;

										// 启用主线程的调度任务，通过 busId 告诉主线程，在重连后，需要提取哪个车号队列继续进行发送数据
		//										client.taskService.schedule(() -> client.doConnect(busFile, reader, busFileBufReader, isDisconnectByManual),
		//												sendInterval, TimeUnit.SECONDS);
										// 需要断开重连
										client.taskService.schedule(() ->
										{
											while (true)
											{
												if (sendingCount == 0)
												{
													// 真正断开后，再执行重连任务
													ctx.disconnect();
													break;
												}
											}
										} , 0, TimeUnit.SECONDS);

									}

								}
							}
						});
				

			}
			catch (Exception e)
			{
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发送 GPS 数据失败。" + nextSendGpsDataLine.toString());
				e.printStackTrace();
			}
		}

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
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

		// 断开次数
		channelThreadInfo.incrementDisconnectionCount();
		// 断开时间
		long endTime = System.currentTimeMillis();
		channelThreadInfo.setEndTime(endTime);

		// FIXME
		// 无车号
		if (busId == null || busId.isEmpty())
		{
			System.out.println("[未知车号] [" + df.format(new Date()) + "] 断开连接。 [运行时长："
					+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知") + "]");
		}
		// 有车号
		else
		{
			// 遇到注册包，需要调度重连
			if (isDisconnectByManual)
			{
				if (reConnectInterval > 0)
					// 启用主线程的调度任务，通过 busId 告诉主线程，在重连后，需要提取哪个车号队列继续进行发送数据
					client.taskService.schedule(() -> client.doConnect(busFile, reader, busFileBufReader, false, isDisconnectByManual),
							reConnectInterval, TimeUnit.SECONDS);
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] " + diconnectionMessage + " [运行时长："
						+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知") + "]");
			}
			// 意外断开重连
			else
			{
				if (sendingCount > 0)
				{
					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 正在发送  " + sendingCount + " 条指令，但因为意外断开而发送失败。");
				}

				String lineTxt;
				try
				{
					// 从缓冲区预读出一行
					busFileBufReader.mark((int) (busFile.length() + 1));
					lineTxt = busFileBufReader.readLine();
					busFileBufReader.reset();
				}
				//缓冲区已关闭，会引发异常
				catch (Exception e)
				{
//					int i = client.busSendCount.incrementAndGet();
//					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 预读行发生异常。发送了 "
//							+ client.busSendCountMap.get(busId) + " 条指令。已完成 " + i + " 个车号。 [运行时长："
//							+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知") + "]");
//					client.busSendCountDownLatch.countDown();
//					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 预读行发生异常(channelInactive)。 ");
					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 缓冲区已关闭，连接断开。 ");
					return;
				}

				// 从缓冲区预读出一行
//				busFileBufReader.mark((int) (busFile.length() + 1));
//				lineTxt = busFileBufReader.readLine();
//				busFileBufReader.reset();

				// 没有可以发送的数据了
				if (lineTxt == null)
				{
					int i = client.busSendCount.incrementAndGet();
					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 所有的包已发完，关闭缓冲区，连接断开。发送了 "
							+ client.busSendCountMap.get(busId) + " 条指令。已完成 " + i + " 个车号。 [运行时长："
							+ (startTime > 0 ? NettyClientUtil.getFormatTime(endTime - startTime) : "未知") + "]");
//					client.busSendCountDownLatch.countDown();
					//查看是否仍有数据在发，确定没有再发的数据了，再关闭缓冲区
					client.taskService.schedule(() ->
					{
						while (true)
						{
							if (sendingCount == 0)
							{
								try
								{
									reader.close();
									break;
								}
								catch (Exception e)
								{
									e.printStackTrace();
								}
							}
						}
					} , 0, TimeUnit.SECONDS);
					// ctx.disconnect();

				}
				// 还有数据要发送
				else
				{
					System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 即将重连。 ");
					client.doConnect(busFile, reader, busFileBufReader, false, isDisconnectByManual);

//					// 分析下一条数据
//					String[] strings = lineTxt.split(",");
//					nextSendGpsDataLine = new GPSDataLineFromAFile(busId, strings);
//
//					// 将要发送的是注册包
//					if ("32".equals(nextSendGpsDataLine.getCommand()))
//					{
//
//						// 一个包都没发，就断开了，那么就立即进行重连
//						if (lastSendGpsDataLine == null)
//						{
//							client.doConnect(busFile, false);
//						}
//						// 之前有发送过数据包，那么这个断开就当做是调度重连
//						else
//						{
//							// 1、计算两包之间的 GPS 时间间隔
//							// long gpsInterval = (nextSendGpsDataLine.getGpsOriginalTimestamp() - lastSendGpsDataLine.getGpsOriginalTimestamp());
//							// gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
//							// 2、计算两包之间的发送间隔
//							long sendInterval = (nextSendGpsDataLine.getReceiveTimestamp() - lastSendGpsDataLine.getReceiveTimestamp())
//									/ 1000;
//							sendInterval = sendInterval < 0 ? 0 : sendInterval;
//							// 3、利用两包之间的 GPS 时间间隔，更新将要发包的 GPS 时间
//							// nextSendGpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);
//
//							// 把要发的包，重新加载到队列里
//							// Queue<GPSDataLineFromAFile> gpsDataNewQueue = new LinkedList<GPSDataLineFromAFile>();
//							// gpsDataNewQueue.add(nextSendGpsDataLine); // 首先加入注册包
//							// gpsDataNewQueue.addAll(gpsDataQueue); // 再把剩下的包，都往后加
//							// client.busMap.put(busId, gpsDataNewQueue);// 把这个车号的队列，增加到断开重连专用的map里，等待重连后的提取
//							// gpsDataQueue.clear();// 当前这个队列已全部转移，进行清空
//
//							// 启用主线程的调度任务，通过 busId 告诉主线程，在重连后，需要提取哪个车号队列继续进行发送数据
//							client.taskService.schedule(() -> client.doConnect(busFile, true), sendInterval, TimeUnit.SECONDS);
//						}
//
//						client.doConnect(busFile, false);
//					}
//					// 将要发送的不是注册包，那么在重连后，需要先编造一个注册包
//					else
//					{
//						client.doConnect(busFile, false);
//					}

				}
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

		busFile = ctx.channel().attr(client.busFileKey).get();

		if (busFile == null)
		{
//			System.out.println("取车号出错！");
//			diconnectionMessage = "取车号出错";
			ctx.disconnect();
			return;
		}

		busId = busFile.getName();
		isDisconnectByManual = ctx.channel().attr(client.isDisconnectByManualKey).get();

		reader = ctx.channel().attr(client.busReaderKey).get();
		busFileBufReader = ctx.channel().attr(client.busBufferedReaderKey).get();
		// 是否是第一次连接
		isFirstActive = ctx.channel().attr(client.isFirstActiveKey).get();
		// 上一次发送的包
		if (!isFirstActive)
			lastSendGpsDataLine = client.busSendGpsDataLineMap.get(busId);

		// 预读一行
		busFileBufReader.mark((int) (busFile.length() + 1));
		String lineTxt = busFileBufReader.readLine();
		busFileBufReader.reset();

		String[] strings = lineTxt.split(",");

		// 将要发送的注册包
		nextSendGpsDataLine = new GPSDataLineFromAFile();
		// 车号
		nextSendGpsDataLine.setBusId(busId);
		// 接收时间
//		nextSendGpsDataLine.setReceiveTimestamp(Long.parseLong(strings[1]));
		// gps 原始时间
//		nextSendGpsDataLine.setGpsOriginalTimestamp(Long.parseLong(strings[2]));
		// 命令字
//		nextSendGpsDataLine.setCommand(strings[3]);
		// gps数据串
//		nextSendGpsDataLine.setGpsData(strings[4]);

		// FIXME

		// 此包是注册
		if ("32".equals(strings[3]))
		{
			isLoginFromFile = true;
			// 接收时间
			nextSendGpsDataLine.setReceiveTimestamp(Long.parseLong(strings[1]));
			// gps 原始时间
			nextSendGpsDataLine.setGpsOriginalTimestamp(Long.parseLong(strings[2]));
			// 命令字
			nextSendGpsDataLine.setCommand(strings[3]);
			// gps数据串
			nextSendGpsDataLine.setGpsData(strings[4]);

			// 第一次连接
			if (isFirstActive)
			{
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 第一次连接！目前已连接 "
						+ client.busInFirstActiveCount.incrementAndGet() + " 个车号。");
				// 以当前时间更新 GPS 包里的 GPS 时间
				nextSendGpsDataLine.updateGpsDataByTimestamp(System.currentTimeMillis());
				// 下一包要使用当前时间来更新 GPS 里的时间
				updateGpsTimeByCurrentTime = true;

				// 立即注册，发送就间隔选取配置文件中的设置
				logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, NettyClientUtil.LOGIN_INTERVAL,
						TimeUnit.SECONDS);
			}
			// 之前遇到了注册包，调度中断后的重连
			else if (isDisconnectByManual)
			{
				//重置此标记，否则会一直的 true 下去
				isDisconnectByManual = false;
				
				// 没有上一个包，以当前时间处理注册包的 GPS 时间，这个情况应该是基本不会发生的
				if (lastSendGpsDataLine == null)
				{
					// 以当前时间更新 GPS 包里的 GPS 时间
					nextSendGpsDataLine.updateGpsDataByTimestamp(System.currentTimeMillis());
					// 下一包要使用当前时间来更新 GPS 里的时间
					updateGpsTimeByCurrentTime = true;
				}
				// 有上一个包
				else
				{
					// 1、计算两包之间的 GPS 时间间隔
					long gpsInterval = (nextSendGpsDataLine.getGpsOriginalTimestamp() - lastSendGpsDataLine.getGpsOriginalTimestamp());
					gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
					// 2、以上一包的 GPS 时间，加上 GPS 时间间隔，作为将要发送的包的 GPS 时间
					nextSendGpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);

					// 下一包要使用参考上一包来更新 GPS 里的时间
					updateGpsTimeByCurrentTime = false;
				}

				// 立即注册，发送就间隔要短，这里采用 2 秒
				logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, 2, TimeUnit.SECONDS);
			}
			// 非正常中断后的重连
			else
			{
				// 一个包也没发成功
				if (lastSendGpsDataLine == null)
				{
					// 以当前时间更新 GPS 包里的 GPS 时间
					nextSendGpsDataLine.updateGpsDataByTimestamp(System.currentTimeMillis());
					// 下一包要使用当前时间来更新 GPS 里的时间
					updateGpsTimeByCurrentTime = true;

					// 立即注册，发送就间隔选取配置文件中的设置
					logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, NettyClientUtil.LOGIN_INTERVAL,
							TimeUnit.SECONDS);
				}
				else
				{
					// 1、计算两包之间的 GPS 时间间隔
					long gpsInterval = (nextSendGpsDataLine.getGpsOriginalTimestamp() - lastSendGpsDataLine.getGpsOriginalTimestamp());
					gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
					// 2、以上一包的 GPS 时间，加上 GPS 时间间隔，作为将要发送的包的 GPS 时间
					nextSendGpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);
					// 下一包要使用参考上一包来更新 GPS 里的时间
					updateGpsTimeByCurrentTime = false;

					// 立即注册，发送就间隔要短，这里采用 2 秒
					logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, 2, TimeUnit.SECONDS);
				}
			}
		}
		// 此包不是注册包，就要编造一个注册包
		else
		{
			// 标识这个将要发送的注册包，是编造出来的
			isLoginFromFile = false;

			// 接收时间
//			nextSendGpsDataLine.setReceiveTimestamp(Long.parseLong(strings[1]));
			// gps 原始时间
//			nextSendGpsDataLine.setGpsOriginalTimestamp(Long.parseLong(strings[2]));
			// 命令字
			nextSendGpsDataLine.setCommand("32");
			// 编造gps数据串
			nextSendGpsDataLine.setGpsData(NettyClientCommand.getLoginHexString(busId, System.currentTimeMillis()));
			// 下一包要使用当前时间来更新 GPS 里的时间
			updateGpsTimeByCurrentTime = true;

			// 第一次连接
			if (isFirstActive)
			{
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 第一次连接！目前已连接 "
						+ client.busInFirstActiveCount.incrementAndGet() + " 个车号。");
				// 立即注册，发送就间隔选取配置文件中的设置
				logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, NettyClientUtil.LOGIN_INTERVAL,
						TimeUnit.SECONDS);
			}
			// 之前遇到了注册包，调度中断后的重连，那么当前的包应该是注册包，逻辑上不会走到这步
			else if (isDisconnectByManual)
			{
				//重置此标记，否则会一直的 true 下去
				isDisconnectByManual = false;
				
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 发生奇怪现象——没有遇到注册包却进行了调度重连。 ");
				// 上一包没有保存到
				if (lastSendGpsDataLine == null)
				{

				}
				// 有上一包
				else
				{
					// 1、计算两包之间的 GPS 时间间隔
					long gpsInterval = ((Long.parseLong(strings[2]) - lastSendGpsDataLine.getGpsOriginalTimestamp())) / 2;
					gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
					// 填充 gps 原始时间，以便给再下一个要发送的包作参考
					nextSendGpsDataLine.setGpsOriginalTimestamp(Long.parseLong(strings[2]) + gpsInterval);

					// 2、计算两包之间的接收间隔
					long receiveInterval = (Long.parseLong(strings[1]) - lastSendGpsDataLine.getReceiveTimestamp()) / 2;
					receiveInterval = receiveInterval < 0 ? 0 : receiveInterval;
					// 填充接收时间，以便给再下一个要发送的包作参考
					nextSendGpsDataLine.setReceiveTimestamp(Long.parseLong(strings[1]) + receiveInterval / 2);

					// 3、利用两包之间的 GPS 时间间隔的一半，更新将要发送的注册包的 GPS 时间
					nextSendGpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);

					// 下一包要使用参考上一包来更新 GPS 里的时间
					updateGpsTimeByCurrentTime = false;
				}
				// 立即注册，发送就间隔要短，这里采用 2 秒
				logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, 2, TimeUnit.SECONDS);
			}
			// 非正常中断后的重连
			else
			{
				// 上一包没有保存到
				if (lastSendGpsDataLine == null)
				{

				}
				// 有上一包
				else
				{
					// 1、计算两包之间的 GPS 时间间隔
					long gpsInterval = ((Long.parseLong(strings[2]) - lastSendGpsDataLine.getGpsOriginalTimestamp())) / 2;
					gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
					// 填充 gps 原始时间，以便给再下一个要发送的包作参考
					nextSendGpsDataLine.setGpsOriginalTimestamp(Long.parseLong(strings[2]) + gpsInterval);

					// 2、计算两包之间的接收间隔
					long receiveInterval = (Long.parseLong(strings[1]) - lastSendGpsDataLine.getReceiveTimestamp()) / 2;
					receiveInterval = receiveInterval < 0 ? 0 : receiveInterval;
					// 填充接收时间，以便给再下一个要发送的包作参考
					nextSendGpsDataLine.setReceiveTimestamp(Long.parseLong(strings[1]) + receiveInterval / 2);

					// 3、利用两包之间的 GPS 时间间隔的一半，更新将要发送的注册包的 GPS 时间
					nextSendGpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);

					// 下一包要使用参考上一包来更新 GPS 里的时间
					updateGpsTimeByCurrentTime = false;
				}

				// 立即注册，发送就间隔要短，这里采用 2 秒
				logInTask = client.taskService.scheduleWithFixedDelay(new LogInTask(ctx), 0, 2, TimeUnit.SECONDS);
			}
		}
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

//			if

			// 之前发送的注册指令，是从文件加载的，这里注册成功后，就把光标移到下一行
			if (isLoginFromFile)
			{
				busFileBufReader.readLine();

				// 证明之前发送的注册指令已经发送成功，记录此车号发送指令的数量
				if (client.busSendCountMap.containsKey(busId))
					client.busSendCountMap.put(busId, client.busSendCountMap.get(busId) + 1);
				else
					client.busSendCountMap.put(busId, 1);
			}

			// 从缓冲区读出一行
			String lineTxt;
//			try
//			{
//				// 收到注册应答后，预读出第二条要发送的数据
//				busFileBufReader.mark((int) (busFile.length() + 1));
//				lineTxt = busFileBufReader.readLine();
//				busFileBufReader.reset();
//			}
//			catch (Exception e)
//			{
//				client.busSet.remove(busId);
//				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 所有的包已发完，缓冲区已关闭。发送了 " + client.busSendCountMap.get(busId)
//						+ " 条指令。还剩 "+client.busSet.size()+" 个车号。");
//				return;
//			}

			// 收到注册应答后，预读出第二条要发送的数据
			busFileBufReader.mark((int) (busFile.length() + 1));
			lineTxt = busFileBufReader.readLine();
			busFileBufReader.reset();

			// 只有一个注册包的情况下发生
			if (lineTxt == null)
			{
				int i = client.busSendCount.incrementAndGet();
				System.out.println("[" + busId + "] [" + df.format(new Date()) + "] 所有的包已发完，缓冲区即将关闭(channelRead)。发送了 "
						+ client.busSendCountMap.get(busId) + " 条指令。已完成 " + i + " 个车号。");
//				client.busSendCountDownLatch.countDown();
				// ctx.disconnect();
				//查看是否仍有数据在发，确定没有再发的数据了，再关闭缓冲区
				client.taskService.schedule(() ->
				{
					while (true)
					{
						if (sendingCount == 0)
						{
							try
							{
								reader.close();
								break;
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				} , 0, TimeUnit.SECONDS);
				return;
			}

			String[] strings = lineTxt.split(",");
			nextSendGpsDataLine = new GPSDataLineFromAFile(busId, strings);

			// FIXME

			// 第二个包的 GPS 时间使用当前的系统时间进行替换
			if (updateGpsTimeByCurrentTime)
			{
				// 1、计算发送与采集时间的间隔（秒）
				long interval = (nextSendGpsDataLine.getReceiveTimestamp() - nextSendGpsDataLine.getGpsOriginalTimestamp()) / 1000;
				interval = interval < 0 ? 0 : interval;
				// 2、以当前时间更改第二个包的 GPS 数据
				nextSendGpsDataLine.updateGpsDataByTimestamp(System.currentTimeMillis());

				sendGpsDataTask = client.taskService.schedule(new SendGpsDataTask(ctx), interval, TimeUnit.SECONDS);
			}
			// 断开重连并注册成功后，第二包的 GPS 时间要参照注册包的 GPS 时间进行计算
			else
			{
				// 1、计算两包之间的 GPS 时间间隔
				long gpsInterval = (nextSendGpsDataLine.getGpsOriginalTimestamp() - lastSendGpsDataLine.getGpsOriginalTimestamp());
				gpsInterval = gpsInterval < 0 ? 0 : gpsInterval;
				// 2、计算两包之间的发送间隔
				long sendInterval = (nextSendGpsDataLine.getReceiveTimestamp() - lastSendGpsDataLine.getReceiveTimestamp()) / 1000;
				sendInterval = sendInterval < 0 ? 0 : sendInterval;
				// 3、以上一包的 GPS 时间，加上 GPS 时间间隔，作为将要发送的包的 GPS 时间，并在 sendInterval 时间间隔之后发送出去
				nextSendGpsDataLine.updateGpsDataByTimestamp(lastSendGpsDataLine.getGpsCurrentTimestamp() + gpsInterval);

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
//		super.exceptionCaught(ctx, cause);

		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] [" + busId + "] 因为异常而关闭连接。运行时长："
				+ (startTime > 0 ? NettyClientUtil.getFormatTime(System.currentTimeMillis() - startTime) : "未知"));

//		if (ctx.channel().isActive())
//			ctx.close();
		if (ctx.channel().isActive())
			ctx.disconnect();
	}
}
