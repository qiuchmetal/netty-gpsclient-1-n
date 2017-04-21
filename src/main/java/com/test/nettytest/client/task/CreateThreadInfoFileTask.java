package com.test.nettytest.client.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.test.nettytest.client.pojo.ThreadInfo;
import com.test.nettytest.client.pojo.ThreadInfoStatistics;	

/**
 * 定时创建 ThreadInfoFile 文件
 */
public class CreateThreadInfoFileTask implements Runnable
{
	/**
	 * 线程统计信息列表
	 */
	private List<ThreadInfo> threadInfoList;
	/**
	 * 线程池信息
	 */
//	private ThreadPoolExecutor executor;

//	public CreateThreadInfoFileTask(List<ThreadInfo> threadInfoList, ThreadPoolExecutor executor)
	public CreateThreadInfoFileTask(List<ThreadInfo> threadInfoList)
	{
		this.threadInfoList = threadInfoList;
//		this.executor = executor;
	}

	/**
	 * 对线程信息列表进行统计
	 */
	private String doThreadStatistics()
	{
		int size = threadInfoList.size();
		if (size > 0)
		{
			ThreadInfoStatistics tis = new ThreadInfoStatistics();
			//线程创建总数
			tis.setThreadListCount(size);
			//当前线程总数
			//			tis.setCurrentThreadCount(executor.getActiveCount());
			for (ThreadInfo t : threadInfoList)
			{
				//起始时间 毫秒
				tis.setStartTime(tis.getStartTime() > 0 ? Math.min(tis.getStartTime(), t.getStartTime()) : t.getStartTime());
				//统计终止时间 毫秒
				tis.setEndTime(System.currentTimeMillis());
				//当前连接总数
				tis.setCurrentChannelActiveCount(tis.getCurrentChannelActiveCount()
						+ ((t.getChannel() != null && t.getChannel().isActive()) ? 1 : 0));
				//尝试连接次数
				tis.setTryToConnectCount(tis.getTryToConnectCount() + t.getTryToConnectCount());
				//成功连接次数
				tis.setConnectionCount(tis.getConnectionCount() + t.getConnectionCount());
				//连接失败次数
				tis.setFailToConnectCount(tis.getFailToConnectCount() + t.getFailToConnectCount());
				//断开次数
				tis.setDisconnectionCount(tis.getDisconnectionCount() + t.getDisconnectionCount());
				//发送的注册包个数
				tis.setLoginPackageCount(tis.getLoginPackageCount() + t.getLoginPackageCount());
				//发送的定时定距包个数
				tis.setTimingPackageCount(tis.getTimingPackageCount() + t.getTimingPackageCount());
				//发送的异常包个数
				tis.setAbnormalPackageCount(tis.getAbnormalPackageCount() + t.getAbnormalPackageCount());
				//接收到的异常应答包个数
				tis.setAbnormalResponsePackageCount(tis.getAbnormalResponsePackageCount() + t.getAbnormalResponsePackageCount());
				//接收到的心跳个数
				tis.setHeartBeatPackageCount(tis.getHeartBeatPackageCount() + t.getHeartBeatPackageCount());
			}
			return tis.toString();
		}
		return null;
	}

	@Override
	public void run()
	{
		String fileName = (new SimpleDateFormat("yyyy-MM-dd-HH")).format(new Date());
		
		//创建线程信息明细记录文件
//		File threadInfoFile = new File("ThreadInfo_" + fileName + ".txt");
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

		if (threadInfoList.size() < 1)
			return;

		//创建线程信息统计记录文件
		File threadInfoStatisticsFile = new File("ThreadInfoStatistics_" + fileName + ".txt");
		try
		{
			if (!threadInfoStatisticsFile.exists())
			{
				threadInfoStatisticsFile.createNewFile();
			}
			//创建之后，立即写入内容
			FileWriter writer = new FileWriter(threadInfoStatisticsFile, true);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(doThreadStatistics()+"\n\r\n\r\n\r\n\r");
			bufferedWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}