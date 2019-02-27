package com.cisco.adt.bpmn;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import com.cisco.adt.data.ReturnCodes;
import com.cisco.adt.data.controllers.nso.KarajanPluginsController;
import com.cisco.adt.data.model.bpmn.TaskResult;

public class SSHSendCommand implements JavaDelegate {

	@Override
	public void execute(DelegateExecution arg0) throws Exception {

		String host = (String) arg0.getVariable("host");
		String user = (String) arg0.getVariable("user");
		int port = Integer.parseInt((String) arg0.getVariable("port"));
		String pass = (String) arg0.getVariable("pass");
		String type = (String) arg0.getVariable("type");
		boolean debug = false;
		if (arg0.getVariable("debug") != null) {
			debug = (Boolean) arg0.getVariable("debug");
		}

		String command = (String) arg0.getVariable("command");

		String contained = "";
		if (arg0.getVariable("contained") != null) {
			contained = (String) arg0.getVariable("contained");
		}

		TaskResult taskResult = new TaskResult();

		String resultString = "";
		switch (type) {
		case "config":
			if ((!command.endsWith("exit\n")) || (!command.endsWith("exit"))) {
				command += "\nexit\n";
			}
			resultString = KarajanPluginsController.sendSSHConfig(host, port, user, pass, 10000, command, debug);
			break;
		case "shell":
			resultString = KarajanPluginsController.sendSSHCommands(host, port, user, pass, command, debug);
			break;
		case "terminal":
			resultString = KarajanPluginsController.sendSSHTerminal(host, port, user, pass, command, debug);
			break;
		}

		if (contained.length() != 0) {
			boolean testResult = false;
			if (resultString != null) {
				testResult = resultString.contains(contained);
			}
			taskResult.setValue("" + testResult);
		} else {
			if (resultString != null) {
				taskResult.setValue(resultString);
			}
		}
		taskResult.setCode(ReturnCodes.OK);
		arg0.setVariable("taskResult", taskResult);

	}
}
