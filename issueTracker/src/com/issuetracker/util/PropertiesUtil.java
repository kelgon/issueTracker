package com.issuetracker.util;

import java.util.Properties;

public class PropertiesUtil {
	private static Properties fileProps = null;
	
	public static void setFileProps(Properties a) {
		fileProps = a;
	}
	
	public static String getFileProp(String key) {
		return fileProps.getProperty(key);
	}
}
