package com.convallyria.forcepack.api.utils;

import com.convallyria.forcepack.api.verification.ResourcePackURLData;
import jakarta.xml.bind.DatatypeConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.function.Consumer;

public class HashingUtil {
	
	public static String toHexString(byte[] array) {
	    return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String s) {
	    return DatatypeConverter.parseHexBinary(s);
	}

	public static String getHashFromUrl(String url) throws Exception {
		return getHashFromUrl(url, null);
	}

	public static String getHashFromUrl(String url, @Nullable Consumer<Integer> size) throws Exception {
		// This is not done async on purpose. We don't want the server to start without having checked this first.
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		final URLConnection urlConnection = new URL(url).openConnection();

		// Notify size of file
		if (size != null) {
			final int sizeInMB = urlConnection.getContentLength() / 1024 / 1024;
			size.accept(sizeInMB);
		}

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
		return toHexString(urlBytes);
	}

	public static ResourcePackURLData performPackCheck(String url, String hash, Consumer<Integer> size) throws Exception {
		// This is not done async on purpose. We don't want the server to start without having checked this first.
		final String urlCheckHash = getHashFromUrl(url, size);
		return new ResourcePackURLData(urlCheckHash, hash);
	}
}
