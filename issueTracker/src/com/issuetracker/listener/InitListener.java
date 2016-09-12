package com.issuetracker.listener;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.notice.EmailConsumer;
import com.issuetracker.notice.NoticeProducer;
import com.issuetracker.notice.RemindJob;
import com.issuetracker.notice.ReportJob;
import com.issuetracker.notice.SMSConsumer;
import com.issuetracker.notice.TaskQueues;
import com.issuetracker.service.TagService;
import com.issuetracker.util.CacheUtil;
import com.issuetracker.util.ContextManager;
import com.issuetracker.util.PropertiesUtil;

public class InitListener implements ServletContextListener {

	private static final Logger log = Logger.getLogger(InitListener.class);
	
	private NoticeProducer noticeProducer = null;
	private Set<SMSConsumer> smscSet;
	private Set<EmailConsumer> ecSet;
	private Scheduler sched;
	private Timer timer;

	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("停止定时任务...");
		try {
			sched.shutdown(true);
		} catch (SchedulerException e) {
			log.error("终止Quartz任务时发生异常", e);
		}
		timer.cancel();
		log.info("停止提醒相关线程...");
		noticeProducer.sigStop();
		try {
			while(true) {
				Thread.sleep(100);
				if(State.TERMINATED.equals(noticeProducer.getState()))
					break;
			}
		} catch(Exception e) {
			log.error("终止noticeProducer线程时发生异常", e);
		}
		try {
			log.info("检查队列剩余任务...");
			while(true) {
				Thread.sleep(200);
				if(TaskQueues.smsQueue.size() == 0 && TaskQueues.emailQueue.size() == 0)
					break;
			}
			log.info("中止SMSConsumer和EmailConsumer线程...");
			for(SMSConsumer smsc : smscSet) {
				smsc.sigStop();
			}
			for(EmailConsumer ec : ecSet) {
				ec.sigStop();
			}
			while(true) {
				Thread.sleep(200);
				boolean all = true;
				for(SMSConsumer smsc : smscSet) {
					if(!State.TERMINATED.equals(smsc.getState())) {
						all = false;
						break;
					}
				}
				if(all)
					break;
			}
			while(true) {
				Thread.sleep(200);
				boolean all = true;
				for(EmailConsumer ec : ecSet) {
					if(!State.TERMINATED.equals(ec.getState())) {
						all = false;
						break;
					}
				}
				if(all)
					break;
			}
			log.info("Shutdown完成");
		} catch(Exception e) {
			log.error("终止noticeConsumer线程时发生异常", e);
		}
	
	}

	public void contextInitialized(ServletContextEvent sc) {
		try {
			PropertyConfigurator.configure(sc.getServletContext().getRealPath("/WEB-INF/classes/log4j.properties"));
			log.info("启动初始化...");
			log.info("加载properties文件...");
			InputStream in = new BufferedInputStream(new FileInputStream(sc.getServletContext().getRealPath("/WEB-INF/classes/issueTracker.properties")));
	        Properties p = new Properties();
	        p.load(in);
	        PropertiesUtil.setFileProps(p);
	        log.info("加载Spring配置文件...");
	        ContextManager.setContext(new ClassPathXmlApplicationContext("spring.xml"));
			log.info("初始化MongoDB Client...");
			MongoDBUtil.test();
			log.info("初始化定时任务...");
			
			timer = new Timer();
			timer.schedule(new TimerTask() {
				public void run() {
					log.debug("更新用户信息缓存...");
					List<Document> uList = MongoDBUtil.find("user", null);
					String[] users = new String[uList.size()];
					for(int i=0; i<uList.size(); i++) {
						Document user = uList.get(i);
						users[i] = user.getString("name")+"("+user.getString("userName")+")";
					}
					CacheUtil.users = users;
					
					log.debug("更新标签信息缓存...");
					TagService.renewTagCache();
					
					log.debug("更新配置文件...");
					InputStream in = this.getClass().getClassLoader().getResourceAsStream("issueTracker.properties");
			        Properties p = new Properties();
			        try {
						p.load(in);
					} catch (IOException e) {
						log.error("", e);
					}
			        PropertiesUtil.setFileProps(p);
				}
			}, 0, 300000);
			
			SchedulerFactory sf = new StdSchedulerFactory();
		    sched = sf.getScheduler();
		    JobDetail reportJob = JobBuilder.newJob(ReportJob.class).withIdentity("ReportJob", "group1").build();
		    String reportJobCron = "0 0 10,17 * * ?";
		    CronTrigger reportJobTrigger = (CronTrigger)TriggerBuilder.newTrigger().withIdentity("ReportJobTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(reportJobCron)).build();
		    sched.scheduleJob(reportJob, reportJobTrigger);
		    JobDetail remindJob = JobBuilder.newJob(RemindJob.class).withIdentity("RemindJob", "group1").build();
		    String remindJobCron = "0 0 10 * * ?";
		    CronTrigger remindJobTrigger = (CronTrigger)TriggerBuilder.newTrigger().withIdentity("RemindJobTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(remindJobCron)).build();
		    sched.scheduleJob(remindJob, remindJobTrigger);
		    sched.start();
		    
			log.info("初始化提醒线程...");
			noticeProducer = new NoticeProducer();
			noticeProducer.start();
			int smsThreadCount = Integer.parseInt(PropertiesUtil.getFileProp("notice.smsThreadCount"));
			int emailThreadCount = Integer.parseInt(PropertiesUtil.getFileProp("notice.emailThreadCount"));
			smscSet = new HashSet<SMSConsumer>();
			for(int i=0; i<smsThreadCount; i++) {
				SMSConsumer smsc = new SMSConsumer();
				smscSet.add(smsc);
				smsc.start();
			}
			ecSet = new HashSet<EmailConsumer>();
			for(int i=0; i<emailThreadCount; i++) {
				EmailConsumer ec = new EmailConsumer();
				ecSet.add(ec);
				ec.start();
			}
			log.info("启动初始化完成");
		} catch(Exception e) {
			log.error("启动初始化失败", e);
		}
		
	}

}
