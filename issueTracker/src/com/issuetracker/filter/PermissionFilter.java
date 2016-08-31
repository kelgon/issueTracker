package com.issuetracker.filter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.alibaba.fastjson.JSON;
import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.RetMsg;

public class PermissionFilter implements Filter {
	private static final Logger log = Logger.getLogger(PermissionFilter.class);
	
	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
		String beanName = request.getParameter("bn");
		String method = request.getParameter("mn");
		if("us".equals(beanName) && "login".equals(method)) {
			chain.doFilter(req, res);
			return;
		} else {
			if(((HttpServletRequest)request).getSession().getAttribute("user") == null) {
				Cookie[] cookies = request.getCookies();
				if(cookies != null) {
					for(Cookie cookie : cookies) {
						if("authToken".equals(cookie.getName())) {
							String[] tokenCookie = cookie.getValue().split("%2C");
							if(tokenCookie.length == 2) {
								log.info("检测到有效身份cookie："+cookie.getValue()+"，进入自动登录流程");
								Document token = MongoDBUtil.findOne("authToken", new Document("token", tokenCookie[1]).append("userName", tokenCookie[0]));
								if(token != null) {
									String expDate = token.getString("expires");
									SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
									try {
										if(sdf.parse(expDate).getTime() > new Date().getTime()) {
											log.info(tokenCookie[0]+"自动登录成功");
											request.getSession().setAttribute("user", MongoDBUtil.findOne("user", new Document("userName", tokenCookie[0])));
											chain.doFilter(req, res);
											return;
										}
									} catch (ParseException e) {
										log.error("解析日期出错", e);
									}
								}
								log.info("token已失效，自动登录失败");
							}
						}
					}
				}
				res.setContentType("text/json;charset=UTF-8");
				res.getWriter().print(JSON.toJSONString(new RetMsg("-1","not login")));
				return;
			} else {
				chain.doFilter(req, res);
				return;
			}
		}
	}

	public void init(FilterConfig arg0) throws ServletException {
	}

}
