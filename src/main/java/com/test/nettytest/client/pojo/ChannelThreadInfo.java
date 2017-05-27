package com.test.nettytest.client.pojo;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

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
	// private ConcurrentSkipListSet<Channel> channelSet = new
	// ConcurrentSkipListSet<Channel>();
	// private List<Channel> channelList = new ArrayList<Channel>();
	/**
	 * 当前活跃连接数
	 */
	// private int currentActiveChannelCount;
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
	private AtomicInteger disconnectionCount = new AtomicInteger(0);
	/**
	 * 模拟断开次数
	 */
	private AtomicInteger disconnectInRandomTimeCount = new AtomicInteger(0);
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
	private AtomicInteger loginCount = new AtomicInteger(0);
	/**
	 * 接收的注册应答包个数
	 */
	private AtomicInteger loginResponseCount = new AtomicInteger(0);
	/**
	 * 发送的准备就绪包个数
	 */
	private AtomicInteger inReadyCount = new AtomicInteger(0);
	/**
	 * 发送的定时定距包个数
	 */
	private AtomicInteger timingCount = new AtomicInteger(0);
	/**
	 * 发送的校时请求包个数
	 */
	private AtomicInteger adjustTimeCount = new AtomicInteger(0);
	/**
	 * 接收到的校时应答包个数
	 */
	private AtomicInteger adjustTimeResponseCount = new AtomicInteger(0);
	/**
	 * 发送的关键数据包个数
	 */
	private AtomicInteger abnormalCount = new AtomicInteger(0);
	/**
	 * 接收到的关键数据应答包个数
	 */
	private AtomicInteger abnormalResponseCount = new AtomicInteger(0);
	/**
	 * 接收到的心跳个数
	 */
	private AtomicInteger heartBeatCount = new AtomicInteger(0);
	/*
	 ***************************************************
	 */
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

		// 起始时间 yyyy-MM-dd HH:mm:ss
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

			// 终止时间 yyyy-MM-dd HH:mm:ss
			this.endTimeString = df.format(endTime);

			// 计算运行时长
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

	/*
	 ***************************************************
	 */

	public final int getDisconnectionCount()
	{
		return disconnectionCount.get();
	}

//	public final void setDisconnectionCount(int disconnectionCount)
//	{
//		this.disconnectionCount = disconnectionCount;
//	}

	public final void incrementDisconnectionCount()
	{
		this.disconnectionCount.incrementAndGet();
	}

	public final int getDisconnectInRandomTimeCount()
	{
		return disconnectInRandomTimeCount.get();
	}

