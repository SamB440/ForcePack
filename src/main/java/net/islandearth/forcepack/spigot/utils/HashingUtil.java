package net.islandearth.forcepack.spigot.utils;

import javax.xml.bind.DatatypeConverter;

public class HashingUtil {
	
	public static String toHexString(byte[] array) {
	    return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String s) {
	    return DatatypeConverter.parseHexBinary(s);
	}
}
