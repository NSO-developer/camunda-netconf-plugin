package com.cisco.adt.bpmn;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import com.cisco.adt.data.connections.NSOConnection;
import com.cisco.adt.data.controllers.nso.NSOController;
import com.tailf.jnc.NetconfSession;

public class NetconfSendAction implements JavaDelegate {

	@Override
	public void execute(DelegateExecution arg0) throws Exception {
		String actionXml = (String) arg0.getVariable("actionXml");
		NetconfSession netconfSession = NSOConnection.getInstance().getNetconfSession(arg0);
		String commandResult = NSOController.sendActionToNSO(actionXml, netconfSession);
		arg0.setVariable("adtResult", commandResult);
	}

}
