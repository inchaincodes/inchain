package org.inchain.test;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.crypto.ECKey;
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
	
	public static void main(String[] args){
//		ECKey key1 = new ECKey();
//		ECKey key2 = new ECKey();
//
//		System.out.println(new String(key1.getPubKey()));
//
//		System.out.println(new String(key2.getPubKey()));

		long max = Long.MAX_VALUE;

		if(3 == 0x7fffffffffffffffL + 0x7fffffffffffffffL + 5) {
			System.out.println("yes");
		}else {
			System.out.println("no");
		}
	}
}
