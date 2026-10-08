package com.tk.app.common;

import java.io.InputStream;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface StorageService {
	public String getRootRealPath();
	public String getRealPath(String webPath);
	
	public String uploadFileToServer(MultipartFile multiFile, String directoryPath);
	public String uploadFileToServer(InputStream inputStream, String originalFilename, String directoryPath);
	
	public ResponseEntity<?> downloadFile(String directoryPath, String saveFilename, String originalFilename);
	
	public ResponseEntity<StreamingResponseBody> downloadLargeFile(String directoryPath, String saveFilename, String originalFilename, boolean deleteAfterDownload);
	public ResponseEntity<StreamingResponseBody> downloadZipFile(String[] sources, String[] originals, String zipFilename);

	public boolean deleteFile(String pathString);
	public boolean deleteFile(String directoryPath, String filename);
	
	public List<String> listAllFiles(String directoryPath);
}
