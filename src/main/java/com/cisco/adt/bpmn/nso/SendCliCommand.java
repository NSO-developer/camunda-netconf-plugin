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

/**
 * Conveniency plugin to send cli commands to devices over nso. It uses the karajan nso service also part of this project,
 * which basically provides a generic action to send cli commands to the devices using live status NED feature.
 * The action will look at the device type and it will send the required cli command usiong the correct NED feature.
 * Supported for now: cisco-ios, cisco-ios-xr, alu-sr, juniper-junos, redback-se
 * Note that the same functionality can be reached using the NetconfSendAction plugin, however this plugin hides the differences between
 * NEDs and offers an unified abstract plugin
 *  Will fill a @{@link TaskResult} object back to the workflow process, containing the result code (OK or not), a detail in case of error,
 *  as well as value containing the result of the read operation in case it was successful
 *  If a "contained" string is specified, it the task will check if the string is contained in the result and return true/false
 */
public class SendCliCommand implements JavaDelegate {

	private Logger logger = LoggerFactory.getLogger(SendCliCommand.class);

	/**
	 * Method called when task is executed by the process
	 * As input variables:
	 * - command - string containing the command(s) to be executed
	 * - configmode - specifies if the command should be executed in config mode (not for all NEDs)
	 * - device - the device to send the commands to, needs to be onboarded ion NSO
	 * - contained - if present, will check if the string is contained in the result
	 * @param execution
	 */
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

		logger.debug("Request: " + commandStr + ", Device: " + device + ", Config mode: " + configMode + ", Test string contained: "
				+ contained);

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
		} catch (Exception e) {
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
			execution.setVariableLocal("taskResult", taskResult);

		}

	}

}
