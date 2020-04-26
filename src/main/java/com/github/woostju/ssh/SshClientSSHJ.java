package com.github.woostju.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.woostju.ssh.exception.AuthException;
import com.github.woostju.ssh.exception.LostConnectionException;
import com.github.woostju.ssh.exception.SshException;
import com.github.woostju.ssh.exception.TimeoutException;

import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.ExpectIOException;
import net.sf.expectit.Result;
import net.sf.expectit.matcher.Matcher;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.regexp;
import static net.sf.expectit.matcher.Matchers.contains;

/**
 * 
 * implement sshclient with net.schmizz.sshj.SSHClient
 * 
 * @author jameswu
 * 
 *         Trouble and shooting: 1. authPublicKey throw exceptions contains
 *         "net.schmizz.sshj.common.Buffer$BufferException:Bad item length"
 *         很可能是id_rsa文件格式，建议使用linux服务的ssh-keygen生成公钥私钥
 *
 */
public class SshClientSSHJ implements SshClient {

	private final static Logger logger = LoggerFactory.getLogger(SshClientSSHJ.class);

	private SshClientConfig clientConfig;
	
	private SSHClient client;

	private Expect expect = null;

	private Session session = null;

	private Shell shell = null;

	private boolean shellMode = false;

	private SshClientState state = SshClientState.inited;
	
	private SshClientEventListener eventListener;
	
	public static String COMMAND_PROMOT_REGEX_STR = "[\\[]?.+@.+~[\\]]?[#\\$] *";
	
	private static Matcher<Result> COMMAND_PROMOT_REGEX = regexp(COMMAND_PROMOT_REGEX_STR);
	
	// initialize DefaultConfig will consume resources, so we should cache it
	private static DefaultConfig defaultConfig = null;
	
	public static DefaultConfig getDefaultConfig() {
		if(defaultConfig==null) {
			defaultConfig = new DefaultConfig();
		}
		return defaultConfig;
	}
	
	@Override
	public SshClient init(SshClientConfig config) {
		this.clientConfig = config;
		return this;
	}
	
	private void validate() throws SshException{
		if(this.clientConfig == null) {
			throw new SshException("missing client config");
		}
	}
	
	/**
	 * 创建 ssh连接
	 */
	@Override
	public SshClient connect(int timeoutInSeconds) throws SshException{
		this.validate();
		if (timeoutInSeconds <= 0) {
			timeoutInSeconds = Integer.MAX_VALUE;
		} else {
			timeoutInSeconds = timeoutInSeconds * 1000;
		}
		return this.connect(timeoutInSeconds, false);
	}
	
	private SshClient connect(int timeoutInSeconds, boolean retry) throws SshException{
		logger.debug("connecting to " + this.clientConfig.getHost() + " port:" + this.clientConfig.port + " timeout in:"
				+ (timeoutInSeconds / 1000) + " s");
		client = new SSHClient(getDefaultConfig());
		try {
			client.setConnectTimeout(timeoutInSeconds);
			client.addHostKeyVerifier(new PromiscuousVerifier());
			// client.loadKnownHosts();
			client.connect(this.clientConfig.getHost().trim(), this.clientConfig.getPort());
			logger.debug("connected to " + this.clientConfig.getHost().trim() + " port:" + this.clientConfig.getPort());
		} catch (TransportException e) {
			if(!retry) {
				logger.error("sshj get exception when connect and will retry one more time ", e);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
				return this.connect(timeoutInSeconds, true);
			}else {
				String errorMessage ="connect to " + this.clientConfig.getHost().trim() + " failed";
				logger.error(errorMessage, e);
				throw new SshException(errorMessage, e);
			}
		} catch (Exception e) {
			String errorMessage ="connect to " + this.clientConfig.getHost().trim() + " failed";
			logger.error(errorMessage, e);
			throw new SshException(errorMessage, e);
		}
		return this;
	}

	@Override
	public SshClient setEventListener(SshClientEventListener listener) {
		this.eventListener = listener;
		return this;
	}
	
	@Override
	public SshClient authPassword() throws SshException {
		try {
			logger.debug("auth with password");
			client.authPassword(this.clientConfig.getUsername(), this.clientConfig.getPassword());
		} catch (Exception e) {
			String errorMessage = "ssh auth " + this.clientConfig.getHost() + " fail";
			logger.error(errorMessage, e);
			throw new AuthException(errorMessage, e);
		}
		return this;
	}

