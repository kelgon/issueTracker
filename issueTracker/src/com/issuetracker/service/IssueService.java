package com.issuetracker.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.alibaba.fastjson.JSON;
import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.Issue;
import com.issuetracker.model.RetMsg;
import com.issuetracker.util.PropertiesUtil;

public class IssueService {
	private static final Logger log = Logger.getLogger(IssueService.class);
	
	public String newIssue(HttpServletRequest request) {
		log.info("准备新建事项...");
		String issueTitle = request.getParameter("issueTitle");
		String ownerS = request.getParameter("owner");
		String remindFreq = request.getParameter("remindFreq");
		String deadline = request.getParameter("deadline");
		String issueDetail = request.getParameter("issueDetail");
		
		if(StringUtils.isBlank(issueTitle) || StringUtils.isBlank(ownerS) || StringUtils.isBlank(deadline)) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		if(issueTitle.length() > 60 || issueDetail.length() > 600) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		String[] ownerT = ownerS.split("\\(");
		if(ownerT.length != 2 || !ownerS.endsWith(")")) {
			return JSON.toJSONString(new RetMsg("1","负责人信息格式错误"));
		}
		String ownerUserName = ownerT[1].split("\\)")[0];
		String ownerName = ownerT[0];
		int remindFreqI = 0;
		if(StringUtils.isNotBlank(remindFreq)) {
			try {
				remindFreqI = Integer.parseInt(remindFreq);
			} catch(NumberFormatException e) {
				return JSON.toJSONString(new RetMsg("1","提醒周期格式错误"));
			}
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Date deadLineD;
		try {
			deadLineD = sdf.parse(deadline);
		} catch (ParseException e) {
			return JSON.toJSONString(new RetMsg("1","到期日格式错误"));
		}
		Date date = new Date();
		log.info("事项信息校验通过，开始创建...");
		Document user = ((Document)request.getSession().getAttribute("user"));
		Document creator = new Document("userName", user.getString("userName")).append("name", user.getString("name"));
		Document owner = new Document("userName", ownerUserName).append("name", ownerName);
		String objId = MongoDBUtil.insert("issue", new Document("issueTitle", issueTitle).append("owner", owner)
				.append("deadline", deadLineD).append("creator", creator).append("remindFreq", remindFreqI)
				.append("issueDetail", issueDetail).append("createDate", date).append("state", "新建")
				.append("lastRemind", date)).toString();
		
		log.info("事项创建成功，ID："+objId);
		
		MongoDBUtil.insert("notice", new Document("type", "new").append("issue", objId).append("smsState", "新建")
				.append("emailState","新建").append("report", "true").append("dealState", "新建")
				.append("date", new Date()).append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success",objId));
	}

	public String modifyIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		log.info("准备更新事项："+id+"...");
		String remindFreq = request.getParameter("remindFreq");
		String deadline = request.getParameter("deadline");
		String issueDetail = request.getParameter("issueDetail");

		Document user = ((Document)request.getSession().getAttribute("user"));
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待更新事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		if(!((Document)issue.get("creator")).getString("userName").equals(user.getString("userName"))) {
			log.info("更新请求提交者不是待更新事项"+id+"的创建者，拒绝请求");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}

		int remindFreqI = 0;
		if(StringUtils.isNotBlank(remindFreq)) {
			try {
				remindFreqI = Integer.parseInt(remindFreq);
			} catch(NumberFormatException e) {
				return JSON.toJSONString(new RetMsg("1","提醒周期格式错误"));
			}
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Date deadLineD;
		try {
			deadLineD = sdf.parse(deadline);
		} catch (ParseException e) {
			return JSON.toJSONString(new RetMsg("1","到期日格式错误"));
		}
		
		log.info("待更新事项信息校验通过，开始更新...");
		long updated = MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$set",
				new Document("deadline", deadLineD).append("remindFreq", remindFreqI).append("issueDetail", issueDetail)));

		MongoDBUtil.insert("notice", new Document("type", "modify").append("issue", id).append("report", "true")
				.append("date", new Date()).append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success", updated));
	}

	public String getIssues(HttpServletRequest request) {
		String choice = request.getParameter("choice");
		String page = request.getParameter("page");
		if(StringUtils.isBlank(choice)) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		int pageNo = 1;
		if(StringUtils.isNotBlank(page)) {
			try {
				pageNo = Integer.parseInt(page);
			} catch(NumberFormatException e) {
			}
		}
		if(pageNo < 1)
			pageNo = 1;
		
		Document filter = null;
		String userName = ((Document)request.getSession().getAttribute("user")).getString("userName");
		if("created".equals(choice)) {
			filter = new Document("creator.userName", userName);
		} else if("owned".equals(choice)) {
			filter = new Document("owner.userName", userName);
		} else if("followed".equals(choice)) {
			filter = new Document("follower", new Document("$elemMatch", new Document("userName", userName)));
		}
		int pcount = Integer.parseInt(PropertiesUtil.getFileProp("page.records"));
		List<Document> issueList = MongoDBUtil.find("issue", filter, new Document("createDate", -1) , 
				(pageNo-1)*pcount, pageNo*pcount);
		return JSON.toJSONString(new RetMsg("0","ok", convert2IssueList(issueList)));
	}
	
	public String getIssueById(HttpServletRequest request) {
		String id = request.getParameter("oid");
		if(StringUtils.isEmpty(id)) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		} else {
			Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
			Issue is = new Issue();
			is.setOid(issue.getObjectId("_id").toString());
			is.setTitle(issue.getString("issueTitle"));
			is.setState(issue.getString("state"));
			Document owner = (Document)issue.get("owner");
			is.setOwner(owner.getString("name")+"("+owner.getString("userName")+")");
			Document creator = (Document)issue.get("creator");
			is.setCreator(creator.getString("name")+"("+creator.getString("userName")+")");
			is.setCreateDate(sdf1.format(issue.getDate("createDate")));
			is.setDeadline(sdf2.format(issue.getDate("deadline")));
			is.setDetail(issue.getString("issueDetail"));
			is.setRemindFreq(issue.getInteger("remindFreq"));
			is.setProgress(issue.getString("progress"));
			List<?> ofollow = (List<?>)issue.get("follower");
			if(ofollow != null) {
				List<String> fList = new ArrayList<String>();
				for(Object of : ofollow) {
					Document follower = (Document)of;
					fList.add(follower.getString("name")+"("+follower.getString("userName")+")");
				}
				is.setFollower(fList.toArray(new String[0]));
			}
			return JSON.toJSONString(new RetMsg("0","ok",is));
		}
	}
	
