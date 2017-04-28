package com.test.nettytest.client.pojo;

import java.text.SimpleDateFormat;

import com.test.nettytest.client.util.NettyClientUtil;

/**
 * 父类线程信息
 */
public class ConnectionThreadInfo
{
	/**
	 * 起始时间 毫秒
	 */
	private long startTime;
	/**
	 * 起始时间 yyyy-MM-dd HH:mm:ss
	 */
	private String startTimeString;
	/**
	 * 终止时间 毫秒
	 */
	private long endTime;
	/**
	 * 终止时间 yyyy-MM-dd HH:mm:ss
	 */
	private String endTimeString;
	/**
	 * 运行时长
	 */
	private String runDuration;
	/**
	 * 尝试连接次数
	 */
	private int tryToConnectCount = 0;
	/**
	 * 成功连接次数
	 */
	private int connectionCount = 0;
	/**
	 * 连接失败次数
	 */
	private int failToConnectCount = 0;
	/**
	 * 时间表现格式
	 */
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public final long getStartTime()
	{
		return startTime;
	}

	public final void setStartTime(long startTime)
	{
		this.startTime = startTime;

		//起始时间 yyyy-MM-dd HH:mm:ss
		this.startTimeString = df.format(startTime);
	}

	public final String getStartTimeString()
	{
		return startTimeString;
	}

	public final long getEndTime()
	{
		return endTime;
	}

	public final void setEndTime(long endTime)
	{
		this.endTime = endTime;

		//终止时间 yyyy-MM-dd HH:mm:ss
		this.endTimeString = df.format(endTime);

		//计算运行时长
		this.runDuration = NettyClientUtil.getFormatTime(this.endTime - this.startTime);
	}

	public final String getEndTimeString()
	{
		return endTimeString;
	}

	public final String getRunDuration()
	{
		return runDuration;
	}

	public final int getTryToConnectCount()
	{
		return tryToConnectCount;
	}

	public final void setTryToConnectCount(int tryToConnectCount)
	{
		this.tryToConnectCount = tryToConnectCount;
	}

	public final int getConnectionCount()
	{
		return connectionCount;
	}

	public final void setConnectionCount(int connectionCount)
	{
		this.connectionCount = connectionCount;
	}

	public final int getFailToConnectCount()
	{
		return failToConnectCount;
	}

	public final void setFailToConnectCount(int failToConnectCount)
	{
		this.failToConnectCount = failToConnectCount;
	}

	@Override
	public String toString()
	{
		return "总线程信息： [开始时间=" + startTimeString + ", 记录截止时间=" + endTimeString + ", 运行时长=" + runDuration + ", 尝试连接次数=" + tryToConnectCount
				+ ", 成功连接次数=" + connectionCount + ", 连接失败次数=" + failToConnectCount + "]";
	}
}
