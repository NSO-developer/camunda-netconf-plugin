package com.cisco.adt.data.model.nso.karajan;

import com.cisco.adt.data.model.nso.NSOServiceModel;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "karajan")
public class Karajan extends NSOServiceModel {

	@XmlElement(name = "actions")
	protected Actions actions;

	public Actions getActions() {
		return actions;
	}

	public void setActions(Actions actions) {
		this.actions = actions;
	}

}
