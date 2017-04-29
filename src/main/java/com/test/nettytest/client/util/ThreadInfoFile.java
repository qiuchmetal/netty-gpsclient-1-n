package com.test.nettytest.client.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 用来记录管道线程信息的文件类
 */
public class ThreadInfoFile
{
	public static void writeToTxtFile(String content)
	{
		Date date = new Date();
		
		String fileName = (new SimpleDateFormat("yyyy-MM-dd-HH")).format(new Date());
		//查看是否已经创建了文件，这里不负责创建文件，创建文件在 CreateThreadInfoFileTask 类里
		File file = new File("ChannelThreadInfo_" + fileName + ".txt");
		try
		{
			if (!file.exists())
			{
//				file.createNewFile();
				return;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		try
		{
			//写入文件  
			FileWriter writer = new FileWriter(file,true);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(content+"\n\r\n\r\n\r\n\r");
			bufferedWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
