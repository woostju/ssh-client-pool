package com.github.woostju.ssh.pool;

import java.util.List;
import java.util.Map;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.woostju.ssh.SshClientConfig;
import com.github.woostju.ssh.SshClientEventListener;
import com.github.woostju.ssh.SshClientState;
import com.github.woostju.ssh.config.SshClientPoolConfig;
import com.github.woostju.ssh.exception.SshException;

/**
 * 
 * @author jameswu
 * 
 * sshClient的对象池
 * 池子中保存了连接上的SshClient
 * 
 * common pool 2
 * 
 * 对每个主机的ssh连接进行池化，以减少创建ssh连接的开销。
 * 但是，因为每台主机可以创建的ssh连接数是有限的，所以我们需要对超过连接数的请求进行阻塞
 * 
 */

public class SshClientsPool extends GenericKeyedObjectPool<SshClientConfig, SshClientWrapper>{
	private final static Logger logger = LoggerFactory.getLogger(SshClientsPool.class);
	
	
	// 每隔【recycle_window】对idle的client进行销毁
	static long recycle_window = 120;
	
	// 每个主机的工作client数，超过该数，请求新的client需要等待工作client空闲
	static int core_pool_size = 20;
	
	static int client_connect_timeout = 60;
	
	static long request_wait_timeout = 120;
	
	private SshClientPoolConfig poolConfig;
	/**
	 * maxTotal 20, maxIdle 20, idleTime 120 seconds, maxWaitMillis 120 seconds
	 * @return
	 */
	public SshClientsPool() {
		this(core_pool_size, core_pool_size, recycle_window, request_wait_timeout);
	}
	
	/**
	 * 
	 * @param poolConfig
	 */
	public SshClientsPool(SshClientPoolConfig poolConfig) {
		super(new SshClientsObjectFactory(), poolConfig);
		((SshClientsObjectFactory)this.getFactory()).setSshClientsPool(this);
		this.poolConfig = poolConfig;
	}
	
	/**
	 * 
	 * @param maxTotal pool 中允许的最大对象数
	 * @param maxIdle pool中允许的最大空闲对象数
	 * @param idleTime 当pool中已经达到最大值并无空闲，请求者将被阻塞MaxWaitMillis时间
	 * @param maxWaitMillis 阻塞等待时间
	 * @return
	 */
	public SshClientsPool(int maxTotal, int maxIdle, long idleTime, long maxWaitMillis) {
		this(new SshClientPoolConfig(maxTotal, maxIdle, idleTime, maxWaitMillis));
	}
	
	public SshClientWrapper client(SshClientConfig config) {
		try {
			return this.borrowObject(config);
		} catch (Exception e) {
			logger.error("create ssh client error", e);
			throw new RuntimeException(e);
		}
	}
	
	public SshClientPoolConfig getPoolConfig() {
		return poolConfig;
	}

	public List<DefaultPooledObjectInfo> getObjects(SshClientConfig config) {
		Map<String, List<DefaultPooledObjectInfo>> objects = listAllObjects();
		return objects.get(config.toString());
	}
	
	@Override
	public void clear() {
		super.clear();
	}
	
}

class SshClientsObjectFactory extends BaseKeyedPooledObjectFactory<SshClientConfig, SshClientWrapper> implements SshClientEventListener{
	private final static Logger logger = LoggerFactory.getLogger(SshClientsObjectFactory.class);
	
	private SshClientsPool pool;
	
	public void setSshClientsPool(SshClientsPool pool) {
		this.pool = pool;
	}
	
	@Override
	public boolean validateObject(SshClientConfig key, PooledObject<SshClientWrapper> p) {
		return p.getObject().getState() == SshClientState.connected;
	}

	@Override
	public void destroyObject(SshClientConfig key, PooledObject<SshClientWrapper> p) throws Exception {
		logger.debug("销毁对象 "+p);
		p.getObject().setEventListener(null);
		p.getObject().disconnect();
	}
	
	@Override
	public void activateObject(SshClientConfig key, PooledObject<SshClientWrapper> p) throws Exception {
		super.activateObject(key, p);
	}
	
	@Override
	public PooledObject<SshClientWrapper> wrap(SshClientWrapper value) {
		return new DefaultPooledObject<SshClientWrapper>(value);
	}
	
	@Override
	public SshClientWrapper create(SshClientConfig config) {
		SshClientWrapper wrapper = new SshClientWrapper(config, pool.getPoolConfig());
		try {
			wrapper.setEventListener(this).connect(SshClientsPool.client_connect_timeout).auth().startSession();
		} catch (SshException e) {
			throw new RuntimeException("create ssh client fail");
		}
		return wrapper;
	}

	@Override
	public void didExecuteCommand(Object client) {
		if(client instanceof SshClientWrapper) {
			SshClientWrapper wrapper = (SshClientWrapper)client;
			pool.returnObject(wrapper.getConfig(), wrapper);
		}
	}

	@Override
	public void didDisConnected(Object client) {
		if(client instanceof SshClientWrapper) {
			SshClientWrapper wrapper = (SshClientWrapper)client;
			try {
				pool.invalidateObject(wrapper.getConfig(), wrapper);
			} catch (Exception e) {
				logger.error("invalidate object "+client+" failed",e);
			}
		}
	}
	
	@Override
	public void didConnected(Object client) {
		
	}
	
}
