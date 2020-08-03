package org.springblade.modules.video.controller;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.camera.entity.Camera;
import org.springblade.modules.camera.service.ICameraService;
import org.springblade.modules.file.FileController;
import org.springblade.modules.video.cache.CacheUtil;
import org.springblade.modules.video.entity.CameraPojo;
import org.springblade.modules.video.entity.Config;
import org.springblade.modules.video.thread.CameraThread;
import org.springblade.modules.video.util.CameraPush;
import org.springblade.org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @ClassName :  CameraController
 * @Author :   xcuni dingxb
 * @Date :   2020/6/17
 * @Time :   15:54
 * @Decription :   rtsp 2 rtmp 接口层
 */

@RestController
public class VideoController {

	private final static Logger logger = LoggerFactory.getLogger(VideoController.class);

	@Autowired
	public Config config;// 配置文件bean

	@Autowired
	private ICameraService cameraService;

	@Autowired
	private FileController fileController;

	// 存放任务 线程
	public static Map<String, CameraThread.MyRunnable> jobMap = new HashMap<String, CameraThread.MyRunnable>();

	/**
	 * @MethodName :  openCamera
	 * @Author :   xcuni dingxb
	 * @Date :   2020/6/17
	 * @Time :   15:55
	 * @Decription :   开启视频流
	 */
	@PostMapping("/openvideo")
	public Map<String, String> openCamera(CameraPojo pojo) {
		// 返回结果
		Map<String, String> map = new HashMap<String, String>();
		// 校验参数
		if (pojo.getDeviceSerial() != null && !"".equals(pojo.getDeviceSerial())) {
			CameraPojo cameraPojo = new CameraPojo();
			// 获取当前时间,用于视频保活时间
			String openTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
			Set<String> keys = CacheUtil.STREAMMAP.keySet();
			// 缓存是否为空
			if (0 == keys.size()) {
				// 开始推流
				cameraPojo = openStream(pojo.getDeviceSerial(), openTime);
				map.put("token", cameraPojo.getToken());
				map.put("url", cameraPojo.getUrl());
				map.put("out_url", cameraPojo.getOutRtmp());
				logger.info("打开：" + cameraPojo.getRtsp());
			} else {
				// 是否存在的标志；0：不存在；1：存在
				int sign = 0;
				if (pojo.getDeviceSerial() != null && !"".equals(pojo.getDeviceSerial())) {// 直播流
					Camera detail = new Camera();
					detail.setDeviceSerial(pojo.getDeviceSerial());
					detail = cameraService.getOne(Condition.getQueryWrapper(detail));
					for (String key : keys) {
						if (detail.getToken() != null && detail.getToken().equals(CacheUtil.STREAMMAP.get(key).getToken())) {
							cameraPojo = CacheUtil.STREAMMAP.get(key);
							sign = 1;
							break;
						}
					}
					if (sign == 1) {// 存在
						cameraPojo.setCount(cameraPojo.getCount() + 1);
						cameraPojo.setOpenTime(openTime);
						map.put("token", cameraPojo.getToken());
						map.put("url", cameraPojo.getUrl());
						map.put("out_url", cameraPojo.getOutRtmp());
						logger.info("打开：" + cameraPojo.getRtsp());
					} else {
						cameraPojo = openStream(pojo.getDeviceSerial(), openTime);
						map.put("token", cameraPojo.getToken());
						map.put("url", cameraPojo.getUrl());
						map.put("out_url", cameraPojo.getOutRtmp());
						logger.info("打开：" + cameraPojo.getRtsp());
					}
				}
			}
		}
		return map;
	}

