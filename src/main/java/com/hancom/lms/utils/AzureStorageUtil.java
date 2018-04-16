package com.hancom.lms.utils;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.Constants;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;

@Component
public class AzureStorageUtil {
	
	@Autowired
	private CloudStorageAccount cloudStorageAccount;
	
	private CloudBlobClient blobClient;
	
	private static final Logger logger = LoggerFactory.getLogger(AzureStorageUtil.class);
	 
	@PostConstruct
	public void init() {
		blobClient = cloudStorageAccount.createCloudBlobClient();
    }
	
	// return container Object. If the container does not exist, create the container first
	private CloudBlobContainer getContainer(String containerName) throws URISyntaxException, StorageException{
		CloudBlobContainer container = blobClient.getContainerReference(containerName);

		if(!container.exists()){
			container.createIfNotExists();
			
			// SAS URL 사용을 위해 컨테이너에 policy 정의
			BlobContainerPermissions permissions = new BlobContainerPermissions();
			SharedAccessBlobPolicy readPolicy = new SharedAccessBlobPolicy();
			readPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
			permissions.getSharedAccessPolicies().put("readPolicy", readPolicy);
			
			container.uploadPermissions(permissions);
		}
		
		return container;
	}

	// upload file stream as a block blob
	public boolean uploadFile(String containerName, InputStream is, String fileName,  long length) {
		try {
			logger.info("start upload file {} time {}", fileName, System.currentTimeMillis());
			
			CloudBlobContainer container = getContainer(containerName);
			CloudBlockBlob blob = container.getBlockBlobReference(fileName);
			
			// 해당 설정은 이후 테스트 해보면서 바꿔야할 듯.
			int concurrentRequestCount = (length>64000)? 3 : 1 ;
			BlobRequestOptions options = new BlobRequestOptions();
			options.setConcurrentRequestCount(concurrentRequestCount);
			
			blob.upload(is, length, AccessCondition.generateEmptyCondition(), options, new OperationContext());
			
			logger.info("complete upload file {} time {}", fileName, System.currentTimeMillis());
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
		return true;
	}
	
	// upload file byte array
	public boolean uploadFile(String containerName, byte[] res, String fileName) {       
		try {
			logger.info("start upload file {} time {}", fileName, System.currentTimeMillis());
			
			CloudBlobContainer container = getContainer(containerName);
			CloudBlockBlob blob = container.getBlockBlobReference(fileName);
			int length = res.length;
	        blob.uploadFromByteArray(res, 0, length);
	        
	        // 해당 설정은 이후 테스트 해보면서 바꿔야할 듯.
			int concurrentRequestCount = (length>64000)? 3 : 1 ;
			BlobRequestOptions options = new BlobRequestOptions();
			options.setConcurrentRequestCount(concurrentRequestCount);
			
	        blob.uploadFromByteArray(res, 0, length, AccessCondition.generateEmptyCondition(), options, new OperationContext());
	        
	        logger.info("complete upload file {} time {}", fileName, System.currentTimeMillis());
	        
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
		return true;
	}


	
	// file SAS(공유 엑세스 서명) URL with expire time
	public String getFileDownloadUri(String containerName, String blobName) throws URISyntaxException, StorageException {
		
		CloudBlobContainer container = getContainer(containerName);
		CloudBlob blob = container.getBlobReferenceFromServer(blobName);
		
		if(!blob.exists()){
			logger.error("blob {} not exist", blobName);
			return null;
		}

		SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
		
		// expire time 지정 (UTC 시가능로 지정됨)
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.setTime(new Date());
		calendar.add(Calendar.MINUTE , 5);
		policy.setSharedAccessExpiryTime(calendar.getTime());	
		
		String sasUri = null;
		try {
			String sasToken = blob.generateSharedAccessSignature(policy, "readPolicy");
			sasUri = String.format("%s?%s", blob.getUri(), sasToken);
		} catch (InvalidKeyException e) {
			logger.error("invalid SAS Token error : {}" , e.getMessage());
		}
		
		return sasUri;
	}
	
	// delete blob
	public boolean deleteFile(String containerName, String blobName) throws URISyntaxException, StorageException{
		CloudBlobContainer container = getContainer(containerName);
		CloudBlob blob = container.getBlobReferenceFromServer(blobName);

		if(!blob.exists()){
			logger.error("blob {} not exist", blobName);
			return false;
		}
		
		// true : delete file success, false: delete file failed.
		return blob.deleteIfExists();
	}
	
}