	private List<Issue> convert2IssueList(List<Document> dList) {
		List<Issue> iList = new ArrayList<Issue>();
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
		for(Document issue : dList) {
			Issue is = new Issue();
			is.setOid(issue.getObjectId("_id").toString());
			is.setTitle(issue.getString("issueTitle"));
			is.setState(issue.getString("state"));
			Document owner = (Document)issue.get("owner");
			is.setOwner(owner.getString("name")+"("+owner.getString("userName")+")");
			Document creator = (Document)issue.get("creator");
			is.setCreator(creator.getString("name")+"("+creator.getString("userName")+")");
			is.setCreateDate(sdf1.format(issue.getDate("createDate")));
			is.setDeadline(sdf2.format(issue.getDate("deadline")));
			iList.add(is);
		}
		return iList;
	}

	public String closeIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		log.info("准备关闭事项："+id+"...");

		Document user = ((Document)request.getSession().getAttribute("user"));
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待关闭事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		if(!((Document)issue.get("creator")).getString("userName").equals(user.getString("userName"))) {
			log.info("关闭请求提交者不是待关闭事项"+id+"的创建者，拒绝请求");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		
		long updated = MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$set",
				new Document("state", "已关闭")));
		log.info("事项"+id+"责任人已关闭事项");

		MongoDBUtil.insert("notice", new Document("type", "close").append("issue", id).append("emailState","新建")
				.append("report", "true").append("dealState", "新建").append("date", new Date())
				.append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success", updated));
	}

	public String cancelIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		log.info("准备取消事项："+id+"...");

		Document user = ((Document)request.getSession().getAttribute("user"));
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待取消事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		if(!((Document)issue.get("creator")).getString("userName").equals(user.getString("userName"))) {
			log.info("取消请求提交者不是待取消事项"+id+"的创建者，拒绝请求");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		
		long updated = MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$set", 
				new Document("state", "已取消")));
		log.info("事项"+id+"责任人已取消事项");

		MongoDBUtil.insert("notice", new Document("type", "cancel").append("issue", id).append("emailState","新建")
				.append("report", "true").append("dealState", "新建").append("date", new Date())
				.append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success", updated));
	}
	
	public String ackIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");

		Document user = ((Document)request.getSession().getAttribute("user"));
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待确认接收事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		if(!((Document)issue.get("owner")).getString("userName").equals(user.getString("userName"))) {
			log.info("确认接收请求提交者不是待确认接收事项"+id+"的负责人，拒绝请求");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		
		long updated = MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$set", 
				new Document("state","已接收")));
		log.info("事项"+id+"责任人已确认接收");

		MongoDBUtil.insert("notice", new Document("type", "ack").append("issue", id).append("report", "true")
				.append("date", new Date()).append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success", updated));
	}
	
	public String updateIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		String progress = request.getParameter("progress");

		if(StringUtils.isBlank(progress)) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		if(progress.length() > 600) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		
		Document user = ((Document)request.getSession().getAttribute("user"));
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待更新进展事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		if(!((Document)issue.get("owner")).getString("userName").equals(user.getString("userName"))) {
			log.info("更新进展请求提交者不是待更新进展事项"+id+"的负责人，拒绝请求");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		
		long updated = MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$set", 
				new Document("state","进行中").append("progress", progress)));
		log.info("事项"+id+"责任人已更新进展");

		MongoDBUtil.insert("notice", new Document("type", "update").append("issue", id).append("report", "true")
				.append("date", new Date()).append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success", updated));
	}

	public String completeIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		String progress = request.getParameter("progress");

		if(StringUtils.isBlank(progress)) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		if(progress.length() > 600) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		
		Document user = ((Document)request.getSession().getAttribute("user"));
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待更新进展事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		if(!((Document)issue.get("owner")).getString("userName").equals(user.getString("userName"))) {
			log.info("更新进展请求提交者不是待更新进展事项"+id+"的负责人，拒绝请求");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		
		long updated = MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$set", 
				new Document("state","已完成").append("progress", progress)));
		log.info("事项"+id+"责任人已更新进展");

		MongoDBUtil.insert("notice", new Document("type", "complete").append("issue", id).append("emailState","新建")
				.append("dealState", "新建").append("report", "true").append("date", new Date())
				.append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success", updated));
	}
	
	public String reopenIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		String issueDetail = request.getParameter("issueDetail");

		if(StringUtils.isBlank(issueDetail)) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		if(issueDetail.length() > 600) {
			return JSON.toJSONString(new RetMsg("1","invalid param(s)"));
		}
		
		Document user = ((Document)request.getSession().getAttribute("user"));
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待重开事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		if(!((Document)issue.get("creator")).getString("userName").equals(user.getString("userName"))) {
			log.info("重开请求提交者不是事项"+id+"的创建者，拒绝请求");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		
		long updated = MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$set", 
				new Document("state","重开").append("issueDetail", issueDetail)));
		log.info("事项"+id+"责任人已重开事项");

		MongoDBUtil.insert("notice", new Document("type", "reopen").append("issue", id).append("smsState", "新建")
				.append("dealState", "新建").append("emailState","新建").append("report", "true")
				.append("date", new Date()).append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success", updated));
	}
	
	public String followIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		Document user = ((Document)request.getSession().getAttribute("user"));
		
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待关注事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		if(((Document)issue.get("owner")).getString("userName").equals(user.getString("userName")) || 
				((Document)issue.get("creator")).getString("userName").equals(user.getString("userName"))) {
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}
		
		MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$addToSet", 
				new Document("follower", new Document("userName",user.getString("userName"))
				.append("name", user.getString("name")))));
		log.info(user.getString("userName") + "已关注事项"+id);
		
		issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));

		MongoDBUtil.insert("notice", new Document("type", "follow").append("issue", id).append("report", "true")
				.append("dealState", "新建").append("date", new Date()).append("operator", user.getString("userName")));
		
		return JSON.toJSONString(new RetMsg("0","success", issue.get("follower")));
	}
	
	public String unfollowIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		Document user = ((Document)request.getSession().getAttribute("user"));
		
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待取消关注事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}

		MongoDBUtil.update("issue", new Document("_id", new ObjectId(id)), new Document("$pull", 
				new Document("follower", new Document("userName",user.getString("userName"))
				.append("name", user.getString("name")))));
		log.info(user.getString("userName") + "已取消关注事项"+id);

		issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		return JSON.toJSONString(new RetMsg("0","success", issue.get("follower")));
	}

	public String urgeIssue(HttpServletRequest request) {
		String id = request.getParameter("oid");
		
		Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(id)));
		if(issue == null) {
			log.info("待催促事项"+id+"不存在");
			return JSON.toJSONString(new RetMsg("2","invalid operation"));
		}

		MongoDBUtil.insert("notice", new Document("type", "urge").append("issue", id).append("smsState", "新建")
				.append("emailState","新建").append("dealState", "新建").append("date", new Date()));
		log.info("责任人催促事项进展"+id);

		return JSON.toJSONString(new RetMsg("0","success"));
	}
}
