package com.issuetracker.notice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.issuetracker.db.MongoDBUtil;
import com.issuetracker.model.EmailTask;
import com.issuetracker.model.Report;
import com.issuetracker.util.PropertiesUtil;

public class ReportJob implements Job {
	private static final Logger log = Logger.getLogger(ReportJob.class);
	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			Calendar ca = Calendar.getInstance();
			Date date = new Date();
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
			Document filter = new Document("report","true");
			String period;
			if(ca.get(Calendar.HOUR_OF_DAY) == 10) {
				filter.append("date", new Document("$gte", new Date(date.getTime() - 17*60*60*1000L)));
				period = "昨天17:00至今天10:00，与您相关的事项进展情况如下：\r\n\r\n";
			} else {
				filter.append("date", new Document("$gte", new Date(date.getTime() - 7*60*60*1000L)));
				period = "今天10:00至17:00，与您相关的事项进展情况如下：\r\n\r\n";
			}
			List<Document> noticeList = MongoDBUtil.find("notice", filter);
			if(noticeList == null)
				return;
			
			Map<String, List<Report>> rmap = new HashMap<String, List<Report>>();
			for(Document notice : noticeList) {
				Document issue = MongoDBUtil.findOne("issue", new Document("_id", new ObjectId(notice.getString("issue"))));
				String ownerUserName = ((Document)issue.get("owner")).getString("userName");
				String creatorUserName = ((Document)issue.get("creator")).getString("userName");
				List<?> followers = (List<?>)issue.get("follower");
				List<String> follower = new ArrayList<String>();
				if(followers != null && followers.size() > 0) {
					for(Object f : followers) {
						follower.add(((Document)f).getString("userName"));
					}
				}
				Report report = new Report();
				report.setIssueTitle(issue.getString("issueTitle"));
				report.setAction(notice.getString("type"));
				report.setDate(notice.getDate("date"));
				report.setOperator(notice.getString("operator"));
				report.setNoticeId(notice.getObjectId("_id").toString());
				report.setRelation("创建");
				if(rmap.containsKey(creatorUserName)) {
					rmap.get(creatorUserName).add(report);
				} else {
					List<Report> rlist = new ArrayList<Report>();
					rlist.add(report);
					rmap.put(creatorUserName, rlist);
				}
				
				report = new Report();
				report.setIssueTitle(issue.getString("issueTitle"));
				report.setAction(notice.getString("type"));
				report.setDate(notice.getDate("date"));
				report.setOperator(notice.getString("operator"));
				report.setNoticeId(notice.getObjectId("_id").toString());
				report.setRelation("负责");
				if(rmap.containsKey(ownerUserName)) {
					rmap.get(ownerUserName).add(report);
				} else {
					List<Report> rlist = new ArrayList<Report>();
					rlist.add(report);
					rmap.put(ownerUserName, rlist);
				}

				report = new Report();
				report.setIssueTitle(issue.getString("issueTitle"));
				report.setAction(notice.getString("type"));
				report.setDate(notice.getDate("date"));
				report.setOperator(notice.getString("operator"));
				report.setNoticeId(notice.getObjectId("_id").toString());
				report.setRelation("关注");
				for(String f : follower) {
					if(rmap.containsKey(f)) {
						rmap.get(f).add(report);
					} else {
						List<Report> rlist = new ArrayList<Report>();
						rlist.add(report);
						rmap.put(f, rlist);
					}
				}
			}
			
			for(Entry<String, List<Report>> entry : rmap.entrySet()) {
				Document target = MongoDBUtil.findOne("user", new Document("userName", entry.getKey()));
				StringBuffer created = new StringBuffer("您创建的事项：\r\n");
				StringBuffer owned = new StringBuffer("您负责的事项：\r\n");
				StringBuffer followed = new StringBuffer("您关注的事项：\r\n");
				for(Report r : entry.getValue()) {
					if("创建".equals(r.getRelation())) {
						created = makeReport(created, r);
					}
					if("负责".equals(r.getRelation())) {
						owned = makeReport(owned, r);
					}
					if("关注".equals(r.getRelation())) {
						followed = makeReport(followed, r);
					}
				}
				EmailTask emailTask = new EmailTask();
				emailTask.setReceivers(new String[] {target.getString("email")});
				emailTask.setTitle("【iTracker】事项进展日报"+sdf2.format(date));
				emailTask.setContent(target.getString("name")+"，您好：\r\n    "+period+created.toString()+"\r\n"+owned.toString()
						+"\r\n"+followed.toString()+PropertiesUtil.getFileProp("mail.sign"));
				TaskQueues.emailQueue.offer(emailTask);
			}
		} catch(Exception e) {
			log.error("reportJob执行出错", e);
		}
	}

	private StringBuffer makeReport(StringBuffer sb, Report r) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		sb.append("  【").append(r.getIssueTitle()).append("】");
		if("new".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("创建");
		}
		if("modify".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("修改");
		}
		if("close".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("关闭");
		}
		if("cancel".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("取消");
		}
		if("ack".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("确认接收");
		}
		if("update".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("更新进展");
		}
		if("complete".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("反馈完成");
		}
		if("reopen".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("重开");
		}
		if("follow".equals(r.getAction())) {
			sb.append(" 于").append(sdf.format(r.getDate())).append(" 由").append(r.getOperator())
			.append("关注");
		}
		sb.append("\r\n");
		return sb;
	}
}
