package org.springblade.modules.video.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.modules.video.cache.CacheUtil;
import org.springblade.modules.video.controller.VideoController;
import org.springblade.modules.video.entity.CameraPojo;
import org.springblade.modules.video.entity.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @ClassName :  TimerUtil
 * @Author :   xcuni dingxb
 * @Date :   2020/6/17
 * @Time :   20:54
 * @Decription :   定时任务
 */
@Component
@Order(value = 1)
public class TimerUtil implements CommandLineRunner {

	private final static Logger logger = LoggerFactory.getLogger(TimerUtil.class);

	@Autowired
	private Config config;// 配置文件bean

	public static Timer timer;

	@Override
	public void run(String... args) throws Exception {
//		// 超过5分钟，结束推流
		timer = new Timer("timeTimer");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				//logger.info("******   执行定时任务       BEGIN   ******");
				// 管理缓存
				if (null != CacheUtil.STREAMMAP && 0 != CacheUtil.STREAMMAP.size()) {
					Iterator<Map.Entry<String, CameraPojo>> iterator = CacheUtil.STREAMMAP.entrySet().iterator();
					while(iterator.hasNext()){
						Map.Entry<String, CameraPojo> next = iterator.next();
						String key = next.getKey();
						try {
							// 最后打开时间
							long openTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
								.parse(CacheUtil.STREAMMAP.get(key).getOpenTime()).getTime();
							// 当前系统时间
							long newTime = System.currentTimeMillis();
							// 如果通道使用人数为0，则关闭推流
							if (CacheUtil.STREAMMAP.get(key).getCount() == 0) {
								// 结束线程
								VideoController.jobMap.get(key).setInterrupted();
								// 清除缓存
								iterator.remove();
								CacheUtil.STREAMMAP.remove(key);
								VideoController.jobMap.remove(key);
							} else if ((newTime - openTime) / 1000 / 60 > Integer.valueOf(config.getKeepalive())) {
								VideoController.jobMap.get(key).setInterrupted();
								logger.debug("[定时任务：]  结束： " + CacheUtil.STREAMMAP.get(key).getRtsp() + "  推流任务！");
								iterator.remove();
								VideoController.jobMap.remove(key);
								CacheUtil.STREAMMAP.remove(key);
							}
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				}
				//logger.info("******   执行定时任务       END     ******");
			}
		}, 1, 1000 * 60);
	}
}
