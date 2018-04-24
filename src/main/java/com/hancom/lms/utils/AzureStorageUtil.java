package com.hancom.lms.utils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
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
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
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
			String mimeType = MimeTypeUtil.getMimeType(fileName);
			
			logger.info("start upload image {} mimeType {}", fileName, mimeType);
			
			blob.getProperties().setContentType(mimeType);
			blob.upload(is, length);
			
			logger.info("complete upload image {} mimeType {}", fileName, mimeType);

		} catch (Exception e) {
			logger.error("azure uploadImage error : {}", e);
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
			CloudBlobContainer container = getContainer(containerName);
			CloudBlockBlob blob = container.getBlockBlobReference(fileName);
			String mimeType = MimeTypeUtil.getMimeType(fileName);
			
			BufferedInputStream bis = new BufferedInputStream(is);
			
			// TODO: 임시로 8로 설정해 놓음. 나중에 테스트 하면서 바꿔야 할 듯 
			logger.info("start upload file {} time {} mimeType {}", fileName, System.currentTimeMillis(), mimeType);
			int concurrentRequestCount = 8;
			BlobRequestOptions options = new BlobRequestOptions();
			options.setConcurrentRequestCount(concurrentRequestCount); // 블록 병렬 처리
			
			blob.getProperties().setContentType(mimeType);
			blob.upload(bis, length, null, options, null);
			
			logger.info("complete upload file {} time {}", fileName, System.currentTimeMillis());

		} catch (Exception e) {
			logger.error("azure uploadStream error : {}", e);
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
			
			CloudBlobContainer container = getContainer(containerName);
			CloudBlockBlob blob = container.getBlockBlobReference(fileName);
			String mimeType = MimeTypeUtil.getMimeType(fileName);

	        // TODO: 임시로 8로 설정해 놓음. 나중에 테스트 하면서 바꿔야 할 듯 
			logger.info("start upload file {} time {}  mimeType {}", fileName, System.currentTimeMillis(), mimeType);

			int concurrentRequestCount = 8;
			BlobRequestOptions options = new BlobRequestOptions();
			options.setConcurrentRequestCount(concurrentRequestCount); // 블록 병렬 처리
			
			blob.getProperties().setContentType(mimeType);
	        blob.uploadFromByteArray(res, 0, res.length, null, options, null);
	        
	        logger.info("complete upload file {} time {}", fileName, System.currentTimeMillis());
	        
		} catch (Exception e) {
			logger.error("azure uploadByteArray error : {}", e);
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
	public String getFileDownloadUri(String containerName, String blobName){
		String sasUri = null;
		
		try {
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
			
			String sasToken = blob.generateSharedAccessSignature(policy, "readPolicy");
			sasUri = String.format("%s?%s", blob.getUri(), sasToken);
			
		} catch (URISyntaxException | StorageException e) {
			logger.error("invalid azure storage : {}" , e);
		} catch (InvalidKeyException e) {
			logger.error("invalid SAS Token error : {}" , e);
		}
		
		return sasUri;
	}

	/**
	 * 가상 디렉토리 내에 있는 파일 리스트 리턴
	 * @param containerName
	 * @param directoryName
	 * @return
	 * @throws StorageException
	 * @throws URISyntaxException
	 */
	public List<CloudBlob> getAllFiles(String containerName, String directoryName) throws StorageException, URISyntaxException {
		
		List<CloudBlob> fileList = new ArrayList<>();
		
		CloudBlobContainer container = getContainer(containerName);
		CloudBlobDirectory directory = container.getDirectoryReference(directoryName);
		
		for(ListBlobItem blobItem : directory.listBlobs()){
			if(blobItem instanceof CloudBlob) {
				fileList.add((CloudBlob) blobItem);
			}
		}
		
		return fileList;
	}
	
	/**
	 * blob 삭제 (단일 파일 삭제)
	 * @param containerName
	 * @param blobName
	 * @return
	 * @throws URISyntaxException
	 * @throws StorageException
	 */
	public boolean deleteFile(String containerName, String blobName) {
		try {
			CloudBlobContainer container = getContainer(containerName);
			CloudBlob blob = container.getBlobReferenceFromServer(blobName);
			
			return blob.deleteIfExists();
			
		} catch(URISyntaxException | StorageException e) {
			logger.error("file {} delete failed : {}", blobName, e);
			return false;
		}
	}
	
	/**
	 * 디렉토리 내에 있는 파일 모두 삭제
	 * @param containerName
	 * @param directoryName
	 * @return
	 */
	public boolean deleteAllFiles(String containerName, String directoryName) {
		
		try {
			List<CloudBlob> fileList = getAllFiles(containerName, directoryName);
			boolean result = false;

			for(CloudBlob blob : fileList){
				result = blob.deleteIfExists();
				logger.info("blob {} delete {}", blob.getName(), result);
			}
		} catch(URISyntaxException | StorageException e) {
			logger.error("fail to delete directory {} : {}", directoryName, e);
			return false;
		}
		
		return true;
	}
}
