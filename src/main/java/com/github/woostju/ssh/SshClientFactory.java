package com.github.woostju.ssh;

/**
 * @author jameswu
 *
 */
public class SshClientFactory {
	
	static Class<?> clientClass = SshClientSSHJ.class;
	
	/**
	 * register your custom SshClient implementation to override the default
	 * @param clientClass
	 */
	public static void registerSshClientImpl(Class<SshClient> clientClass) {
		SshClientFactory.clientClass = clientClass;
	}
	
	/**
	 * 
	 */
	public static SshClient newInstance(SshClientConfig config){
		try {
			SshClient client = (SshClient)clientClass.newInstance();
			client.init(config);
			return client;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	} 
	
}
