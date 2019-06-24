package com.cisco.adt.data.model.bpmn;

/**
 * POJO class to formalise any return variable from the plugins
 */
public class TaskResult {

	private String code;

	private String value;

	private String detail;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}


	public String toString() {
		String msg = "Task result: ";
		msg += "\n Result code: " + getCode();
		msg += "\n Result detail: " + getDetail();
		msg += "\n Result value: " + getValue();
		return msg;
	}

}
