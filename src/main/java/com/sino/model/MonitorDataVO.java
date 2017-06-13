package com.sino.model;

import java.util.Arrays;

public class MonitorDataVO {

	private String hostIP;
	
	private float[] cpuInfo = new float[5];
	private float[] diskInfo = new float[3];
	private float[] netInfo = new float[2];
	private float[] memInfo = new float[4];//memTotal,memused%,swapTotal,swapused%
//	private float[] cpuInfo;
//	private float[] diskInfo;
//	private float[] netInfo;
//	private float[] memInfo; //memTotal,memused%,swapTotal,swapused%
	
	
	public float[] getCpuInfo() {
		return cpuInfo;
	}
	public void setCpuInfo(float[] cpuInfotemp) {
		System.arraycopy(cpuInfotemp, 0, cpuInfo, 0, 5);
	}
	public float[] getDiskInfo() {
		return diskInfo;
	}
	public void setDiskInfo(float[] diskInfotemp) {
		System.arraycopy(diskInfotemp, 0, diskInfo, 0, 3);
	}
	public float[] getNetInfo() {
		return netInfo;
	}
	public void setNetInfo(float[] netInfo) {
		this.netInfo = netInfo;
	}
	public float[] getMemInfo() {
		return memInfo;
	}
	public void setMemInfo(float[] memInfo) {
		this.memInfo = memInfo;
	}
	public String getHostIP() {
		return hostIP;
	}
	public void setHostIP(String hostIP) {
		this.hostIP = hostIP;
	}
	
	
	
}
