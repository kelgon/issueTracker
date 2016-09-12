package com.issuetracker.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.alibaba.fastjson.JSON;
import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.RetMsg;
import com.issuetracker.model.Tag;
import com.issuetracker.util.CacheUtil;

public class TagService {
	private static final Logger log = Logger.getLogger(TagService.class);
	
	public String getAllTags(HttpServletRequest request) {
		return JSON.toJSONString(new RetMsg("0", "ok", CacheUtil.tags));
	}
	
	public String deleteTag(HttpServletRequest request) {
		String oid = request.getParameter("oid");
		if(StringUtils.isEmpty(oid)) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		} else {
			MongoDBUtil.deleteOne("tag", new Document("_id", new ObjectId(oid)));
			log.info("已删除Tag："+oid);
			renewTagCache();
			return JSON.toJSONString(new RetMsg("0","ok"));
		}
	}
	
	public String createTag(HttpServletRequest request) {
		String name = request.getParameter("tag");
		if(StringUtils.isBlank(name))
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		if(name.length() > 6)
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		
		for(Tag tag : CacheUtil.tags) {
			if(tag.getName().equals(name)) {
				return JSON.toJSONString(new RetMsg("1","已存在重名标签"));
			}
		}
		MongoDBUtil.insert("tag", new Document("name", name));
		log.info("已创建标签："+name);
		renewTagCache();
		return JSON.toJSONString(new RetMsg("0","ok"));
	}
	
	public static void renewTagCache() {
		List<Document> tList = MongoDBUtil.find("tag", null);
		List<Tag> tagList = new ArrayList<Tag>();
		for(Document t : tList) {
			Tag tag = new Tag();
			tag.setName(t.getString("name"));
			tag.setOid(t.getObjectId("_id").toString());
			tagList.add(tag);
		}
		CacheUtil.tags = tagList;
	}
}
