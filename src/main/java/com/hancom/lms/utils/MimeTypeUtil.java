package com.hancom.lms.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimeTypeUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(MimeTypeUtil.class);
	 
	private static final Tika tika = new Tika();
	
	// get mime type from file name
	public static String getMimeType(String fileName){
		return tika.detect(fileName);
	}
	
	// get mime type from input stream
	public static String getMimeType(InputStream stream){
		String mimeType = null;
		try {
			mimeType = tika.detect(stream);
		} catch (IOException e) {
			logger.error("CommonUtil.getMimType from InputStream error {}", e);
		}
		return mimeType;
	}
	
	// get mime type from byte array
	public static String getMimeType(byte[] bytes){
		return tika.detect(bytes);
	}
}
