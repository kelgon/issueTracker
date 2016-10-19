package com.issuetracker.notice;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.EmailTask;
import com.issuetracker.util.NoticeUtil;

public class EmailConsumer extends Thread {
	private static final Logger log = Logger.getLogger(EmailConsumer.class);
	
	private boolean run = true;
	
	public void run() {
		while(run) {
			try {
				EmailTask task = TaskQueues.emailQueue.poll(5000, TimeUnit.MILLISECONDS);
				if(task == null)
					continue;
				log.info("获取到待发送Email任务");
				try {
					NoticeUtil.sendEmail(task.getReceivers(), task.getCopies(), task.getTitle(), task.getContent());
				} catch(Exception e) {
					if(task.getNoticeId() != null) {
						log.error("Email发送失败，noticeId：["+task.getNoticeId()+"]", e);
						MongoDBUtil.update("notice", new Document("_id", new ObjectId(task.getNoticeId())), 
								new Document("$set", new Document("emailState", "失败")));
					} else {
						log.error("Email发送失败，收件人："+task.getReceivers()[0], e);
					}
					continue;
				}
				if(task.getNoticeId() != null) {
					log.info("Email发送成功，noticeId：["+task.getNoticeId()+"]");
					MongoDBUtil.update("notice", new Document("_id", new ObjectId(task.getNoticeId())), 
							new Document("$set", new Document("emailState", "成功")));
				} else {
					log.info("Email发送成功，收件人："+task.getReceivers()[0]);
				}
			} catch(Throwable t) {
				log.error("发送Email时出错", t);
			}
		}
	}
	
	public void sigStop() {
		this.run = false;
	}
}
