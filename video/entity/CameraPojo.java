package org.springblade.modules.video.entity;

import java.io.Serializable;

public class CameraPojo implements Serializable {
	private static final long serialVersionUID = 8183688502930584159L;
	private Integer id;
	private String deviceSerial;// 摄像头序列号
	private String username;// 摄像头账号
	private String password;// 摄像头密码
	private String ip;// 摄像头ip
	private Integer port; //摄像头端口
	private String channel;// 摄像头通道
	private String stream;// 摄像头码流
	private String rtsp;// rtsp地址
	private String rtmp;// rtmp地址
	private String outRtmp; //rtmp外网地址
	private String url;// 播放地址
	private String startTime;// 回放开始时间
	private String endTime;// 回放结束时间
	private String openTime;// 打开时间
	private Integer count;// 使用人数
	private String token;


	public String getOutRtmp() {
		return outRtmp;
	}

	public void setOutRtmp(String outRtmp) {
		this.outRtmp = outRtmp;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDeviceSerial() {
		return deviceSerial;
	}

	public void setDeviceSerial(String deviceSerial) {
		this.deviceSerial = deviceSerial;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public String getRtsp() {
		return rtsp;
	}

	public void setRtsp(String rtsp) {
		this.rtsp = rtsp;
	}

	public String getRtmp() {
		return rtmp;
	}

	public void setRtmp(String rtmp) {
		this.rtmp = rtmp;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getOpenTime() {
		return openTime;
	}

	public void setOpenTime(String openTime) {
		this.openTime = openTime;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "CameraPojo [username=" + username + ", password=" + password + ", ip=" + ip + ", channel=" + channel
				+ ", stream=" + stream + ", rtsp=" + rtsp + ", rtmp=" + rtmp + ", url=" + url + ", startTime="
				+ startTime + ", endTime=" + endTime + ", openTime=" + openTime + ", count=" + count + ", token="
				+ token + "]";
	}

}
