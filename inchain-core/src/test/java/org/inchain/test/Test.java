package org.inchain.test;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.store.TransactionStore;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
		try {
			byte[] b = "胜多负少的".getBytes("utf-8");

			String s = new String(b, "utf-8");
			System.out.println(s);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
