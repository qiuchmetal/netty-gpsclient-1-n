package com.test.nettytest.client.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.primitives.Bytes;

public class NettyClientUtil
{
	/**
	 * 要连接的 Netty Server IP
	 */
	public static final String SERVER_IP = getPropertiesValueByKey("netty-server");
	/**
	 * 要连接的 Netty Server port
	 */
	public static final int SERVER_PORT = Integer.parseInt(getPropertiesValueByKey("netty-port"));
	/**
	 * Netty Client 的启动参数 线程连接池大小
	 */
	public static final int THREAD_POOL_SIZE = Integer.parseInt(getPropertiesValueByKey("netty-client-threadpoolsize"));
	/**
	 * Netty Client 的启动参数 每条线程要启动的连接数
	 */
	public static final int PER_THREAD_CONNETIONS = Integer.parseInt(getPropertiesValueByKey("netty-client-perthreadconnection"));
	/**
	 * 车号生成段
	 */
	public static final String BUSID_SEGMENT = getPropertiesValueByKey("netty-client-busidsegment").toUpperCase();
	/**
	 * 发送注册信息间隔（秒）
	 */
	public static final int LOGIN_INTERVAL = Integer.parseInt(getPropertiesValueByKey("netty-client-logininterval"));
	/**
	 * 发送定时定距间隔（秒）
	 */
	public static final int TIMING_INTERVAL = Integer.parseInt(getPropertiesValueByKey("netty-client-timinginterval"));
	/**
	 * 发送定时定距是否需要固定时间间隔
	 */
	public static final int TIMING_INTERVAL_FIXED = Integer.parseInt(getPropertiesValueByKey("netty-client-timinginterval-fixed"));
	/**
	 * 心跳超时（秒）
	 */
	public static final int HEARTBEAT_TIMEOUT = Integer.parseInt(getPropertiesValueByKey("netty-client-heartbeattimeout"));
	/**
	 * 发送异常信息间隔（秒）
	 */
	public static final int ABNORMAL_INTERVAL = Integer.parseInt(getPropertiesValueByKey("netty-client-abnormalinterval"));
	/**
	 * 模拟断开连接的时间范围（分钟）
	 */
	public static final int DISCONNECT_RANGE = Integer.parseInt(getPropertiesValueByKey("netty-client-disconnectrange"));
	
	/**
	 * 注册信息
	 */
	//	public static final String LOGIN_STRING = "faf5001000a7002002ff000000000000000051800001004253313131313144303030305a0000000100000000000000010700000000000000000000000000000000017022311285100000000000000008806ffff00000307543136313232373931000000000000000000000000000000000000000000000042533131313131440000000000000000000000000000000000000000000000000003000007000000000000000000000000000000000cc0";
	//	public static final byte[] LOGIN_BYTES = { -6, -11, 0, 16, 0, -89, -43, 32, 2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 81, -128,
	//			0, 1, 0, 66, 83, 49, 49, 49, 49, 49, 68, 48, 48, 48, 48, 90, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0,
	//			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 4, 9, 22, 84, 49, 0, 0, 0, 0, 0, 0, 0, 0, -116, 6, -1, -1, 0,
	//			0, 8, 56, 84, 49, 54, 49, 50, 50, 55, 57, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	//			0, 0, 66, 83, 49, 49, 49, 49, 49, 68, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	//			0, 0, 3, 0, 0, 7, 0, 52, 54, 48, 48, 49, 57, 55, 49, 51, 55, 50, 56, 53, 52, 48, 16, -53 };
	/**
	 * 0~9: 48~57
	 * A~Z: 65~90
	 */
	/**
	 * 定时定距
	 */
	//	public static final String TIMINGANDFIXEDDISTANCE_STRING = "faf500100052024102ff00000000000000005180000101535A42434D313738303038350000000000000000000000000050000000000000008000000000000000000017010313163900000000000000009006FF000997529609cf";
	//	public static final byte[] TIMINGANDFIXEDDISTANCE_BYTES = { -6, -11, 0, 16, 0, 82, 2, 65, 2, -1, 0, 0, 0, 0, 0, 0,
	//			0, 0, 81, -128, 0, 1, 1, 83, 90, 66, 67, 77, 49, 55, 56, 48, 48, 56, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	//			0, 80, 0, 0, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 1, 3, 19, 22, 57, 0, 0, 0, 0, 0, 0, 0, 0,
	//			-112, 6, -1, 0, 9, -105, 82, -106, 9, -49 };

