package com.cisco.adt.data.model.nso.karajan;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.cisco.adt.data.model.nso.NSOServiceModel;

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
