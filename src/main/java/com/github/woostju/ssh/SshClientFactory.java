package com.github.woostju.ssh;

import com.github.woostju.ssh.config.SshClientPoolConfig;
import com.github.woostju.ssh.exception.SshException;

/**
 * @author jameswu
 *
 */
public class SshClientFactory {
	
	/**
	 * 
	 */
	public static SshClient newInstance(SshClientConfig config){
		return newInstance(config, new SshClientPoolConfig());
	} 
	
	/**
	 * @throws SshException 
	 * 
	 */
	public static SshClient newInstance(SshClientConfig config, SshClientPoolConfig poolConfig){
		if (poolConfig.getSshClientImplClass()==null) {
			poolConfig.setSshClientImplClass(SshClientSSHJ.class);
		}
		try {
			SshClient client = (SshClient)poolConfig.getSshClientImplClass().newInstance();
			client.init(config);
			if(client instanceof SshClientSSHJ && poolConfig.getServerCommandPromotRegex()!=null) {
				((SshClientSSHJ)client).setCommandPromotRegexStr(poolConfig.getServerCommandPromotRegex());
			}
			return client;
		} catch (InstantiationException e) {
			throw new RuntimeException("new instance failed", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("new instance failed", e);
		}
	} 
	
}
