package com.issuetracker.notice;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.SMSTask;
import com.issuetracker.util.NoticeUtil;

public class SMSConsumer extends Thread {
	private static final Logger log = Logger.getLogger(SMSConsumer.class);
	
	private boolean run = true;
	
	public void run() {
		while(run) {
			try {
				SMSTask task = TaskQueues.smsQueue.poll(5000, TimeUnit.MILLISECONDS);
				if(task == null)
					continue;
				log.info("获取到待发送短信任务");
				boolean ret = NoticeUtil.sendSMS(task.getPhone(), task.getMsg());
				if(!ret) {
					log.warn("短信首次发送失败，重试...");
					ret = NoticeUtil.sendSMS(task.getPhone(), task.getMsg());
				}
				if(!ret) {
					log.error("短信发送失败，noticeId：["+task.getNoticeId()+"]");
					MongoDBUtil.update("notice", new Document("_id", new ObjectId(task.getNoticeId())), 
							new Document("$set", new Document("smsState", "失败")));
				} else {
					MongoDBUtil.update("notice", new Document("_id", new ObjectId(task.getNoticeId())), 
							new Document("$set", new Document("smsState", "成功")));
				}
			} catch(Throwable t) {
				log.error("发送短信时出错", t);
			}
		}
	}
	
	public void sigStop() {
		this.run = false;
	}
}
