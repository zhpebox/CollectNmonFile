package com.sino.model;

public class HostModel {
	
	private String ip;
	private String username;
	private String password;
	
	public HostModel() {
	}
	public HostModel(String[] hostArray) {
		super();
		this.ip = hostArray[0];
		this.username = hostArray[1];
		this.password = hostArray[2];
	}
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
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
	
	
}
