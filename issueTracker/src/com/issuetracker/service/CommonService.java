package com.issuetracker.service;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.issuetracker.util.CacheUtil;

public class CommonService {
	public String initTypeahead(HttpServletRequest request) {
		String[] tags = new String[CacheUtil.tags.size()];
		for(int i=0; i<CacheUtil.tags.size(); i++) {
			tags[i] = CacheUtil.tags.get(i).getName();
		}
		return "{\"users\":"+JSON.toJSONString(CacheUtil.users)+", \"tags\":"+JSON.toJSONString(tags)+"}";
	}
}
