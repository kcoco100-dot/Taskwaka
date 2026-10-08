async function fetchRequest(url, method='GET', params=null, contentType='form', responseType='json', headers={'AJAX': true}) {
	const upperMethod = method.toUpperCase();
	const lowerContentType = contentType.toLowerCase();
	
	const sentinelNode = document.querySelector('.sentinel');
	
	const options = {
		method: upperMethod,
		headers: { ...headers }
	};
	
	if(params) {
		if (upperMethod === 'GET') {
			// GET 요청일 경우 URL에 파라미터 추가
			if (typeof params === 'string') {
				url += (url.includes('?') ? '&' : '?') + params;
			} else if (typeof params === 'object') {
				const query  = new URLSearchParams(params).toString();
				url += (url.includes('?') ? '&' : '?') + query;
			}
		} else {
			// POST / PUT / DELETE body 처리
			switch (lowerContentType) {
			case 'json':
				options.headers['Content-Type'] = 'application/json';
				options.body = typeof params === 'string' ? params : JSON.stringify(params);
				break;

			case 'formdata':
				// FormData는 자동으로 Content-Type을 설정하므로 헤더 생략
				options.body = params instanceof FormData ? params : objectToFormData(params);
				break;

			case 'form':
				options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
				options.body = typeof params === 'string' ? params : new URLSearchParams(params).toString();
				break;

			default:
				throw new Error('지원하지 않는 Content-Type: ' + contentType);
			}
		}
	}
	
	try {
		if(sentinelNode) {
			sentinelNode.setAttribute('data-loading', 'true');
		}
		
		fadeTo('#loadingLayout');
						
		const resp = await fetch(url, options);
		if (! resp.ok ) {
			switch (resp.status) {
				case 401: alert('로그인이 필요 합니다.'); break;
				case 403: alert('권한이 필요 합니다.'); break;
				case 406: alert('요청 처리가 실패 했습니다.'); break;
				case 410: alert('삭제된 게시물입니다.'); break;
				case 500: alert('요청 처리가 실패 했습니다.'); break;
				default: throw new Error('HTTP 오류: ' + resp.status);
			}
			
			return;
		}
		
		switch (responseType) {
		case 'json': {
			// return await resp.json();
			// body 없이 200 OK만 반환되는 경우
			const text = await resp.text();
			return text ? JSON.parse(text) : null;
		}
		case 'text': return await resp.text();
		case 'blob': return await resp.blob();
		default: throw new Error('지원하지 않는 responseType: ' + responseType);
		}
	} catch (error) {
	    throw error;
	} finally {
		hideNode('#loadingLayout');
	}
}

// object를 formData로 변환
function objectToFormData(obj) {
    const formData = new FormData();
    for (const key in obj) {
        if (obj.hasOwnProperty(key)) {
            formData.append(key, obj[key]);
        }
    }
    
    return formData;
}

// 요소에 부드럽게 투명도를 적용하여 보이기
function fadeTo(selector, duration = 600, targetOpacity = 0.6, displayType = '') {
	const element = document.querySelector(selector);
	if (!element) {
		return;
	}

	element.style.display = displayType;
	
	let opacity = parseFloat(window.getComputedStyle(element).opacity);
	const interval = 10;
	const step = (opacity - targetOpacity) / (duration / interval);
	
	const fade = setInterval(() => {
		opacity -= step;
		if (opacity <= targetOpacity) {
			opacity = targetOpacity;
			clearInterval(fade);
		}
		element.style.opacity = opacity;
	}, interval);
}

// 요소 보이기
function showNode(selector, displayType = '') {
  const element = document.querySelector(selector);

  if (! element) {
    return;
  }
  element.style.display = displayType;
}

// 요소 숨기기
function hideNode(selector) {
	const element = document.querySelector(selector);
	if (! element) {
		return;
	}
	element.style.display = 'none';
}
