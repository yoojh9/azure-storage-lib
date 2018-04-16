# azure-storage-lib

- azure-storage 중 blob 을 사용하여 upload, delete, get file download url 등의 기능이 포함되어 있는 utility를 구현
- 블록 Blob 은 문서 및 미디어 파일과 같은 텍스트 또는 이진 파일을 저장하기에 적합하다

#### 1. 저장소 계정 생성
- [저장소 계정 만들기 참고](https://docs.microsoft.com/ko-kr/azure/storage/common/storage-quickstart-create-account?tabs=azure-cli) <br/>

#### 2. Azure용 Spring Boot Starter
- [Azure용 Srping Boot Starter 참고](https://docs.microsoft.com/ko-kr/java/azure/spring-framework/spring-boot-starters-for-azure)

#### 3. Blob 저장소에 관한 문서
- [Blob 저장소 소개](https://docs.microsoft.com/ko-kr/azure/storage/blobs/storage-blobs-introduction)


#### 4. azure.storage.connection-string 값
application.properties의 azure.storage.connection-string 값을 구하기 위해 다음과 같이 실행
```
$ az storage account show-connection-string --name {저장소} --resource-group {리소스그룹명}
```

#### 5. SAS
- 공유 액세스 서명을 사용하면 클라이언트에게 제한된 엑세스 권한을 부여할 수 있다.
- 읽기 및 쓰기 권한 부여, 만료 시간, IP 주소 제한, 프로토콜 제한 등의 제어가 가능하다
- SAS URI = Storage Resource URI + SAS Token
- SAS의 만료 시간과 시작 시간은 UTC로 표시해야 한다.
- [SAS(공유 엑세스 서명) 참고](https://docs.microsoft.com/ko-kr/azure/storage/common/storage-dotnet-shared-access-signature-part-1#examples-of-sas-uris)
