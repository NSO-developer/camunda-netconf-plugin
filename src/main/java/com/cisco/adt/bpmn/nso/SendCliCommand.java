package com.cisco.adt.bpmn.nso;

import com.cisco.adt.util.Utils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.adt.data.ReturnCodes;
import com.cisco.adt.data.connections.NetconfClient;
import com.cisco.adt.data.controllers.nso.KarajanPluginsController;
import com.cisco.adt.data.model.bpmn.TaskResult;
import com.cisco.adt.data.model.nso.karajan.CliCommand;
import com.cisco.stbarth.netconf.anc.NetconfException;
import com.cisco.stbarth.netconf.anc.NetconfException.ProtocolException;
import com.cisco.stbarth.netconf.anc.NetconfSession;

public class SendCliCommand implements JavaDelegate {

	private Logger logger = LoggerFactory.getLogger(SendCliCommand.class);

	@Override
	public void execute(DelegateExecution execution) {

		String commandStr = (String) execution.getVariable("command");
		boolean configMode = false;
		if (execution.getVariable("configmode") != null) {
			configMode = (Boolean) execution.getVariable("configmode");
		}
		String device = (String) execution.getVariable("device");
		String contained = "";
		if (execution.getVariable("contained") != null) {
			contained = (String) execution.getVariable("contained");
		}

		CliCommand cliCommand = new CliCommand();
		cliCommand.setCommand(commandStr);
		cliCommand.setDevice(device);
		cliCommand.setConfigMode(configMode);

		TaskResult taskResult = new TaskResult();

		NetconfSession ncSession = null;
		CliCommand cmdResult = null;
		try {
			ncSession = NetconfClient.getSession(execution);
			if (ncSession == null) {
				taskResult.setCode(ReturnCodes.ERROR_NC_SESSION);
				execution.setVariableLocal("taskResult", taskResult);
				logger.debug(ReturnCodes.ERROR_NC_SESSION + ", Could not create netconf session");
				return;
			} else {
				cmdResult = KarajanPluginsController.sendCliCommand(ncSession, cliCommand);
			}
		} catch (ProtocolException e) {
			taskResult.setCode(ReturnCodes.ERROR_NC_PROTOCOL);
			taskResult.setDetail(Utils.getRootException(e).getMessage());
			execution.setVariableLocal("taskResult", taskResult);
			logger.debug(ReturnCodes.ERROR_NC_PROTOCOL + ", " + Utils.getRootException(e).getMessage());
			return;
		} finally {
			try {
				ncSession.getClient().close();
			} catch (NetconfException e) {
			}
		}

		if (!cmdResult.getSuccess()) {
			taskResult.setCode(ReturnCodes.ERROR);
			taskResult.setDetail(cmdResult.getMessage());
			execution.setVariable("taskResult", taskResult);
			return;
		}

		if (contained.length() != 0) {
			boolean result = false;
			if (cmdResult.getMessage() != null) {
				result = cmdResult.getMessage().contains(contained);
			}
			taskResult.setValue("" + result);
			taskResult.setCode(ReturnCodes.OK);
		} else {
			String adtResult = cmdResult.getMessage();
			if ((adtResult != null) || (!adtResult.isEmpty())) {
				if (adtResult.startsWith("\n")) {
					adtResult = adtResult.substring(adtResult.indexOf("\n") + 1);
				}
				adtResult = adtResult.substring(adtResult.indexOf("\n") + 1);
				if (adtResult.lastIndexOf("\n") > 0) {
					adtResult = adtResult.substring(0, adtResult.lastIndexOf("\n"));
				}

			}

			logger.debug("Result: " + ReturnCodes.OK + ", " + adtResult);

			taskResult.setValue(adtResult);
			taskResult.setCode(ReturnCodes.OK);
			execution.setVariable("taskResult", taskResult);

		}

	}

}
