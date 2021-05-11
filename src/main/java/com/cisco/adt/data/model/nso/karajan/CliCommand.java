package com.cisco.adt.data.model.nso.karajan;

import com.cisco.adt.data.model.nso.NSOServiceModel;

import jakarta.xml.bind.annotation.XmlElement;

public class CliCommand extends NSOServiceModel {

	@XmlElement(name = "device")
	protected String device;

	@XmlElement(name = "command")
	protected String command;

	@XmlElement(name = "configmode")
	protected Boolean configMode;

	@XmlElement(name = "success")
	protected Boolean success;

	@XmlElement(name = "message")
	protected String message;

	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public Boolean getConfigMode() {
		return configMode;
	}

	public void setConfigMode(Boolean configMode) {
		this.configMode = configMode;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
