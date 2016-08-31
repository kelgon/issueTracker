package com.issuetracker.model;

public class EmailTask {
	private String[] receivers;
	private String[] copies;
	private String title;
	private String content;
	private String noticeId;
	public String[] getReceivers() {
		return receivers;
	}
	public void setReceivers(String[] receivers) {
		this.receivers = receivers;
	}
	public String[] getCopies() {
		return copies;
	}
	public void setCopies(String[] copies) {
		this.copies = copies;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getNoticeId() {
		return noticeId;
	}
	public void setNoticeId(String noticeId) {
		this.noticeId = noticeId;
	}
}
