package com.anchora.sre.service;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.woostju.ssh.SshClientConfig;
import com.github.woostju.ssh.SshResponse;
import com.github.woostju.ssh.multithread.JobExecutor;
import com.github.woostju.ssh.multithread.JobExecutorState;
import com.github.woostju.ssh.multithread.SyncThreadPool;
import com.github.woostju.ssh.pool.SshClientWrapper;
import com.github.woostju.ssh.pool.SshClientsPool;

public class TestSshClientsPool {
	
	private SshClientConfig clientConfig;
	
	private String host = "52.80.212.95";
	
	@Before
	public void init() throws IOException {
		String path = this.getClass().getClassLoader().getResource("id_rsa").getPath();
		clientConfig = new SshClientConfig(host, 22, "centos", null, path);
	}
	
	@After
	public void destroy() {
		SshClientsPool.pool().clear();
	}
	
	@Test
	public void testSuccess(){
		// 
		SyncThreadPool threadPool = SyncThreadPool.newPool();
		SshClientsPool clientsPool = SshClientsPool.pool(2, 2, 120, 100);
		Set<SshClientWrapper> clients = new HashSet<SshClientWrapper>();
		for (int i = 0; i < 4; i++) {
			threadPool.addJob("task"+i, ()->{
				SshClientWrapper client = clientsPool.client(clientConfig);
				clients.add(client);
				SshResponse response = client.executeCommand("echo '123'", 100);
				return response;
			});
		}
		Map<Object, JobExecutor> result = threadPool.run();
		
		List<DefaultPooledObjectInfo> objectsInPool = clientsPool.getObjects(clientConfig);
		assertArrayEquals(new Object[]{
				2, 4, 0, 2
		}, new Object[]{
				clients.size(),
				result.keySet().size(),
				((SshResponse)result.get("task1").getRtObject()).getCode(),
				objectsInPool.size()
		});
	}
	
	
	/**
	 * 当在池子中所有的client意外中断后，我们任然可以获取到可用的client 
	 */
	@Test
	public void testDisconnectedIdleclientsSuccess(){
		// 
		SyncThreadPool threadPool = SyncThreadPool.newPool();
		SshClientsPool clientsPool = SshClientsPool.pool(2, 2, 30, 100);
		Set<SshClientWrapper> clients = new HashSet<SshClientWrapper>();
		for (int i = 0; i < 2; i++) {
			threadPool.addJob("task"+i, ()->{
				SshClientWrapper client = clientsPool.client(clientConfig);
				clients.add(client);
				SshResponse response = client.executeCommand("echo '123'", 100);
				return response;
			});
		}
		Map<Object, JobExecutor> result = threadPool.run();
		clients.forEach(item->{
			item.disconnect();
		});
		
		threadPool.clearJobs();
		for (int i = 0; i < 2; i++) {
			threadPool.addJob("task"+i, ()->{
				SshClientWrapper client = clientsPool.client(clientConfig);
				SshResponse response = client.executeCommand("echo '123'", 100);
				return response;
			});
		}
		result = threadPool.run();
		List<DefaultPooledObjectInfo> objectsInPool = clientsPool.getObjects(clientConfig);
		assertArrayEquals(new Object[]{
				2, 0, 0, 2l
		}, new Object[]{
				result.keySet().size(),
				((SshResponse)result.get("task1").getRtObject()).getCode(),
				((SshResponse)result.get("task0").getRtObject()).getCode(),
				objectsInPool.stream().count()
		});
	}
	
	/**
	 * 如果客户端连接不上，那么是无法获取到client的
	 */
	@Test
	public void testUnreachableServerWillNotStayInPoolSuccess(){
		// 
		SyncThreadPool threadPool = SyncThreadPool.newPool();
		SshClientsPool clientsPool = SshClientsPool.pool(2, 2, 30, 100);
		Set<SshClientWrapper> clients = new HashSet<SshClientWrapper>();
		clientConfig.setUsername(clientConfig.getUsername()+"2");
		for (int i = 0; i < 2; i++) {
			threadPool.addJob("task"+i, ()->{
				SshClientWrapper client = clientsPool.client(clientConfig);
				clients.add(client);
				SshResponse response = client.executeCommand("echo '123'", 100);
				return response;
			});
		}
		Map<Object, JobExecutor> result = threadPool.run();
		List<DefaultPooledObjectInfo> objectsInPool = clientsPool.getObjects(clientConfig);
		assertArrayEquals(new Object[]{
				2, true, true, true
		}, new Object[]{
				result.keySet().size(),
				result.get("task1").getState() == JobExecutorState.failed,
				result.get("task0").getState() == JobExecutorState.failed,
				objectsInPool==null || objectsInPool.size()==0
		});
	}
	
	/**
	 * 测试client会被池回收
	 */
	@Test
	public void testClientsWillBeRecycledSuccess(){
		// 
		SyncThreadPool threadPool = SyncThreadPool.newPool();
		SshClientsPool clientsPool = SshClientsPool.pool(2, 2, 30, 100);
		Set<SshClientWrapper> clients = new HashSet<SshClientWrapper>();
		for (int i = 0; i < 2; i++) {
			threadPool.addJob("task"+i, ()->{
				SshClientWrapper client = clientsPool.client(clientConfig);
				clients.add(client);
				SshResponse response = client.executeCommand("echo '123'", 100);
				return response;
			});
		}
		Map<Object, JobExecutor> result = threadPool.run();
		List<DefaultPooledObjectInfo> objectsInPool = clientsPool.getObjects(clientConfig);
		assertArrayEquals(new Object[]{
				2, true, true, 2
		}, new Object[]{
				result.keySet().size(),
				result.get("task1").getState() == JobExecutorState.successful,
				result.get("task0").getState() == JobExecutorState.successful,
				objectsInPool.size()
		});
		
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		objectsInPool = clientsPool.getObjects(clientConfig);
		assertArrayEquals(new Object[]{
				true
		}, new Object[]{
				objectsInPool==null || objectsInPool.size()==0
		});
	}
	
}