	@Override
	public SshClient authPublickey() throws SshException {
		try {
			logger.debug("auth with key:"+this.clientConfig.getUsername()+","+this.clientConfig.getPrivateKeyPath());
			if (this.clientConfig.getPrivateKeyPath() != null) {
				KeyProvider keys = client.loadKeys(this.clientConfig.getPrivateKeyPath());
				client.authPublickey(this.clientConfig.getUsername(), keys);
			} else {
				client.authPublickey(this.clientConfig.getUsername());
			}
		} catch (Exception e) {
			String errorMessage = "ssh auth " + this.clientConfig.getHost() + " fail";
			logger.error(errorMessage, e);
			throw new AuthException(errorMessage, e);
		}
		return this;
	}

	@Override
	public SshClient startSession(boolean shellMode) {
		logger.info("start session " + (shellMode ? " in shellMode" : ""));
		try {
			session = client.startSession();
			this.shellMode = shellMode;
			if (shellMode) {
				session.allocateDefaultPTY();
				shell = session.startShell();
				shell.changeWindowDimensions(1024, 1024, 20, 20);
				this.renewExpect(60);
				expect.expect(COMMAND_PROMOT_REGEX);
			}
			this.state = SshClientState.connected;
			try {
				if(this.eventListener!=null) {
					this.eventListener.didConnected(this);
				}
			} catch (Exception e) {
			}
		} catch (Exception e) {
			if(e instanceof ExpectIOException) {
				ExpectIOException ioException = (ExpectIOException)e;
				logger.error("start session fail with server input:"+ioException.getInputBuffer().replaceAll("[\\\n\\\r]", ""), e);
			}else {
				logger.error("start session fail", e);
			}
			this.disconnect();
			throw new RuntimeException("start session fail." + e.getMessage());
		} finally {
			// close expect
			try {
				if (expect != null) {
					expect.close();
				}
			} catch (IOException e) {
				logger.error("close IO error", e);
			}
			expect = null;
		}
		return this;
	}
	
	/**
	 * in shell mode, if server output content, code in response will be 0
	 * if not in shell mode, code in response 1 represents fail, 0 represents success
	 */
	@Override
	public synchronized SshResponse executeCommand(String command, int timeoutInSeconds) {
		if (this.shellMode) {
			return this.sendCommand(command, timeoutInSeconds);
		} else {
			return this.executeCommand_(command, timeoutInSeconds);
		}
	}