	/**
	 * @MethodName :  openStream
	 * @Author :   xcuni dingxb
	 * @Date :   2020/6/17
	 * @Time :   15:59
	 * @Decription :   推流器，将rtsp转rtmp的任务加入线程池
	 */
	private CameraPojo openStream(String deviceSerial, String openTime) {
		Camera camera = new Camera();
		CameraPojo cameraPojo = new CameraPojo();
		camera.setDeviceSerial(deviceSerial);
		//获取摄像机基础信息
		Camera detail = cameraService.getOne(Condition.getQueryWrapper(camera));
		if (detail != null) {
			BeanUtil.copyProperties(detail, cameraPojo);
			// 生成token，通过token区别唯一线程
			String token = UUID.randomUUID().toString();
			detail.setToken(token);
			detail.setOpenTime(openTime);
			//将token回填
			cameraService.updateById(detail);
			cameraPojo.setCount(1);
			cameraPojo.setToken(token);
			cameraPojo.setOpenTime(openTime);
			// 执行任务
			CameraThread.MyRunnable job = new CameraThread.MyRunnable(cameraPojo);
			CameraThread.MyRunnable.es.execute(job);
			jobMap.put(token, job);
		}
		return cameraPojo;
	}


	/**
	 * @MethodName :  closeCamera
	 * @Author :   xcuni dingxb
	 * @Date :   2020/6/18
	 * @Time :   10:50
	 * @Decription :   关闭视频流
	 */
	@RequestMapping(value = "/closevideo/{tokens}", method = RequestMethod.DELETE)
	public void closeCamera(@PathVariable("tokens") String tokens) {
		if (null != tokens && !"".equals(tokens)) {
			String[] tokenArr = tokens.split(",");
			for (String token : tokenArr) {
				if (jobMap.containsKey(token) && CacheUtil.STREAMMAP.containsKey(token)) {
					if (0 < CacheUtil.STREAMMAP.get(token).getCount()) {
						// 人数-1
						CacheUtil.STREAMMAP.get(token).setCount(CacheUtil.STREAMMAP.get(token).getCount() - 1);
						logger.info("关闭：" + CacheUtil.STREAMMAP.get(token).getRtsp() + ";当前使用人数为："
							+ CacheUtil.STREAMMAP.get(token).getCount());
					}
				}
			}
		}
	}


	/**
	 * @MethodName :  getCameras
	 * @Author :   xcuni dingxb
	 * @Date :   2020/6/18
	 * @Time :   14:32
	 * @Decription :   获取当前视频流信息
	 */
	@RequestMapping(value = "/videosinfo", method = RequestMethod.GET)
	public Map<String, CameraPojo> getCameras() {
		logger.info("获取视频流信息：" + CacheUtil.STREAMMAP.toString());
		return CacheUtil.STREAMMAP;
	}


