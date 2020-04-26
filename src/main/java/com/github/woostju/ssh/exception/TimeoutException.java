package com.github.woostju.ssh.exception;

public class TimeoutException extends SshException{

	public TimeoutException(String message) {
		this(message, null);
	}
	
	public TimeoutException(String message, Throwable error) {
		super(message, error);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3961871786667342727L;

}
