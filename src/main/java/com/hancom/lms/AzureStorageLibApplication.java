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
		File file = new File("chopper.jpg");
		InputStream is = new FileInputStream(file);
		String path = "image/"+FilenameUtils.getName(file.getAbsolutePath());
		long length = file.length();
		azureStorageUtil.uploadFile(containerName, is, path, length);
		
		String uri = azureStorageUtil.getFileDownloadUri(containerName, "image/chopper.jpg");
		System.out.println(uri);
		
		// 3) delete blob
//		boolean result = azureStorageUtil.deleteFile(containerName, "image/chopper.jpg");
//		System.out.println(result);
	}
	
	
}
