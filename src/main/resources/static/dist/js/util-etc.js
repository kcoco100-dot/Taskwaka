// 이미지 파일인지 검사
function isImageFile(filename){
	let format = /(\.gif|\.jpg|\.jpeg|\.png)$/i;
	return format.test(filename);
}

// -------------------------------------------------
// 전체화면
const fullscreen = el => {
    if ( el.requestFullscreen ) return el.requestFullscreen();
    if ( el.webkitRequestFullscreen ) return el.webkitRequestFullscreen();
    if ( el.mozRequestFullScreen ) return el.mozRequestFullScreen();
    if ( el.msRequestFullscreen ) return el.msRequestFullscreen();
};

// 전체화면 취소
const cancelFullScreen = () => {
    if ( document.exitFullscreen ) return document.exitFullscreen();
    if ( document.webkitCancelFullscreen ) return document.webkitCancelFullscreen();
    if ( document.mozCancelFullScreen ) return document.mozCancelFullScreen();
    if ( document.msExitFullscreen ) return document.msExitFullscreen();
};

// 전체 화면 토글
function toggleFullScreen(el) {
    if ( ! document.fullscreenElement ) {
        fullscreen(el);
    } else {
        cancelFullScreen();
    }
}

// -------------------------------------------------
// 이벤트 등록
/* 
    // 사용 예
    let func= function() { alert('예제'); }
    addEvent(window, load, func);
*/
function addEvent(el, evType, fn) {
    if (el.addEventListener) {
        el.addEventListener(evType, fn, false);
        return true;
    } else if (el.attachEvent) {
        let r = el.attachEvent('on' + evType, fn);
        return r;
    } else {
        el['on' + evType] = fn;
    }
}

// -------------------------------------------------
// 팝업 윈도우즈
function winOpen(url, windowName, windowFeatures) {
	if(! theURL) return;
	if(! windowName) windowName = '';
	
	let flag = windowFeatures;
    if(flag === undefined) {
		flag = 'left=10, ';
		flag += 'top=10, ';
		flag += 'width=372, ';
		flag += 'height=466, ';
		flag += 'toolbar=no, ';
		flag += 'menubar=no, ';
		flag += 'status=no, ';
		flag += 'scrollbars=no, ';
		flag += 'resizable=no';
		// flag = 'scrollbars=no,resizable=no,width=220,height=230';
	}
	
    window.open(url, windowName, flag);
}

// -------------------------------------------------
// 기타 형식 검사
// 영문, 숫자 인지 확인
 function isValidEngNum(str) {
    for(let i = 0; i < str.length; i++) {
        achar = str.charCodeAt(i);                 
        if( achar > 255 ) {
            return false;
        }
    }
    return true; 
}

// 전화번호 형식(숫자-숫자-숫자)인지 체크
function isValidPhone(data) {
    // let format = /^(\d+)-(\d+)-(\d+)$/;
    let format = /^(010)-[0-9]{4}-[0-9]{4}$/g;
    return format.test(data);
}
