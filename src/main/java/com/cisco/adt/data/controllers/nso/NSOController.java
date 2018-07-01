package com.cisco.adt.data.controllers.nso;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.cisco.adt.data.controllers.exceptions.ValidationFailException;
import com.cisco.adt.data.model.nso.NSOServiceModel;
import com.tailf.jnc.Element;
import com.tailf.jnc.JNCException;
import com.tailf.jnc.NetconfSession;
import com.tailf.jnc.NodeSet;
import com.tailf.jnc.XMLParser;
//import com.vaadin.ui.UI;

// All requests to and from NSO
public class NSOController {

	private static int messageId = 0;

	public static NodeSet readConfigFromNSO(String xpath, NetconfSession netconfSession) {

		try {
			if (netconfSession == null) {
				return null;
			}
			NodeSet result = netconfSession.getConfig(xpath);
			netconfSession.closeSession();
			return result;
		} catch (JNCException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static String readFromNSOAsString(String xpath, boolean includeOperational, NetconfSession netconfSession) {

		try {
			if (netconfSession == null) {
				return null;
			}
			NodeSet result = null;
			if (includeOperational) {
				result = netconfSession.get(xpath);
			} else {
				result = netconfSession.getConfig(xpath);
			}
			netconfSession.closeSession();
			return result.toXMLString();
		} catch (JNCException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static NodeSet readOperFromNSO(String xpath, NetconfSession netconfSession) {
		try {

			if (netconfSession == null) {
				return null;
			}
			NodeSet result = netconfSession.get(xpath);
			netconfSession.closeSession();
			return result;
		} catch (JNCException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static Element sendActionToNSO(NSOServiceModel action, NetconfSession netconfSession) {
		if (netconfSession == null) {
			// Notification.show("Error connecting to NSO");
			return null;
		}

		StringWriter xml = new StringWriter();

		try {
			JAXBContext context = JAXBContext.newInstance(action.getClass());
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(action, xml);
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}
		NodeSet ns = new NodeSet();

		try {
			XMLParser xmlParser = new XMLParser();
			Element element = netconfSession.action(xmlParser.parse(xml.toString()));
			return element;
		} catch (JNCException | IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	public static String sendActionToNSO(String actionXML, NetconfSession netconfSession) {
		if (netconfSession == null) {
			return null;
		}

		try {
			XMLParser xmlParser = new XMLParser();
			Element element = netconfSession.action(xmlParser.parse(actionXML));
			if (element != null) {
				return element.toXMLString();
			}
		} catch (JNCException | IOException e) {
			e.printStackTrace();
			return null;
		}

		return null;
	}

	public static boolean sendConfigToNSO(String xmlString, NetconfSession netconfSession)
			throws ValidationFailException {
		NodeSet ns = null;
		try {
			XMLParser xmlParser = new XMLParser();
			ns = new NodeSet(xmlParser.parse(xmlString));
			netconfSession.editConfig(ns);
			netconfSession.closeSession();
			return true;
		} catch (JNCException | IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void sendConfigToNSO(NSOServiceModel service, NetconfSession netconfSession)
			throws ValidationFailException {
		sendConfigToNSO(service, true, netconfSession);
	}

	public static void sendConfigToNSO(NSOServiceModel service, boolean enableDebug, NetconfSession netconfSession)
			throws ValidationFailException {
		boolean commit = false;

		StringWriter xml = new StringWriter();

		try {
			JAXBContext context = JAXBContext.newInstance(service.getClass());
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			m.marshal(service, xml);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		NodeSet ns = new NodeSet();

		// System.out.println(xml.toString());

		try {
			XMLParser xmlParser = new XMLParser();
			ns = new NodeSet(xmlParser.parse(xml.toString()));
		} catch (JNCException e) {
			e.printStackTrace();
		}

		try {
			netconfSession.validate(ns.getElement(0));
			commit = true;
		} catch (JNCException | IOException e) {
			e.printStackTrace();
			try {
				netconfSession.closeSession();
			} catch (Exception e1) {
			}
			throw new ValidationFailException(e.toString());
		}

		if (enableDebug) {
			// if
			// (ConfigProperties.getInstance().getProperty("debug").equalsIgnoreCase("true"))
			// {
			// DebugWindow debug = new DebugWindow(xml.toString());
			// UI.getCurrent().addWindow(debug);
			// }
		}
		if (commit) {
			try {
				netconfSession.editConfig(ns);
				netconfSession.closeSession();

			} catch (JNCException | IOException e) {
				e.printStackTrace();
				try {
					netconfSession.closeSession();
				} catch (Exception e1) {
				}
				throw new ValidationFailException(e.toString());
			}
		}
	}

	public static void sendRPCToNSO(String xmlCommand, NetconfSession netconfSession) throws ValidationFailException {
		try {
			Element response = netconfSession.rpc(xmlCommand);
			System.out.println(response.toXMLString());
		} catch (IOException | JNCException e) {
			e.printStackTrace();
			try {
				netconfSession.closeSession();
			} catch (Exception e1) {
			}
			throw new ValidationFailException(e.toString());
		}
	}

}
