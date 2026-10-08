// 엔터를 누르면 다음 요소로 이동
document.addEventListener('DOMContentLoaded', () => {
    // input 요소 중 type="button"이 아닌 요소 선택
    const inputs = document.querySelectorAll('input:not([type="button"])');

    inputs.forEach(input => {
        input.addEventListener('keydown', (evt) => {
            // 키 코드 확인 (Enter는 key: 'Enter' 또는 keyCode: 13)
            const key = evt.key || evt.keyCode;

            if (key === 'Enter' || key === 13) {
                // 부모 요소(form 또는 body) 찾기
                const parent = evt.target.closest('form, body'); 

                if (parent) {
                    // 포커스 가능한 필드들 찾기
                    const fieldsNodeList = parent.querySelectorAll('button, input, textarea, select');
                    
                    // NodeList를 배열로 변환(indexOf 사용을 위해)
                    const fields = Array.from(fieldsNodeList);
                    
                    // 현재 요소의 인덱스 찾기
                    const index = fields.indexOf(evt.target);

                    // 다음 요소가 존재하면 포커스 이동
                    if (index > -1 && (index + 1) < fields.length) {
                        fields[index + 1].focus();
                    }
                }

                // 이벤트 취소
                evt.preventDefault();
            }
        });
    });
});

// 전체 파일 크기 반환
function getTotalFileSize(fileInput) {
  if (!fileInput || fileInput.type !== 'file' || !fileInput.files) {
    return 0;
  }

  let totalSize = 0;

  for(let f of fileInput.files) {
    totalSize += f.size; // byte
  }

  return totalSize;
}

// 이메일 형식 검사
function isValidEmail(data){
    let format = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/;
    return format.test(data); // true : 올바른 포맷 형식
}

// SQL 문 존재 여부 검사
function isValidSQL(data){
    let format = /(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|EXEC|UNION|FETCH|DECLARE|TRUNCATE)/gi;
    return format.test(data);
}

// 자바스크립트 xss 방지 HTML 특수문자 변환
// Cross-site Scripting(XSS) : SQL injection과 함께 웹 상에서 가장 기초적인 취약점 공격 방법의 일종
function symbolHtml(content) {
    if (! content) return content;

    content = content.replace(/</g, '&lt;');
    content = content.replace(/>/g, '&gt;');
    content = content.replace(/\"/g, '&quot;');
    content = content.replace(/\'/g, '&#39;');
    content = content.replace(/\(/g, '&#40;');
    content = content.replace(/\)/g, '&#41;');

    return content;
}

// 기호를 특수문자로
function restoreHtml(content) {
    if (! content) return content;
    
    content = content.replace(/\&lt;/gi, '<');
    content = content.replace(/\&gt;/gi, '>');
    content = content.replace(/\&#40;/gi, '(');
    content = content.replace(/\&#41;/gi, ')');
    content = content.replace(/\&#39;/gi, "'");
    content = content.replace(/\&quot;/gi, "\"");
    
    return content;
}

// 문자열에 특수문자(",  ',  <,  >, (, ) ) 검사
function isValidSpecialChar(data) {
    let format = /[\",\',<,>,\(,\)]/g;
    return format.test(data);
}

