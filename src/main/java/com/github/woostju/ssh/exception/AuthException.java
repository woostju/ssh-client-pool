package com.github.woostju.ssh.exception;

public class AuthException extends SshException{

	public AuthException(String message) {
		this(message, null);
	}
	
	public AuthException(String message, Throwable error) {
		super(message, error);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3961871786667342327L;

}
