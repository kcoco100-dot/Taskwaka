// 요소가 보이지 않으면 true를 반환
function isElementHidden(el) {
  if (!el) return true;

  return (
    el.offsetParent === null ||      // display: none
    getComputedStyle(el).visibility === 'hidden' ||
    getComputedStyle(el).opacity === '0'
  );
}

// 요소 보이기
function showElement(selector, displayType = '') {
	const elements = document.querySelectorAll(selector);
	
	if(! elements) {
		return;
	}
	
	elements.forEach(element => {
		// 인자가 없으면 '', 인자가 있으면 'flex', 'grid' 등이 적용
		element.style.display = displayType;
	});
}

// 요소 숨기기
function hideElement(selector) {
	const elements = document.querySelectorAll(selector);

	if(! elements) {
		return;
	}
		
	elements.forEach(element => {
		element.style.display = 'none';
	});
}

// 요소 보이기/숨기기 토글
function toggleElement(selector, displayType = '') {
	const elements = document.querySelectorAll(selector);

	if (! elements) {
		return;
	}

	elements.forEach(element => {
		const currentDisplay = getComputedStyle(element).display;

		if (currentDisplay === 'none') {
			// 숨겨져 있으면 보이기
			element.style.display = displayType;
		} else {
			// 보이고 있으면 숨기기
			element.style.display = 'none';
		}
	});
}

/**
 * 요소를 서서히 나타나게 하는 함수
 * @param {string} selector - 선택자
 * @param {number} duration - 애니메이션 시간 (ms, 기본값 400)
 * @param {string} displayType - (선택) 강제할 display 속성 (기본값: '' -> CSS 따름)
 * @param {function} callback - 완료 후 실행할 함수
 */
function fadeIn(selector, duration = 400, displayType = '', callback) {
	const elements = document.querySelectorAll(selector);
	if (!elements) {
		return;
	}
	
	elements.forEach(element => {
		// 애니메이션 준비: 투명도 0으로 설정
		element.style.opacity = 0;
	        
		// 공간 차지하도록 설정 (display 복구)
		// 빈 문자열('')을 주면 CSS 파일의 원래 속성(flex, grid 등)을 따름
		element.style.display = displayType || '';

		// CSS 파일에도 display가 없어서 여전히 none이라면 block으로 강제
		if (window.getComputedStyle(element).display === 'none') {
			element.style.display = 'block';
		}

		// 애니메이션 실행 (투명도 0 -> 1)
		const animation = element.animate([
			{ opacity: 0 }, // 시작
			{ opacity: 1 }  // 끝
		], {
			duration: duration,
			easing: 'ease-in', // 서서히 가속
			fill: 'forwards'   // 끝난 상태 유지
		});

		// 애니메이션 종료 후 처리
		animation.onfinish = () => {
			// 애니메이션 객체의 스타일 점유를 해제하고 
			// 요소 자체의 투명도를 1로 고정 (또는 CSS에 맡기기 위해 제거)
			element.style.opacity = 1; 

			if (callback && typeof callback === 'function') {
				callback();
			}
		};
	});
}

/**
 * 요소를 서서히 숨기는 함수
 * @param {string} selector - 선택자
 * @param {number} duration - 애니메이션 시간 (ms, 기본값 400)
 * @param {function} callback - 완료 후 실행할 함수 (선택 사항)
 */
function fadeOut(selector, duration = 400, callback) {
    const elements = document.querySelectorAll(selector);
	if (!elements) {
		return;
	}
	
    elements.forEach(element => {
        //애니메이션 실행 (투명도 1 -> 0)
        // WAAPI: element.animate(keyframes, options)
        const animation = element.animate([
            { opacity: 1 }, // 시작 상태
            { opacity: 0 }  // 종료 상태
        ], {
            duration: duration,
            easing: 'ease-out', // 서서히 감속
            fill: 'forwards'    // 애니메이션 끝난 상태(투명) 유지
        });

        // 애니메이션 종료 후 처리
        animation.onfinish = () => {
            // 화면에서 공간 차지하지 않도록 숨김
            element.style.display = 'none';
            
            // (선택) 다음번 fadeIn을 위해 투명도 스타일은 초기화할 수도 있음
            // element.style.opacity = ''; 

            // 콜백 함수가 있다면 실행
            if (callback && typeof callback === 'function') {
                callback();
            }
        };
    });
}
