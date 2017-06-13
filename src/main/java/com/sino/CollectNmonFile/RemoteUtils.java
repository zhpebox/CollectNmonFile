package com.sino.CollectNmonFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class RemoteUtils {
	
	private int port = 22;
	private Connection conn = null;
	private Session session = null;
	
	private StringBuffer commonStr = new StringBuffer("");
	
	/**
	 * 私有方法->直供自己调用 在远程客户端执行commStr命令
	 * 
	 * @param commonStr 待执行的命令行
	 * @throws IOException
	 */
	public String runCommonByConsole(String hostIp,String userName,String password,String commonStr) throws IOException {

		StringBuffer resultStr = new StringBuffer("");
		
		conn = new Connection(hostIp, port);
		conn.connect();
		boolean isconn = conn.authenticateWithPassword(userName, password);
		if (!isconn) {
			System.out.println("用户名或者密码不正确！");
			return "";
		}
		// 执行远程命令
		session = conn.openSession();
		session.execCommand(commonStr);

		// 获取远程Terminal屏幕上的输出并打印出来
		InputStream is = new StreamGobbler(session.getStdout());
		BufferedReader brs = new BufferedReader(new InputStreamReader(is));

		while (true) {
			String line = brs.readLine();
			if (line == null) {
				break;
			}
			resultStr.append(line+"\n");
			this.commonStr.append(line+"\n");
			System.out.println(line);
		}

		// 关闭连接对象
		if (session != null) {
			session.close();
		}
		
		return resultStr.toString();
	}
}
