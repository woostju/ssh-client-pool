package com.github.woostju.ssh;

import java.util.ArrayList;
import java.util.List;


public class SshResponse {
	
	private int code;
	
	private Exception exception;
	
	private List<String> stdout = new ArrayList<>();
	
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public Exception getException() {
		return exception;
	}
	public void setException(Exception exception) {
		this.exception = exception;
	}
	public List<String> getStdout() {
		return stdout;
	}
	public void setStdout(List<String> stdout) {
		this.stdout = stdout;
	}
	
	
	
}
