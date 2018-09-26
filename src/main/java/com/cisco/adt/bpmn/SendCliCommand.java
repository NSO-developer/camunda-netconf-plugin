package com.cisco.adt.bpmn;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import com.cisco.adt.data.connections.NSOConnection;
import com.cisco.adt.data.controllers.nso.KarajanPluginsController;
import com.cisco.adt.data.model.nso.karajan.CliCommand;
import com.tailf.jnc.NetconfSession;

public class SendCliCommand implements JavaDelegate {

	@Override
	public void execute(DelegateExecution arg0) throws Exception {

		String commandStr = (String) arg0.getVariable("command");
		boolean configMode = false;
		if (arg0.getVariable("configmode") != null) {
			configMode = (Boolean) arg0.getVariable("configmode");
		}
		String device = (String) arg0.getVariable("device");
		String contained = "";
		if (arg0.getVariable("contained") != null) {
			contained = (String) arg0.getVariable("contained");
		}

		CliCommand cliCommand = new CliCommand();
		cliCommand.setCommand(commandStr);
		cliCommand.setDevice(device);
		cliCommand.setConfigMode(configMode);

		NetconfSession netconfSession = NSOConnection.getInstance().getNetconfSession(arg0);
		CliCommand cmdResult = KarajanPluginsController.sendCliCommand(cliCommand, netconfSession);
		if (contained.length() != 0) {
			boolean result = false;
			if (cmdResult.getMessage() != null) {
				result = cmdResult.getMessage().contains(contained);
			}
			arg0.setVariable("adtResult", result);
		} else {
			String adtResult = cmdResult.getMessage();
			if ((adtResult == null) || (adtResult.isEmpty())) {
				adtResult = "|EMPTY RESULT|";
			} else {
				if (adtResult.startsWith("\n")) {
					adtResult = adtResult.substring(adtResult.indexOf("\n") + 1);
				}
				adtResult = adtResult.substring(adtResult.indexOf("\n") + 1);
				if (adtResult.lastIndexOf("\n") > 0) {
					adtResult = adtResult.substring(0, adtResult.lastIndexOf("\n"));
				}
			}
			arg0.setVariable("adtResult", adtResult);
		}
	}

}
