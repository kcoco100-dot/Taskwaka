package com.tk.app.common;

import org.springframework.stereotype.Service;

@Service
public class PaginateUtil {
	/**
	 * 전체 페이지수를 구하는 메소드
	 * 
	 * @param dataCount 총 데이터 개수
	 * @param size      한 화면에 출력할 데이터 개수
	 * @return 총 페이지 수
	 */
	public int pageCount(int dataCount, int size) {
		try {
			return (dataCount + size - 1) / size;
		} catch (ArithmeticException e) {
			return 0;
		}
	}

	/**
	 * 페이징(paging) 처리를 하는 메소드(GET 방식, a 태그를 이용하여 해당 페이지의 URL로 이동)
	 * 
	 * @param currentPage 화면에 출력할 페이지 번호
	 * @param totalPage   총 페이지 수
	 * @param listUrl     페이지 번호에 link를 설정할 URL
	 * @param blockSize   페이징 블록 크기
	 * @return 페이징 처리 결과
	 */
	public String paging(int currentPage, int totalPage, String listUrl) {
		return paging(currentPage, totalPage, listUrl, 10);
	}

	public String paging(int currentPage, int totalPage, String listUrl, int blockSize) {
		if (currentPage < 1 || totalPage < 1)
			return "";

		if (currentPage > totalPage)
			currentPage = totalPage;

		StringBuilder sb = new StringBuilder();

		String connector = listUrl.contains("?") ? "&" : "?";
		String fullUrl = listUrl + connector;

		// 표시할 페이지 - 1
		int currentBlock = ((currentPage - 1) / blockSize) * blockSize;

		// 반복할 시작과 마지막 페이지 번호
		int startPage = currentBlock + 1;
		int endPage = Math.min(startPage + blockSize - 1, totalPage);

		sb.append("<div class='paginate'>");

		// 처음과 이전 페이지
		if (currentBlock > 0) {
			int prevBlockPage = startPage - 1;
			// int prevBlockPage = currentPage - blockSize;
			sb.append(createLinkUrl(fullUrl, 1, "&#x226A;", "처음"));
			sb.append(createLinkUrl(fullUrl, prevBlockPage, "&#x003C;", "이전"));
		}

		// 페이지 반복
		for (int i = startPage; i <= endPage; i++) {
			if (i == currentPage) {
				sb.append("<span class='active' aria-current='page'>").append(i).append("</span>");
			} else {
				sb.append(createLinkUrl(fullUrl, i, String.valueOf(i), String.valueOf(i)));
			}
		}

		// 다음과 마지막 페이지
		if (endPage < totalPage) {
			int nextBlockPage = endPage + 1;
			// int nextBlockPage = Math.min(currentPage + blockSize, totalPage);
			sb.append(createLinkUrl(fullUrl, nextBlockPage, "&#x003E;", "다음"));
			sb.append(createLinkUrl(fullUrl, totalPage, "&#x226B;", "마지막"));
		}

		sb.append("</div>");

		return sb.toString();
	}

	/**
	 * javascript를 이용하여 페이징 처리를하는 메소드 : javascript의 지정한 함수(methodName)를 호출
	 * 
	 * @param current_page 화면에 출력할 페이지 번호
	 * @param total_page   총 페이지 수
	 * @param methodName   호출할 자바스크립트 함수명
	 * @param blockSize    페이징 블록 크기
	 * @return 페이징 처리 결과
	 */
	public String pagingMethod(int currentPage, int totalPage, String methodName) {
		return pagingMethod(currentPage, totalPage, methodName, 10);
	}

	public String pagingMethod(int currentPage, int totalPage, String methodName, int blockSize) {
		if (currentPage < 1 || totalPage < 1)
			return "";

		if (currentPage > totalPage)
			currentPage = totalPage;

		StringBuilder sb = new StringBuilder();

		int currentBlock = ((currentPage - 1) / blockSize) * blockSize;

		int startPage = currentBlock + 1;
		int endPage = Math.min(startPage + blockSize - 1, totalPage);

		sb.append("<div class='paginate'>");

		if (currentBlock > 0) {
			int prevBlockPage = startPage - 1;
			// int prevBlockPage = currentPage - blockSize;
			sb.append(createLinkClick(methodName, 1, "&#x226A"));
			sb.append(createLinkClick(methodName, prevBlockPage, "&#x003C"));
		}

		for (int i = startPage; i <= endPage; i++) {
			if (i == currentPage) {
				sb.append("<span class='active' aria-current='page'>").append(i).append("</span>");
			} else {
				sb.append(createLinkClick(methodName, i, String.valueOf(i)));
			}
		}

		if (endPage < totalPage) {
			int nextBlockPage = endPage + 1;
			// int nextBlockPage = Math.min(currentPage + blockSize, totalPage);
			sb.append(createLinkClick(methodName, nextBlockPage, "&#x003E;"));
			sb.append(createLinkClick(methodName, totalPage, "&#x226B;"));
		}

		sb.append("</div>");

		return sb.toString();
	}

