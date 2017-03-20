package org.inchain.rpc;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.Configure;

/**
 * rpc客户端，通过命令调用远程客户端功能
 * 用法示例 java org.inchain.rpc.RPCClient [-options] <command> [params]
 * [-options] 包括：
 *  -help     		客户端帮助，显示命令列表
 * 	-rpc_host   	远程服务端地址
 * 	-rpc_port		 远程服务端端口
 *  -rpc_user	 	 远程服务端访问用户名
 *  -rpc_password 	远程服务端访问密码
 *  -config	  		配置文件地址，以上配置都可直接配置到文件里
 *  
 * <command> 包括：
 *  help	显示服务端命令列表
 *  其它参考 {@link org.inchain.rpc.RPCServer}
 *  
 *  [params] 也参考  {@link org.inchain.rpc.RPCServer}
 *  
 * @author ln
 *
 */
public class RPCClient {
	
	public final static String RPC_HOST_KEY = "rpc_host";
	public final static String RPC_PORT_KEY = "rpc_port";
	public final static String RPC_USER_KEY = "rpc_user";
	public final static String RPC_PASSWORD_KEY = "rpc_password";
	
	private Socket socket;
	private BufferedReader br;
	private PrintWriter pw;
	
	public static void main(String[] args) throws IOException, JSONException {
//		args = new String[12];
//		args[0] = "-rpc_host";
//		args[1] = "localhost";
//		args[2] = "-rpc_port";
//		args[3] = "8632";
//		args[4] = "-rpc_user";
//		args[5] = "user";
//		args[6] = "-rpc_password";
//		args[7] = "klJZKtyloogQzZRTHPKj";
//		args[8] = "help";
//		args[9] = "tb1LkNUTGgtPBhu5MNGoK8Hdfp3tJi7nKa";
//		args[10] = "5.111";
//		args[11] = "0";
		
		String result = new RPCClient().processCmd(args);
		printMsg(result);
	}

	/*
	 * 处理命令
	 * @param args
	 * @return
	 */
	private String processCmd(String[] commands) throws IOException, JSONException {
		if(commands == null || commands.length == 0) {
			return "缺少参数";
		}
		
		//先判断是不是help命令
		if(commands.length == 1 && "-help".equals(commands[0].trim())) {
			return helpInfos();
		}

		try {
			//rpc参数
			JSONObject optionsInfos = new JSONObject();
			//发送到服务端的命令
			JSONObject remoteCommandsInfos = new JSONObject();
			
			//分析命令
			analysisCommands(commands, optionsInfos, remoteCommandsInfos);
			
			//帮助命令，直接打印
			if("help".equals(remoteCommandsInfos.getString("command"))) {
				return RPCHanlder.getHelpCommands();
			}
			
			//初始化rpc服务器信息
			initServer(optionsInfos);
			
			//发送钱包命令到rpc服务器
			sendCommands(remoteCommandsInfos);
			
			//接收服务器响应
			JSONObject result = receiveResult();
			
			//判断是否需要输入信息才能继续
			result = processInput(result);
			return result.toString(6).toString();
		} finally {
			close();
		}
	}

	/*
	 * 处理需要输入信息才能继续的命令
	 * 主要是密码相关
	 */
	private JSONObject processInput(JSONObject result) throws IOException {
		try {
			if(result.has("needInput") && result.getBoolean("needInput")) {
				Console console = System.console();
				if(result.has("inputTip")) {
					System.out.println(result.get("inputTip"));
				}
				JSONObject inputInfos = new JSONObject();
				
				int type = result.getInt("inputType");
				
				//只输一次的密码
				if(type == 1) {
//				inputInfos.put("input", new String(console.readPassword("*")));
					Scanner sc = new Scanner(System.in);
					inputInfos.put("password", sc.nextLine());
					sc.close();
				} else if(type == 2) {
					//输2次密码，并且两次要一致，不一致就退出
					Scanner sc2 = new Scanner(System.in);
					
					String p1 = sc2.nextLine();
					
					System.out.println("再次确认密码");
					
					String p2 = sc2.nextLine();
					
					if(!p1.equals(p2)) {
						System.out.println("两次输入的密码不一致");
						closeAndExit();
					}
					
					inputInfos.put("newPassword", p1);
					sc2.close();
					
				} else if(type == 3) {
					
					System.out.println("请输入钱包旧密码");
					
					Scanner sc = new Scanner(System.in);
					inputInfos.put("password", sc.nextLine());
					
					System.out.println("请输入钱包新密码");
					//修改密码，输1次原密码，2次新密码，并且两次要一致，不一致就退出
					
					String p1 = sc.nextLine();
					
					System.out.println("再次确认新密码");
					
					String p2 = sc.nextLine();
					
					if(!p1.equals(p2)) {
						System.out.println("两次输入的新密码不一致");
						closeAndExit();
					}
					
					inputInfos.put("newPassword", p1);
					sc.close();
				}
				
				sendCommands(inputInfos);
				result = receiveResult();
				
				//是否需要再次输入
				JSONObject newResult = processInput(result);
				return newResult;
			}
		} catch (Exception e) {
			return result;
		}
		return result;
	}

	/*
	 * 接收响应信息
	 */
	private JSONObject receiveResult() throws JSONException, IOException {
		String s = br.readLine();
		JSONObject result = new JSONObject(s);
		return result;
	}

