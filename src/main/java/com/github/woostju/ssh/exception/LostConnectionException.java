package com.github.woostju.ssh.exception;

public class LostConnectionException extends SshException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3961871786667342727L;

	public LostConnectionException(String message) {
		this(message, null);
	}
	
	public LostConnectionException(String message, Throwable error) {
		super(message, error);
	}
}
