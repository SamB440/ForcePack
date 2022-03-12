package com.convallyria.forcepack.api.utils;

import jakarta.xml.bind.DatatypeConverter;

import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;

public class HashingUtil {
	
	public static String toHexString(byte[] array) {
	    return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String s) {
	    return DatatypeConverter.parseHexBinary(s);
	}

	public static String getHashFromUrl(String url) throws Exception {
		// This is not done async on purpose. We don't want the server to start without having checked this first.
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		InputStream fis = new URL(url).openStream();
		int n = 0;
		byte[] buffer = new byte[8192];
		while (n != -1) {
			n = fis.read(buffer);
			if (n > 0) {
				digest.update(buffer, 0, n);
			}
		}
		fis.close();
		final byte[] urlBytes = digest.digest();
		return toHexString(urlBytes);
	}

	public static void performPackCheck(String url, String hash, TriConsumer<String, String, Boolean> consumer) throws Exception {
		// This is not done async on purpose. We don't want the server to start without having checked this first.
		final String urlCheckHash = getHashFromUrl(url);
		consumer.accept(urlCheckHash, hash, urlCheckHash.equalsIgnoreCase(hash));
	}
}
