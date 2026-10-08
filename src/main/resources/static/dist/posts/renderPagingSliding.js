/**
 * 전체 페이지 수를 계산하는 함수
 * @param {number|string} dataCount - 전체 데이터 개수
 * @param {number|string} pageSize  - 한 페이지당 보여줄 데이터 개수
 * @returns {number} 계산된 전체 페이지 수
 */
function getPageCount(dataCount, pageSize) {
	const total = Number(dataCount) || 0;
	const size = Number(pageSize) || 0;

	if (total <= 0 || size <= 0) {
		return 0;
	}

	return Math.ceil(total / size);
}

/**
 * 슬라이딩 윈도우 방식의 페이징 URL 생성
 * @param {number} currentPage - 현재 페이지
 * @param {number} totalPage   - 전체 페이지 수
 * @param {string} baseUrl     - 이동할 기본 URL
 * @param {number} blockSize   - 화면에 노출할 페이지 개수
 * @param {string} containerClass   - 페이징 컨테이너 클래스 
 */
function renderPaging(
	currentPage,
	totalPage,
	baseUrl,
	blockSize = 10,
	containerClass = 'paginate'
) {
	let current = Math.max(1, Number(currentPage) || 1);
	let total = Math.max(1, Number(totalPage) || 1);

	if (current > total) current = total;

	let startPage = Math.max(1, current - Math.floor(blockSize / 2));
    
	if (startPage + blockSize > total) {
		startPage = Math.max(1, total - blockSize + 1);
	}

	const getUrl = (page) => {
		const urlObj = new URL(baseUrl, window.location.origin);
		urlObj.searchParams.set('page', page);
		
		return urlObj.pathname + urlObj.search;
	};

	const html = [];	
	html.push(`<div class="${containerClass}">`);

	if (startPage > 1) {
		html.push(`<a href="${getUrl(1)}" title="처음">≪</a>`);
	}

	if (current > 1) {
		html.push(`<a href="${getUrl(current - 1)}" title="이전">&lt;</a>`);
	}

	for (let i = startPage; i < startPage + blockSize && i <= total; i++) {
		if (i === current) {
			html.push(`<span class="active" aria-current="page">${i}</span>`);
		} else {
			html.push(`<a href="${getUrl(i)}">${i}</a>`);
		}
	}

	if (current < total) {
		html.push(`<a href="${getUrl(current + 1)}" title="다음">&gt;</a>`);
	}

	const lastPage = startPage + blockSize - 1;
	if (lastPage < total) {
		html.push(`<a href="${getUrl(total)}" title="마지막">≫</a>`);
	}

	html.push('</div>');
	return html.join('');
}

/**
 * 슬라이딩 윈도우 방식의 자바스크립트 함수(AJAX 등)를 호출
 * @param {number} currentPage - 현재 페이지
 * @param {number} totalPage   - 전체 페이지 수
 * @param {string} methodName  - 실행할 JS 함수명
 * @param {number} blockSize   - 한 번에 보여줄 페이지 개수
 * @param {string} containerClass   - 페이징 컨테이너 클래스 
 */
function renderPagingMethod(
	currentPage,
	totalPage,
	methodName,
	blockSize = 10,
	containerClass = 'paginate'
) {
	let current = Math.max(1, Number(currentPage) || 1);
	let total = Math.max(1, Number(totalPage) || 1);

	if (current > total) current = total;

	let startPage = Math.max(1, current - Math.floor(blockSize / 2));
    
	if (startPage + blockSize > total) {
		startPage = Math.max(1, total - blockSize + 1);
	}

	const html = [];	
	html.push(`<div class="${containerClass}">`);

	if (startPage > 1) {
		html.push(`<a href="javascript:void(0);" onclick="${methodName}(1)" title="처음">≪</a>`);
	}

	if (current > 1) {
		html.push(`<a href="javascript:void(0);" onclick="${methodName}(${current - 1})" title="이전">&lt;</a>`);
	}

	for (let i = startPage; i < startPage + blockSize && i <= total; i++) {
		if (i === current) {
			html.push(`<span class="active" aria-current="page">${i}</span>`);
		} else {
			html.push(`<a href="javascript:void(0);" onclick="${methodName}(${i})">${i}</a>`);
		}
	}

	if (current < total) {
		html.push(`<a href="javascript:void(0);" onclick="${methodName}(${current + 1})" title="다음">&gt;</a>`);
	}

	const lastPage = startPage + blockSize - 1;
	if (lastPage < total) {
		html.push(`<a href="javascript:void(0);" onclick="${methodName}(${total})" title="마지막">≫</a>`);
	}

	html.push('</div>');
	return html.join('');
}
