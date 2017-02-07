package org.inchain.wallet;

import java.util.HashMap;
import java.util.Map;

import javafx.stage.Stage;

/**
 * 全局环境变量
 * @author ln
 *
 */
public class Context {
	private static Map<String, Stage> stages = new HashMap<String, Stage>();
	
	public static boolean addStage(String name, Stage stage) {
		return stages.put(name, stage) != null;
	}
	
	public static Stage getStage(String name) {
		return stages.get(name);
	}
	
	public static Stage deleteStage(String name) {
		return stages.remove(name);
	}
	
	public static Stage getMainStage() {
		return stages.get("main");
	}
}
