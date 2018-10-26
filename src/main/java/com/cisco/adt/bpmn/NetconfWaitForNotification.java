package com.cisco.adt.bpmn;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import com.cisco.adt.data.connections.NSOConnection;
import com.tailf.jnc.Element;
import com.tailf.jnc.NetconfSession;

public class NetconfWaitForNotification implements JavaDelegate {

	@Override
	public void execute(DelegateExecution arg0) throws Exception {

		String stream = (String) arg0.getVariable("stream");
		String identifier = "";
		if (arg0.getVariable("identifier") != null) {
			identifier = (String) arg0.getVariable("identifier");
		}

		long timeout = 600;
		if (arg0.getVariable("timeout") != null) {
			timeout = Long.parseLong((String) arg0.getVariable("timeout"));
		}

		String contained = "";
		if (arg0.getVariable("contained") != null) {
			contained = (String) arg0.getVariable("contained");
		}
		NetconfSession netconfSession = NSOConnection.getInstance().getNetconfSession(arg0);
		netconfSession.createSubscription(stream);
		ExecutorService executor = Executors.newCachedThreadPool();
		Callable<Object> task = new Callable<Object>() {
			public Object call() {
				try {
					return netconfSession.receiveNotification();
				} catch (Exception e) {
					return null;
				}
			}
		};
		Element result = null;
		Future<Object> future = null;
		long secs_passed = 0;
		long after = 0;
		long before = 0;
		boolean timeoutReached = false;
		String notifString = null;
		while (timeout > 0) {
			timeoutReached = false;
			notifString = null;
			try {
				before = System.currentTimeMillis();
				future = executor.submit(task);
				Object resultObj = future.get(timeout, TimeUnit.SECONDS);
				if (resultObj == null) {
					timeout = -1;
					break;
				}
				result = (Element) resultObj;
				notifString = result.toXMLString();
				after = System.currentTimeMillis();
				secs_passed = TimeUnit.MILLISECONDS.toSeconds(after - before);
				timeout -= secs_passed;

				if (identifier.length() == 0) {
					timeout = -1;
				} else {
					if (notifString != null) {
						if (notifString.contains(identifier)) {
							timeout = -1;
						}
					}
				}
			} catch (Exception ex) {
				after = System.currentTimeMillis();
				secs_passed = TimeUnit.MILLISECONDS.toSeconds(after - before);
				timeout -= secs_passed;
				timeoutReached = true;
			} finally {
				if (future != null) {
					future.cancel(true);
				}
			}
		}

		if (contained.length() != 0) {
			boolean testResult = false;
			if (notifString != null) {
				testResult = notifString.contains(contained);
			}
			arg0.setVariable("adtResult", testResult);
		} else {
			if (!timeoutReached) {
				arg0.setVariable("adtResult", notifString);
			} else {
				arg0.setVariable("adtResult", "!TIMEOUT REACHED!");
			}
		}
	}
}
