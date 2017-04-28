package com.test.nettytest.client.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 用来记录线程信息的文件类
 */
public class ThreadInfoFile
{
	public static void writeToTxtFile(String content)
	{
		Date date = new Date();
//		String h = (new SimpleDateFormat("HH")).format(date).substring(0, 1);
//		String m = (new SimpleDateFormat("mm")).format(date).substring(0, 1);
		
		String fileName = (new SimpleDateFormat("yyyy-MM-dd-HH")).format(new Date());
		//创建文件
		File file = new File("ThreadInfo_" + fileName + ".txt");
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

		
		
//		FileOutputStream o = null;
		try
		{
			//写入文件  
			FileWriter writer = new FileWriter(file,true);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(content+"\n\r\n\r\n\r\n\r");
			bufferedWriter.close();
			
			
//			o = new FileOutputStream(file);
//			o.write(content.getBytes("GBK"));
//			o.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
