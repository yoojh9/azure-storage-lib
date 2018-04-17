package com.hancom.lms.utils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
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

import com.microsoft.azure.storage.CloudStorageAccount;
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
	
	/**
	 * 컨테이너 Object 리턴
	 * If the container does not exist, create the container first
	 * @param containerName
	 * @return
	 * @throws URISyntaxException
	 * @throws StorageException
	 */
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

	
	/**
	 * 이미지 업로드나 적은 용량의 파일을 upload 할 때 사용
	 * upload image stream as a block blob
	 * @param containerName
	 * @param is
	 * @param fileName
	 * @param length
	 * @return
	 */
	public boolean uploadImage(String containerName, InputStream is, String fileName, long length) {
		try {
			CloudBlobContainer container = getContainer(containerName);
			CloudBlockBlob blob = container.getBlockBlobReference(fileName);
			
			blob.upload(is, length);
			
			logger.info("complete upload image {}", fileName);

		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		} 
		return true;
	}
	

	/**
	 * video나 audio 등의 큰 용량의 파일을 업르드 할 때 사용 (stream 방식)
	 * upload large file stream as a block blob
	 * @param containerName
	 * @param is
	 * @param fileName
	 * @param length
	 * @return
	 */
	public boolean uploadStream(String containerName, InputStream is, String fileName, long length) {
		try {
			BufferedInputStream bis = new BufferedInputStream(is);
			
			logger.info("start upload file {} time {}", fileName, System.currentTimeMillis());
			
			CloudBlobContainer container = getContainer(containerName);
			CloudBlockBlob blob = container.getBlockBlobReference(fileName);
			
			// TODO: 임시로 8로 설정해 놓음. 나중에 테스트 하면서 바꿔야 할 듯 
			int concurrentRequestCount = 8;
			logger.info("concurrent request count {}" ,concurrentRequestCount );
			BlobRequestOptions options = new BlobRequestOptions();
			options.setConcurrentRequestCount(concurrentRequestCount); // 블록 병렬 처리
			
			blob.upload(bis, length, null, options, null);
			
			logger.info("complete upload file {} time {}", fileName, System.currentTimeMillis());

		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		} 
		return true;
	}
	

	/**
	 * video나 audio 등의 큰 용량의 파일을 업르드 할 때 사용 (byte array 방식)
	 * upload large file(video, audio, etc..) byte array as a block blob
	 * @param containerName
	 * @param res
	 * @param fileName
	 * @return
	 */
	public boolean uploadByteArray(String containerName, byte[] res, String fileName) {       
		try {
			logger.info("start upload file {} time {}", fileName, System.currentTimeMillis());
			
			CloudBlobContainer container = getContainer(containerName);
			CloudBlockBlob blob = container.getBlockBlobReference(fileName);
			int length = res.length;
	        blob.uploadFromByteArray(res, 0, length);
	        
	        // TODO: 임시로 8로 설정해 놓음. 나중에 테스트 하면서 바꿔야 할 듯 
			int concurrentRequestCount = 8;
			logger.info("concurrent request count {}" ,concurrentRequestCount);
			BlobRequestOptions options = new BlobRequestOptions();
			options.setConcurrentRequestCount(concurrentRequestCount); // 블록 병렬 처리
			
	        blob.uploadFromByteArray(res, 0, length, null, options, null);
	        
	        logger.info("complete upload file {} time {}", fileName, System.currentTimeMillis());
	        
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * SAS(공유 엑세스 서명) download uri 리턴
	 * expireTime이 만료된 SAS URI 접근 시 AuthenticationFailed 오류 발생
	 * @param containerName
	 * @param blobName
	 * @return
	 * @throws URISyntaxException
	 * @throws StorageException
	 */
	public String getFileDownloadUri(String containerName, String blobName) throws URISyntaxException, StorageException {
		
		CloudBlobContainer container = getContainer(containerName);
		CloudBlob blob = container.getBlobReferenceFromServer(blobName);
		
		if(!blob.exists()){
			logger.error("blob {} not found", blobName);
			return null;
		}

		SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
		
		// expire time 지정 (UTC 시간으로 지정됨)
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

	/**
	 * blob 삭제
	 * @param containerName
	 * @param blobName
	 * @return
	 * @throws URISyntaxException
	 * @throws StorageException
	 */
	public boolean deleteFile(String containerName, String blobName) throws URISyntaxException, StorageException{
		CloudBlobContainer container = getContainer(containerName);
		CloudBlob blob = container.getBlobReferenceFromServer(blobName);

		if(!blob.exists()){
			logger.error("blob {} not found", blobName);
			return false;
		}
		
		// true : delete file success, false: delete file failed.
		return blob.deleteIfExists();
	}
	
}
