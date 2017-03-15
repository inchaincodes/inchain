package org.inchain.rpc;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.Configure;
import org.inchain.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * 核心客户端RPC服务，RPC服务随核心启动，端口配置参考 {@link org.inchain.Configure.RPC_SERVER_PORT }
 * 命令列表： help 帮助命令，列表出所有命令
 * 
 * --- 区块相关 getblockcount 获取区块的数量 getnewestblockheight 获取最新区块的高度
 * getnewestblockhash 获取最新区块的hash getblockheader [param] (block hash or height)
 * 通过区块的hash或者高度获取区块的头信息 getblock [param] (block hash or height)
 * 通过区块的hash或者高度获取区块的完整信息
 * 
 * --- 内存池 getmempoolinfo [count] 获取内存里的count条交易
 * 
 * --- 帐户 newaccount [mgpw trpw] 创建帐户，同时必需指定帐户管理密码和交易密码 getaccountaddress
 * 获取帐户的地址 getaccountpubkeys 获取帐户的公钥 dumpprivateseed 备份私钥种子，同时显示帐户的hash160
 * 
 * getblanace 获取帐户的余额 gettransaction 获取帐户的交易记录
 * 
 * ---交易相关 TODO ···
 * 
 * @author ln
 *
 */
@Service
public class RPCServer implements Server {
	
	private final static Logger log = LoggerFactory.getLogger(RPCServer.class);
	
	public final static String RPC_USER_KEY = "rpc_user";
	public final static String RPC_PASSWORD_KEY = "rpc_password";
	
	//rpc参数配置
	private final static Properties property = new Properties();

	/**
	 * RPC服务启动方法，启动之后监听本地端口 {@link org.inchain.Configure.RPC_SERVER_PORT}提供服务
	 * 
	 */
	private static ExecutorService executor = Executors.newSingleThreadExecutor();

	@Autowired
	private RPCHanlder rpcHanlder;
	
	private boolean isRunning = false;
	
	public void startSyn() {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					RPCServer.this.start();
				} catch (IOException e) {
					log.error("rpc 服务启动失败，{}", e.getMessage(), e);
				}
			}
		};
		t.setName("rpc service");
		t.start();
		
		log.info("rpc service started");
	}

	public void start() throws IOException {
		
		init();
		
		ServerSocket server = new ServerSocket();
		server.bind(new InetSocketAddress(property.getProperty("rpc_host"), Integer.parseInt(property.getProperty("rpc_port"))));
		log.debug("rpc service started");
		try {
			isRunning = true;
			while (isRunning) {
				// 1.监听客户端的TCP连接，接到TCP连接后将其封装成task，由线程池执行
				executor.execute(new RPCRequestCertification(server.accept()));
			}
		} finally {
			server.close();
		}
	}

	/*
	 * rpc请求认证
	 * @author ln
	 *
	 */
	class RPCRequestCertification implements Runnable {

		private Socket socket;
		private BufferedReader br;
		private PrintWriter pw;
		
		public RPCRequestCertification(Socket socket) throws IOException {
			this.socket = socket;
			this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.pw = new PrintWriter(socket.getOutputStream());
		}
		
		@Override
		public void run() {
			try {
				JSONObject certificationInfo = readMessage();
				if(certificationInfo == null) {
					writeMessage(false, "解析rpc命令失败");
					return;
				}
				if(!certificationInfo.has(RPC_USER_KEY) || !certificationInfo.has(RPC_PASSWORD_KEY) || 
						!property.getProperty(RPC_USER_KEY).equals(certificationInfo.getString(RPC_USER_KEY))
						|| !property.getProperty(RPC_PASSWORD_KEY).equals(certificationInfo.getString(RPC_PASSWORD_KEY))) {
					writeMessage(false, "rpc认证失败");
					return;
				}
				
				//认证通过，处理业务逻辑
				writeMessage(true, "ok");
				
				JSONObject commandInfos = readMessage();
				if(commandInfos == null) {
					writeMessage(false, "rpc命令获取失败");
					return;
				}
				
				JSONObject result = rpcHanlder.hanlder(commandInfos);
				
				if(result.has("needInput") && result.getBoolean("needInput")) {
					writeMessage(result);
					JSONObject inputInfos = readMessage();
					result = rpcHanlder.hanlder(commandInfos, inputInfos);
					writeMessage(result);
				} else {
					writeMessage(result);
				}
			} catch (JSONException | IOException e) {
				try {
					writeMessage(false, "rpc命令错误，详情:" + e.getMessage());
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			} finally {
				try {
					close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		private JSONObject readMessage() throws JSONException, IOException {
			return new JSONObject(br.readLine());
		}
		
		private void writeMessage(boolean success, String msg) throws JSONException {
			JSONObject result = new JSONObject();
			result.put("success", success);
			result.put("message", msg);
			writeMessage(result);
		}
		
		private void writeMessage(JSONObject result) throws JSONException {
			try {
				pw.println(new String(result.toString().getBytes("utf-8")));
				pw.flush();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		public void close() throws IOException {
			br.close();
			pw.close();
			socket.close();
		}
	}
	
	/*
	 * 初始化rpc服务参数，如果有配置文件，则读取配置文件
	 * 如果没有配置文件，则生成新的rpc配置文件
	 */
	private void init() throws IOException {
		//判断配置文件是否存在
		InputStream in = RPCServer.class.getResourceAsStream("/rpc_config.properties");
		if(in != null) {
			property.load(in);
			in.close();
		}
		boolean refresh = false;
		if(!property.containsKey("rpc_host")) {
			refresh = true;
			property.put("rpc_host", Configure.RPC_SERVER_HOST);
		}
		if(!property.containsKey("rpc_port")) {
			refresh = true;
			property.put("rpc_port", "" + Configure.RPC_SERVER_PORT);
		}
		if(!property.containsKey(RPC_USER_KEY)) {
			refresh = true;
			property.put(RPC_USER_KEY, Configure.RPC_SERVER_USER);
		}
		if(!property.containsKey(RPC_PASSWORD_KEY)) {
			refresh = true;
			property.put(RPC_PASSWORD_KEY, StringUtil.randStr(20, 0));
		}
		
		//回写
		if(refresh) {
			FileOutputStream os = new FileOutputStream(RPCServer.class.getResource("/").getPath()+"rpc_config.properties");
			
			property.store(os, "this is rpc server configs");
			
			os.close();
		}
	}

	public void stop() {
		isRunning = false;
	}
}
