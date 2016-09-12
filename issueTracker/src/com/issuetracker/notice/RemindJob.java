package com.issuetracker.notice;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.EmailTask;
import com.issuetracker.util.PropertiesUtil;

public class RemindJob implements Job {
	private static final Logger log = Logger.getLogger(RemindJob.class);
	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			Document filter = Document.parse("{$and: [{state: {$ne: \"已关闭\"}}, {state: {$ne: \"已取消\"}}]}");
			List<Document> issueList = MongoDBUtil.find("issue", filter);
			for(Document issue : issueList) {
				if(issue.getInteger("remindFreq") == 0)
					continue;
				Date remindDate = new Date(issue.getDate("lastRemind").getTime() + 
						issue.getInteger("remindFreq")*24*3600*1000L);
				remindDate = sdf.parse(sdf.format(remindDate));
				Date now = new Date();
				if(now.getTime() > remindDate.getTime()-24*3600*1000L) {
					EmailTask emailTask = new EmailTask();
					String ownerUserName = ((Document)issue.get("owner")).getString("userName");
					Document owner = MongoDBUtil.findOne("user", new Document("userName", ownerUserName));
					String creatorUserName = ((Document)issue.get("creator")).getString("userName");
					Document creator = MongoDBUtil.findOne("user", new Document("userName", creatorUserName));
					if("已完成".equals(issue.getString("state"))) {
						emailTask.setTitle("【iTracker】任务["+issue.getString("issueTitle")+"]结果确认提醒");
						emailTask.setReceivers(new String[]{creator.getString("email")});
						emailTask.setCopies(new String[]{owner.getString("email")});
						emailTask.setContent(creator.getString("name")+"，您好：\r\n    iTracker提醒您尽快确认您创建的任务["
								+issue.getString("issueTitle")+"]，负责人已反馈该任务完成。"+PropertiesUtil.getFileProp("mail.sign"));
					} else {
						emailTask.setTitle("【iTracker】任务["+issue.getString("issueTitle")+"]周期提醒");
						emailTask.setReceivers(new String[]{owner.getString("email")});
						emailTask.setCopies(new String[]{creator.getString("email")});
						Date deadline = issue.getDate("deadline");
						if(now.getTime() < deadline.getTime()) {
							emailTask.setContent(owner.getString("name")+"，您好：\r\n    iTracker提醒您关注您负责的任务["
									+issue.getString("issueTitle")+"]，该任务将于"+sdf.format(issue.getDate("deadline"))+"逾期。"
									+PropertiesUtil.getFileProp("mail.sign"));
						} else {
							emailTask.setContent(owner.getString("name")+"，您好：\r\n    iTracker提醒您关注您负责的任务["
									+issue.getString("issueTitle")+"]，该任务已于"+sdf.format(issue.getDate("deadline"))+"预期，"
									+"请抓紧时间跟进。"+PropertiesUtil.getFileProp("mail.sign"));
						}
					}
					TaskQueues.emailQueue.offer(emailTask);
				}
			}
		} catch(Exception e) {
			log.error("remindJob执行出错", e);
		}
	}

}
