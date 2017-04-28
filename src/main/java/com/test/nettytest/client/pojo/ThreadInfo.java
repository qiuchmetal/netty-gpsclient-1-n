package com.test.nettytest.client.pojo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.test.nettytest.client.util.NettyClientUtil;

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
	private List<Channel> channelList = new ArrayList<Channel>();
	/**
	 * 当前活跃连接数
	 */
	//private int currentActiveChannelCount;
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
	 * 模拟断开次数
	 */
	private int disconnectInRandomTimeCount = 0;
	/**
	 * 因没及时收到心跳而断开次数
	 */
	private int disconnectionOfHeartBeatCount = 0;
	/**
	 * 因没及时收到异常应答而断开次数
	 */
	private int disconnectionOfAbnormalCount = 0;
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
		this.runDuration = NettyClientUtil.getFormatTime(this.endTime - this.startTime);
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

	public final int getDisconnectInRandomTimeCount()
	{
		return disconnectInRandomTimeCount;
	}

	public final void setDisconnectInRandomTimeCount(int disconnectInRandomTimeCount)
	{
		this.disconnectInRandomTimeCount = disconnectInRandomTimeCount;
	}

	public final int getDisconnectionOfHeartBeatCount()
	{
		return disconnectionOfHeartBeatCount;
	}

	public final void setDisconnectionOfHeartBeatCount(int disconnectionOfHeartBeatCount)
	{
		this.disconnectionOfHeartBeatCount = disconnectionOfHeartBeatCount;
	}

	public final int getDisconnectionOfAbnormalCount()
	{
		return disconnectionOfAbnormalCount;
	}

	public final void setDisconnectionOfAbnormalCount(int disconnectionOfAbnormalCount)
	{
		this.disconnectionOfAbnormalCount = disconnectionOfAbnormalCount;
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

	public List<Channel> getChannelList()
	{
		return channelList;
	}

	public void setChannelList(Channel channel)
	{
		this.channelList.add(channel);
	}

	/**
	 * 计算当前活跃的连接数
	 */
	public final int getCurrentChannelActiveCount()
	{
		int cnt = 0;
		for (Channel c : channelList)
		{
			if (c != null && c.isActive())
				cnt++;
		}
		return cnt;
	}

	@Override
	public String toString()
	{
		return "线程信息： [线程ID=" + threadID + ", 开始时间=" + startTimeString + ", 记录截止时间=" + endTimeString + ", 运行时长=" + runDuration
				+ ", 当前活跃连接数=" + getCurrentChannelActiveCount() + ", 尝试连接次数=" + tryToConnectCount + ", 成功连接次数=" + connectionCount
				+ ", 连接失败次数=" + failToConnectCount + ", 断开次数=" + disconnectionCount + ", 模拟断开次数=" + disconnectInRandomTimeCount
				+ ", 因为没及时收到心跳而断开次数=" + disconnectionOfHeartBeatCount + ", 因为没及时收到异常应答而断开次数=" + disconnectionOfAbnormalCount
				+ ", 发送注册包个数=" + loginPackageCount + ", 发送定时定距包个数=" + timingPackageCount + ", 发送异常包个数=" + abnormalPackageCount
				+ ", 接收到的异常应答包个数=" + abnormalResponsePackageCount + ", 接收到心跳包个数=" + heartBeatPackageCount + "]";
	}
}
