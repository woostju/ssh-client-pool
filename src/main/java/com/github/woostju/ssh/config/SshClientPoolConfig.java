package com.github.woostju.ssh.config;


import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import com.github.woostju.ssh.pool.SshClientWrapper;

public class SshClientPoolConfig extends GenericKeyedObjectPoolConfig<SshClientWrapper>{
	
	private Class<?> sshClientImplClass;
	
	private String serverCommandPromotRegex;
	
	public SshClientPoolConfig() {
		super();
	}
			
	public SshClientPoolConfig(int maxTotal, int maxIdle, long idleTime, long maxWaitMillis){
		this.setMaxTotalPerKey(maxTotal); // pool 中允许的最大对象数
		this.setMaxIdlePerKey(maxIdle); // pool中允许的最大空闲对象数
		this.setBlockWhenExhausted(true); // 当pool中已经达到最大值并无空闲，请求者将被阻塞MaxWaitMillis时间
		this.setMaxWaitMillis(1000L * maxWaitMillis); // 阻塞等待时间
		//  60秒清理
		this.setMinEvictableIdleTimeMillis(1000L * idleTime); // 后台清理idle过期对象
		this.setTimeBetweenEvictionRunsMillis(1000L * idleTime); // 后台清理idle过期对象的周期
		this.setTestOnBorrow(true); // 在租用时，validate对象
		this.setTestOnReturn(true); //  在归还时，validate对象
		this.setTestWhileIdle(true); // 在idle时，validate对象
		this.setJmxEnabled(false); // 禁止jmx
	}

	public Class<?> getSshClientImplClass() {
		return sshClientImplClass;
	}

	public void setSshClientImplClass(Class<?> sshClientImplClass) {
		this.sshClientImplClass = sshClientImplClass;
	}

	public String getServerCommandPromotRegex() {
		return serverCommandPromotRegex;
	}

	public void setServerCommandPromotRegex(String serverCommandPromotRegex) {
		this.serverCommandPromotRegex = serverCommandPromotRegex;
	}
	
	
}
