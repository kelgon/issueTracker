package com.issuetracker.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.alibaba.fastjson.JSON;
import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.RetMsg;
import com.issuetracker.util.CacheUtil;

public class UserService {
	private static final Logger log = Logger.getLogger(UserService.class);
	
	public String login(HttpServletRequest request) throws Exception {
		String userName = request.getParameter("userName");
		String pwd = request.getParameter("pwd");
		if(StringUtils.isNotBlank(userName)) {
			List<Document> users = MongoDBUtil.find("user", new Document("userName", userName));
			if(users.size() == 0) {
				return JSON.toJSONString(new RetMsg("2","user doesn't exist"));
			} else {
				Document user = users.get(0);
				if(StringUtils.isNotBlank(pwd) && pwd.equals(user.getString("pwd"))) {
					log.info(userName + "成功登录");
					request.getSession().setAttribute("user", user);
					String token = RandomStringUtils.randomAlphanumeric(12);
					Date date = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
					MongoDBUtil.insert("authToken", new Document("token",token).append("userName", userName).append("expires", sdf.format(new Date(date.getTime() + 30*24*60*60*1000L))));
					return JSON.toJSONString(new RetMsg("0",user.getString("name")+"("+user.getString("userName")+")", token));
				} else {
					log.info(userName + "尝试登录，密码错误");
					//int loginFailThreshold = Integer.parseInt(PropertiesUtil.getFileProp("loginFailThreshold"));
					//if(loginFailThreshold == LoginCounter.incAndGet(userName)) {
					//	return JSON.toJSONString(new RetMsg("3","too many failed attempts"));
					//} else
						return JSON.toJSONString(new RetMsg("1","incorrect pwd"));
				}
			}
		} else {
			return JSON.toJSONString(new RetMsg("9","illegal request"));
		}
	}
	
	public String passive(HttpServletRequest request) throws Exception {
		Document user = (Document)request.getSession().getAttribute("user");
		if(user != null) {
			return JSON.toJSONString(new RetMsg("0","ok",user.getString("name")+"("+user.getString("userName")+")"));
		} else {
			return JSON.toJSONString(new RetMsg("1","not logined"));
		}
	}
	
	public String getAllUsers(HttpServletRequest request) {
		return JSON.toJSONString(new RetMsg("0","ok",CacheUtil.users));
	}
	
	public String logout(HttpServletRequest request) {
		request.getSession().invalidate();
		return JSON.toJSONString(new RetMsg("0","ok"));
	}
	
	public String chgpasswd(HttpServletRequest request) {
		String oldpwd = request.getParameter("oldpwd");
		String newpwd = request.getParameter("newpwd");
		Document user = (Document)request.getSession().getAttribute("user");
		if(!user.getString("pwd").equals(oldpwd))
			return JSON.toJSONString(new RetMsg("2","旧密码验证未通过"));
		if(StringUtils.isBlank(newpwd))
			return JSON.toJSONString(new RetMsg("1","新密码不合法"));
		if(newpwd.trim().length() < 6 || newpwd.trim().length() > 20)
			return JSON.toJSONString(new RetMsg("1","新密码不合法"));
		MongoDBUtil.update("user", new Document("userName", user.getString("userName")), new Document("$set", 
				new Document("pwd", newpwd)));
		return JSON.toJSONString(new RetMsg("0","ok"));
	}
}
