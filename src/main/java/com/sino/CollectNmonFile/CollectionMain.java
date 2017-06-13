package com.sino.CollectNmonFile;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;

import com.sino.model.HostModel;
import com.sino.model.MonitorDataVO;
import com.sino.model.NmonDataModel;

/**
 * nmon监控数据收集---主控类
 * @author Administrator 2017.06.05
 * nmon_x86_rhel52.rhel52
 */
public class CollectionMain {
	private static final Logger logger = LoggerFactory.getLogger(com.sino.CollectNmonFile.CollectionMain.class);
	
	private static SimpleDateFormat sf = new SimpleDateFormat("yyMMddHHmmss");
	private static String dateStr = sf.format(new Date()); //本次监控数据已本机系统时间为准，收集数据和获取数据文件都以dataStr为标识
	
	private static String netWorkCard = "";
	
	private static int isCutTime = 0;
	private static int startTime = 0;
	private static int endTime = 0;
	
	public static void main(String[] filePath) {
		if(filePath==null || filePath.length<=0 || filePath.equals("")){
			logger.info("请输入配置文件路径！ 例如： /home/nmon");
			return;
		}
		String sourcePath = filePath[0];
		//读取配置文件
		Properties prop = getProps(sourcePath+"/conf.properties");
		//获取网卡标识
		netWorkCard = prop.get("netWorkCard").toString();
		//获取监控的起始结束时间点
		isCutTime = Integer.parseInt(prop.get("isCutTime").toString()); //是否截取时间段
		startTime = Integer.parseInt(prop.get("startTime").toString());
		endTime = Integer.parseInt(prop.getProperty("endTime").toString());
		int monitorTime = Integer.parseInt(prop.get("s").toString()) * Integer.parseInt(prop.get("c").toString());
		
		//判断是否截取时间段 isCutTime=1截取 isCutTime=0不截取，收取全部时间段的监控数据
		if(isCutTime==1){
			logger.info(" 按照 startTime 和 endTime 整合全部时间的监控数据！");
			if(startTime<=0 || endTime<=0){
				logger.error(" 开始时间点、结束时间点 取值不能小于0");
				return;
			}
			if(startTime>=endTime){
				logger.error(" 开始时间点取值 不能大于 结束时间点");
				return;
			}
			if(endTime>Integer.parseInt(prop.get("c").toString())){
				logger.error(" 结束时间点 取值不能大于 实际监控时间");
				return;
			}
			logger.info("startTime = "+startTime+", endTime = "+endTime);
			//数值存储从0开始
			startTime --;
			endTime --;
		}else{
			logger.info("整合全部时间的监控数据！");
			startTime = 0;
			endTime = Integer.parseInt(prop.get("c").toString());
			logger.info("startTime = "+startTime+", endTime = "+endTime);
		}
		
		
		// 1.获取host主机列表list<主机>
		List<HostModel> hostList = getHostList(sourcePath+"/hosts.properties");
		
		// 2.传输nmon应用文件到所有主机,首先判断是否安装过了
		if(!prop.get("nmonIsExist").equals("1")){
//			String path = (com.sino.CollectNmonFile.CollectionMain.class.getResource("/resources/").toString()).substring(6);
//			logger.debug("hostlist,path"+path);
			boolean sendFile = sendFile2HostList(sourcePath+"/"+prop.get("file").toString(),hostList);
		}
		
		// 3.检测所有nmon服务
		
		// 4.读取nmon启动配置,启动所有主机的nmon服务 
		dateStr = sf.format(new Date());//获取当前时间标识
		String satrtCommand = "cd ~ && chmod 777 "+prop.get("file").toString()+" &&./"+prop.get("file").toString()
								+" -f -s "+prop.get("s")+" -c "+ prop.get("c");
		boolean startNmon = startNmon(hostList,satrtCommand);
		
		//读秒，等待监控完毕……
		try {
			int sleepTime = Integer.parseInt(prop.get("s").toString()) * Integer.parseInt(prop.get("c").toString());
			logger.info("等待监控完成……");
			for(int i=sleepTime;i>0;i--){
				logger.info("剩余时间"+i+"秒");
				Thread.sleep(1000);//等待1秒
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// 5.从各个主机收集监控文件，拷贝到本地，将本地路径赋值到list<文件>
		Map<String,String> hostNmonFilePath = getNmonFile4HostList(hostList,dateStr,prop.get("localNmonFolder").toString());
		
		// 7.遍历 解析list<文件>中的监控数据
		Map<String,List<NmonDataModel>> nmonDataParseList = getDataFromFileList(hostNmonFilePath);
		
		// 8.整合计算监控结果, 按配置文件给出的startTime,endTime
		List<MonitorDataVO> monitorList = computeMonitorData(nmonDataParseList);
		
		// 9.展示或输出监控结果
		showMonitorData2Excel(prop.get("localNmonFolder").toString(),monitorList);
		logger.info("收集完成…………监控文件："+dateStr+".xml");
	}
	
	
	private static void showMonitorData2Excel(String outExcelPath,List<MonitorDataVO> monitorList) {
		String head0[] = { "服务器地址", "CPU使用情况", "磁盘使用情况", "网络使用情况", "内存使用情况" };
		String head1[] = { "User%", "sys%", "wait%", "idle%", "CPU%",
				"Disk Read", "Disk Write", "IO/sec", "en0-reads", "en0.writes",
				"memtotal", "memUsed%", "swaptotal", "swapUsed%" };
		ExcelUtil.out2Excel(outExcelPath, head0, head1, monitorList);
	}



	/**
	 * 计算监控数值结果
	 * @param nmonDataParseList
	 * @return
	 */
	private static List<MonitorDataVO> computeMonitorData(
			Map<String,List<NmonDataModel>> nmonDataParseList) {
		List<MonitorDataVO> monitorList = new ArrayList<MonitorDataVO>();
		
		Iterator<Entry<String,List<NmonDataModel>>> iterator = nmonDataParseList.entrySet().iterator();
		
		while(iterator.hasNext()){
			Entry<String,List<NmonDataModel>> entry = iterator.next();
			MonitorDataVO monitorDataVO = new MonitorDataVO();
			float[] cpuInfo = monitorDataVO.getCpuInfo();
			float[] diskInfo = monitorDataVO.getDiskInfo();
			float[] netInfo = monitorDataVO.getNetInfo();
			float[] memInfo = monitorDataVO.getMemInfo(); 
			
			monitorDataVO.setHostIP(entry.getKey());
			
			int numOfTime = 0;
			
			//求和 startTime endTime
			NmonDataModel nmonData4times;
			for(int i = startTime;i<endTime;i++){
				nmonData4times = entry.getValue().get(i);
				sumOfFloat(cpuInfo, nmonData4times.getCPU_ALL());
				sumOfFloat(diskInfo, nmonData4times.getDISK_SUMM());
				sumOfFloat(netInfo, nmonData4times.getNET()); 
				sumOfMem(memInfo, nmonData4times.getMEM());
				numOfTime++;
			}
//			for(NmonDataModel nmonData4times: entry.getValue()){
//				sumOfFloat(cpuInfo, nmonData4times.getCPU_ALL());
//				sumOfFloat(diskInfo, nmonData4times.getDISK_SUMM());
//				sumOfFloat(netInfo, nmonData4times.getNET()); 
//				sumOfMem(memInfo, nmonData4times.getMEM());
//				numOfTime++;
//			}
			
			//取平均
			avgOfFloat(cpuInfo,numOfTime);
			avgOfFloat(diskInfo,numOfTime);
			avgOfFloat(netInfo,numOfTime);
			avgOfFloat(memInfo,numOfTime);
			
			//计算MEM最终的显示值
			computeMen(memInfo);
			
			//将解析后的监控对象赋值到监控列表中
			monitorList.add(monitorDataVO);
		}
		
		return monitorList;
	}
	
	private static void computeMen(float[] mem){
		//mem使用率：(total-free)/total
		mem[1] = (mem[0] - mem[1])/mem[0] * 100;
		mem[3] = (mem[2] - mem[3])/mem[2] * 100;
	}
	
	private static void avgOfFloat(float[] sum, int times){
		for(int i = 0; i<sum.length;i++){
			sum[i] = sum[i] / times;
		}
	}
	
	/**
	 * 求sum数组的和,用于CPU和DISK
	 * @return
	 */
	private static void sumOfFloat(float[] sum,float[] addOne){
		for(int i = 0;i<sum.length;i++){
			sum[i] += addOne[i];
		}
	}
	
	/**
	 * 整合计算mem的信息
	 * 对应数组 [内存总量，内存使用率，交换空间，交换空间使用率]
	 * 算法：
	 * 	1. 获取addOne内存数组的四个位置，0memtotal 4memfree 3swaptotal 7swapfree
	 * 	2. 将所有时刻监控值addOne相加
	 *  3. 计算的出内存监控值
	 * @param sum
	 * @param addOne
	 */
	private static void sumOfMem(float[] sum,float[] addOne){
		//0memtotal 4memfree 3swaptotal 7swapfree
		sum[0] += addOne[0]; //0memtotal
		sum[1] += addOne[4]; //4memfree 
		sum[2] += addOne[3]; //3swaptotal
		sum[3] += addOne[7]; //7swapfree
	}
	
	
	/**
	 * 获取nmon监控文件列表，解析成为相应的nmon监控对象
	 * @param resultList
	 * @return
	 */
	private static Map<String,List<NmonDataModel>> getDataFromFileList(Map<String,String> hostNmonFilePath){
		Map<String,List<NmonDataModel>> nmonList = new HashMap<String,List<NmonDataModel>>();
		
		Iterator<Entry<String, String>> it = hostNmonFilePath.entrySet().iterator();
		
		while(it.hasNext()){
			Entry<String, String> entry = it.next();
			nmonList.put(entry.getKey(),parseFileString(entry.getValue()));
		}
		
		return nmonList;
	}
	
	/**
	 * 解析nmon文件
	 * @param filePath
	 */
	private static List<NmonDataModel> parseFileString(String nmonFilePath){//String netMark
		List<NmonDataModel> nmonListOfFile = new ArrayList<NmonDataModel>();
		try {
			FileReader reader = new FileReader(nmonFilePath);
			BufferedReader br = new BufferedReader(reader);
			String temp ;
			int netmark = 0;
			int[] netWorkCard_read_write = new int[2];
			while((temp=br.readLine())!=null){
				
				NmonDataModel model = new NmonDataModel();
				
				//过滤掉文件开始内容的描述内容
				if(temp.startsWith("AAA")||temp.startsWith("BBB")) continue;
				//记录下网卡信息
				if(netmark==0 && temp.startsWith("NET,Network")){
					String[] netName = temp.split(",");
					int networkIO = 0;
					for(int i=2;i<netName.length;i++){
//						logger.debug(netName[i]);
						if(netName[i].startsWith(netWorkCard)){
							netWorkCard_read_write[networkIO++] = i;
						}
					}
					netmark=1; //防止字符串的多次判断
				}
				
				//以“ZZZZ"所在行往下16行(包含本行)为一个时刻的监控数值
				if(temp.startsWith("ZZZZ")){
					//1 获取 记录标号 和 监控时间  信息
					String[] head = temp.split(",");
					model.setDataNum(head[1]);
					model.setDataTime(head[3]+" "+head[2]);
					
					//2 获取CPU_ALL信息
					while(!temp.startsWith("CPU_ALL")){
						temp=br.readLine();
					}
					model.setCPU_ALL(temp);
					
					//3 获取MEM信息
					while(!temp.startsWith("MEM")){
						temp=br.readLine();
					}
					model.setMEM(temp);
					
					//4 获取NET信息
					while(!temp.startsWith("NET")){
						temp=br.readLine();
					}
					model.setNET(temp,netWorkCard_read_write);
					
					//5 获取DISK_SUM
					float[] disk_sum = new float[6]; //定义DISK_SUM记录标识
					
					//5.1 获取DISK_READ
					while(!temp.startsWith("DISKREAD")){
						temp=br.readLine();
					}
					disk_sum[0] = sumString(temp);
					
					//5.2 获取DISK_WRITE
					while(!temp.startsWith("DISKWRITE")){
						temp=br.readLine();
					}
					disk_sum[1] = sumString(temp);
					
					//5.3 获取DISK_DISKXFER(IO/sec)
					while(!temp.startsWith("DISKXFER")){
						temp=br.readLine();
					}
					disk_sum[2] = sumString(temp);
					
					//5.4 将汇总信息赋值给model
					model.setDISK_SUMM(disk_sum);
					
					logger.debug(model.toString());
					nmonListOfFile.add(model);
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nmonListOfFile;
	}
	
	/**
	 * 根据nmon文件格式，将一行监控数据值的所有项相加（去除前两个描述信息）
	 * @param line
	 * @return
	 */
	public static float sumString(String line){
		String[] temp = line.split(",");
		float sum = 0;
		for(int i=2;i<temp.length;i++){
			if(temp[i].equals("")) continue;
			sum += Float.parseFloat(temp[i]);
		}
		return sum;
	}
	
	/**
	 * 从主机列表中的各个主机中，获取本次nmon监控的数据文件
	 * @param hostList
	 * @param dateStr 本次监控的数据标识
	 * @param localNmonFolder 收集到本地的文件路径
	 * @return
	 */
	private static Map<String,String> getNmonFile4HostList(List<HostModel> hostList,String dateStr,String localNmonFolder) {
		Map<String,String> hostNmonFilePath = new HashMap<String, String>();
		
		//在各主机收集监控文件
		for(HostModel model:hostList){
			//远程登录主机
			Connection conn = new Connection(model.getIp(), 22);
			try {
				conn.connect();
				boolean isconn = conn.authenticateWithPassword(model.getUsername(), model.getPassword());
				if (!isconn) {
					System.out.println("用户名或者密码不正确！");
				}
				SCPClient clt = conn.createSCPClient();
				clt.get("~/"+dateStr+"_"+model.getIp()+model.getUsername()+".nmon", localNmonFolder);
				hostNmonFilePath.put(model.getIp()+model.getUsername(),localNmonFolder+dateStr+"_"+model.getIp()+model.getUsername()+".nmon");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return hostNmonFilePath;
	}

	/**
	 * 执行 启动nmon监控的命令
	 * @param hostList
	 * @param command
	 * @return
	 */
	private static boolean startNmon(List<HostModel> hostList,String command) {
		boolean result = true;
		for(HostModel model:hostList){
			try {
				String commands = command+" -F "+dateStr+"_"+model.getIp()+model.getUsername()+".nmon";
				logger.debug(commands);
				(new RemoteUtils()).runCommonByConsole(model.getIp(),model.getUsername(),model.getPassword(),commands);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return result;
	}

	/**
	 * 将nmon应用传输到主机列表中的各个主机
	 * @param filePath
	 * @param hostList
	 * @return
	 */
	private static Boolean sendFile2HostList(String filePath,
			List<HostModel> hostList) {
		boolean result = true;
		for(HostModel model : hostList){
			logger.debug("send file!!!");
			Connection conn = new Connection(model.getIp(), 22);
			try {
				conn.connect();
				boolean isconn = conn.authenticateWithPassword(model.getUsername(), model.getPassword());
				if (!isconn) {
					logger.error("用户名或者密码不正确！");
				}
				
				String commands = " find ~/nmon64 ";
				logger.debug(commands);
				String resultStr = (new RemoteUtils()).runCommonByConsole(model.getIp(),model.getUsername(),model.getPassword(),commands);
				logger.debug(resultStr);
				if(resultStr.endsWith("nmon64\n")){
					logger.debug(model.getIp()+"nmon应用已存在！"); 
					continue;
				}
				logger.debug("开始执行文件传输");
				SCPClient clt = conn.createSCPClient();
				clt.put(filePath, "~"); //远程传输到当前用户的主目录   
				logger.debug("文件传输结束");
			} catch (Exception e) {
				logger.error("nmon应用程序传输失败，需检查目标主机 "+model.getIp()+"/"+model.getUsername()+" 的当前用户主目录是否已存在  文件 ~/nmon64");
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 从配置文件hosts.properites获取监控主机列表
	 * @param fileName
	 * @return
	 */
	private static List<HostModel> getHostList(String fileName) {
		List<HostModel> hostList = new ArrayList<HostModel>();
		
		FileReader reader = null;
		BufferedReader br = null;
		try {
//			String path = (com.sino.CollectNmonFile.CollectionMain.class.getResource("/").toString()).substring(6);
			logger.info("hostList file :"+fileName);
			reader = new FileReader(fileName);
			br = new BufferedReader(reader);
			
			String temp; //记录配置文件每行主机信息的字符创
			String[] hostArray ;//记录每台主机的各配置项信息
			while ((temp = br.readLine()) != null) {
				hostArray = temp.split(" ");
				if(hostArray.length < 3) continue;
				hostList.add(new HostModel(hostArray));
			}
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return hostList;
	}

	/**
	 * 读取配置文件
	 * @param fileName
	 * @return
	 */
	private static Properties getProps(String fileName) {
		logger.debug(fileName);
		// File file = new File("something.properties");
		FileInputStream fis;
		Properties prop = new Properties();
		try {
			fis = new FileInputStream(fileName);
			// InputStream in = ClassLoader.getSystemResourceAsStream(fileName);
			prop.load(fis);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prop;
	}
	
	@Test
	public void testparseFileString(){
		parseFileString("D:/test.nmon");
	}
	
}
	