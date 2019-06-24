package com.cisco.adt.data.model.nso.karajan;

import javax.xml.bind.annotation.XmlElement;

import com.cisco.adt.data.model.nso.NSOServiceModel;

public class Actions extends NSOServiceModel {

	@XmlElement(name = "cliCommand")
	protected CliCommand cliCommand;

	public CliCommand getCliCommand() {
		return cliCommand;
	}

	public void setCliCommand(CliCommand cliCommand) {
		this.cliCommand = cliCommand;
	}

}
