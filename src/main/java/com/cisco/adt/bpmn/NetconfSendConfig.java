package com.cisco.adt.bpmn;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import com.cisco.adt.data.connections.NSOConnection;
import com.cisco.adt.data.controllers.nso.NSOController;
import com.tailf.jnc.NetconfSession;

public class NetconfSendConfig implements JavaDelegate {

	@Override
	public void execute(DelegateExecution arg0) throws Exception {

		String configStr = (String) arg0.getVariable("configXml");
		NetconfSession netconfSession = NSOConnection.getInstance().getNetconfSession(arg0);
		boolean commandSent = NSOController.sendConfigToNSO(configStr, netconfSession);
		arg0.setVariable("adtResult", commandSent);
	}

}
