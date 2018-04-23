package com.hancom.lms;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.hancom.lms.utils.AzureStorageUtil;
import com.microsoft.azure.storage.blob.CloudBlob;


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
		InputStream is = null;
		
		try {
			File file = new File("chopper2.png");
			is = new FileInputStream(file);
			String path = "image/"+FilenameUtils.getName(file.getAbsolutePath());	// blob path
			long length = file.length();
			azureStorageUtil.uploadImage(containerName, is, path, length);
			is.close();
		} catch(Exception e){
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
		
		// 2) get SAS block uri
		String uri = azureStorageUtil.getFileDownloadUri(containerName, "image/chopper2.png");
		System.out.println(uri);
		
		// 3) delete blob
//		boolean result = azureStorageUtil.deleteFile(containerName, "image/chopper.jpg");
//		System.out.println(result);
		
		// 4) get All files
		List<CloudBlob> blobs =  azureStorageUtil.getAllFiles(containerName, "image");
		for(CloudBlob blob : blobs){
			System.out.println(blob.getName());
		}
		
		// 5) delete All files in directory
//		azureStorageUtil.deleteAllFiles(containerName, "image");
	}
	
	
}