	// 슬라이딩 윈도우 방식의 페이징 URL : 화면에 표시할 페이지를 중앙에 출력
	public String slidingPaging(int currentPage, int totalPage, String listUrl) {
		return slidingPaging(currentPage, totalPage, listUrl, 10);
	}

	public String slidingPaging(int currentPage, int totalPage, String listUrl, int blockSize) {
		if (currentPage < 1 || totalPage < 1)
			return "";

		if (currentPage > totalPage)
			currentPage = totalPage;

		StringBuilder sb = new StringBuilder();

		String connector = listUrl.contains("?") ? "&" : "?";
		String fullUrl = listUrl + connector;

		// 시작 페이지
		int half = blockSize / 2;
		int startPage = Math.max(1, currentPage - half);

		if (startPage + blockSize > totalPage) {
			startPage = Math.max(1, totalPage - blockSize + 1);
		}

		sb.append("<div class='paginate'>");

		// 처음
		if (startPage > 1) {
			sb.append(createLinkUrl(fullUrl, 1, "&#x226A;"));
		}

		// 이전
		if (currentPage > 1) {
			sb.append(createLinkUrl(fullUrl, currentPage - 1, "&#x003C;"));
		}

		// 페이지 번호 루프
		for (int i = startPage; i < startPage + blockSize && i <= totalPage; i++) {
			if (i == currentPage) {
				sb.append("<span class='active' aria-current='page'>").append(i).append("</span>");
			} else {
				sb.append(createLinkUrl(fullUrl, i, String.valueOf(i)));
			}
		}

		// 다음 페이지
		if (currentPage < totalPage) {
			sb.append(createLinkUrl(fullUrl, currentPage + 1, "&#x003E"));
		}

		// 마지막
		int lastPage = startPage + blockSize - 1;
		if (lastPage < totalPage) {
			sb.append(createLinkUrl(fullUrl, totalPage, "&#x226B;"));
		}

		sb.append("</div>");

		return sb.toString();
	}

	// 슬라이딩 윈도우 방식의 페이징 URL : javascript 함수 호출
	public String slidingPagingMethod(int currentPage, int totalPage, String methodName) {
		return slidingPagingMethod(currentPage, totalPage, methodName, 10);
	}

	public String slidingPagingMethod(int currentPage, int totalPage, String methodName, int blockSize) {
		if (currentPage < 1 || totalPage < 1)
			return "";

		if (currentPage > totalPage)
			currentPage = totalPage;

		StringBuilder sb = new StringBuilder();

		// 시작 페이지
		int half = blockSize / 2;
		int startPage = Math.max(1, currentPage - half);

		if (startPage + blockSize > totalPage) {
			startPage = Math.max(1, totalPage - blockSize + 1);
		}

		sb.append("<div class='paginate'>");

		// 처음
		if (startPage > 1) {
			sb.append(createLinkClick(methodName, 1, "&#x226A;"));
		}

		// 이전
		if (currentPage > 1) {
			sb.append(createLinkClick(methodName, currentPage - 1, "&#x003C;"));
		}

		// 페이지 번호 루프
		for (int i = startPage; i < startPage + blockSize && i <= totalPage; i++) {
			if (i == currentPage) {
				sb.append("<span class='active' aria-current='page'>").append(i).append("</span>");
			} else {
				sb.append(createLinkClick(methodName, i, String.valueOf(i)));
			}
		}

		// 다음 페이지
		if (currentPage < totalPage) {
			sb.append(createLinkClick(methodName, currentPage + 1, "&#x003E"));
		}

		// 마지막
		int lastPage = startPage + blockSize - 1;
		if (lastPage < totalPage) {
			sb.append(createLinkClick(methodName, totalPage, "&#x226B;"));
		}

		sb.append("</div>");

		return sb.toString();
	}

	protected String createLinkUrl(String url, int page, String label) {
		return String.format("<a href='%spage=%d'>%s</a>", url, page, label);
	}

	protected String createLinkUrl(String url, int page, String label, String title) {
		return String.format("<a href='%spage=%d' title='%s'>%s</a>", url, page, title, label);
	}

	protected String createLinkClick(String methodName, int page, String label) {
		return String.format("<a href='javascript:void(0);' onclick='%s(%d);'>%s</a>", methodName, page, label);
	}

	protected String createLinkClick(String methodName, int page, String label, String title) {
		return String.format("<a href='javascript:void(0);' onclick='%s(%d);' title='%s'>%s</a>", methodName, page,
				title, label);
	}
}
