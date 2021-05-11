package com.cisco.adt.data.model.nso;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class NSOServiceModel {

	@XmlAttribute(name = "xmlns:nc")
	private String nc = "urn:ietf:params:xml:ns:netconf:base:1.0";

	@XmlAttribute(name = "operation")
	private String operation;

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public void remove() {
		this.setOperation("delete");
	}

}
