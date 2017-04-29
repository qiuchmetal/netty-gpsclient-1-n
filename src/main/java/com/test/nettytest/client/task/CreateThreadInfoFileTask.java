package com.test.nettytest.client.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;

import com.test.nettytest.client.pojo.ChannelThreadInfo;
import com.test.nettytest.client.pojo.ConnectionThreadInfo;
import com.test.nettytest.client.pojo.ThreadInfoStatistics;

/**
 * 定时创建 ThreadInfoFile 文件
 */
public class CreateThreadInfoFileTask implements Runnable
{
	/**
	 * 连接线程
	 */
	private ConnectionThreadInfo connectionThreadInfo;
	/**
	 * 管道线程统计信息
	 */
	private ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque;

	public CreateThreadInfoFileTask(ConnectionThreadInfo connectionThreadInfo,
			ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque)
	{
		this.connectionThreadInfo = connectionThreadInfo;
		this.channelThreadInfodDeque = channelThreadInfodDeque;
	}

	/**
	 * 对线程信息列表进行统计
	 */
	private String doThreadStatistics()
	{
		ThreadInfoStatistics tis = new ThreadInfoStatistics();

		//连接线程
		tis.setConnectionStartTime(connectionThreadInfo.getStartTime());//连接开始时间 毫秒
		tis.setConnectionEndTime(System.currentTimeMillis());//连接结束时间 毫秒
		tis.setTryToConnectCount(connectionThreadInfo.getTryToConnectCount());//尝试连接次数
		tis.setConnectionCount(connectionThreadInfo.getConnectionCount());//成功连接次数
		tis.setFailToConnectCount(connectionThreadInfo.getFailToConnectCount());//连接失败次数

		//管道线程
		for (ChannelThreadInfo t : channelThreadInfodDeque)
		{
			//管道线程创建总数
			tis.setChannelThreadsCount(tis.getChannelThreadsCount() + 1);
			//当前连接总数
			tis.setCurrentChannelActiveCount(tis.getCurrentChannelActiveCount()
					+ ((t.getChannel() != null && t.getChannel().isActive()) ? 1 : 0));
			//断开次数
			tis.setDisconnectionCount(tis.getDisconnectionCount() + t.getDisconnectionCount());
			//模拟断开次数
			tis.setDisconnectInRandomTimeCount(
					tis.getDisconnectInRandomTimeCount() + t.getDisconnectInRandomTimeCount());
			//因没及时收到心跳而断开次数
			tis.setDisconnectionOfHeartBeatCount(
					tis.getDisconnectionOfHeartBeatCount() + t.getDisconnectionOfHeartBeatCount());
			//因没及时收到异常应答而断开次数
			tis.setDisconnectionOfAbnormalCount(
					tis.getDisconnectionOfAbnormalCount() + t.getDisconnectionOfAbnormalCount());

			//发送的注册包个数
			tis.setLoginPackageSendCount(tis.getLoginPackageSendCount() + t.getLoginPackageSendCount());
			//收到的注册应答包个数
			tis.setLoginPackageReceivedCount(tis.getLoginPackageReceivedCount() + t.getLoginPackageReceivedCount());
			//发送的定时定距包个数
			tis.setTimingPackageCount(tis.getTimingPackageCount() + t.getTimingPackageCount());
			//发送的异常包个数
			tis.setAbnormalPackageCount(tis.getAbnormalPackageCount() + t.getAbnormalPackageCount());
			//接收到的异常应答包个数
			tis.setAbnormalResponsePackageCount(
					tis.getAbnormalResponsePackageCount() + t.getAbnormalResponsePackageCount());
			//接收到的心跳个数
			tis.setHeartBeatPackageCount(tis.getHeartBeatPackageCount() + t.getHeartBeatPackageCount());
		}
		return tis.toString();
	}

	@Override
	public void run()
	{
		String fileName = (new SimpleDateFormat("yyyy-MM-dd-HH")).format(new Date());

		//创建管道线程信息明细记录文件
//		File threadInfoFile = new File("ChannelThreadInfo_" + fileName + ".txt");
//		try
//		{
//			if (!threadInfoFile.exists())
//			{
//				threadInfoFile.createNewFile();
//			}
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}

		//创建线程信息统计记录文件
		File threadInfoStatisticsFile = new File("ThreadInfoStatistics_" + fileName + ".txt");
		try
		{
			if (!threadInfoStatisticsFile.exists())
			{
				threadInfoStatisticsFile.createNewFile();
			}

			//有数据了再写入文件
			if (connectionThreadInfo.getStartTime() > 0)
			{
				//创建之后，立即写入内容
				FileWriter writer = new FileWriter(threadInfoStatisticsFile, true);
				BufferedWriter bufferedWriter = new BufferedWriter(writer);
				bufferedWriter.write(doThreadStatistics() + "\n\r\n\r\n\r\n\r");
				bufferedWriter.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}