//	public final void setDisconnectInRandomTimeCount(int disconnectInRandomTimeCount)
//	{
//		this.disconnectInRandomTimeCount = disconnectInRandomTimeCount;
//	}

	public final void incrementDisconnectInRandomTimeCount()
	{
		this.disconnectInRandomTimeCount.incrementAndGet();
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

	public final int getLoginCount()
	{
		return loginCount.get();
	}

	public final void incrementLoginCount()
	{
		this.loginCount.incrementAndGet();
	}

	public final int getLoginResponseCount()
	{
		return loginResponseCount.get();
	}

	public final void incrementLoginResponseCount()
	{
		this.loginResponseCount.incrementAndGet();
	}

	public final int getTimingCount()
	{
		return timingCount.get();
	}

	public final void incrementTimingCount()
	{
		this.timingCount.incrementAndGet();
	}

	public final int getAbnormalCount()
	{
		return abnormalCount.get();
	}

	public final void incrementAbnormalCount()
	{
		this.abnormalCount.incrementAndGet();
	}

	public final int getAbnormalResponseCount()
	{
		return abnormalResponseCount.get();
	}

	public final void incrementAbnormalResponseCount()
	{
		this.abnormalResponseCount.incrementAndGet();
	}

	public final int getAdjustTimeCount()
	{
		return adjustTimeCount.get();
	}

	public final void incrementAdjustTimeCount()
	{
		this.adjustTimeCount.incrementAndGet();
	}

	public final int getAdjustTimeResponseCount()
	{
		return adjustTimeResponseCount.get();
	}

	public final void incrementAdjustTimeResponseCount()
	{
		this.adjustTimeResponseCount.incrementAndGet();
	}

	public final int getHeartBeatCount()
	{
		return heartBeatCount.get();
	}

	public final void incrementHeartBeatCount()
	{
		this.heartBeatCount.incrementAndGet();
	}
	
	public final int getInReadyCount()
	{
		return inReadyCount.get();
	}

	public final void incrementInReadyCount()
	{
		this.inReadyCount.incrementAndGet();
	}
	

	/**
	 * 根据不同的命令，统计不同的数据包
	 */
	public final void setPackageCount(String command)
	{
		switch (command)
		{
		case "3": // 校时请求
			this.adjustTimeCount.incrementAndGet();
			break;
		case "-125":// 校时应答
			this.adjustTimeResponseCount.incrementAndGet();
			break;
		case "32": // 注册请求
			this.loginCount.incrementAndGet();
			break;
		case "-96":// 注册应答
			this.loginResponseCount.incrementAndGet();
			break;
		case "64": // 准备就绪
			this.inReadyCount.incrementAndGet();
			break;
		case "65": // 定时定距
			this.timingCount.incrementAndGet();
			break;
		case "66": // 关键数据：到站、离站信息
		case "67": // 关键数据：到离始发站、终点站信息
		case "68": // 关键数据：进出停场信息
		case "69": // 关键数据：终端异常报警信息
		case "70": // 关键数据：请求及报告信息发送
		case "72": // 关键数据：IC卡操作信息
			this.abnormalCount.incrementAndGet();
			break;
		case "-63": // 关键数据应答
			this.abnormalResponseCount.incrementAndGet();
			break;
		case "1": //接收到的心跳
			this.heartBeatCount.incrementAndGet();
			break;
		}

	}

	// public List<Channel> getChannelList()
	// {
	// return channelList;
	// }
	//
	// public void setChannelList(Channel channel)
	// {
	// this.channelList.add(channel);
	// }
	//
	// /**
	// * 计算当前活跃的连接数
	// */
	// public final int getCurrentChannelActiveCount()
	// {
	// int cnt = 0;
	// // for (Channel c : channelList)
	// // {
	// // if (c != null && c.isActive())
	// // cnt++;
	// // }
	// for (Channel c : channelSet)
	// {
	// if (c != null && c.isActive())
	// cnt++;
	// }
	// return cnt;
	// }

//	@Override
//	public String toString()
//	{
//		if (channel.isActive())
//			return "子线程信息： [线程ID=" + threadID + ", 连接状态=在线" + ", 开始时间=" + startTimeString + ", 记录截止时间=" + endTimeString + ", 运行时长="
//					+ runDuration + ", 断开次数=" + disconnectionCount + ", 模拟断开次数=" + disconnectInRandomTimeCount + ", 因为没及时收到心跳而断开次数="
//					+ disconnectionOfHeartBeatCount + ", 因为没及时收到异常应答而断开次数=" + disconnectionOfAbnormalCount + ", 发送注册包个数="
//					+ loginPackageSendCount + ", 接收到注册应答包个数=" + loginPackageReceivedCount + ", 发送定时定距包个数=" + timingPackageCount
//					+ ", 发送异常包个数=" + abnormalPackageCount + ", 接收到的异常应答包个数=" + abnormalResponsePackageCount + ", 接收到心跳包个数="
//					+ heartBeatPackageCount + "]";
//		else
//			return "子线程信息： [线程ID=" + threadID + ", 连接状态=失效" + ", 开始时间=" + startTimeString + ", 失效时间=" + endTimeString + ", 运行时长="
//					+ runDuration + ", 断开次数=" + disconnectionCount + ", 模拟断开次数=" + disconnectInRandomTimeCount + ", 因为没及时收到心跳而断开次数="
//					+ disconnectionOfHeartBeatCount + ", 因为没及时收到异常应答而断开次数=" + disconnectionOfAbnormalCount + ", 发送注册包个数="
//					+ loginPackageSendCount + ", 接收到注册应答包个数=" + loginPackageReceivedCount + ", 发送定时定距包个数=" + timingPackageCount
//					+ ", 发送异常包个数=" + abnormalPackageCount + ", 接收到的异常应答包个数=" + abnormalResponsePackageCount + ", 接收到心跳包个数="
//					+ heartBeatPackageCount + "]";
//	}
}
