package com.test.nettytest.client.pojo;

import java.text.SimpleDateFormat;

import com.test.nettytest.client.util.NettyClientUtil;

/**
 * 每条线程收集到信息
 */
public class ThreadInfoStatistics
{
	/*
	 ************* 连接情况 ********************************
	 */
	/**
	 * 连接开始时间 毫秒
	 */
	private long connectionStartTime;
	/**
	 * 连接开始时间 yyyy-MM-dd HH:mm:ss
	 */
	private String connectionStartTimeString;
	/**
	 * 连接结束时间 毫秒
	 */
	private long connectionEndTime;
	/**
	 * 连接结束时间 yyyy-MM-dd HH:mm:ss
	 */
	private String connectionEndTimeString;
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
	/*
	 ************* 管道情况 ********************************
	 */
	/**
	 * 管道线程创建总数
	 */
	private int channelThreadsCount;
	/**
	 * 当前连接总数
	 */
	private int currentChannelActiveCount;
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

	/*
	 ************* 连接情况 ********************************
	 */
	public final long getConnectionStartTime()
	{
		return connectionStartTime;
	}

	public final void setConnectionStartTime(long connectionStartTime)
	{
		this.connectionStartTime = connectionStartTime;

		this.connectionStartTimeString = df.format(connectionStartTime);
	}

	public final String getConnectionStartTimeString()
	{
		return connectionStartTimeString;
	}

	public final void setConnectionEndTime(long connectionEndTime)
	{
		this.connectionEndTime = connectionEndTime;

		this.connectionEndTimeString = df.format(connectionEndTime);

		//计算运行时长
		this.runDuration = NettyClientUtil.getFormatTime(connectionEndTime - this.connectionStartTime);
	}	

	public final String getConnectionEndTimeString()
	{
		return connectionEndTimeString;
	}

//	public final void setRunDuration(String runDuration)
//	{
//		this.runDuration = runDuration;
//	}

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

	/*
	 ************* 管道断开情况 ********************************
	 */

	public final int getChannelThreadsCount()
	{
		return channelThreadsCount;
	}

	public final void setChannelThreadsCount(int channelThreadsCount)
	{
		this.channelThreadsCount = channelThreadsCount;
	}

	public final int getCurrentChannelActiveCount()
	{
		return currentChannelActiveCount;
	}

	public final void setCurrentChannelActiveCount(int currentChannelActiveCount)
	{
		this.currentChannelActiveCount = currentChannelActiveCount;
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
	 ************* 发包情况 ********************************
	 */

	public final int getLoginPackageSendCount()
	{
		return loginPackageSendCount;
	}

	public final void setLoginPackageSendCount(int loginPackageSendCount)
	{
		this.loginPackageSendCount = loginPackageSendCount;
	}

	public final int getLoginPackageReceivedCount()
	{
		return loginPackageReceivedCount;
	}

	public final void setLoginPackageReceivedCount(int loginPackageReceivedCount)
	{
		this.loginPackageReceivedCount = loginPackageReceivedCount;
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
		StringBuilder sb = new StringBuilder();
		sb.append("[创建连接的线程信息： 开始时间=" + connectionStartTimeString + ", 记录截止时间=" + connectionEndTimeString + ", 运行时长=" + runDuration
				+ ", 尝试连接次数=" + tryToConnectCount + ", 成功连接次数=" + connectionCount + ", 连接失败次数=" + failToConnectCount + "]\n\r\n\r");

		sb.append("[管道线程连接情况统计： 管道线程创建总数=" + channelThreadsCount + ", 当前连接总数=" + currentChannelActiveCount + ", 断开次数=" + disconnectionCount
				+ ", 模拟断开次数=" + disconnectInRandomTimeCount + ", 因为没及时收到心跳而断开次数=" + disconnectionOfHeartBeatCount + ", 因为没及时收到异常应答而断开次数="
				+ disconnectionOfAbnormalCount + "]\n\r\n\r");

		sb.append("[管道线程数据包情况统计：发送注册包个数=" + loginPackageSendCount + ", 接收到注册应答包个数=" + loginPackageReceivedCount + ", 发送定时定距包个数="
				+ timingPackageCount + ", 发送异常包个数=" + abnormalPackageCount + ", 接收到的异常应答包个数=" + abnormalResponsePackageCount + ", 接收到心跳包个数="
				+ heartBeatPackageCount + "]");

		return sb.toString();
	}
}
