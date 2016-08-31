package com.issuetracker.notice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.EmailTask;
import com.issuetracker.model.SMSTask;
import com.issuetracker.util.PropertiesUtil;

public class NoticeProducer extends Thread {
	private static final Logger log = Logger.getLogger(NoticeProducer.class);
	
	private boolean run = true;
	
	public void run() {
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
		while(run) {
			try {
				Date date = new Date();
				List<Document> noticeList = MongoDBUtil.find("notice", new Document("dealState", "新建")
					.append("date", new Document("$lte", date)));
				if(noticeList.size() == 0) {
					Thread.sleep(5000);
					continue;
				}
				log.info("已读取提醒任务数:"+noticeList.size());
				for(Document notice : noticeList) {
					Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(notice.getString("issue"))));
					if(issue != null) {
						String ownerUserName = ((Document)issue.get("owner")).getString("userName");
						Document owner = MongoDBUtil.findOne("user", new Document("userName", ownerUserName));
						String creatorUserName = ((Document)issue.get("creator")).getString("userName");
						Document creator = MongoDBUtil.findOne("user", new Document("userName", creatorUserName));
						List<?> followers = (List<?>)issue.get("follower");
						List<String> copies = new ArrayList<String>();
						copies.add(creator.getString("email"));
						if(followers != null && followers.size() > 0) {
							for(Object f : followers) {
								Document follower = MongoDBUtil.findOne("user", new Document("userName",
										((Document)f).getString("userName")));
								if(follower != null) {
									copies.add(follower.getString("email"));
								}
							}
						}
						if("new".equals(notice.getString("type"))) {
							SMSTask smsTask = new SMSTask();
							smsTask.setNoticeId(notice.getObjectId("_id").toString());
							smsTask.setPhone(owner.getString("phone"));
							smsTask.setMsg(owner.getString("name")+"您好，"+creator.getString("name")+"分派给您一个新任务["
									+issue.getString("issueTitle")+"]，请关注，该任务将于"+sdf2.format(issue.getDate("deadline"))
									+"逾期【iTracker】");
							TaskQueues.smsQueue.offer(smsTask);
							EmailTask emailTask = new EmailTask();
							emailTask.setNoticeId(notice.getObjectId("_id").toString());
							emailTask.setTitle("【iTracker】任务["+issue.getString("issueTitle")+"]创建提醒");
							emailTask.setReceivers(new String[] {owner.getString("email")});
							emailTask.setCopies(new String[] {creator.getString("email")});
							emailTask.setContent(owner.getString("name")+"，您好：\r\n    "+creator.getString("name")
									+"分派给您一个新任务["+issue.getString("issueTitle")+"]，请关注，该任务将于"
									+sdf2.format(issue.getDate("deadline"))+"逾期。"+PropertiesUtil.getFileProp("mail.sign"));
							TaskQueues.emailQueue.offer(emailTask);
						} else if("close".equals(notice.getString("type"))) {
							EmailTask emailTask = new EmailTask();
							emailTask.setTitle("【iTracker】任务["+issue.getString("issueTitle")+"]关闭提醒");
							emailTask.setReceivers(new String[] {owner.getString("email")});
							emailTask.setCopies(copies.toArray(new String[0]));
							emailTask.setContent(owner.getString("name")+"，您好：\r\n    "+creator.getString("name")
									+"关闭了由您负责的任务["+issue.getString("issueTitle")+"]，请关注。"
									+PropertiesUtil.getFileProp("mail.sign"));
							TaskQueues.emailQueue.offer(emailTask);
						} else if("cancel".equals(notice.getString("type"))) {
							EmailTask emailTask = new EmailTask();
							emailTask.setNoticeId(notice.getObjectId("_id").toString());
							emailTask.setTitle("【iTracker】任务["+issue.getString("issueTitle")+"]取消提醒");
							emailTask.setReceivers(new String[] {owner.getString("email")});
							emailTask.setCopies(copies.toArray(new String[0]));
							emailTask.setContent(owner.getString("name")+"，您好：\r\n    "+creator.getString("name")
									+"取消了由您负责的任务["+issue.getString("issueTitle")+"]，请关注。"
									+PropertiesUtil.getFileProp("mail.sign"));
							TaskQueues.emailQueue.offer(emailTask);
						} else if("complete".equals(notice.getString("type"))) {
							EmailTask emailTask = new EmailTask();
							emailTask.setNoticeId(notice.getObjectId("_id").toString());
							emailTask.setTitle("【iTracker】任务["+issue.getString("issueTitle")+"]反馈完成提醒");
							emailTask.setReceivers(new String[] {creator.getString("email")});
							emailTask.setCopies(copies.toArray(new String[0]));
							emailTask.setContent(creator.getString("name")+"，您好：\r\n    "+owner.getString("name")
									+"反馈完成了由您创建的任务["+issue.getString("issueTitle")+"]，请关注。"
									+PropertiesUtil.getFileProp("mail.sign"));
							TaskQueues.emailQueue.offer(emailTask);
						} else if("reopen".equals(notice.getString("type"))) {
							SMSTask smsTask = new SMSTask();
							smsTask.setNoticeId(notice.getObjectId("_id").toString());
							smsTask.setPhone(owner.getString("phone"));
							smsTask.setMsg(owner.getString("name")+"您好，"+creator.getString("name")+"重开了由您负责的任务["
									+issue.getString("issueTitle")+"]，请关注【iTracker】");
							TaskQueues.smsQueue.offer(smsTask);
							EmailTask emailTask = new EmailTask();
							emailTask.setNoticeId(notice.getObjectId("_id").toString());
							emailTask.setTitle("【iTracker】任务["+issue.getString("issueTitle")+"]重开提醒");
							emailTask.setReceivers(new String[] {owner.getString("email")});
							emailTask.setCopies(copies.toArray(new String[0]));
							emailTask.setContent(owner.getString("name")+"，您好：\r\n    "+creator.getString("name")
									+"重开了由您负责的任务["+issue.getString("issueTitle")+"]，请关注。"
									+PropertiesUtil.getFileProp("mail.sign"));
							TaskQueues.emailQueue.offer(emailTask);
						} else if("urge".equals(notice.getString("type"))) {
							SMSTask smsTask = new SMSTask();
							smsTask.setNoticeId(notice.getObjectId("_id").toString());
							smsTask.setPhone(owner.getString("phone"));
							smsTask.setMsg(owner.getString("name")+"您好，"+creator.getString("name")+"催促您加速任务["
									+issue.getString("issueTitle")+"]的进度，该任务将于"+sdf2.format(issue.getDate("deadline"))
									+"逾期【iTracker】");
							TaskQueues.smsQueue.offer(smsTask);
							EmailTask emailTask = new EmailTask();
							emailTask.setNoticeId(notice.getObjectId("_id").toString());
							emailTask.setTitle("【iTracker】任务["+issue.getString("issueTitle")+"]催促邮件");
							emailTask.setReceivers(new String[] {owner.getString("email")});
							emailTask.setCopies(copies.toArray(new String[0]));
							emailTask.setContent(owner.getString("name")+"，您好：\r\n    "+creator.getString("name")
									+"催促您加速任务["+issue.getString("issueTitle")+"]的进度，该任务将于"
									+sdf2.format(issue.getDate("deadline"))+"逾期。"+PropertiesUtil.getFileProp("mail.sign"));
							TaskQueues.emailQueue.offer(emailTask);
						}
	
						MongoDBUtil.update("notice", new Document("_id", notice.getObjectId("_id")), 
								new Document("$set", new Document("dealState","已处理")));
					}
				}
				log.info("提醒队列任务添加完成");
			} catch(Throwable t) {
				log.error("提醒生产者线程出错", t);
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void sigStop() {
		this.run = false;
	}
}
