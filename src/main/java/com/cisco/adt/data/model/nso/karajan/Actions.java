package com.cisco.adt.data.model.nso.karajan;

import com.cisco.adt.data.model.nso.NSOServiceModel;

import jakarta.xml.bind.annotation.XmlElement;

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
