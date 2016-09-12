package com.issuetracker.model;

public class Issue {
	private String oid;
	private String title;
	private String owner;
	private String deadline;
	private String creator;
	private String createDate;
	private String state;
	private String detail;
	private String progress;
	private int remindFreq;
	private String[] follower;
	private String[] tags;
	public String getOid() {
		return oid;
	}
	public void setOid(String oid) {
		this.oid = oid;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getDeadline() {
		return deadline;
	}
	public void setDeadline(String deadline) {
		this.deadline = deadline;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public String getCreateDate() {
		return createDate;
	}
	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getDetail() {
		return detail;
	}
	public void setDetail(String detail) {
		this.detail = detail;
	}
	public String[] getFollower() {
		return follower;
	}
	public void setFollower(String[] follower) {
		this.follower = follower;
	}
	public int getRemindFreq() {
		return remindFreq;
	}
	public void setRemindFreq(int remindFreq) {
		this.remindFreq = remindFreq;
	}
	public String getProgress() {
		return progress;
	}
	public void setProgress(String progress) {
		this.progress = progress;
	}
	public String[] getTags() {
		return tags;
	}
	public void setTags(String[] tags) {
		this.tags = tags;
	}
}
