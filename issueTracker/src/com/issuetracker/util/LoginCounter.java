package com.issuetracker.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginCounter {
	private static ConcurrentHashMap<String, AtomicInteger> counter = new ConcurrentHashMap<String, AtomicInteger>();
	
	public static int incAndGet(String userName) {
		if(counter.containsKey(userName)) {
			return counter.get(userName).incrementAndGet();
		} else {
			counter.put(userName, new AtomicInteger(1));
			return 1;
		}
	}
	
	public static void clear() {
		counter.clear();
	}
}
