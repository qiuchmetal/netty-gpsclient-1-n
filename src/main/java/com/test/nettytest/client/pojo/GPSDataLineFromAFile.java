package com.test.nettytest.client.pojo;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.test.nettytest.client.util.NettyClientUtil;

/**
 * 从一个车号文件里加载的每一行的记录
 */
public class GPSDataLineFromAFile
{
	/**
	 * 接收包的时间
	 */
	private long receiveTimestamp;
	/**
	 * GPS 包里的原始时间，主要用于计算原始两个包之间的时间间隔
	 */
	private long gpsOriginalTimestamp;
	/**
	 * 改写后的 GPS 时间
	 */
	private long gpsCurrentTimestamp;
	/**
	 * 命令字
	 */
	private String command;
	/**
	 * GPS原字串
	 */
	private String gpsData;

	public final long getReceiveTimestamp()
	{
		return receiveTimestamp;
	}

	public final void setReceiveTimestamp(long receiveTimestamp)
	{
		this.receiveTimestamp = receiveTimestamp;
	}

	public final long getGpsOriginalTimestamp()
	{
		return gpsOriginalTimestamp;
	}

	public final void setGpsOriginalTimestamp(long gpsOriginalTimestamp)
	{
		this.gpsOriginalTimestamp = gpsOriginalTimestamp;
	}

	public final long getGpsCurrentTimestamp()
	{
		return gpsCurrentTimestamp;
	}

	public final void setGpsCurrentTimestamp(long gpsCurrentTimestamp)
	{
		this.gpsCurrentTimestamp = gpsCurrentTimestamp;
	}

	public final String getGpsData()
	{
		return gpsData;
	}

	public final void setGpsData(String gpsData)
	{
		this.gpsData = gpsData;
	}

	public final String getCommand()
	{
		return command;
	}

	public final void setCommand(String command)
	{
		this.command = command;
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");

	/**
	 * 一共处理3、32、64、65、66、67、68、69、70、72等10个指令。
	 */
	private String handleGpsData(long timestamp)
	{
		Date date = new Date(timestamp);
		String newGpsData = null;
		// 把当前时间数据替换进去
		switch (command)
		{
		case "3":
			newGpsData = gpsData.substring(4, 74) + sdf.format(date) + gpsData.substring(86, gpsData.length() - 4);
			break;
		default:
			newGpsData = gpsData.substring(4, 132) + sdf.format(date) + gpsData.substring(144, gpsData.length() - 4);
			break;
		}
		// 并更新 gpsCurrentTimestamp
		gpsCurrentTimestamp = timestamp;

		try
		{
			newGpsData = "faf5" + newGpsData + NettyClientUtil.getCheckString(newGpsData);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		System.out.println(newGpsData);

		return newGpsData;
	}

	/**
	 * 使用具体的时间戳来改写 GPS 原始数据包
	 */
	public byte[] getGpsDataBytesByCurrentTime(long timestamp)
	{
		return NettyClientUtil.hexStringToByteArray(handleGpsData(timestamp));
	}
}
