package com.issuetracker.model;

public class RetMsg {
	public RetMsg(String retCode, String retMsg) {
		super();
		this.retCode = retCode;
		this.retMsg = retMsg;
	}
	public RetMsg(String retCode, String retMsg, Object ext) {
		super();
		this.retCode = retCode;
		this.retMsg = retMsg;
		this.ext = ext;
	}
	private String retCode;
	private String retMsg;
	private Object ext;
	public String getRetCode() {
		return retCode;
	}
	public void setRetCode(String retCode) {
		this.retCode = retCode;
	}
	public String getRetMsg() {
		return retMsg;
	}
	public void setRetMsg(String retMsg) {
		this.retMsg = retMsg;
	}
	public Object getExt() {
		return ext;
	}
	public void setExt(Object ext) {
		this.ext = ext;
	}
}
