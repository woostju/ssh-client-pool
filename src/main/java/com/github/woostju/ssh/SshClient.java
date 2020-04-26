package com.github.woostju.ssh;

import com.github.woostju.ssh.exception.SshException;

/**
 * 
 * @author jameswu
 *
 */
public interface SshClient {
	
	/**
	 * set the config
	 * @param config
	 * @return
	 */
	public SshClient init(SshClientConfig config);
	
	/**
	 * connect to server in timeoutInSeconds
	 */
	public SshClient connect(int timeoutInSeconds) throws SshException;
	
	/**
	 * auth with password
	 * @param username
	 * @param password
	 * @return
	 */
	public SshClient authPassword() throws SshException;
	
	/**
	 * auth with key
	 * @param username
	 * @return
	 */
	public SshClient authPublickey() throws SshException;
	
	/**
	 * in shellMode, you can communicate with server interactively, which means execute commands continuously 
	 * not in shellMode, only execute command once
	 */
	public SshClient startSession(boolean shellMode) throws SshException;
	
	/**
	 * @param command
	 * @param line
	 * @return
	 */
	public SshResponse executeCommand(String command, int timeoutInSeconds);
	
	/**
	 * 
	 * @param listener
	 * @return
	 */
	public SshClient setEventListener(SshClientEventListener listener);
	
	/**
	 * disconnect the server
	 */
	public void disconnect();
	
	/**
	 * state of SshClient 
	 */
	public SshClientState getState();
	
}
