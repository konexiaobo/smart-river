package org.springblade.modules.video.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @ClassName :  IpUtil
 * @Author :   xcuni dingxb
 * @Date :   2020/6/18
 * @Time :   15:53
 * @Decription :   可根据域名获取ip地址
 */
public class IpUtil {
	public static String IpConvert(String domainName) {
		String ip = domainName;
		try {
			ip = InetAddress.getByName(domainName).getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return domainName;
		}
		return ip;
	}
}
