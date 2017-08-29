package org.inchain.test;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Test {

	public static class Mytask implements Callable<String> {
		@Override
		public String call() throws Exception {
			System.out.println("---------------------start");
			Thread.sleep(4000l);
			System.out.println("---------------------end");
			return "result";
		}
		
	}
	
	public static class MytaskListener {
		public void done() {
			
		}
	}
	
	public static void main(String[] args) throws JSONException{
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		jsonArray.put(new JSONObject().put("name", "aaa").put("age", 1));
		jsonArray.put(new JSONObject().put("name", "bbb").put("age", 2));
		jsonArray.put(new JSONObject().put("name", "ccc").put("age", 3));
		jsonObject.put("infos", jsonArray);
		System.out.println(jsonObject.toString());
	}
}
