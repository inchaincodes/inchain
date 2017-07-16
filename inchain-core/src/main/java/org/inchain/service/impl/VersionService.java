package org.inchain.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.Configure;
import org.inchain.core.Definition;
import org.inchain.listener.VersionUpdateListener;
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

	private static final String UPDATE_DOMAIN = "http://test1.update.inchain.org";
	
	private boolean runing;
	
	private int runModel;
	//是否有新版本
	private boolean hasNew;
	private JSONObject versionJsonInfo;
	
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

	/**
	 * 检查最新
	 * @return JSONObject
	 * @throws JSONException 
	 */
	public JSONObject check() throws JSONException {
		JSONObject json = new JSONObject();
		
		if(hasNew && versionJsonInfo != null) {
			json.put("success", true).put("newVersion", true)
			.put("version", versionJsonInfo.getString("version"))
			.put("fileList", versionJsonInfo.getJSONArray("filelist"));
		} else {
			json.put("success", true).put("newVersion", false);
		}
		
		return json;
	}
	
	/**
	 * 更新版本
	 * @param listener
	 * @throws JSONException 
	 */
	public void update(VersionUpdateListener listener) throws JSONException {
		
		if(!hasNew) {
			return;
		}
		
		//版本不一致，需要更新
		JSONArray filelist = versionJsonInfo.getJSONArray("filelist");
		//保存目录
		File parent = new File(Configure.DATA_DIR).getParentFile();
		
		if(listener != null) {
			listener.startDownload();
		}
		
		for (int i = 0; i < filelist.length(); i++) {
			String fileurl = filelist.getString(i);
			String fileFullUrl = UPDATE_DOMAIN + fileurl;
			
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
				
				File oldFile = new File(parent, fileurl);

				if(oldFile.exists()) {
					//备份旧版本
					File bakFile = new File(parent, fileurl + ".bak");
					nioTransferCopy(oldFile, bakFile);
				}
				nioTransferCopy(file, oldFile);
				
				file.delete();
				
				if(listener != null) {
					listener.downloading(fileurl, (float)(i+1)/filelist.length());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//成功后回写文件
		writeToFile(versionJsonInfo);
		if(listener != null) {
			listener.onComplete();
		}
	}
	
	/**
	 * 获取最新版本
	 * @return String
	 * @throws JSONException
	 */
	public String getNewestVersion() throws JSONException {
		String version = "unknown";
		if(versionJsonInfo != null) {
			version = versionJsonInfo.getString("version");
		}
		return version;
	}

	protected void startVersionCheck() {
		runing = true;
		
		while(runing && !hasNew) {
			try {
				//每分钟检测一次
				Thread.sleep(5000l);
				checkAndDown();
				Thread.sleep(55000l);
			} catch (Exception e) {
			}
		}
	}
	
	private void checkAndDown() throws JSONException {
		
		String url = UPDATE_DOMAIN + "/version.json";
		
		String response = RequestUtil.doGet(url, null);
		
		if(response == null) {
			return;
		}
		//远程版本
		versionJsonInfo = new JSONObject(response);
		
		if(!versionJsonInfo.getString("version").equals(Definition.LIBRARY_SUBVER)) {
			
			hasNew = true;
			
//			//版本不一致，需要更新
//			JSONArray filelist = versionJsonInfo.getJSONArray("filelist");
//			//保存目录
//			File parent = new File(Configure.DATA_DIR).getParentFile();
//			
//			for (int i = 0; i < filelist.length(); i++) {
//				String fileurl = filelist.getString(i);
//				String fileFullUrl = "http://test.update.inchain.org" + fileurl;
//				
//				byte[] content = RequestUtil.get(fileFullUrl);
//				
//				File file = new File(parent, fileurl + ".update");
//				
//				if(!file.exists()) {
//					file.getParentFile().mkdirs();
//				}
//				try {
//					FileOutputStream fos = new FileOutputStream(file);
//					fos.write(content);
//					fos.flush();
//					fos.close();
//					System.out.println(file);
//					
//					File oldFile = new File(parent, fileurl);
//
//					File bakFile = new File(parent, fileurl + ".bak");
//					
//					nioTransferCopy(oldFile, bakFile);
//					nioTransferCopy(file, oldFile);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			
//			//成功后回写文件
//			writeToFile(versionJsonInfo);
//			
//			//重新启动
//    		try {
//    			if(runModel == 1) {
//    				if(System.getProperty("os.name").toUpperCase().contains("LINUX")) {
//	    				String path = "nohup " + System.getProperty("user.dir") + "/bin/inchain restart &";
//	    				
//	    				log.info("运行命令：{}", path);
//    				}
//    			} else {
//    				//只针对win添加计划任务重启
//    				if(System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
//    					String path = System.getProperty("user.dir") + "\\inchain.exe";
//    					
//    					Date startTime = new Date(System.currentTimeMillis() + 60000);
//    					String time = DateUtil.convertDate(startTime, "HH:mm:ss");
//    					String date = DateUtil.convertDate(startTime, "yyyy/MM/dd");
//    					String cmd = "schtasks /create /tn inchain_autoupdate /tr \"" + path + "\" /sc ONCE /st " + time + " /sd " + date + " /F";
//    					Runtime.getRuntime().exec(cmd);
//    					
//    					log.info("重新启动：{}", cmd);
//    					
//    					while(true) {
//	    		            String seconds = DateUtil.convertDate(new Date(), "ss");
//	    		            int i = Integer.parseInt(seconds);
//	    		            if(i > 50) {
//		    		            //关闭程序
//		    		            System.exit(0);
//	    		            }
//    					}
//    				}
//    			}
//			} catch (IOException e) {
//				log.error("", e);
//			}
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
