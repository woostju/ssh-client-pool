# ssh-client-pool
a java implementation of ssh clients object pool with [sshj](https://github.com/hierynomus/sshj), [apache common pool2](https://github.com/apache/commons-pool), [expectIt](https://github.com/Alexey1Gavrilov/ExpectIt)



## usage

ssh-client-pool is available from **Maven Central**

```xml
<dependency>
  <groupId>com.github.woostju</groupId>
  <artifactId>ssh-client-pool</artifactId>
  <version>1.0.1-RELEASE</version>
</dependency>
```

### Who is this for?

Anyone who wants to connect server instances through SSH, send commands to server and consume the output continuously. 

Anyone who wants to cache the connected clients in an object pool, to reuse the client.


### How do I use this?

The SshClientsPool is auto-configured, use it directly as:

```java

@Autowired
SshClientsPool pool;

public void echo(){
	SshClientConfig clientConfig = new SshClientConfig("hostip", 22, "username", "password", null);
	SshClientWrapper client = pool.client(clientConfig);
	SshResponse response = client.executeCommand("echo 'hi'", 100);
	return response;
}

```

### Can I configure the pool?

You can configure SshClientsPool in your SpringBoot properties file as:

```
woostju.ssh-client-pool.maxActive=16
woostju.ssh-client-pool.maxIdle=16
woostju.ssh-client-pool.idleTime=20000
woostju.ssh-client-pool.maxWait=20000
```

what's more, you can also register the SshClientsPool yourself:

 ```java
@Bean
public SshClientsPool sshclientpool() {
	SshClientPoolConfig poolConfig = SshClientPoolConfig();
	poolConfig.setMaxTotalPerKey(maxTotal);
	poolConfig.setMaxIdlePerKey(maxIdle); 
	poolConfig.setBlockWhenExhausted(true);
	poolConfig.setMaxWaitMillis(1000L * maxWaitMillis); 
		
	poolConfig.setMinEvictableIdleTimeMillis(1000L * idleTime); 
	poolConfig.setTimeBetweenEvictionRunsMillis(1000L * idleTime);
	poolConfig.setTestOnBorrow(true); 
	poolConfig.setTestOnReturn(true); 
	poolConfig.setTestWhileIdle(true);
	poolConfig.setJmxEnabled(false); //disbale jmx
	
	return new SshClientsPool(poolConfig);
}
```

SshClientPoolConfig is a subclass of GenericKeyedObjectPool in Apacha Common Pool2, learn more from [apache common pool2](https://github.com/apache/commons-pool) to configure the pool.

### How does it work?

When you request a client from pool, it will pull an idle one, if there is no idle client, a created one return.
After you execute a command, it will return the client to pool as an idle one.
If you close the client explicitly, the client will be destroyed and remove from pool.

## License

This code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0).


## Additional Resources

* [hierynomus/sshj](https://github.com/hierynomus/sshj)  ssh, scp and sftp for java
* [apache common pool2](https://github.com/apache/commons-pool)  The Apache Commons Object Pooling Library.
* [Alexey1Gavrilov/expectIt](https://github.com/Alexey1Gavrilov/ExpectIt)  ExpectIt - is yet another pure Java 1.6+ implementation of the Expect tool. It is designed to be simple, easy to use and extensible. Written from scratch.
