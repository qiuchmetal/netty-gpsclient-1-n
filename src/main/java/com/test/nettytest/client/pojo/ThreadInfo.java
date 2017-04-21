package com.test.nettytest.client.pojo;

import java.text.SimpleDateFormat;

import io.netty.channel.Channel;

/**
 * 每条线程收集到信息
 */
public class ThreadInfo
{
	/**
	 * 线程ID
	 */
	private String threadID;
	/**
	 * 当前连接
	 */
	private Channel channel;
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
	 * 断开次数
	 */
	private int disconnectionCount = 0;
	/**
	 * 发送的注册包个数
	 */
	private int loginPackageCount = 0;
	/**
	 * 发送的定时定距包个数
	 */
	private int timingPackageCount = 0;
	/**
	 * 发送的异常包个数
	 */
	private int abnormalPackageCount = 0;
	/**
	 * 接收到的异常应答包个数
	 */
	private int abnormalResponsePackageCount = 0;
	/**
	 * 接收到的心跳个数
	 */
	private int heartBeatPackageCount = 0;

	/**
	 * 时间表现格式
	 */
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public final String getThreadID()
	{
		return threadID;
	}

	public final void setThreadID(String threadID)
	{
		this.threadID = threadID;
	}

	public final Channel getChannel()
	{
		return channel;
	}

	public final void setChannel(Channel channel)
	{
		this.channel = channel;
	}

	public final long getStartTime()
	{
		return startTime;
	}

	public final void setStartTime(long startTime)
	{
		this.startTime = startTime;

		this.startTimeString = df.format(startTime);
	}

	public final String getStartTimeString()
	{
		return startTimeString;
	}

	//	public final void setStartTimeString(String startTimeString)
	//	{
	//		this.startTimeString = startTimeString;
	//	}
	//	public final Date getEndTime()
	//	{
	//		return endTime;
	//	}
	public final void setEndTime(long endTime)
	{
		this.endTime = endTime;

		this.endTimeString = df.format(endTime);

		//计算运行时长
		this.runDuration = getFormatTime(this.endTime - this.startTime);
	}

	public final String getEndTimeString()
	{
		return endTimeString;
	}

	//	public final void setEndTimeString(String endTimeString)
	//	{
	//		this.endTimeString = endTimeString;
	//	}
	public final String getRunDuration()
	{
		return runDuration;
	}

	//	public final void setRunDuration(String runDuration)
	//	{
	//		this.runDuration = runDuration;
	//	}

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

	public final int getDisconnectionCount()
	{
		return disconnectionCount;
	}

	public final void setDisconnectionCount(int disconnectionCount)
	{
		this.disconnectionCount = disconnectionCount;
	}

	public final int getLoginPackageCount()
	{
		return loginPackageCount;
	}

	public final void setLoginPackageCount(int loginPackageCount)
	{
		this.loginPackageCount = loginPackageCount;
	}

	public final int getTimingPackageCount()
	{
		return timingPackageCount;
	}

	public final void setTimingPackageCount(int timingPackageCount)
	{
		this.timingPackageCount = timingPackageCount;
	}

	public final int getAbnormalPackageCount()
	{
		return abnormalPackageCount;
	}

	public final void setAbnormalPackageCount(int abnormalPackageCount)
	{
		this.abnormalPackageCount = abnormalPackageCount;
	}

	public final int getAbnormalResponsePackageCount()
	{
		return abnormalResponsePackageCount;
	}

	public final void setAbnormalResponsePackageCount(int abnormalResponsePackageCount)
	{
		this.abnormalResponsePackageCount = abnormalResponsePackageCount;
	}

	public final int getHeartBeatPackageCount()
	{
		return heartBeatPackageCount;
	}

	public final void setHeartBeatPackageCount(int heartBeatPackageCount)
	{
		this.heartBeatPackageCount = heartBeatPackageCount;
	}

	@Override
	public String toString()
	{
		return "线程信息： [线程ID=" + threadID + ", 开始时间=" + startTimeString + ", 记录截止时间=" + endTimeString + ", 运行时长=" + runDuration + ", 当前连接状态="
				+ ((channel != null && channel.isActive()) ? "已连接" : "未连接") + ", 尝试连接次数=" + tryToConnectCount + ", 成功连接次数="
				+ connectionCount + ", 连接失败次数=" + failToConnectCount + ", 断开次数=" + disconnectionCount + ", 发送注册包个数=" + loginPackageCount
				+ ", 发送定时定距包个数=" + timingPackageCount + ", 发送异常包个数=" + abnormalPackageCount + ", 接收到的异常应答包个数="
				+ abnormalResponsePackageCount + ", 接收到心跳包个数=" + heartBeatPackageCount + "]";
	}

	/**
	 * 运行时长格式 xx小时xx分xx秒
	 */
	private String getFormatTime(long duration)
	{
		StringBuilder sb = new StringBuilder();
		duration = duration / 1000;

		//取秒
		int s = (int) (duration % 60);

		sb.append(s + "秒");

		//取分钟
		int m = (int) (duration - s > 0 ? (duration - s) / 60 : 0);

		if (m > 0)
		{
			if (m >= 60)
			{
				//取小时
				int h = (m - m % 60) / 60;
				m = m % 60;
				sb.insert(0, h + "小时" + m + "分钟");
			}
			else
			{
				sb.insert(0, m + "分钟");
			}
		}

		return sb.toString();
	}
}
