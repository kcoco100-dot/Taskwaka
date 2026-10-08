package com.tk.app.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class MyUtil {
	/**
	 * 문자열이 null 이거나 길이가 0 또는 공백 문자(스페이스, 탭, 개행 등)만 있는지 검사
	 * @param str
	 * @return
	 */
	public boolean isEmpty(String str) {
	    return str == null || str.isBlank();
	}
	
	/**
	 * 문자열을 주소형식으로 인코딩
	 * @param str 인코딩할 문자열
	 * @return 주소형식으로 인코딩된 문자열
	 */
	public String encodeUrl(String str) {
		 if (str == null) {
			 return null;
		 }
		 
		 try {
			str = URLEncoder.encode(str, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
		}
		
		return str;
	}

	/**
	 * 주소 형식의 문자열을 디코딩
	 * @param str 디코딩할 인코딩된 문자열
	 * @return 디코딩된 문자열
	 */
	public String decodeUrl(String str) {
		Pattern pattern = Pattern.compile(".*%[0-9a-fA-F]{2}.*");
		
		 if (str == null) {
			 return null;
		 }
		 
		 try {
			  if (! str.contains("%")) { // % 가 없으면 디코딩하지 않아도 됨
				  return str;
			  }
			  
			   // 인코딩 패턴이 유효한 경우만 디코딩 시도
			  if (pattern.matcher(str).matches()) {
				  return URLDecoder.decode(str, StandardCharsets.UTF_8.name());
			  }
		} catch (IllegalArgumentException e) {
			// 잘못된 % 인코딩 형식(%가 있지만 2자리 16진수가 아닌 경우)이 존재하는 경우
		} catch (UnsupportedEncodingException e) {
		}
		
		return str;
	}
	
	/**
	* 문자열에서 HTML 태그를 제거하는 메소드
	* 
	* @param str		HTML 태그를 제거할 문자열
	* @return			HTML 태그가 제거된 문자열
	*/
	public String removeHtmlTag(String str) {
		if(str == null || str.length() == 0) {
			return "";
		}

		String regex = "<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>";
		String result = str.replaceAll(regex, "");
		
		return result;
    }

	/**
	* HTML 문서에서 img 태그의 src 속성값을 추출하는 메소드
	* 
	* @param html		html 문자열
	* @return			추출된 src 속성값을 가지고 있는 List 객체 
	*/
	public List<String> getImgSrc(String html) {
		List<String> result = new ArrayList<String>();
		
		if(html == null || html.length() == 0) {
			return result;
		}

		String regex = "<img[^>]*src=[\"']?([^>\"']+)[\"']?[^>]*>";
		Pattern nonValidPattern = Pattern.compile(regex);

		Matcher matcher = nonValidPattern.matcher(html);
		while (matcher.find()) {
			result.add(matcher.group(1));
		}
		
		return result;
    }

	/**
	 * XSS 방지을 위한 <script>, onerror, onclick, javascript: URL 등의 태그를 제거하고,
	 *   <a href>는 안전한 스킴만 허용하는 새니타이즈 메소드
	 * @param html	정화할 HTML 
	 * @return
	 */
	public String sanitize(String html) {
		if (html == null) return null;

        // script 태그 제거
        html = html.replaceAll("(?i)<script.*?>.*?</script>", "");

        // iframe, object, embed 제거
        html = html.replaceAll("(?i)<(iframe|object|embed).*?>.*?</\\1>", "");

        // on* 이벤트 제거(onclick, onerror 등)
        html = html.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*(['\"]).*?\\1", "");
        
        // javascript:, data:, vbscript: 제거
        html = html.replaceAll("(?i)javascript\\s*:", "");
        html = html.replaceAll("(?i)data\\s*:", "");
        html = html.replaceAll("(?i)vbscript\\s*:", "");
        
        /* <a href> 개별 검사 */
        html = sanitizeAnchorHref(html);
        
        return html;
	}
	
	/* <a href> 화이트 리스트 처리 */
	protected String sanitizeAnchorHref(String html) {
    	final Pattern A_TAG_PATTERN = Pattern.compile("(?i)<a\\s+[^>]*href\\s*=\\s*(['\"])(.*?)\\1[^>]*>");
    	
    	Matcher matcher = A_TAG_PATTERN.matcher(html);
    	StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            // String quote = matcher.group(1);
            String href = matcher.group(2).trim().toLowerCase();

            if (isSafeHref(href)) {
                matcher.appendReplacement(sb, matcher.group());
            } else {
                // 위험한 href 제거
                String safeATag = matcher.group().replaceAll("(?i)href\\s*=\\s*(['\"]).*?\\1", "");
                matcher.appendReplacement(sb, safeATag);
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
	}

	/* 허용 href 정책 */
	protected boolean isSafeHref(String href) {
		return href.startsWith("http://")
			|| href.startsWith("https://")
			|| href.startsWith("/")
			|| href.startsWith("#");
	}
	
	/**
	* 특수 문자를 HTML 문자로 변경 및 엔터를 <br>로 변경 하는 메소드
	* 
	* @param str		특수 문자를 HTML 문자로 변경할 문자열
	* @return			특수 문자가 HTML 문자로 변경된 문자열
	*/
	public String htmlSymbols(String str) {
		if(str == null || str.isBlank()) {
			return "";
		}

		str = str.replaceAll("&", "&amp;");
		str = str.replaceAll("\"", "&quot;");
		str = str.replaceAll(">", "&gt;");
		str = str.replaceAll("<", "&lt;");
    	 
		str = str.replaceAll(" ", "&nbsp;"); // \\s를 사용할 경우 \n 아래에서 사용해야 한다.
		str = str.replaceAll("\n", "<br>");
    	 
		return str;
	}
     
	/**
	* 중간 이름 마스킹 처리
	* @param name		이름
	* @return			마스킹 처리된 이름
	*/
	public String nameMasking(String name) {
		int length;

		try {
			name = name.replaceAll("\\s", "");
			length = name.length();

			if (length < 2) {
				return name;
			}

			if (length == 2) {
				return name.charAt(0) + "*";
			}

			StringBuilder masked = new StringBuilder();
			masked.append(name.charAt(0));
			for (int i = 1; i < length - 1; i++) {
				masked.append("*");
			}
			masked.append(name.charAt(length - 1));

			return masked.toString();
		} catch (Exception e) {
		}
		
		return null;
	}

	/**
	* 정규식을 이용하여 E-Mail을 검사하는 메소드
	* 
	* @param email		검사할 E-Mail
	* @return			올바른 E-Mail 이지의 여부
	*/
	public boolean isValidEmail(String email) {
		if (email == null) return false;
		
		boolean b = Pattern.matches("[\\w\\~\\-\\.]+@[\\w\\~\\-]+(\\.[\\w\\~\\-]+)+", 
						email.trim());
		return b;
	}
}
