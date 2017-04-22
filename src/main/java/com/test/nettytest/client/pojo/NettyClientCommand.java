package com.test.nettytest.client.pojo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.test.nettytest.client.util.NettyClientUtil;

public class NettyClientCommand
{
	/**
	 * 随机生成的车号
	 */
	private String busId;

	public NettyClientCommand()
	{
		//		this.busId = getBusIdHexStringByUUID();
		this.busId = getBusIdHexStringFromDeque();
	}

	/**
	 * 车号的生成：使用UUID产生随机5位车号，5位前3位是固定的“SZB”三个字母
	 */
	private String getBusIdHexStringByUUID()
	{
		String uuid = UUID.randomUUID().toString().toUpperCase();
		return NettyClientUtil.byteArrayToHexString(("SZB" + uuid.substring(uuid.length() - 5)).getBytes());
	}

	/**
	 * 车号字母
	 */
	private static String busIdLetters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * 根据车号段产生是车号列表
	 */
	public static ConcurrentLinkedDeque<String> busIdDeque = getBusIdHexStringBySegment();

	/**
	 * 根据配置文件分配的车号段来产生车号
	 */
	private static ConcurrentLinkedDeque<String> getBusIdHexStringBySegment()
	{
		String busIdSegment = NettyClientUtil.BUSID_SEGMENT;
		if (busIdSegment == null)
			busIdSegment = "0";

		ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<String>();

		StringBuilder sb = new StringBuilder();
		int len = busIdLetters.length();

		//循环下来，一共可得到 1679616 个车号
		for (int i2 = 0; i2 < len; i2++) //车牌左边第二位
		{
			for (int i3 = 0; i3 < len; i3++) //车牌左边第三位
			{
				for (int i4 = 0; i4 < len; i4++) //车牌左边第四位
				{
					for (int i5 = 0; i5 < len; i5++) //车牌左边第五位
					{
						sb.append("SZB") //车牌开头
								.append(busIdSegment) //车牌左边第一位
								.append(busIdLetters.charAt(i2)) //车牌左边第二位
								.append(busIdLetters.charAt(i3)) //车牌左边第三位
								.append(busIdLetters.charAt(i4)) //车牌左边第四位
								.append(busIdLetters.charAt(i5)); //车牌左边第五位
						//生成车牌，并加入队列
						deque.add(NettyClientUtil.byteArrayToHexString(sb.toString().getBytes()));
						sb.delete(0, sb.length()); //清空 StringBuilder
					}
				}
			}
		}

		return deque;
	}

	/**
	 * 从列表里获取车号
	 */
	private String getBusIdHexStringFromDeque()
	{
		return busIdDeque.pollFirst();
	}

	/**
	 * 实时获取注册信息
	 */
	public byte[] getLoginBytes()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("001000a7002002ff00000000000000005180000101");
		sb.append(this.busId);
		sb.append("303030305a000000010000000000000001070000000000000000000000000000000000");
		//sb.append(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));
		sb.append(new SimpleDateFormat("ddHHmmssSSSS").format(new Date()));
		sb.append("00000000000000008806ffff000003075431363132323739310000000000000000000000000000000000000000000000");
		sb.append(this.busId);
		sb.append("000000000000000000000000000000000000000000000000000300000700000000000000000000000000000000");

		//计算校验位
		try
		{
			sb.append(NettyClientUtil.getCheckString(sb.toString()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return NettyClientUtil.hexStringToByteArray("faf5" + sb.toString());
	}

	/**
	 * 实时获取定时定距信息
	 */
	public byte[] getTimingAndFixedDistanceBytes()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("00100052004102ff00000000000000005180000101");
		sb.append(this.busId);
		sb.append("3030383500000000000000000000000000500000000000000080000000000000000000");
		//sb.append(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));
		sb.append(new SimpleDateFormat("ddHHmmssSSSS").format(new Date()));
		sb.append("00000000000000009006FF0009975296");

		//计算校验位
		try
		{
			sb.append(NettyClientUtil.getCheckString(sb.toString()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return NettyClientUtil.hexStringToByteArray("faf5" + sb.toString());
	}

	/**
	 * 实时获取异常信息
	 */
	public byte[] getAbnormalBytes()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("00100059004502FF00000000000000005180000101");
		sb.append(this.busId);
		sb.append("3033303800000000000000000000000000190000000000000025011411671822334119");
		//sb.append(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));
		sb.append(new SimpleDateFormat("ddHHmmssSSSS").format(new Date()));
		//		sb.append(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));
		sb.append("00000663076000000004FF001591822601010109000800");

		//计算校验位
		try
		{
			sb.append(NettyClientUtil.getCheckString(sb.toString()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return NettyClientUtil.hexStringToByteArray("faf5" + sb.toString());
	}
}
