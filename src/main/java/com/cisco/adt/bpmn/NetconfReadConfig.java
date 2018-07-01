package com.cisco.adt.bpmn;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import com.cisco.adt.data.connections.NSOConnection;
import com.cisco.adt.data.controllers.nso.NSOController;
import com.tailf.jnc.NetconfSession;

public class NetconfReadConfig implements JavaDelegate {

	@Override
	public void execute(DelegateExecution arg0) throws Exception {

		String xpathStr = (String) arg0.getVariable("xpath");
		boolean includeOperational = (Boolean) arg0.getVariable("oper");
		String contained = "";
		if (arg0.getVariable("contained") != null) {
			contained = (String) arg0.getVariable("contained");
		}
		NetconfSession netconfSession = NSOConnection.getInstance().getNetconfSession(arg0);
		String configString = NSOController.readFromNSOAsString(xpathStr, includeOperational, netconfSession);

		if (contained.length() != 0) {
			boolean testResult = false;
			if (configString != null) {
				testResult = configString.contains(contained);
			}
			arg0.setVariable("adtResult", testResult);
		} else {
			arg0.setVariable("adtResult", configString);
		}
	}
}
