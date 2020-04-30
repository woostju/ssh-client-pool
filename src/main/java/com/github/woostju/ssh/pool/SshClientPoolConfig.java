package com.github.woostju.ssh.pool;


import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import com.github.woostju.ssh.SshClient;
import com.github.woostju.ssh.SshClientSSHJ;


/**
 * 
 * The configuration of SshClientPool library
 * <p>SshClientPoolConfig is a subclass of GenericKeyedObjectPoolConfig to control the pool behavior
 * <p>Also, you can replace the build-in {@link SshClient} implementation by {@link SshClientPoolConfig#setSshClientImplClass(Class)} if you want
 * 
 * @author jameswu
 */
public class SshClientPoolConfig extends GenericKeyedObjectPoolConfig<SshClientWrapper>{
	
	private Class<?> sshClientImplClass;
	
	private String serverCommandPromotRegex;
	
	public SshClientPoolConfig() {
		super();
	}
			
	/**
	 * quick way to create SshClientPoolConfig
	 * set TestOnBorrow to true
	 * set TestOnReturn to true
	 * set TestWhileIdle to true
	 * set JmxEnabled to false 
	 * @param maxActive maxTotalPerKey
	 * @param maxIdle maxIdlePerKey
	 * @param idleTime idle time
	 * @param maxWaitTime maxWaitMillis
	 */
	public SshClientPoolConfig(int maxActive, int maxIdle, long idleTime,  long maxWaitTime){
		this.setMaxTotalPerKey(maxActive);
		this.setMaxIdlePerKey(maxIdle);
		this.setMaxWaitMillis(maxWaitTime);
		this.setBlockWhenExhausted(true);
		this.setMinEvictableIdleTimeMillis(idleTime); 
		this.setTimeBetweenEvictionRunsMillis(idleTime);
		this.setTestOnBorrow(true); 
		this.setTestOnReturn(true);
		this.setTestWhileIdle(true); 
		this.setJmxEnabled(false);
	}
	
	public Class<?> getSshClientImplClass() {
		return sshClientImplClass;
	}

	/**
	 * replace the build-in {@link SshClient} by {@link SshClientPoolConfig#setSshClientImplClass(Class)}
	 * @param sshClientImplClass the implementation of {@link SshClient}
	 */
	public void setSshClientImplClass(Class<?> sshClientImplClass) {
		this.sshClientImplClass = sshClientImplClass;
	}

	/**
	 * 
	 * @return regex string used to match promot from server
	 */
	public String getServerCommandPromotRegex() {
		return serverCommandPromotRegex;
	}

	/**
	 * see {@link SshClientSSHJ#setCommandPromotRegexStr(String)}
	 * @param serverCommandPromotRegex regex string used to match promot from server
	 */
	public void setServerCommandPromotRegex(String serverCommandPromotRegex) {
		this.serverCommandPromotRegex = serverCommandPromotRegex;
	}
	
	
}
