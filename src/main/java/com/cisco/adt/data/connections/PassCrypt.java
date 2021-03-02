package com.cisco.adt.data.connections;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encrypts/decrypts the password saved in the netconf profiles file
 * Authentication on nso can be done in 2 ways in the plugin: using specific credentials (nc_host, nc_port, nc_user, nc_pass) as input
 * variables, or using a predefined profile in the netconf-profiles.properties file - in this case the password has to be encrypted with the same
 * method and key as in this file (an encryption iutility is provided as part of the project)
 */
public class PassCrypt {

	private static Logger logger = LoggerFactory.getLogger(PassCrypt.class);

	private static String key = "2A472D4A614E645267556B5870327335";

	public static String encrypt(String strToEncrypt) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			SecretKey secretKey = new SecretKeySpec(decodeHexString(key), "AES");

			cipher.init(Cipher.ENCRYPT_MODE, secretKey);

			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));

		} catch (Exception e) {

			logger.debug("Error while encrypting: " + e.toString());
			return null;
		}
	}

	public static String decrypt(String strToDecrypt) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			SecretKey secretKey = new SecretKeySpec(decodeHexString(key), "AES");

			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));

		} catch (Exception e) {
			logger.debug("Error while decrypting: " + e.toString());
			return null;

		}
	}

	private static byte[] decodeHexString(String hexString) {
		if (hexString.length() % 2 == 1) {
			throw new IllegalArgumentException("Invalid hexadecimal String supplied.");
		}

		byte[] bytes = new byte[hexString.length() / 2];
		for (int i = 0; i < hexString.length(); i += 2) {
			bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
		}
		return bytes;
	}

	private static byte hexToByte(String hexString) {
		int firstDigit = toDigit(hexString.charAt(0));
		int secondDigit = toDigit(hexString.charAt(1));
		return (byte) ((firstDigit << 4) + secondDigit);
	}

	private static int toDigit(char hexChar) {
		int digit = Character.digit(hexChar, 16);
		if (digit == -1) {
			throw new IllegalArgumentException("Invalid Hexadecimal Character: " + hexChar);
		}
		return digit;
	}

}