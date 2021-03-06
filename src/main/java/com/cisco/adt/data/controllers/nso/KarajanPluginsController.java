package com.cisco.adt.data.controllers.nso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.AccessDeniedException;
import java.util.Optional;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.adt.data.model.nso.karajan.Actions;
import com.cisco.adt.data.model.nso.karajan.CliCommand;
import com.cisco.adt.data.model.nso.karajan.Karajan;
import com.cisco.stbarth.netconf.anc.NetconfSession;
import com.cisco.stbarth.netconf.anc.XMLElement;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;

/**
 * Utility methods used in the different plugins - mainly ssh, sendcli
 */
public class KarajanPluginsController {

	private static final int MAX_OUTPUT_BUFFER_SIZE = 1024 * 1024;

	private static Logger logger = LoggerFactory.getLogger(KarajanPluginsController.class);

	public static CliCommand sendCliCommand(NetconfSession ncSession, CliCommand cliCommand) {
		Karajan karajan = new Karajan();
		Actions actions = new Actions();
		actions.setCliCommand(cliCommand);
		karajan.setActions(actions);

		XMLElement data = ANCNetconfController.sendActionToNSO(ncSession, karajan);

		CliCommand returnCli = new CliCommand();
		returnCli.setSuccess(false);

		try {
			Optional<XMLElement> cliReturnNode = data.getFirst("http://com/cisco/adt","karajan");
			String xmlConfig = cliReturnNode.get().toXML();

			karajan = new Karajan();
			if (!xmlConfig.isEmpty()) {
				try {
					JAXBContext jaxbContext = JAXBContext.newInstance(Karajan.class);
					Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
					jaxbUnmarshaller.setEventHandler(new ValidationEventHandler() {

						@Override
						public boolean handleEvent(ValidationEvent event) {
							logger.debug(event.getMessage());
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

	public static String sendSSHCommands(String host, int port, String user, String pass, String cmdsToExecute,
										 boolean debug) {

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

				if (debug) {
					result += ">>>>>>>>>> " + command;
				}

				while (true) {
					String line = br.readLine();
					if (line == null)
						break;
					if (result.length() > 0) {
						result += "\n";
					}
					result += line;
				}
				if (debug) {
					result += "\n";
				}
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
									   String command, boolean debug) throws IOException, AccessDeniedException {
		Connection connection = new Connection(host, port);

		connection.connect(null, timeout, timeout);

		boolean authenticated = connection.authenticateWithPassword(username, password);

		if (!authenticated)
			return null;

		Session session = connection.openSession();
		session.startShell();

		return sendSSHConfig(session, timeout, command, debug);
	}

	public static String sendSSHConfig(Session session, int timeout, String command, boolean debug)
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
	public static String sendSSHTerminal(String host, int port, String user, String pass, String cmdsToExecute,
										 boolean debug) {
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

			boolean interactiveMode = true;

			InputStream stdout = new StreamGobbler(sess.getStdout());
			Scanner scanner = new Scanner(cmdsToExecute);
			boolean stop = false;
			boolean addToOutput1 = false;
			boolean addToOutput2 = false;
			boolean first = true;

			while (!stop & scanner.hasNextLine()) {
				String command = scanner.nextLine();
				if (command.trim().isEmpty()) {
					continue;
				}
				if (command.startsWith("////")) {
					continue;
				}
				String[] parts = command.split("\\|\\|");
				String prompt = null;
				String answer = null;


				if (parts.length > 1) {
					prompt = parts[0];
					answer = parts[1];
					if (parts.length == 3) {
						if (!first) {
							addToOutput1 = addToOutput2;
						}
						if (!"true".equalsIgnoreCase(parts[2])) {
							addToOutput2 = false;
						} else {
							addToOutput2 = true;
						}
						if (first) {
							addToOutput1 = addToOutput2;
							first = false;
						}

					} else {
						addToOutput1 = addToOutput2;
						addToOutput2 = false;
					}
				} else {
					interactiveMode = false;
					answer = command;
				}

				boolean goon = true;
				String toRead = "";
				byte[] tmp = new byte[1024];

				long now = System.currentTimeMillis();
				while (goon) {
					if (System.currentTimeMillis() - now > 300000) {
						result += "Execution BREAK - 300s timeout reached";
						stop = true;
						break;
					}

					if (prompt != null) {
						while (stdout.available() > 0) {

							int i = stdout.read(tmp, 0, 1024);
							if (i < 0) {
								goon = false;
								break;
							}
							toRead += new String(tmp, 0, i);
						}
						// System.out.println(new String(tmp, 0, i));
						if (toRead.contains(prompt)) {
							if (addToOutput1) {
								result += toRead;
							}
							pw.println(answer);
							pw.flush();
							goon = false;
							break;
						}

					} else {
						pw.println(answer);
						pw.flush();
						goon = false;
					}
				}

			}

			if (!interactiveMode) {
				BufferedReader remoteStdoutReader = new BufferedReader(new InputStreamReader(stdout));
				String line;
				// sess.waitForCondition(ChannelCondition.STDOUT_DATA, 600000);
				while ((line = remoteStdoutReader.readLine()) != null) {
					result += line + "\n";
				}
				remoteStdoutReader.close();
			}

			scanner.close();
			stdout.close();
			pw.close();
			sess.close();
			conn.close();

			return result;

		} catch (

				Exception e) {
			e.printStackTrace(System.err);
			if (result.isEmpty()) {
				result = "No output";
			}
			return result;
		}

	}

}