	/**
	 * @MethodName :  keepAlive
	 * @Author :   xcuni dingxb
	 * @Date :   2020/6/18
	 * @Time :   16:35
	 * @Decription :   视频流保活
	 */
	@RequestMapping(value = "/video/keepalive/{tokens}", method = RequestMethod.PUT)
	public void keepAlive(@PathVariable("tokens") String tokens) {
		// 校验参数
		if (null != tokens && !"".equals(tokens)) {
			String[] tokenArr = tokens.split(",");
			for (String token : tokenArr) {
				CameraPojo cameraPojo = new CameraPojo();
				// 直播流token
				if (null != CacheUtil.STREAMMAP.get(token)) {
					cameraPojo = CacheUtil.STREAMMAP.get(token);
					// 更新当前系统时间
					cameraPojo.setOpenTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime()));
					logger.info("视频流：" + cameraPojo.getRtmp() + "保活！");
				}
			}
		}
	}


	/**
	 * @MethodName :  getConfig
	 * @Author :   xcuni dingxb
	 * @Date :   2020/6/18
	 * @Time :   18:41
	 * @Decription :    获取服务信息
	 */
	@RequestMapping(value = "/videos/status", method = RequestMethod.GET)
	public Map<String, Object> getConfig() {
		// 获取当前时间
		long nowTime = System.currentTimeMillis();
		String upTime = (nowTime - CacheUtil.STARTTIME) / (1000 * 60 * 60) + "h"
			+ (nowTime - CacheUtil.STARTTIME) % (1000 * 60 * 60) / (1000 * 60) + "m"
			+ (nowTime - CacheUtil.STARTTIME) % (1000 * 60 * 60) / (1000) + "s";
		logger.info("获取服务信息：" + config.toString() + ";服务运行时间：" + upTime);
		Map<String, Object> status = new HashMap<String, Object>();
		status.put("config", config);
		status.put("uptime", upTime);
		return status;
	}


	/**
	 * @MethodName :  getScreenShot
	 * @Author :   xcuni dingxb
	 * @Date :   2020/7/13
	 * @Time :   15:00
	 * @Decription :   获取指定视频截图
	 */
	@PostMapping("/screenshot")
	public Map<String, String> getScreenShot(CameraPojo cameraPojo) {
		//判断当前设备是否已经在推流
		if (cameraPojo.getDeviceSerial() != null && !"".equals(cameraPojo.getDeviceSerial()) &&
			null != CacheUtil.STREAMMAP && 0 != CacheUtil.STREAMMAP.size()) {
			Iterator<Map.Entry<String, CameraPojo>> iterator = CacheUtil.STREAMMAP.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, CameraPojo> next = iterator.next();
				String key = next.getKey();
				String deviceSerial = next.getValue().getDeviceSerial();
				try {
					if (deviceSerial.equals(cameraPojo.getDeviceSerial())) {
						//1. 如果这个设备正在推流，需要再次判断一下时间，防止在截图的过程中，时间已经结束导致截图失败
						long openTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
							.parse(CacheUtil.STREAMMAP.get(key).getOpenTime()).getTime();
						//当前系统时间
						long nowTime = System.currentTimeMillis();
						//判断时间差是否大于30秒，如果超过30秒，就推迟最后打开时间,设置为当前时间
						if ((nowTime - openTime) / 1000 < Integer.valueOf(config.getScreen_shot())) {
							CacheUtil.STREAMMAP.get(key).setOpenTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
							//2. 判断当前推流的通道人数是否为0（为0的情况一般就是有人手动调用了停止推流的接口），如果是0的话，需要加1
							if (CacheUtil.STREAMMAP.get(key).getCount() == 0) {
								CacheUtil.STREAMMAP.get(key).setCount(1);
							}
							//3.当以上两点条件都满足了，就可以直接利用转码的视频进行截图
							printImage(VideoController.jobMap.get(key).getCameraPush());
						}
					} else {
						// 如果这个设备没有在推流，就需要将该设备加入推流线程池中，再次获取截图
						openCamera(cameraPojo);
						printImage(VideoController.jobMap.get(key).getCameraPush());
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * @MethodName :  printImage
	 * @Author :   xcuni dingxb
	 * @Date :   2020/7/13
	 * @Time :   16:31
	 * @Decription :   获取视频流截图
	 */
	private void printImage(CameraPush cameraPush) {
		if (cameraPush != null && cameraPush.getFFmpegFrameGrabber() != null) {
			FFmpegFrameGrabber grabber = cameraPush.getFFmpegFrameGrabber();
			try {
				grabber.start();
				int length = grabber.getLengthInFrames();
				int i = 0;
				Frame f = null;
				while (i < length) {
					// 过滤前5帧，避免出现全黑的图片，依自己情况而定
					f = grabber.grabFrame();
					if ((i > 5) && (f.image != null)) {
						break;
					}
					i++;
				}
				// 截取的帧图片
				Java2DFrameConverter converter = new Java2DFrameConverter();
				BufferedImage srcImage = converter.getBufferedImage(f);
				int srcImageWidth = srcImage.getWidth();
				int srcImageHeight = srcImage.getHeight();
				// 对截图进行等比例缩放(缩略图)
				int width = 480;
				int height = (int) (((double) width / srcImageWidth) * srcImageHeight);
				BufferedImage thumbnailImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
				thumbnailImage.getGraphics().drawImage(srcImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
				//将图片转为流数据，方便写入文件
				ByteArrayOutputStream bs = new ByteArrayOutputStream();
				ImageOutputStream imOut = null;
				try {
					imOut = ImageIO.createImageOutputStream(bs);
					ImageIO.write(thumbnailImage, "jpg", imOut);
					InputStream inputStream = new ByteArrayInputStream(bs.toByteArray());
					MultipartFile multipartFile = new MockMultipartFile("", "", "", inputStream);
					//调用上传方法，上传截取后的图像
					R result = fileController.upload(multipartFile);
					logger.info(result.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (FrameGrabber.Exception e) {
				e.printStackTrace();
			}
		}
	}


}
