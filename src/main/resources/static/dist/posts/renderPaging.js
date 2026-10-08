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
 * URL 이동 방식
 * @param {number|string} currentPage - 현재 페이지
 * @param {number|string} totalPage   - 전체 페이지 수
 * @param {string} baseUrl  - 이동할 기본 URL
 * @param {number} blockSize   - 한 블록당 표시할 페이지 수
 * @param {string} containerClass   - 페이징 컨테이너 클래스
 */
function renderPaging(
	currentPage,
	totalPage,
	baseUrl,
	blockSize = 10,
	containerClass = 'paginate'
) {
	currentPage = Math.max(1, Number(currentPage) || 1);
	totalPage = Math.max(1, Number(totalPage) || 1);
	
	if (currentPage > totalPage) currentPage = totalPage;

	const getUrl = (page) => {
		const urlObj = new URL(baseUrl, window.location.origin);
		urlObj.searchParams.set('page', page);
		
		return urlObj.pathname + urlObj.search;
	};

	const currentBlock = Math.floor((currentPage - 1) / blockSize);
	const startPage = currentBlock * blockSize + 1;
	const endPage = Math.min(startPage + blockSize - 1, totalPage);
    
	const html = [];	
	html.push(`<div class="${containerClass}">`);

	if (currentBlock > 0) {
		const prevBlockPage = startPage - 1;
		html.push(`<a href="${getUrl(1)}" title="처음">≪</a>`);
		html.push(`<a href="${getUrl(prevBlockPage)}" title="이전">&lt;</a>`);
	}

	for (let i = startPage; i <= endPage; i++) {
		if (i === currentPage) {
			html.push(`<span class="active" aria-current="page">${i}</span>`);
		} else {
			html.push(`<a href="${getUrl(i)}">${i}</a>`);
		}
	}

	if (endPage < totalPage) {
		const nextBlockPage = endPage + 1;
		
		html.push(`<a href="${getUrl(nextBlockPage)}" title="다음">&gt;</a>`);
		html.push(`<a href="${getUrl(totalPage)}" title="마지막">≫</a>`);
	}

	html.push(`</div>`);
	
	return html.join('');
}


/**
 * 자바스크립트 함수(AJAX 등)를 호출하는 방식
 * @param {number|string} currentPage - 현재 페이지
 * @param {number|string} totalPage   - 전체 페이지 수
 * @param {string} methodName  - 클릭 시 호출할 함수 이름
 * @param {number} blockSize   - 한 블록당 표시할 페이지 수
 * @param {string} containerClass   - 페이징 컨테이너 클래스 
 */
function renderPagingMethod(
	currentPage,
	totalPage,
	methodName,
	blockSize = 10,
	containerClass = 'paginate'
) {
	const current = Math.max(1, Number(currentPage) || 1);
	const total = Math.max(1, Number(totalPage) || 1);
    
	if (total < 1 || current > total) return '';

	const currentBlock = Math.floor((current - 1) / blockSize);
	const startPage = currentBlock * blockSize + 1;
	const endPage = Math.min(startPage + blockSize - 1, total);

	const html = [];
	html.push(`<div class="${containerClass}">`);

    if (currentBlock > 0) {
		const prevBlockPage = startPage - 1;
		html.push(`<a href="javascript:void(0);" onclick="${methodName}(1)" title="처음">≪</a>`);
		html.push(`<a href="javascript:void(0);" onclick="${methodName}(${prevBlockPage})" title="이전">&lt;</a>`);
	}

	for (let i = startPage; i <= endPage; i++) {
		if (i === current) {
			html.push(`<span class="active" aria-current="page">${i}</span>`);
		} else {
			html.push(`<a href="javascript:void(0);" onclick="${methodName}(${i})">${i}</a>`);
		}
	}

	if (endPage < total) {
		const nextBlockPage = endPage + 1;
		html.push(`<a href="javascript:void(0);" onclick="${methodName}(${nextBlockPage})" title="다음">&gt;</a>`);
		html.push(`<a href="javascript:void(0);" onclick="${methodName}(${total})" title="마지막">≫</a>`);
	}

	html.push('</div>');

	return html.join('');
}