	/**
	 * execute the command with session.exec
	 */
	private SshResponse executeCommand_(String command, int timeoutInSeconds) {
		logger.info("execute command: " + command);
		SshResponse response = new SshResponse();
		try {
			Command cmd = session.exec(command);
			if (timeoutInSeconds < 0) {
				cmd.join(Long.MAX_VALUE, TimeUnit.SECONDS);
			} else {
				cmd.join(timeoutInSeconds, TimeUnit.SECONDS);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream(), "UTF-8"));
			BufferedReader error_reader = new BufferedReader(new InputStreamReader(cmd.getErrorStream(), "UTF-8"));
			List<String> outputLines = new ArrayList<>();
			logger.debug("finish executing command on " + this.clientConfig.getHost() + ", console:");
			String outputLine;
			while ((outputLine = error_reader.readLine()) != null) {
				logger.debug(outputLine);
				outputLines.add(outputLine);
			}
			while ((outputLine = reader.readLine()) != null) {
				logger.debug(outputLine);
				outputLines.add(outputLine);
			}
			response.setStdout(outputLines);
			logger.info(
					"execute ssh command on " + this.clientConfig.getHost() + " completed, with exit status:" + cmd.getExitStatus());
			response.setCode(cmd.getExitStatus());
		} catch (Exception e) {
			if (e.getCause() instanceof InterruptedException || e.getCause() instanceof java.util.concurrent.TimeoutException) {
				logger.error("execute ssh on " + this.clientConfig.getHost() + " timeout");
				response.setException(new TimeoutException("execute ssh command timeout"));
			} else {
				logger.error("execute ssh on " + this.clientConfig.getHost() + ", command error", e);
				response.setException(new SshException("execute ssh command error "+e.getMessage()));
			}
		}finally {
			try {
				if(this.eventListener!=null) {
					this.eventListener.didExecuteCommand(this);
				}
			} catch (Exception e) {
			}
		}
		return response;
	}

	private SshResponse sendCommand(String command, int timeoutInSeconds) {
		SshResponse response = new SshResponse();
		if (this.state != SshClientState.connected) {
			response.setException(new LostConnectionException("client not connected"));
			response.setCode(0);
			return response;
		}
		try {
			this.renewExpect(timeoutInSeconds);
			// start expect
			logger.info(this + " execute command : " + command);
			expect.send(command);
			logger.debug(this + " command sent ");
			if (!command.endsWith("\n")) {
				expect.send("\n");
				logger.debug(this + " command \\n sent ");
			}
			Result result2 = expect.expect(contains(command));
			Result result = expect.expect(COMMAND_PROMOT_REGEX);
			logger.debug("command execute success with raw output");
			logger.debug("------------------------------------------");
			String[] inputArray = result.getInput().split("\\r\\n");
			List<String> stout = new ArrayList<String>();
			if(inputArray.length>0) {
				for(int i=0;i<inputArray.length;i++) {
					logger.debug(inputArray[i]);
					if(i==inputArray.length-1 && inputArray[i].matches(COMMAND_PROMOT_REGEX_STR)) {
						break;
					}
					stout.add(inputArray[i]);
				}
			}
			logger.debug("------------------------------------------");
			response.setStdout(stout);
			response.setCode(0);
			logger.info("execute ssh command on " + this.clientConfig.getHost() + " completed, with code:" + 0);
		} catch (Exception e) {
			response.setCode(1);
			response.setException(new SshException(e.getMessage()));
			logger.error("execute command fail", e);
			if(e instanceof ArrayIndexOutOfBoundsException) {
				// server may be shutdown
				response.setException(new LostConnectionException("lost connection"));
				this.disconnect();
			} else if (e instanceof ClosedByInterruptException) {
				response.setException(new TimeoutException("execute command timeout"));
				this.sendCtrlCCommand();
			}
			else if (e.getCause() instanceof SocketException) {
				// the socket may be closed
				response.setException(new LostConnectionException("lost connection"));
				this.disconnect();
			} else if (e.getMessage().contains("timeout")) {
				response.setException(new TimeoutException("execute command timeout"));
				this.sendCtrlCCommand();
			}
			else {
				this.sendCtrlCCommand();
			}
		} finally {
			// close expect
			try {
				if (expect != null) {
					expect.close();
				}
			} catch (IOException e) {
				logger.error("close IO error", e);
			}
			expect = null;
			try {
				if(this.eventListener!=null) {
					this.eventListener.didExecuteCommand(this);
				}
			} catch (Exception e) {
			}
		}
		return response;
	}

	private void renewExpect(int timeoutInSeconds) throws IOException {
		if (expect!=null) {
			try {
				expect.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		expect = new ExpectBuilder().withOutput(shell.getOutputStream())
				.withInputs(shell.getInputStream(), shell.getErrorStream())
				.withInputFilters(removeColors(), removeNonPrintable()).withExceptionOnFailure()
				.withTimeout(timeoutInSeconds, TimeUnit.SECONDS).build();
	}

	private void sendCtrlCCommand() {
		try {
			logger.debug("send ctr-c command ... ");
			expect.send("\03");
			expect.expect(COMMAND_PROMOT_REGEX);
			logger.debug("send ctr-c command success ");
		} catch (IOException e1) {
			logger.error("send ctrl+c command fail", e1);
		}
	}

	@Override
	public void disconnect() {
		if(this.state==SshClientState.disconnected) {
			return;
		}
		this.state = SshClientState.disconnected;
		try {
			if (shell != null) {
				shell.close();
			}
		} catch (IOException e) {
			logger.error("close ssh shell error", e);
		}
		try {
			if (session != null) {
				session.close();
			}
		} catch (IOException e) {
			logger.error("close sesion error", e);
		}
		try {
			if (client != null) {
				client.disconnect();
				client.close();
			}
		} catch (IOException e) {
			logger.error("close ssh conenction error", e);
		}
		logger.debug("ssh disconnect");
		try {
			if(this.eventListener!=null) {
				this.eventListener.didDisConnected(this);
			}
		} catch (Exception e) {
		}
		
	}

	@Override
	public SshClientState getState() {
		return this.state;
	}
}
