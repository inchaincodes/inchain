package org.inchain.wallet.controllers;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.SpringContextUtils;
import org.inchain.rpc.RPCHanlder;
import org.inchain.utils.StringUtil;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class ConsoleController extends DailogController{

	public Button sendId;
	public TextArea contentId;
	public TextField commandId;
	
	public RPCHanlder rpcHanlder;
	
	public void initialize() {
		
		contentId.setWrapText(true);
		contentId.setText(RPCHanlder.getHelpCommands());
		updateContent();
		
		commandId.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.ENTER){
                	updateContent();
                }
            }
        });
		
		sendId.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				updateContent();
			}
		});
		
		rpcHanlder = SpringContextUtils.getBean(RPCHanlder.class);
	}
	
	private void updateContent() {
		
		
		String command = commandId.getText().trim();
		if(StringUtil.isNotEmpty(command)) {
			try {
				
				String[] commands = StringUtil.split(command);
				
				String result = null;
				if("help".equals(commands[0])) {
					result = RPCHanlder.getHelpCommands();
				} else {
					JSONObject commandInfos = new JSONObject();
					commandInfos.put("command", commands[0]);
					
					JSONArray params = new JSONArray();
					if(commands.length > 1) {
						for (int i = 1; i < commands.length; i++) {
							String param = commands[i];
							params.put(param);
						}
					}
					commandInfos.put("params", params);
					
					JSONObject resultJson = rpcHanlder.hanlder(commandInfos);
					if(resultJson.has("needInput") && resultJson.getBoolean("needInput")) {
					} else {
					}
					result = resultJson.toString(3);
				}
				contentId.setText(contentId.getText().trim() + "\n" + command + "\n" + result + "\n\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		commandId.setText("");
		new Thread() {
			public void run() {
				try {
					Thread.sleep(20l);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Platform.runLater(new Runnable() {
				    @Override
				    public void run() {
				    	contentId.setScrollTop(Double.MAX_VALUE);
				    }
				});
			};
		}.start();
	}
}
