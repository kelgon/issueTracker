package com.issuetracker.util;

import javax.ws.rs.core.MediaType;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class NoticeUtil {
	private static final Logger log = Logger.getLogger(NoticeUtil.class);

	public static void sendEmail2(String[] receivers, String[] copies, String title, String content) throws Exception {
		String rs = "";
		String cs = "";
		for(String r : receivers) {
			rs += r +"; ";
		}
		if(copies != null)
			for(String c : copies) {
				cs += c +"; ";
			}
		log.info("发送邮件，收件人："+rs);
		log.info("抄送："+cs);
		log.info("标题："+title);
		log.info("内容：\r\n"+content);
	}

	public static boolean sendSMS2(String phone, String msg) throws Exception {
		log.info("向"+phone+"发送短信："+msg);
		return true;
	}
	
	public static void sendEmail(String[] receivers, String[] copies, String title, String content) throws Exception {
		Email email = new SimpleEmail();
		email.setHostName(PropertiesUtil.getFileProp("mail.smtp"));
		email.setAuthenticator(new DefaultAuthenticator(PropertiesUtil.getFileProp("mail.account"), 
				PropertiesUtil.getFileProp("mail.pwd")));
		email.setSSLOnConnect(true);
		email.setFrom(PropertiesUtil.getFileProp("mail.account"));
		email.setSubject(title);
		email.setMsg(content);
		email.addTo(receivers);
		if(copies != null)
			email.addCc(copies);
		email.send();
	}
	
	public static boolean sendSMS(String phone, String msg) throws Exception {
	    Client client = Client.create();
	    client.addFilter(new HTTPBasicAuthFilter("api",PropertiesUtil.getFileProp("smsapikey")));
	    WebResource webResource = client.resource(PropertiesUtil.getFileProp("smsapi"));
	    MultivaluedMapImpl formData = new MultivaluedMapImpl();
	    formData.add("mobile", phone);
	    formData.add("message", msg);
	    ClientResponse response =  webResource.type(MediaType.APPLICATION_FORM_URLENCODED ).
	    post(ClientResponse.class, formData);
	    String textEntity = response.getEntity(String.class);
	    int status = response.getStatus();
	    if(status != 200) {
	    	log.error("发送短信接口响应状态码异常：" + status);
	    	return false;
	    } else {
	    	JSONObject ret = JSON.parseObject(textEntity);
	    	if(!"0".equals(ret.getString("error"))) {
		    	log.error("短信发送失败：" + textEntity);
		    	return false;
	    	} else {
	    		log.info("短信发送成功，["+phone+","+msg+"]");
	    		return true;
	    	}
	    }
	}
}
