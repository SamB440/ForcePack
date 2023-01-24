package com.convallyria.forcepack.api.utils;

import com.convallyria.forcepack.api.verification.ResourcePackURLData;
import jakarta.xml.bind.DatatypeConverter;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

public class HashingUtil {
	
	public static String toHexString(byte[] array) {
	    return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String s) {
	    return DatatypeConverter.parseHexBinary(s);
	}

	public static String getHashFromUrl(String url) throws Exception {
		return getDataFromUrl(url).getFirst();
	}

	public static Pair<String, Integer> getDataFromUrl(String url) throws Exception {
		// This is not done async on purpose. We don't want the server to start without having checked this first.
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		final URLConnection urlConnection = new URL(url).openConnection();

		// Notify size of file
		final int sizeInMB = urlConnection.getContentLength() / 1024 / 1024;

		final InputStream fis = urlConnection.getInputStream();
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
		return Pair.of(toHexString(urlBytes), sizeInMB);
	}

	public static ResourcePackURLData performPackCheck(String url, String configHash) throws Exception {
		// This is not done async on purpose. We don't want the server to start without having checked this first.
		final Pair<String, Integer> data = getDataFromUrl(url);
		return new ResourcePackURLData(data.getFirst(), configHash, data.getSecond());
	}
}
