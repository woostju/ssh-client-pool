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
import com.github.woostju.ssh.exception.SshException;

/**
 * 
 * SshClient objects pool
 * <p>Cache shell mode connected SshClient in the pools, to save connect time, also improve performance
 * 
 * @author jameswu
 */

public class SshClientsPool extends GenericKeyedObjectPool<SshClientConfig, SshClientWrapper>{
	private final static Logger logger = LoggerFactory.getLogger(SshClientsPool.class);
	
	private SshClientPoolConfig poolConfig;
	
	/**
	 * maxActive 20, maxIdle 20,  maxWaitMillis 40 seconds
	 */
	public SshClientsPool() {
		this(20, 20, 120*1000l, 120*1000l);
	}
	
	/**
	 * 
	 * @param poolConfig create SshClientsPool with {@code poolConfig}
	 */
	public SshClientsPool(SshClientPoolConfig poolConfig) {
		super(new SshClientsObjectFactory(), poolConfig);
		((SshClientsObjectFactory)this.getFactory()).setSshClientsPool(this);
		this.poolConfig = poolConfig;
	}
	
	/**
	 * 
	 * @param maxActive max clients in pool
	 * @param maxIdle max idle clients in pool
	 * @param idleTime idle time clients live in the pool
	 * @param maxWaitTime wait time when request block
	 */
	public SshClientsPool(int maxActive, int idle, long idleTime, long maxWaitTime) {
		this(new SshClientPoolConfig(maxActive, idle, idleTime, maxWaitTime));
	}
	
	/**
	 * request a connected client from pool, may be a cached one, maybe a brand-new one  
	 * @param config the connection information to host
	 * @return SshClientWrapper
	 */
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

	/**
	 * query objects with same server connection information
	 * @param config server connection information
	 * @return lists of DefaultPooledObjectInfo with SshClientWrapper inside
	 */
	public List<DefaultPooledObjectInfo> getObjects(SshClientConfig config) {
		Map<String, List<DefaultPooledObjectInfo>> objects = listAllObjects();
		return objects.get(config.toString());
	}
	
	@Override
	public SshClientWrapper borrowObject(SshClientConfig key) throws Exception {
		SshClientWrapper object = super.borrowObject(key);
		logger.debug("borrow object:" + object);
		return object;
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
		logger.debug("destroy object: "+p);
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
			wrapper.setEventListener(this).connect(60).auth().startSession();
		} catch (SshException e) {
			throw new RuntimeException("create ssh client fail");
		}
		logger.debug("sshclient created: "+wrapper);
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
