package org.springblade.modules.video.thread;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.modules.video.cache.CacheUtil;
import org.springblade.modules.video.controller.VideoController;
import org.springblade.modules.video.entity.CameraPojo;
import org.springblade.modules.video.util.CameraPush;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @ClassName :  CameraThread
 * @Author :   xcuni dingxb
 * @Date :   2020/6/17
 * @Time :   14:32
 * @Decription :   将推流任务加入线程中并添加至缓存
 */
public class CameraThread {

	private final static Logger logger = LoggerFactory.getLogger(CameraThread.class);

	public static class MyRunnable implements Runnable {

		// 创建线程池
		public static ExecutorService es = Executors.newCachedThreadPool();

		private CameraPojo cameraPojo;
		private Thread nowThread;
		private CameraPush push;


		public MyRunnable(CameraPojo cameraPojo) {
			this.cameraPojo = cameraPojo;
		}

		public CameraPush getCameraPush(){
			return this.push;
		}

		// 中断线程
		public void setInterrupted() {
			nowThread.interrupt();
		}
		@Override
		public void run() {
			// 直播流
			try {
				// 获取当前线程存入缓存
				nowThread = Thread.currentThread();
				CacheUtil.STREAMMAP.put(cameraPojo.getToken(), cameraPojo);
				// 执行转流推流任务
				push = new CameraPush(cameraPojo).from();
				if (push != null) {
					push.to().go(nowThread);
				}
				// 清除缓存
				CacheUtil.STREAMMAP.remove(cameraPojo.getToken());
				VideoController.jobMap.remove(cameraPojo.getToken());
			} catch (Exception e) {
				logger.error("当前任务： " + cameraPojo.getRtsp() + "停止...");
				CacheUtil.STREAMMAP.remove(cameraPojo.getToken());
				VideoController.jobMap.remove(cameraPojo.getToken());
			}
		}
	}
}
