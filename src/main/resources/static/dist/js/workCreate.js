document.addEventListener("DOMContentLoaded", function () {

    const form = document.querySelector(".work-create-form");

    const workSubject = document.getElementById("work-subject");
    const domainKey = document.getElementById("domain-key");
    const workContent = document.getElementById("work-content");

    const workNameError = document.getElementById("workSubjectError");
    const domainError = document.getElementById("domainError");
    const workContentError = document.getElementById("workContentError");
	
	let domainCheckTimer;
	let domainCheckVersion = 0;
	


    // 오류 메시지 표시
    function showError(input, errorElement, message) {
        errorElement.textContent = message;
        input.setAttribute("aria-invalid", "true");
    }

    // 오류 메시지 제거
    function clearError(input, errorElement) {
        errorElement.textContent = "";
        input.removeAttribute("aria-invalid");
    }

    function checkWorkSubject() {
        const value = workSubject.value;

        if (value.trim() === "") {
            showError(workSubject, workNameError, "워크명을 입력해주세요.");
            return false;
        }

        if (workSubject.validity.patternMismatch) {
            showError(workSubject, workNameError, "워크명에는 단독 한글 자음·모음, 앞뒤 공백, 연속 공백을 사용할 수 없습니다.");
            return false;
        }

        clearError(workSubject, workNameError);
        return true;
    }

    function checkDomainKey() {
        const value = domainKey.value;

        if (value === "") {
            showError(domainKey, domainError, "도메인 주소를 입력해주세요.");
            return false;
        }

        if (domainKey.validity.patternMismatch) {
            showError(domainKey, domainError, "도메인은 영어 소문자와 숫자만 사용할 수 있습니다.");
            return false;
        }

        clearError(domainKey, domainError);
        return true;
    }

    function checkWorkContent() {

        if (workContent.value.length > 1000) {
            showError(workContent, workContentError, "소개는 1000자 이하로 입력해주세요.");
            return false;
        }

        clearError(workContent, workContentError);
        return true;
    }

	
    workContent.addEventListener("input", checkWorkContent);

	let isComposing = false;

	workSubject.addEventListener("compositionstart", function () {
	    isComposing = true;
	});

	workSubject.addEventListener("compositionend", function () {
	    isComposing = false;
	    checkWorkSubject();
	});

	workSubject.addEventListener("input", function () {
	    if (! isComposing) {
	        checkWorkSubject();
	    }
	});

	
	domainKey.addEventListener("input", () => {
		
		   clearTimeout(domainCheckTimer);

		   // 이전 요청 결과를 무효화, 버전체크
		   domainCheckVersion++;

		   // 도메인 형식 검사
		   if (! checkDomainKey()) {
		       return;
		   }

		   // 입력을 멈춘 뒤 400ms 후 중복 조회
		   domainCheckTimer = setTimeout(function () {
		       checkDomainDuplicate();
		   }, 400);

	});


	form.addEventListener("submit", async function (e) {

	    e.preventDefault();

	    // 진행 중인 실시간 검사 무효화
	    clearTimeout(domainCheckTimer);
	    domainCheckVersion++;

	    const validSubject = checkWorkSubject();
	    const validDomain = checkDomainKey();
	    const validContent = checkWorkContent();

	    if (! validSubject || ! validDomain || ! validContent) {

	        if (! validSubject) {
	            workSubject.focus();
	        } else if (! validDomain) {
	            domainKey.focus();
	        } else if (! validContent) {
	            workContent.focus();
	        }

	        return;
	    }

	    // 도메인 DB 중복 확인
	    const checkOk = await checkDomainDuplicate();

	    if (! checkOk) {
	        domainKey.focus();
	        return;
	    }

	    form.submit();

	});


    // 서버 오류가 있다면 해당 입력칸으로 focus
    const serverErrorField = document.getElementById("serverErrorField");

    if (serverErrorField) {

        const fields = {
            workSubject: workSubject,
            domainKey: domainKey,
            workContent: workContent
        };

        const input = fields[serverErrorField.value];

        if (input) {
            input.focus();
        }
    }
	


	async function checkDomainDuplicate() {
	    const value = domainKey.value;
		
	    if (! checkDomainKey()) {
	        return false;
	    }

	    const version = ++domainCheckVersion;

	    try {
	        const url = form.dataset.checkDomainUrl
	            + "?domainKey=" + encodeURIComponent(value);

	        const response = await fetch(url);

	        if (! response.ok) {
	            throw new Error("중복 확인 요청 실패");
	        }

	        const data = await response.json();

	        if (version !== domainCheckVersion ||
	                domainKey.value !== value) {
	            return false;
	        }

	        if (data.duplicate) {
	            showError(domainKey, domainError, "이미 사용 중인 도메인 주소입니다.");
	            return false;
	        }

	        clearError(domainKey, domainError);
	        return true;

	    } catch (error) {

	        if (version !== domainCheckVersion || domainKey.value !== value) {
	            return false;
	        }

	        showError(domainKey, domainError, "도메인 중복 확인에 실패했습니다. 다시 시도해주세요.");
			return false;
	    }
	}


});
