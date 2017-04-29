package com.test.nettytest.client.pojo;

import java.text.SimpleDateFormat;

import com.test.nettytest.client.util.NettyClientUtil;

import io.netty.channel.Channel;

/**
 * 管道线程信息
 */
public class ChannelThreadInfo
{
	/**
	 * 线程ID
	 */
	private String threadID;
	/**
	 * 当前管道连接
	 */
	private Channel channel;
	//	private ConcurrentSkipListSet<Channel> channelSet = new ConcurrentSkipListSet<Channel>();
	//	private List<Channel> channelList = new ArrayList<Channel>();
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
	private long endTime = 0;
	/**
	 * 终止时间 yyyy-MM-dd HH:mm:ss
	 */
	private String endTimeString;
	/**
	 * 运行时长
	 */
	private String runDuration;
	/*
	 ***************************************************
	 */
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
	/*
	 ***************************************************
	 */
	/**
	 * 发送的注册包个数
	 */
	private int loginPackageSendCount = 0;
	/**
	 * 接收的注册应答包个数
	 */
	private int loginPackageReceivedCount = 0;
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

		//起始时间 yyyy-MM-dd HH:mm:ss
		this.startTimeString = df.format(startTime);
	}

	public final String getStartTimeString()
	{
		return startTimeString;
	}

	public final void setEndTime(long endTime)
	{
		if (this.endTime == 0)
		{
			this.endTime = endTime;

			//终止时间 yyyy-MM-dd HH:mm:ss
			this.endTimeString = df.format(endTime);

			//计算运行时长
			this.runDuration = NettyClientUtil.getFormatTime(this.endTime - this.startTime);
		}
	}

	public final String getEndTimeString()
	{
		return endTimeString;
	}

	public final String getRunDuration()
	{
		return runDuration;
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

	/*
	 ***************************************************
	 */

	public final int getLoginPackageSendCount()
	{
		return loginPackageSendCount;
	}

//	public final void setLoginPackageSendCount(int loginPackageSendCount)
//	{
//		this.loginPackageSendCount = loginPackageSendCount;
//	}

	public final void incrementLoginPackageSendCount()
	{
		this.loginPackageSendCount++;
	}

	public final int getLoginPackageReceivedCount()
	{
		return loginPackageReceivedCount;
	}

//	public final void setLoginPackageReceivedCount(int loginPackageReceivedCount)
//	{
//		this.loginPackageReceivedCount = loginPackageReceivedCount;
//	}

	public final void incrementLoginPackageReceivedCount()
	{
		this.loginPackageReceivedCount++;
	}

	public final int getTimingPackageCount()
	{
		return timingPackageCount;
	}

//	public final void setTimingPackageCount(int timingPackageCount)
//	{
//		this.timingPackageCount = timingPackageCount;
//	}

	public final void incrementTimingPackageCount()
	{
		this.timingPackageCount++;
	}

	public final int getAbnormalPackageCount()
	{
		return abnormalPackageCount;
	}

//	public final void setAbnormalPackageCount(int abnormalPackageCount)
//	{
//		this.abnormalPackageCount = abnormalPackageCount;
//	}

	public final void incrementAbnormalPackageCount()
	{
		this.abnormalPackageCount++;
	}

	public final int getAbnormalResponsePackageCount()
	{
		return abnormalResponsePackageCount;
	}

//	public final void setAbnormalResponsePackageCount(int abnormalResponsePackageCount)
//	{
//		this.abnormalResponsePackageCount = abnormalResponsePackageCount;
//	}

	public final void incrementAbnormalResponsePackageCount()
	{
		this.abnormalResponsePackageCount++;
	}

	public final int getHeartBeatPackageCount()
	{
		return heartBeatPackageCount;
	}

//	public final void setHeartBeatPackageCount(int heartBeatPackageCount)
//	{
//		this.heartBeatPackageCount = heartBeatPackageCount;
//	}

	public final void incrementHeartBeatPackageCount()
	{
		this.heartBeatPackageCount++;
	}

	//	public List<Channel> getChannelList()
	//	{
	//		return channelList;
	//	}
	//
	//	public void setChannelList(Channel channel)
	//	{
	//		this.channelList.add(channel);
	//	}
	//
	//	/**
	//	 * 计算当前活跃的连接数
	//	 */
	//	public final int getCurrentChannelActiveCount()
	//	{
	//		int cnt = 0;
	//		//		for (Channel c : channelList)
	//		//		{
	//		//			if (c != null && c.isActive())
	//		//				cnt++;
	//		//		}
	//		for (Channel c : channelSet)
	//		{
	//			if (c != null && c.isActive())
	//				cnt++;
	//		}
	//		return cnt;
	//	}

	@Override
	public String toString()
	{
		if (channel.isActive())
			return "子线程信息： [线程ID=" + threadID + ", 连接状态=在线" + ", 开始时间=" + startTimeString + ", 记录截止时间=" + endTimeString
					+ ", 运行时长=" + runDuration + ", 断开次数=" + disconnectionCount + ", 模拟断开次数="
					+ disconnectInRandomTimeCount + ", 因为没及时收到心跳而断开次数=" + disconnectionOfHeartBeatCount
					+ ", 因为没及时收到异常应答而断开次数=" + disconnectionOfAbnormalCount + ", 发送注册包个数=" + loginPackageSendCount
					+ ", 接收到注册应答包个数=" + loginPackageReceivedCount + ", 发送定时定距包个数=" + timingPackageCount + ", 发送异常包个数="
					+ abnormalPackageCount + ", 接收到的异常应答包个数=" + abnormalResponsePackageCount + ", 接收到心跳包个数="
					+ heartBeatPackageCount + "]";
		else
			return "子线程信息： [线程ID=" + threadID + ", 连接状态=失效" + ", 开始时间=" + startTimeString + ", 失效时间=" + endTimeString
					+ ", 运行时长=" + runDuration + ", 断开次数=" + disconnectionCount + ", 模拟断开次数="
					+ disconnectInRandomTimeCount + ", 因为没及时收到心跳而断开次数=" + disconnectionOfHeartBeatCount
					+ ", 因为没及时收到异常应答而断开次数=" + disconnectionOfAbnormalCount + ", 发送注册包个数=" + loginPackageSendCount
					+ ", 接收到注册应答包个数=" + loginPackageReceivedCount + ", 发送定时定距包个数=" + timingPackageCount + ", 发送异常包个数="
					+ abnormalPackageCount + ", 接收到的异常应答包个数=" + abnormalResponsePackageCount + ", 接收到心跳包个数="
					+ heartBeatPackageCount + "]";
	}
}
