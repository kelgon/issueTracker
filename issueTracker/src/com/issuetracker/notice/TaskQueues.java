package com.issuetracker.notice;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.issuetracker.model.EmailTask;
import com.issuetracker.model.SMSTask;

public class TaskQueues {
	public static BlockingQueue<SMSTask> smsQueue = new LinkedBlockingQueue<SMSTask>();
	public static BlockingQueue<EmailTask> emailQueue = new LinkedBlockingQueue<EmailTask>();
}