	/**
	 * 异常信息
	 */
	//	public static final String ABNORMAL_STRING = "FAF5001000593C4502FF00000000000000005180000101535A42434D323337303330380000000000000000000000000019000000000000002501141167182233411917010406510700000663076000000004FF0015918226010101090008000AE5";
	//	public static final byte[] ABNORMAL_BYTES = { -6, -11, 0, 16, 0, 89, 60, 69, 2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 81,
	//			-128, 0, 1, 1, 83, 90, 66, 67, 77, 50, 51, 55, 48, 51, 48, 56, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 25, 0,
	//			0, 0, 0, 0, 0, 0, 37, 1, 20, 17, 103, 24, 34, 51, 65, 25, 23, 1, 4, 6, 81, 7, 0, 0, 6, 99, 7, 96, 0, 0, 0,
	//			4, -1, 0, 21, -111, -126, 38, 1, 1, 1, 9, 0, 8, 0, 10, -27 };

	/**
	 * 字节数组转换成 16 进制
	 */
	public static String byteArrayToHexString(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
		{
			String tmp = Integer.toHexString(b);
			if (tmp.length() > 1)
				sb.append(tmp.substring(tmp.length() - 2));
			else
				sb.append("0").append(tmp);
		}
		return sb.toString();
	}

	/**
	 * 计算一串16进制字符串的无进位累加和
	 */
	public static String getCheckString(String hexString) throws Exception
	{
		int len = hexString.length();
		if (len <= 0 || len % 2 != 0) // 长度为0或基数时，不合法
		{
			throw new Exception("输入的字符串有误！ ");
		}

		int sum = 0;
		for (int i = 0; i < len; i += 2)
		{
			int i1 = Integer.parseInt(hexString.substring(i, i + 2), 16);
			sum += i1;
		}

		String result = Integer.toHexString(sum);

		if (result.length() == 1)
			return "000" + result;
		else if (result.length() == 2)
			return "00" + result;
		else if (result.length() == 3)
			return "0" + result;
		else
			return result;
	}

	/**
	 * 将输入的16进制字符串转换为用于网络通信的字节数据
	 */
	public static byte[] hexStringToByteArray(String hexString)
	{
		int len = hexString.length();
		if (len <= 0 || len % 2 != 0) // 长度为0或基数时，不合法
		{
			//			throw new Exception("输入的16进制字符串有误！ ");
			return null;
		}

		List<Byte> list = new ArrayList<Byte>();
		for (int i = 0; i < len; i += 2)
		{
			list.add((byte) Integer.parseInt(hexString.substring(i, i + 2), 16));
		}
		return Bytes.toArray(list);
	}

	/**
	 * 车号字符库
	 */
	//private static final String BUS_ID_LETTER = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * 加载配置文件里的配置信息
	 */
	private static String getPropertiesValueByKey(String key)
	{
		//		GpsUtil.class.getResource("");
		//		System.out.println("1 "+GpsUtil.class.getResource("/").getPath());
		//		System.out.println("2 "+GpsUtil.class.getResource("").getPath());
		//		
		//		System.out.println("3 "+GpsUtil.class.getClassLoader().getResource("/").getPath());
		//		System.out.println("4 "+GpsUtil.class.getClassLoader().getResource("").getPath());
		//
		//		return "";

		//获取 jar 包所在目录
		String jarPath = (new File(NettyClientUtil.class.getProtectionDomain().getCodeSource().getLocation().getFile())).getParent();
		//				System.out.println(jarPath);
		//				return "";
		String filePath = jarPath + System.getProperty("file.separator") + "netty-client.properties";
		//				System.out.println(filePath);

		//				return "";

		Properties pps = new Properties();
		InputStream in = null;
		try
		{
			//这个方式是获取在 jar 包内的根目录下的配置文件方式
//			in = NettyClientUtil.class.getClassLoader().getResourceAsStream("netty-client.properties");

			//这个方式是获取与 jar 包同路径下的配置文件方式
//			in = new BufferedInputStream(new FileInputStream(filePath));

			//这个方法可以把配置文件放在工程的 resources 目录下
			in = new BufferedInputStream(new FileInputStream(NettyClientUtil.class.getResource("/").getPath() + "netty-client.properties"));
			
			pps.load(in);
			return pps.getProperty(key);
		}
		catch (Exception e)
		{
			try
			{
				if (in != null)
					in.close();
			}
			catch (IOException e1)
			{
				in = null;
			}
			return null;
		}
		finally
		{
			try
			{
				if (in != null)
					in.close();
			}
			catch (IOException e)
			{
				in = null;
			}
		}
	}
}
