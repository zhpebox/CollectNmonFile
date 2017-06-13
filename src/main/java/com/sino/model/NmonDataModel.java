package com.sino.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 用于封装Nmon监控下来的数据，每一时刻封装成一个对象
 * 监控文件中以“ZZZZ”开始
 * 参考conf下的nmonData文件
 * @author Administrator
 * 20170606
 */
public class NmonDataModel {
	
	//line1
	@Deprecated
	private String hostIp;  //当前主机IP
	@Deprecated
	private String nmonFilePath; //监控文件地址
	private String dataNum; //T0001,在excel中表示第一行记录
	private String dataTime; //当前监控时刻，20:26:48,05-JUN-2017
	
	//line4
	private float[] CPU_ALL; //CPU_ALL,float数组{User%,Sys%,Wait%,Idle%,Busy,CPUs} size 6 + 3,多三位备用字段
							 // eg.  2.0, 5.1, 3.9, 89.1, ,2
							 // User%: CPU_ALL[0]=2.0,   Sys%: CPU_ALL[1]=5.1,   Wait%: CPU_ALL[2]=3.9
							 // Idle%: CPU_ALL[3]=89.1,  Busy(CPU%): CPU_ALL[4]=CPU_ALL[0]+CPU_ALL[1],  CPUs:  CPU_ALL[5]=2
	
	//line5
	private float[] MEM; //MEM, float数组{memtotal,hightotal,lowtotal,swaptotal,memfree,highfree,
						 //              lowfree,swapfree,memshared,cached,active,bigfree,buffers,swapcached,inactive}
						 // size 15 + 3

	//line9
	private float[] NET;	//NET 本次测试只针对一个网卡，网卡标识根据conf配置文件配置。
								//数组默认两个大小Net[0]标识read，Net[1]标识write
	
	//line12 - 17
	private float[] DISK_SUMM; //DISK_SUMM, float数组{Disk Read KB/s, Disk Write KB/s, IO/sec}
							   // size 3 + 3
							   // Disk Read KB/s: sum(DISKREAD),     Disk Write KB/s: sum(DISKWRITE)
							   // IO/sec: sum(DISKXFER)


	/*setter and getter*/
	public String getDataNum() {
		return dataNum;
	}

	public void setDataNum(String dataNum) {
		this.dataNum = dataNum;
	}

	public String getDataTime() {
		return dataTime;
	}

	public void setDataTime(String dataTime) {
		this.dataTime = dataTime;
	}

	public float[] getCPU_ALL() {
		return CPU_ALL;
	}

	public void setCPU_ALL(float[] cPU_ALL) {
		CPU_ALL = cPU_ALL;
	}
	
	public void setCPU_ALL(String line) {
		String[] temp = line.split(",");
		float[] cpu = new float[9];
		for(int i=0;i<6;i++){
			if(temp[i+2].equals("")){ continue;}
			cpu[i] = Float.parseFloat(temp[i+2]);
		}
		cpu[4]=cpu[0]+cpu[1];
		CPU_ALL = cpu;
	}

	public float[] getMEM() {
		return MEM;
	}

	public void setMEM(float[] mEM) {
		MEM = mEM;
	}
	
	public void setMEM(String line){
		String[] temp = line.split(",");
		float[] mem = new float[18];
		for(int i=0;i<15;i++){
			if(temp[i+2].equals("")) continue;
			mem[i] = Float.parseFloat(temp[i+2]);
		}
		MEM = mem;
	}

	public float[] getNET() {
		return NET;
	}

	public void setNET(float[] nET) {
		NET = nET;
	}
	
	public void setNET(String line, int[] netWorkCard_read_write){
		NET = new float[2];
		String[] temp = line.split(",");
		NET[0] =  Float.parseFloat(temp[netWorkCard_read_write[0]]);
		NET[1] =  Float.parseFloat(temp[netWorkCard_read_write[1]]);
		
	}

	public float[] getDISK_SUMM() {
		return DISK_SUMM;
	}

	public void setDISK_SUMM(float[] dISK_SUMM) {
		DISK_SUMM = dISK_SUMM;
	}

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
	}

	public String getNmonFilePath() {
		return nmonFilePath;
	}

	public void setNmonFilePath(String nmonFilePath) {
		this.nmonFilePath = nmonFilePath;
	}

	@Deprecated
	public NmonDataModel(String hostIp, String nmonFilePath) {
		super();
		this.hostIp = hostIp;
		this.nmonFilePath = nmonFilePath;
	}
	
	public NmonDataModel(){}

	@Override
	public String toString() {
		return "NmonDataModel [hostIp=" + hostIp + ", nmonFilePath="
				+ nmonFilePath + ", dataNum=" + dataNum + ", dataTime="
				+ dataTime + ", CPU_ALL=" + Arrays.toString(CPU_ALL) + ", MEM="
				+ Arrays.toString(MEM) + ", NET=" + NET + ", DISK_SUMM="
				+ Arrays.toString(DISK_SUMM) + "]";
	}
	
	
	
	
	
}
