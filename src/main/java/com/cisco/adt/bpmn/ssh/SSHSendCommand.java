package com.cisco.adt.bpmn.ssh;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import com.cisco.adt.data.ReturnCodes;
import com.cisco.adt.data.controllers.nso.KarajanPluginsController;
import com.cisco.adt.data.model.bpmn.TaskResult;

/**
 *  Sample plugin for connecting over ssh and executing commands remotely
 *  Three workinmg modes are availeble (specified by the "type" input variable):
 *  - config - send configurations over ssh to any device
 *  - shell - execute bash command(s) over ssh
 *  - terminal - use a terminal emulated ssh session to send commands. Able to use in interactive mode,
 *  where each command can be specified like
 *  <string to expect>||<command/string to send>||<add to output- true/false>
 *      like for example:
 *      sure?||Yes||true
 *      	will wait until the string sure? is available in terminal, then it will send yes and will make sure that the console strings following
 *      	that command will be included ion the response
 *      # ||ls -al||true
 *      	will wait for the prompt "# ", will then send the list command and add the result to the response
 *  Be sure, in all of the cases to add an exit command at the end to be able to close the session
 *
 *  Will fill a @{@link TaskResult} object back to the workflow process, containing the result code (OK or not), a detail in case of error,
 *  as well as value containing the result of the read operation in case it was successful.
 *  If a "contained" string is specified, it the task will check if the string is contained in the result and return true/false
 */
public class SSHSendCommand implements JavaDelegate {

	/**
	 * Method called when task is executed by the process
	 * As input variables:
	 * - host, port, user, pass - for connecting to the ssh server/device
	 * - type - type of session to open (config, shell, terminal)
	 * - command - commands to be sent (one by line)
	 * - debug - will show debug information on the output for each executed command
	 * - contained - if present, will check if the string is contained in the result
	 * @param execution
	 */
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
		arg0.setVariableLocal("taskResult", taskResult);

	}
}
