package org.inchain.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.Configure;
import org.inchain.utils.DateUtil;
import org.inchain.utils.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 版本检测更新服务
 * @author ln
 *
 */
@Service
public class VersionService {
	private static final Logger log = LoggerFactory.getLogger(VersionService.class);
	
	private static final String VERSION_SAVE_FILE = Configure.DATA_DIR + "/.version";
	
	private boolean runing;
	
	private int runModel;
	
	@PostConstruct
	public void init() {
		Thread t = new Thread() {
			@Override
			public void run() {
				startVersionCheck();
			}
		};
		t.setName("version service");
		t.start();
	}
	
	@PreDestroy
	public void stop() {
		runing = false;
	}

	protected void startVersionCheck() {
		runing = true;
		
		while(runing) {
			try {
				//每5分钟检测一次
				Thread.sleep(300000l);
				checkAndDown();
			} catch (Exception e) {
			}
		}
	}
	
	private void checkAndDown() throws JSONException {
		
		String url = "http://test.update.inchain.org/version.json";
		
		String response = RequestUtil.doGet(url, null);
		
		if(response == null) {
			return;
		}
		//远程版本
		JSONObject json = new JSONObject(response);
		//与本地版本进行对比
		JSONObject localVersion = readFromFile();
		if(localVersion.has("version") && !json.getString("version").equals(localVersion.getString("version"))) {
			//版本不一致，需要更新
			JSONArray filelist = json.getJSONArray("filelist");
			//保存目录
			File parent = new File(Configure.DATA_DIR).getParentFile();
			
			for (int i = 0; i < filelist.length(); i++) {
				String fileurl = filelist.getString(i);
				String fileFullUrl = "http://test.update.inchain.org" + fileurl;
				
				byte[] content = RequestUtil.get(fileFullUrl);
				
				File file = new File(parent, fileurl + ".update");
				
				if(!file.exists()) {
					file.getParentFile().mkdirs();
				}
				try {
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(content);
					fos.flush();
					fos.close();
					System.out.println(file);
					
					File oldFile = new File(parent, fileurl);

					File bakFile = new File(parent, fileurl + ".bak");
					
					nioTransferCopy(oldFile, bakFile);
					nioTransferCopy(file, oldFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			//成功后回写文件
			writeToFile(json);
			
			//重新启动
    		try {
    			if(runModel == 1) {
    				if(System.getProperty("os.name").toUpperCase().contains("LINUX")) {
	    				String path = "nohup " + System.getProperty("user.dir") + "/bin/inchain restart &";
	    				
	    				log.info("运行命令：{}", path);
    				}
    			} else {
    				//只针对win添加计划任务重启
    				if(System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
    					String path = System.getProperty("user.dir") + "\\inchain.exe";
    					
    					Date startTime = new Date(System.currentTimeMillis() + 60000);
    					String time = DateUtil.convertDate(startTime, "HH:mm:ss");
    					String date = DateUtil.convertDate(startTime, "yyyy/MM/dd");
    					String cmd = "schtasks /create /tn inchain_autoupdate /tr \"" + path + "\" /sc ONCE /st " + time + " /sd " + date + " /F";
    					Runtime.getRuntime().exec(cmd);
    					log.info("重新启动：{}", cmd);
    				}
    			}
			} catch (IOException e) {
				log.error("", e);
			}
            
            //关闭程序
            System.exit(0);  
		} else if(!localVersion.has("version")) {
			//第一次写文件
			writeToFile(json);
		}
	}
	private static void nioTransferCopy(File source, File target) {  
	    FileChannel in = null;  
	    FileChannel out = null;  
	    FileInputStream inStream = null;  
	    FileOutputStream outStream = null;  
	    try {  
	        inStream = new FileInputStream(source);  
	        outStream = new FileOutputStream(target);  
	        in = inStream.getChannel();  
	        out = outStream.getChannel();  
	        in.transferTo(0, in.size(), out);  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } finally {
	    	try {
		    	if(inStream != null) {
		    		inStream.close();
		    	}
		    	if(in != null) {
		    		in.close();
		    	}
		    	if(outStream != null) {
		    		outStream.close();
		    	}
		    	if(out != null) {
		    		out.close();
		    	}
	    	} catch (Exception e) {
	    		e.printStackTrace();
			}
	    }  
	}
	
	public static void writeToFile(JSONObject json) {
        File file = new File(VERSION_SAVE_FILE);
        try {
            PrintWriter pw = new PrintWriter(file);
            pw.write(json.toString());
            pw.flush();
            pw.close();
        } catch (IOException e) {
        	log.warn("write version failed! {}", e.getMessage());
        }
    }
	
	public static JSONObject readFromFile() {
        File file =new File(VERSION_SAVE_FILE);
        if(!file.exists()) {
        	return new JSONObject(); 
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
			br.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) {
        	e.printStackTrace();
        	file.delete();
        }
        return new JSONObject();
    }

	public int getRunModel() {
		return runModel;
	}

	public void setRunModel(int runModel) {
		this.runModel = runModel;
	}
}
