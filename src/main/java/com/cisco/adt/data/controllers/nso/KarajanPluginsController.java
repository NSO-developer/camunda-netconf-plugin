package com.cisco.adt.data.controllers.nso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.AccessDeniedException;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import com.cisco.adt.data.model.nso.karajan.Actions;
import com.cisco.adt.data.model.nso.karajan.CliCommand;
import com.cisco.adt.data.model.nso.karajan.Karajan;
import com.tailf.jnc.Element;
import com.tailf.jnc.NetconfSession;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class KarajanPluginsController {

	// private static final String PROMPT_SYMBOL = "%";
	// private static final char ERROR_SYMBOL = '^';
	// private static final String ACCESS_DENIED_MESSAGE = "Access denied";
	private static final int MAX_OUTPUT_BUFFER_SIZE = 1024 * 1024;

	public static CliCommand sendCliCommand(CliCommand cliCommand, NetconfSession netconfSession) {
		Karajan karajan = new Karajan();
		Actions actions = new Actions();
		actions.setCliCommand(cliCommand);
		karajan.setActions(actions);

		Element data = NSOController.sendActionToNSO(karajan, netconfSession);

		CliCommand returnCli = new CliCommand();
		returnCli.setSuccess(false);

		try {
			Element cliReturnNode = data.getChild("karajan");

			String xmlConfig = cliReturnNode.toXMLString();

			karajan = new Karajan();
			if (!xmlConfig.isEmpty()) {
				try {
					JAXBContext jaxbContext = JAXBContext.newInstance(Karajan.class);
					Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
					jaxbUnmarshaller.setEventHandler(new ValidationEventHandler() {

						@Override
						public boolean handleEvent(ValidationEvent event) {
							System.out.println(event.getMessage());
							return true;
						}
					});

					karajan = (Karajan) jaxbUnmarshaller.unmarshal(new StringReader(xmlConfig));
				} catch (JAXBException e) {
					e.printStackTrace();
				}
			}

			return karajan.getActions().getCliCommand();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return returnCli;
		}

	}

	public static String sendSSHCommands(String host, int port, String user, String pass, String cmdsToExecute) {

		String result = "";

		try {
			Connection conn = new Connection(host, port);
			conn.connect();

			boolean isAuthenticated = conn.authenticateWithPassword(user, pass);

			if (isAuthenticated == false)
				return "SSH: AUTH FAILED";

			Scanner scanner = new Scanner(cmdsToExecute);
			while (scanner.hasNextLine()) {
				String command = scanner.nextLine();

				Session sess = conn.openSession();

				sess.execCommand(command);

				InputStream stdout = new StreamGobbler(sess.getStdout());
				BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

				if (result.length() > 0) {
					result += "\n";
				}
				result += ">>>>>>>>>> " + command;

				while (true) {
					String line = br.readLine();
					if (line == null)
						break;
					if (result.length() > 0) {
						result += "\n";
					}
					result += line;
				}
				result += "\n";
				br.close();
				sess.close();

			}
			scanner.close();

			conn.close();

			return result;

		} catch (IOException e) {
			e.printStackTrace(System.err);
			return "SSH: ERROR";
		}
	}

	public static String sendSSHConfig(String host, int port, String username, String password, int timeout,
			String command) throws IOException, AccessDeniedException {
		Connection connection = new Connection(host, port);

		connection.connect(null, timeout, timeout);

		boolean authenticated = connection.authenticateWithPassword(username, password);

		if (!authenticated)
			return null;

		Session session = connection.openSession();
		session.startShell();

		return sendSSHConfig(session, timeout, command);
	}

	public static String sendSSHConfig(Session session, int timeout, String command)
			throws IOException, AccessDeniedException {
		OutputStream in = session.getStdin();

		in.write(command.getBytes());

		InputStream stdout = session.getStdout();
		InputStream stderr = session.getStderr();

		StringBuilder sb = new StringBuilder();
		boolean valid = false;
		String line = null;

		boolean flag = true;
		while (flag) {
			int conditions = session.waitForCondition(
					ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, timeout);

			if ((conditions & ChannelCondition.TIMEOUT) != 0) {
				break;
			}

			if ((conditions & ChannelCondition.EOF) != 0) {
				if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
					break;
				}
			}

			BufferedReader reader = null;
			try {
				if ((ChannelCondition.STDOUT_DATA & conditions) != 0) {
					reader = new BufferedReader(new InputStreamReader(stdout));
				} else {
					reader = new BufferedReader(new InputStreamReader(stderr));
				}

				// Reader.readLine() may hang if 'exit' is not added to the command as the last
				// line (use the 'exit' to exit the shell)

				boolean toAppend = true;

				while (true) {
					line = reader.readLine();

					if (line == null) {
						valid = true;

						flag = false;
						break;
					}

					if (toAppend) {
						toAppend = append(sb, line);
					}

					// if (line.trim().startsWith(PROMPT_SYMBOL)) {
					// if (line.trim().indexOf(ERROR_SYMBOL) >= 0) {
					// valid = false;
					// flag = false;
					// break;
					// } else if (line.trim().contains(ACCESS_DENIED_MESSAGE)) {
					// throw new AccessDeniedException(line.trim());
					// }
					// }

					// line = reader.readLine();
				}
			} finally {
				if (reader != null)
					reader.close();
			}
		}

		String message = sb.toString().trim();

		// keep all output
		// r4(config)#int Fastethernet1/0
		// ^
		// % Invalid input detected at '^' marker.

		return message;
	}

	private static boolean append(StringBuilder sb, String line) {

		// System.out.println(line);

		if (sb.length() >= MAX_OUTPUT_BUFFER_SIZE) {
			sb.setLength(MAX_OUTPUT_BUFFER_SIZE - 3);
			sb.append("...");

			return false;
		}

		if (sb.length() + line.length() > MAX_OUTPUT_BUFFER_SIZE) {
			// Minimum abbreviation width is 4
			if (MAX_OUTPUT_BUFFER_SIZE - sb.length() < 4) {
				sb.setLength(MAX_OUTPUT_BUFFER_SIZE - 3);
				sb.append("...");
			} else {
				// String abbreviated = StringUtil.abbreviate(line, MAX_OUTPUT_BUFFER_SIZE -
				// sb.length());

				sb.append(line);
			}

			sb.append('\n');

			return false;
		}

		sb.append(line);
		sb.append('\n');

		return true;
	}

	/**
	 * @param host
	 * @param port
	 * @param user
	 * @param pass
	 * @param cmdsToExecute
	 * @return
	 */
	public static String sendSSHTerminal(String host, int port, String user, String pass, String cmdsToExecute) {
		String result = "";

		try {
			Connection conn = new Connection(host, port);
			conn.connect();

			boolean isAuthenticated = conn.authenticateWithPassword(user, pass);

			if (isAuthenticated == false)
				return "SSH: AUTH FAILED";

			ch.ethz.ssh2.Session sess = conn.openSession();

			sess.requestDumbPTY();
			sess.startShell();
			OutputStream out = sess.getStdin();
			PrintStream pw = new PrintStream(out);
			pw.flush();

			/*
			 * This basic example does not handle stderr, which is sometimes dangerous
			 * (please read the FAQ).
			 */
			InputStream stdout = new StreamGobbler(sess.getStdout());
			Scanner scanner = new Scanner(cmdsToExecute);
			boolean stop = false;
			while (!stop & scanner.hasNextLine()) {
				String command = scanner.nextLine();
				String[] parts = command.split("\\|\\|");
				String prompt = parts[0];
				String answer = parts[1];
				boolean goon = true;
				String toRead = "";
				byte[] tmp = new byte[1024];

				long now = System.currentTimeMillis();
				while (goon) {
					if (System.currentTimeMillis() - now > 300000) {
						result += "Execution BREAK";
						stop = true;
						break;
					}
					while (stdout.available() > 0) {

						int i = stdout.read(tmp, 0, 1024);
						if (i < 0) {
							goon = false;
							break;
						}
						toRead += new String(tmp, 0, i);
						// System.out.println(new String(tmp, 0, i));
						if (toRead.contains(prompt)) {
							result += toRead + answer;
							pw.println(answer);
							pw.flush();
							goon = false;
							break;
						}
					}
				}

			}
			scanner.close();
			stdout.close();
			pw.close();
			sess.close();
			conn.close();

			return result;

		} catch (Exception e) {
			e.printStackTrace(System.err);
			if (result.isEmpty()) {
				result = "No output";
			}
			return result;
		}

	}

}
