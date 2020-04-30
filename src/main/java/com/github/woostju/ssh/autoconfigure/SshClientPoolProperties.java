package com.github.woostju.ssh.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("woostju.ssh-client-pool")
public class SshClientPoolProperties {
	
	/**
	 * Max number of "idle" connections in the pool. Use a negative value to indicate
	 * an unlimited number of idle connections.
	 */
	private int maxIdle = 20;

	/**
	 * 
	 */
	private int idleTime = 120*1000;

	/**
	 * Max number of connections that can be allocated by the pool at a given time.
	 * Use a negative value for no limit.
	 */
	private int maxActive = 20;

	/**
	 * Maximum amount of time (in milliseconds) a connection allocation should block
	 * before throwing an exception when the pool is exhausted. Use a negative value
	 * to block indefinitely.
	 */
	private int maxWait = 120*1000;
	
	private String sshClientImplClass = "com.github.woostju.ssh.SshClientSSHJ";
	
	private SshClientProperites sshj;
	
	
	public int getMaxIdle() {
		return maxIdle;
	}


	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}



	public int getIdleTime() {
		return idleTime;
	}



	public void setIdleTime(int idleTime) {
		this.idleTime = idleTime;
	}



	public int getMaxActive() {
		return maxActive;
	}



	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}



	public int getMaxWait() {
		return maxWait;
	}



	public void setMaxWait(int maxWait) {
		this.maxWait = maxWait;
	}



	public String getSshClientImplClass() {
		return sshClientImplClass;
	}



	public void setSshClientImplClass(String sshClientImplClass) {
		this.sshClientImplClass = sshClientImplClass;
	}



	public SshClientProperites getSshj() {
		return sshj;
	}



	public void setSshj(SshClientProperites sshj) {
		this.sshj = sshj;
	}



	public static class SshClientProperites{
		private String serverCommandPromotRegex;

		public String getServerCommandPromotRegex() {
			return serverCommandPromotRegex;
		}

		public void setServerCommandPromotRegex(String serverCommandPromotRegex) {
			this.serverCommandPromotRegex = serverCommandPromotRegex;
		}
		
	}
	
}
