package org.springblade.modules.video.cache;



import org.springblade.modules.video.entity.CameraPojo;

import java.util.HashMap;
import java.util.Map;


/**
 * @ClassName :  CacheUtil
 * @Author :   xcuni dingxb
 * @Date :   2020/6/17
 * @Time :   19:55
 * @Decription :   存储推流缓存信息
 */
public final class CacheUtil {
	/*
	 * 保存已经开始推的流
	 */
	public static Map<String, CameraPojo> STREAMMAP = new HashMap<String, CameraPojo>();

	/*
	 * 保存服务启动时间
	 */
	public static long STARTTIME;

}
