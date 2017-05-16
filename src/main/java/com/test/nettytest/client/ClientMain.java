package com.test.nettytest.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.pojo.ChannelThreadInfo;
import com.test.nettytest.client.pojo.ConnectionThreadInfo;
import com.test.nettytest.client.pojo.GPSDataLineFromAFile;
import com.test.nettytest.client.pojo.NettyClientCommand;
import com.test.nettytest.client.task.CreateThreadInfoFileTask;

public class ClientMain
{
	public static void main(String[] args)
	{
		System.out.println("开始加载车号文件。");
		ConcurrentLinkedDeque<Queue<GPSDataLineFromAFile>> busDeque = new ConcurrentLinkedDeque<Queue<GPSDataLineFromAFile>>();
		int busCount = loadBusInfo(busDeque);
		System.out.println("成功加载了 " + busCount + " 个车号文件。");

		while (!busDeque.isEmpty())
		{
			Queue<GPSDataLineFromAFile> gpsDataqQueue = busDeque.poll();
			while (!gpsDataqQueue.isEmpty())
			{
				GPSDataLineFromAFile gpsDataLine = gpsDataqQueue.poll();
//				GpsDataHandler.handleGpsData(gpsDataLine);
			}
		}

		try
		{
			TimeUnit.HOURS.sleep(1);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 连接线程，只有一个对象
		ConnectionThreadInfo connectionThreadInfo = new ConnectionThreadInfo();
		// 管道线程信息统计
		ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque = new ConcurrentLinkedDeque<ChannelThreadInfo>();

		// 专门用来创建记录 ThreadInfo 信息的文件的线程
		ScheduledExecutorService createFileService = Executors.newScheduledThreadPool(1);
		// 定时检查一次看是否有创建文件，正常情况下是每个小时创建一次
		createFileService.scheduleAtFixedRate(new CreateThreadInfoFileTask(connectionThreadInfo, channelThreadInfodDeque), 0, 1,
				TimeUnit.MINUTES);

//		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
//		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 即将开启的线程数：[" + NettyClientUtil.THREAD_POOL_SIZE + "]  连接数：["
//				+ NettyClientUtil.THREAD_POOL_SIZE * NettyClientUtil.PER_THREAD_CONNETIONS + "]");

//		for (int i = 0; i < NettyClientUtil.THREAD_POOL_SIZE; i++)
//		(new NettyClientConnetion(threadInfoList)).start();

//		new NettyClientConnetion(connectionThreadInfo, channelThreadInfodDeque).start();

		new NettyClientConnetion(connectionThreadInfo, channelThreadInfodDeque, busDeque, busCount).start();

		// ThreadPoolExecutor executor = (ThreadPoolExecutor)
		// Executors.newFixedThreadPool(NettyClientUtil.THREAD_POOL_SIZE);
		// ExecutorService executorService =
		// Executors.newFixedThreadPool(NettyClientUtil.THREAD_POOL_SIZE);
		// //一条线程开一个连接的方式
		// if ("1:1".equalsIgnoreCase(NettyClientUtil.RUN_TYPE))
		// {
		// for (int i = 0; i < NettyClientUtil.THREAD_POOL_SIZE; i++)
		// executor.execute(new NettyClientInSingleConnetion(threadInfoList));
		// }
		// //一条线程开多个连接的方式
		// else if ("1:n".equalsIgnoreCase(NettyClientUtil.RUN_TYPE))
		// {
		// for (int i = 0; i < NettyClientUtil.THREAD_POOL_SIZE; i++)
		// executor.execute(new NettyClientInMultipleConnection());
		// }
		//
		// executor.shutdown();
	}

	// 把“/buslib”目录下面的车号文件的内容加载到内存里
	private static int loadBusInfo(ConcurrentLinkedDeque<Queue<GPSDataLineFromAFile>> busDeque)
	{
		String filePath = (new File(ClientMain.class.getProtectionDomain().getCodeSource().getLocation().getFile())).getParent()
				+ System.getProperty("file.separator") + "buslib";
//		System.out.println(filePath);

		int busCount = 0;
		File fileDir = new File(filePath);
//		String encoding="UTF-8";
//		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");

		if (fileDir.isDirectory())
		{
			File[] files = fileDir.listFiles();
			for (File f : files) // 循环，对单个文件进行操作
			{
//				String fileName = f.getName();
//				if (fileName.endsWith(".txt"))
//				{
//					String suffix = fileName.substring(fileName.lastIndexOf("."));
				// 车号名称
//					String busName = fileName.substring(0, fileName.length() - suffix.length());
				
				// 车号名称（没有后缀名）
				String busName = f.getName();
				busCount++;
				// 把每行内容加入队列里，因为一条线程只加载一个队列对象，不会产生线程安全问题
				Queue<GPSDataLineFromAFile> gpsDataqQueue = new LinkedList<GPSDataLineFromAFile>();
				try
				{
					// 读取车号文件内容
//						InputStreamReader reader = new InputStreamReader(new FileInputStream(f),encoding);
					InputStreamReader reader = new InputStreamReader(new FileInputStream(f)); // 使用默认字符集
					BufferedReader bufferedReader = new BufferedReader(reader);
					String lineTxt = null;
					boolean isTheFirst = true; //是否是第一条记录
					while ((lineTxt = bufferedReader.readLine()) != null)
					{
//							System.out.println(lineTxt);
						String[] strings = lineTxt.split(",");
						// 在读取第一条数据时，判断是否是注册指令，假如不是，则先编造一条注册指令，加入队列，然后再加文件内容
						if (isTheFirst && strings[3] != "32")
						{
							long currentTimestamp = System.currentTimeMillis();
							GPSDataLineFromAFile gpsDataLine = new GPSDataLineFromAFile();
							// 接收时间
//							gpsDataLine.setReceiveTime(currentTimestamp);
							// gps时间
//							gpsDataLine.setGpsTime(currentTimestamp);
							// 命令字
							gpsDataLine.setCommand("32");
							// gps数据串
							gpsDataLine.setGpsData(NettyClientCommand.getLoginHexString(busName, currentTimestamp));
							gpsDataqQueue.add(gpsDataLine);
							isTheFirst = false;
						}

						GPSDataLineFromAFile gpsDataLine = new GPSDataLineFromAFile();
						// 接收时间
						gpsDataLine.setReceiveTimestamp(Long.parseLong(strings[1]));
						// gps 原始时间
						gpsDataLine.setGpsOriginalTimestamp(Long.parseLong(strings[2]));
						// 命令字
						gpsDataLine.setCommand(strings[3]);
						// gps数据串
						gpsDataLine.setGpsData(strings[4]);
						gpsDataqQueue.add(gpsDataLine);
					}
					busDeque.add(gpsDataqQueue);
					reader.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
//				}
			}
		}

		return busCount;

	}

}
