package com.tk.app.common;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.tk.app.exception.StorageException;
import com.tk.app.exception.StorageFileNotFoundException;

import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class StorageServiceImpl implements StorageService {
	private final ServletContext servletContext;
	private final FileManager fileManager;
	
	/**
	* 웹 루트 경로의 실제 경로를 반환하는 메소드
	* @return 실제 웹 애플리케이션의 루트 경로
	*/
	public String getRootRealPath() {
		return servletContext.getRealPath("/");
	}

	/**
	* 웹 경로의 실제 경로를 반환하는 메소드
	* @return 실제 웹 애플리케이션의 경로
	*/
	public String getRealPath(String webPath) {
		if(webPath.indexOf("/") != 0) {
			webPath = "/" + webPath;
		}
		
		String realPath = servletContext.getRealPath(webPath);
		
        return realPath;
	}
	
	/**
	 * 파일 업로드
	 */
	@Override
	public String uploadFileToServer(MultipartFile multiFile, String directoryPath) {
		try {
			if(multiFile == null || multiFile.isEmpty()) {
				return null;
			}
			
			String originalFilename = multiFile.getOriginalFilename();
			if (originalFilename == null || originalFilename.isBlank()) {
				return null;
			}
			
			if(! fileManager.isDirectoryExist(directoryPath)) {
				fileManager.createAllDirectories(directoryPath);
			}
			
			// 확장자
			String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
			// 서버에 저장할 새로운 파일명
			String saveFilename = fileManager.generateUniqueFileName(directoryPath, extension);
			
			Path location = Paths.get(directoryPath);
			
			/*
			  resolve() : 주어진 경로가 상대 경로인 경우, 이를 현재 경로를 기준으로 절대 경로로 변경
			  normalize() : 경로에서 .(현재 디렉토리) 또는 .. (상위 디렉토리)와 같은 특수한 요소들을 정리하여 경로를 정규화하는 역할
			  toAbsolutePath() : 경로를 절대 경로로 변환하는 데 사용
			*/
			Path destinationFile = location.resolve(Paths.get(saveFilename))
					.normalize()
					.toAbsolutePath();
			
			// 파일 업로드 후 바로 저장할 경우 매우 직관적이고 간단한 방법
			// multiFile.transferTo(destinationFile.toFile());
			
			// 파일 경로를 Path로 처리하거나 복사할 때 추가적인 옵션(예: 덮어쓰기, 스트림 처리)이 필요할 때 사용
			// 파일을 저장할 때 더 많은 제어가 필요하거나 복잡한 처리가 필요한 경우에 적합
			Files.copy(multiFile.getInputStream(), destinationFile);
			
	        return saveFilename;

		} catch (Exception e) {
			throw new StorageException("Failed to store file.", e);
		}
	}

	/**
	 * 파일 업로드
	 */
	@Override
	public String uploadFileToServer(InputStream inputStream, String originalFilename, String directoryPath) {
		try {
			if (originalFilename == null || originalFilename.isBlank()) {
				return null;
			}
			
			if(! fileManager.isDirectoryExist(directoryPath)) {
				fileManager.createAllDirectories(directoryPath);
			}
			
			// 확장자
			String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
			// 서버에 저장할 새로운 파일명
			String saveFilename = fileManager.generateUniqueFileName(directoryPath, extension);
			
			Path location = Paths.get(directoryPath);
			Path destinationFile = location.resolve(saveFilename);
			
			// 파일 복사
	        Files.copy(inputStream, destinationFile);
			
	        return saveFilename;
	        
		} catch (Exception e) {
			throw new StorageException("Failed to store file.", e);
		}
	}
	
	/**
	 * 파일 다운로드
	 * @param directoryPath
	 * @param saveFilename
	 * @param originalFilename
	 * @return
	 */	
	@Override
	public ResponseEntity<?> downloadFile(String directoryPath, String saveFilename, String originalFilename) {
		String pathname = directoryPath + File.separator + saveFilename;
		
		if( ! fileManager.isFileExist(pathname) ) {
			throw new StorageFileNotFoundException("Could not read file : " + saveFilename);
		}
		
		try {
			Path path = Paths.get(pathname);
			
			HttpHeaders headers = downloadHeaders(originalFilename);
			
			return downloadResource(path, headers);
			
		} catch (Exception e) {
			throw new StorageException("Unable to download : " + saveFilename, e);
		}
	}

	/**
	 * 대용량 파일 다운로드
	 * 주의 : StreamingResponseBody는 리턴 타입을 ResponseEntity<?> 처럼 반환하면 런타임 에러가 발생하므로
	 *   컨트롤러도 ResponseEntity<StreamingResponseBody> 로 반환
	 * @param directoryPath
	 * @param saveFilename
	 * @param originalFilename
	 * @param deleteAfterDownload
	 * @return
	 */	
	@Override
	public ResponseEntity<StreamingResponseBody> downloadLargeFile(String directoryPath, String saveFilename,
			String originalFilename, boolean deleteAfterDownload) {
		String pathname = directoryPath + File.separator + saveFilename;
		
		if( ! fileManager.isFileExist(pathname) ) {
			throw new StorageFileNotFoundException("Could not read file : " + saveFilename);
		}
		
		try {
			Path path = Paths.get(pathname);
			
			HttpHeaders headers = downloadHeaders(originalFilename);
			
			return downloadStreaming(path, headers, deleteAfterDownload);
			
		} catch (Exception e) {
			throw new StorageException("Unable to download : " + saveFilename, e);
		}
	}
	
	/**
	 * 파일을 zip 파일로 압축하여 다운로드
	 * 주의 : StreamingResponseBody는 리턴 타입을 ResponseEntity<?> 처럼 반환하면 런타임 에러가 발생하므로
	 *   컨트롤러도 ResponseEntity<StreamingResponseBody> 로 반환
	 * @param directoryPath
	 * @param saveFilename
	 * @param originalFilename
	 * @param deleteAfterDownload
	 * @return
	 */		
	@Override
	public ResponseEntity<StreamingResponseBody> downloadZipFile(String[] sources, String[] originals, String zipFilename) {
		String directoryPath = System.getProperty("user.dir") + File.separator + "temp";
		String archiveFilename = null;
		
		try {
			archiveFilename = fileManager.fileCompression(directoryPath, sources, originals);
			String pathname = directoryPath + File.separator + archiveFilename;
			
			Path path = Paths.get(pathname);
			
			HttpHeaders headers = downloadHeaders(zipFilename);
			
			return downloadStreaming(path, headers, true);
			
		} catch (Exception e) {
			throw new StorageException("Zip file download not possible.", e);
		}
	}
	
	private HttpHeaders downloadHeaders(String originalFilename) {
		String encodedFileName = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
		
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
		
		return headers;
	}
	
	// ByteArrayResource
	//  : 파일을 메모리에 로딩후 다운로드
	//  : 파일이 작을 때 빠름
	//  : 파일이 삭제되어도 이미 메모리에 있으므로 다운로드 가능
	protected ResponseEntity<?> downloadByteArrayResource(Path path, HttpHeaders headers, boolean deleteAfterDownload) {
		try {
			byte[] data = Files.readAllBytes(path);
			ByteArrayResource resource = new ByteArrayResource(data);
			
			if(deleteAfterDownload) {
				Files.deleteIfExists(path);
			}

			return ResponseEntity.ok()
					.headers(headers)
					.header("Access-Control-Expose-Headers", "Content-Disposition") // CORS에서 접근 가능하도록 헤더 노출
					.contentLength(resource.contentLength()) // contentLength를 설정하면 브라우저가 다운로드 진행률 표시
					.body(resource);			 
			 
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	// UrlResource
	//  : 실제 파일 데이터를 메모리에 읽어두지 않는다.
	//  : 메모리 사용량이 매우 적음.
	//  : 동시 다운로드가 많은 경우 유리
	protected ResponseEntity<?> downloadResource(Path path, HttpHeaders headers) {
		try {
			// path.toUri() : 파일 경로를 URL 형식으로 변환(file:///C:/files/test.txt 형식)
			// UrlResource : Spring이 읽을 수 있는 리소스 객체
			Resource resource = new UrlResource(path.toUri());
			
			return ResponseEntity.ok()
					.headers(headers)
					.header("Access-Control-Expose-Headers", "Content-Disposition") // CORS에서 접근 가능하도록 헤더 노출
					.contentLength(resource.contentLength())
					.body(resource);	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	// 대용량 다운로드
	// StreamingResponseBody
	//  : Spring MVC에서 HTTP 응답을 스트리밍 방식으로 전송하기 위한 인터페이스
	//  : 파일이 존재하지 않아도 200 OK가 전송될 수 있으므로 파일 존재 여부 확인 필요
	//  : contentLength 를 설정하지 않으면 Transfer-Encoding: chunked(여러조각으로 나누어) 로 전송
	protected ResponseEntity<StreamingResponseBody> downloadStreaming(Path path, HttpHeaders headers, boolean deleteAfterDownload) {
		try {
			long fileSize = Files.size(path);
			
			StreamingResponseBody stream = outputStream -> {
				try (InputStream in = Files.newInputStream(path)) {
					// transferTo() : 입력 스트림의 모든 데이터를 출력 스트림으로 복사. JDK 9이상
					in.transferTo(outputStream);
	            } finally {

	                if (deleteAfterDownload) {
	                    Files.deleteIfExists(path);
	                }
	            }
	        };

	        return ResponseEntity.ok()
	        		.headers(headers)
	        		.header("Access-Control-Expose-Headers", "Content-Disposition")
	        		.contentLength(fileSize)
	        		.body(stream);			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * 파일 삭제
	 * @param pathString
	 * @return
	 */
	public boolean deleteFile(String pathString) {
		return fileManager.deletePath(pathString);
	}
	
	/**
	 * 파일 삭제
	 * @param uploadPath
	 * @param filename
	 * @return
	 */
	public boolean deleteFile(String directoryPath, String filename) {
		String pathString = directoryPath + File.separator + filename;
		return fileManager.deletePath(pathString);		
	}

	/**
	 * 디렉토리의 모든 파일 목록 반환. 하위 디렉토리는 제외
	 * @param directoryPath
	 * @return 
	 */
	@Override
	public List<String> listAllFiles(String directoryPath) {
		try {
			return fileManager.listAllFiles(directoryPath)
					.map(path -> path.getFileName().toString())
	            	.collect(Collectors.toList()
	            );
		} catch (Exception e) {
			throw new StorageException("Failed to read stored files", e);
		}
	}
	
}
