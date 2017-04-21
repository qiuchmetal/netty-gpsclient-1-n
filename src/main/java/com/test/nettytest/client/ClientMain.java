package com.test.nettytest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.pojo.ThreadInfo;
import com.test.nettytest.client.task.CreateThreadInfoFileTask;

public class ClientMain
{

	public static void main(String[] args)
	{
		//线程信息统计
		List<ThreadInfo> threadInfoList = new ArrayList<ThreadInfo>();

		//		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NettyClientUtil.THREAD_POOL_SIZE);
		//		ExecutorService executorService = Executors.newFixedThreadPool(NettyClientUtil.THREAD_POOL_SIZE);

		//专门用来创建记录 ThreadInfo 信息的文件的线程
		ScheduledExecutorService createFileService = Executors.newScheduledThreadPool(1);
		//定时检查一次看是否有创建文件，正常情况下是每个小时创建一次
		createFileService.scheduleAtFixedRate(new CreateThreadInfoFileTask(threadInfoList), 0, 1, TimeUnit.MINUTES);

		System.out.println("即将开启的连接数：" + NettyClientUtil.THREAD_POOL_SIZE);

		//		//一条线程开一个连接的方式
		//		if ("1:1".equalsIgnoreCase(NettyClientUtil.RUN_TYPE))
		//		{
		//			for (int i = 0; i < NettyClientUtil.THREAD_POOL_SIZE; i++)
		//				executor.execute(new NettyClientInSingleConnetion(threadInfoList));
		//		}
		//		//一条线程开多个连接的方式
		//		else if ("1:n".equalsIgnoreCase(NettyClientUtil.RUN_TYPE))
		//		{
		//			for (int i = 0; i < NettyClientUtil.THREAD_POOL_SIZE; i++)
		//				executor.execute(new NettyClientInMultipleConnection());
		//		}
		//
		//		executor.shutdown();

		for (int i = 0; i < NettyClientUtil.THREAD_POOL_SIZE; i++)
			(new NettyClientConnetion(threadInfoList)).start();;
	}
}
