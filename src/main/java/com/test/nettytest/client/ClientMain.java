package com.test.nettytest.client;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.test.nettytest.client.pojo.ChannelThreadInfo;
import com.test.nettytest.client.pojo.ConnectionThreadInfo;
import com.test.nettytest.client.task.CreateThreadInfoFileTask;

public class ClientMain
{

	public static void main(String[] args)
	{
		//连接线程，只有一个对象
		ConnectionThreadInfo connectionThreadInfo = new ConnectionThreadInfo();
		//管道线程信息统计
		ConcurrentLinkedDeque<ChannelThreadInfo> channelThreadInfodDeque = new ConcurrentLinkedDeque<ChannelThreadInfo>();

		//专门用来创建记录 ThreadInfo 信息的文件的线程
		ScheduledExecutorService createFileService = Executors.newScheduledThreadPool(1);
		//定时检查一次看是否有创建文件，正常情况下是每个小时创建一次
		createFileService.scheduleAtFixedRate(new CreateThreadInfoFileTask(connectionThreadInfo, channelThreadInfodDeque), 0, 1, TimeUnit.MINUTES);
		
//		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
//		System.out.println("[" + Thread.currentThread().getName() + "] [" + df.format(new Date()) + "] 即将开启的线程数：[" + NettyClientUtil.THREAD_POOL_SIZE + "]  连接数：["
//				+ NettyClientUtil.THREAD_POOL_SIZE * NettyClientUtil.PER_THREAD_CONNETIONS + "]");

//		for (int i = 0; i < NettyClientUtil.THREAD_POOL_SIZE; i++)
//		(new NettyClientConnetion(threadInfoList)).start();
	
		new NettyClientConnetion(connectionThreadInfo, channelThreadInfodDeque).start();

		//		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NettyClientUtil.THREAD_POOL_SIZE);
		//		ExecutorService executorService = Executors.newFixedThreadPool(NettyClientUtil.THREAD_POOL_SIZE);		
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
	}
}
