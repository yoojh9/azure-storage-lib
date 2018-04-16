package com.hancom.lms;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.hancom.lms.utils.AzureStorageUtil;
import com.microsoft.azure.storage.core.Logger;


@SpringBootApplication
public class AzureStorageLibApplication implements CommandLineRunner {
	
	@Autowired
	private AzureStorageUtil azureStorageUtil;
	
	@Value("${azure.storage.container.name}")
	private String containerName;
	
	public static void main(String[] args) {
		SpringApplication.run(AzureStorageLibApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		// 1) file upload test
//		File file = new File("chopper.jpg");
//		InputStream is = new FileInputStream(file);
//		String path = "image/"+FilenameUtils.getName(file.getAbsolutePath());
//		long length = file.length();
//		azureStorageUtil.uploadFile(containerName, is, path, length);
		
		
//		File file = new File("BigBuckBunny.mp4");
//		InputStream is = new FileInputStream(file);
//		String path = "video/"+FilenameUtils.getName(file.getAbsolutePath());
//		long length = file.length();
//		boolean result = azureStorageUtil.uploadFile(containerName, is, path, length);
//		System.out.println("upload file result : {}" + result);
		
		// 2) get SAS uri by blobName
//		String uri = azureStorageUtil.getFileDownloadUri(containerName, "video/BigBuckBunny.mp4");
//		System.out.println(uri);
		
		String uri = azureStorageUtil.getFileDownloadUri(containerName, "image/chopper.jpg");
		System.out.println(uri);
		
		// 3) delete blob
//		boolean result = azureStorageUtil.deleteFile(containerName, "chopper.jpg");
//		System.out.println(result);
	}
	
	
}
