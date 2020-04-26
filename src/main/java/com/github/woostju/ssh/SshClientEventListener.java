package com.github.woostju.ssh;

/**
 * 
 * used by SshClientPool to manage the SshClient state
 * @author jameswu
 *
 */
public interface SshClientEventListener {
	
	/**
	 * 
	 * @param client
	 */
	public void didExecuteCommand(Object client);
	/**
	 * 
	 * @param client
	 */
	public void didDisConnected(Object client);
	/**
	 * 
	 * @param client
	 */
	public void didConnected(Object client);
}
