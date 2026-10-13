document.addEventListener("DOMContentLoaded", function () {

    const form = document.querySelector(".work-create-form");

    const workSubject = document.getElementById("work-subject");
    const domainKey = document.getElementById("domain-key");
    const workContent = document.getElementById("work-content");

    const workNameError = document.getElementById("workSubjectError");
    const domainError = document.getElementById("domainError");
    const workContentError = document.getElementById("workContentError");
    const statusError = document.getElementById("statusError");

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
            showError(workSubject, workNameError, "워크명은 앞뒤 공백을 사용할 수 없으며, 단어 사이에는 공백 한 칸만 허용됩니다.");
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

    // 입력할 때 바로 검사
    workSubject.addEventListener("input", checkWorkSubject);
    domainKey.addEventListener("input", checkDomainKey);
    workContent.addEventListener("input", checkWorkContent);

    // 워크 만들기 버튼 클릭 시 검사
    form.addEventListener("submit", function (e) {

        const validSubject = checkWorkSubject();
        const validDomain = checkDomainKey();
        const validContent = checkWorkContent();

        const selectedStatus = form.querySelector(
            'input[name="status"]:checked'
        );

        if (!selectedStatus) {
            statusError.textContent = "워크 공개 여부를 선택해주세요.";
        } else {
            statusError.textContent = "";
        }

        if (!validSubject || !validDomain || !validContent || !selectedStatus) {
            e.preventDefault();

            if (!validSubject) {
                workSubject.focus();
            } else if (!validDomain) {
                domainKey.focus();
            } else if (!validContent) {
                workContent.focus();
            } else {
                form.querySelector(
                    'input[name="status"]'
                ).focus();
            }
        }
    });

    // 서버 오류가 있다면 해당 입력칸으로 focus
    const serverErrorField = document.getElementById("serverErrorField");

    if (serverErrorField) {

        const fields = {
            workSubject: workSubject,
            domainKey: domainKey,
            workContent: workContent,
            status: form.querySelector(
                'input[name="status"]:checked'
            )
        };

        const input = fields[serverErrorField.value];

        if (input) {
            input.focus();
        }
    }

});