	/*
	 * 发送命令
	 */
	private void sendCommands(JSONObject infos) {
		try {
			pw.println(new String(infos.toString().getBytes("utf-8")));
			pw.flush();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 初始化rpc参数
	 */
	private void initServer(JSONObject optionsInfos) throws IOException, JSONException {
		
		//rpc参数配置
		Properties property = new Properties();
		if(optionsInfos.has("config")) {
			try {
				FileInputStream in = new FileInputStream(optionsInfos.getString("config"));
				property.load(in);
				in.close();
			} catch (Exception e) {
				printError("配置文件错误");
				closeAndExit();
			}
		} else {
			InputStream in = RPCClient.class.getResourceAsStream("/rpc_config.properties");
			if(in != null) {
				property.load(in);
				in.close();
			}
		}
		
		//没有的情况，就直接读取配置文件
		if((optionsInfos == null || optionsInfos.length() == 0) && property.isEmpty()) {
			printError("缺少rpc认证参数");
			closeAndExit();
		}
		if(optionsInfos.has(RPC_HOST_KEY)) {
			property.put(RPC_HOST_KEY, optionsInfos.get(RPC_HOST_KEY));
		}
		if(optionsInfos.has(RPC_PORT_KEY)) {
			property.put(RPC_PORT_KEY, optionsInfos.get(RPC_PORT_KEY));
		}
		if(optionsInfos.has(RPC_USER_KEY)) {
			property.put(RPC_USER_KEY, optionsInfos.get(RPC_USER_KEY));
		}
		if(optionsInfos.has(RPC_PASSWORD_KEY)) {
			property.put(RPC_PASSWORD_KEY, optionsInfos.get(RPC_PASSWORD_KEY));
		}
		
		//判断参数是否齐全
		if(!property.containsKey(RPC_HOST_KEY) || !property.containsKey(RPC_PORT_KEY) 
				|| !property.containsKey(RPC_USER_KEY) || !property.containsKey(RPC_PASSWORD_KEY)) {
			printError("rpc参数不齐全");
			closeAndExit();
		}
		//连接并认证
		try {
			socket = new Socket(property.getProperty(RPC_HOST_KEY), Integer.parseInt(property.getProperty(RPC_PORT_KEY)));
		} catch (Exception e) {
			printError("连接不成功");
			closeAndExit();
		}
		this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.pw = new PrintWriter(socket.getOutputStream());
		
		JSONObject certificationInfo = new JSONObject();
		certificationInfo.put(RPC_USER_KEY, property.getProperty(RPC_USER_KEY));
		certificationInfo.put(RPC_PASSWORD_KEY, property.getProperty(RPC_PASSWORD_KEY));
		sendCommands(certificationInfo);
		
		JSONObject result = receiveResult();
		
		if(!result.getBoolean("success")) {
			System.out.println(result.toString());
			closeAndExit();
		}
	}

	private void printError(String msg) throws JSONException {
		JSONObject result = new JSONObject().put("success", false).put("message", msg);
		System.out.println(result.toString());
	}

	/*
	 * 分析命令
	 */
	private void analysisCommands(String[] commands, JSONObject optionsInfos, JSONObject remoteCommandsInfos) throws JSONException {
		JSONArray params = new JSONArray();
		for (int i = 0; i < commands.length; i++) {
			String command = commands[i];
			if(command.startsWith("-")) {
				String key = command.substring(1);
				String value = "";
				if(i + 1 < commands.length) {
					value = commands[i + 1];
					i++;
				}
				optionsInfos.put(key, value);
			} else {
				if(remoteCommandsInfos.length() == 0) {
					remoteCommandsInfos.put("command", command);
				} else {
					params.put(command);
				}
			}
		}
		remoteCommandsInfos.put("params", params);
	}

	/*
	 * 打印帮助内容
	 */
	private String helpInfos() {
		StringBuilder sb = new StringBuilder();

		sb.append("*  \n");
		sb.append("* rpc客户端，通过命令调用远程客户端功能 \n");
		sb.append("* 用法示例 rpc [-options] <command> [params] \n");
		sb.append("* [-options] 包括： \n");
		sb.append("*  -help     	客户端帮助，显示命令列表 \n");
		sb.append("*  -rpc_host     	远程服务端地址 \n");
		sb.append("*  -rpc_port     	远程服务端口\n");
		sb.append("*  -rpc_user     	远程服务端访问用户名 \n");
		sb.append("*  -rpc_password  	远程服务端访问密码 \n");
		sb.append("*  -config     	配置文件地址，以上配置都可直接配置到文件里 \n");
		sb.append("*  \n*  \n");
		sb.append("*  示例：rpc -server 127.0.0.1 -port " + Configure.RPC_SERVER_PORT + " -user rpcuser -password password getblockcount \n");
		sb.append("*  \n");
		sb.append("*  如需获取钱包命令帮助，使用rpc [-options] help \n");
		sb.append("*  \n");
		
		return sb.toString();
	}
	
	private static void printMsg(String msg) {
		System.out.println(msg);
	}

	private void closeAndExit() throws IOException {
		close();
		System.exit(0);
	}

	private void close() throws IOException {
		if(br != null) {
			br.close();
		}
		if(pw != null) {
			pw.close();
		}
		if(socket != null) {
			socket.close();
		}
	}
}
