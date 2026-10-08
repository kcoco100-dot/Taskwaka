function renderReplies(listReply, pageNo) {
  const replyEL = document.querySelector('div.reply-session');
  const cp = replyEL ? (replyEL.getAttribute('data-contextPath') || '').replace(/\/$/, '') : '';
  
  const canLiked = replySessionEL.getAttribute('data-canLiked') || '1';
  const canAnswer = replySessionEL.getAttribute('data-canAnswer') || '1';
  
  return listReply.map(vo => {
    const showReplyText = vo.showReply == 1 ? "숨김" : "표시";
    const likeColor = vo.userReplyLiked == 1 ? 'color:red;' : '';
    const dislikeColor = vo.userReplyLiked == 0 ? 'color:red;' : '';
    const contentClass = vo.showReply == 0 ? 'text-primary text-opacity-50' : '';

    let menuHTML = '';
    if (vo.hasOwner) {
      menuHTML = `
        <div class="deleteReply reply-menu-item" data-replyNum="${vo.replyNum}" data-pageNo="${pageNo}">삭제</div>
        <div class="hideReply reply-menu-item" data-replyNum="${vo.replyNum}" data-showReply="${vo.showReply}">${showReplyText}</div>
      `;
    } else if (vo.hasAdmin) {
      menuHTML = `
        <div class="deleteReply reply-menu-item" data-replyNum="${vo.replyNum}" data-pageNo="${pageNo}">삭제</div>
        <div class="blockReply reply-menu-item" data-replyNum="${vo.replyNum}" data-pageNo="${pageNo}">차단</div>
      `;
    } else {
      menuHTML = `
        <div class="notifyReply reply-menu-item" data-replyNum="${vo.replyNum}">신고</div>
        <div class="blockReply reply-menu-item" data-replyNum="${vo.replyNum}" data-pageNo="${pageNo}">차단</div>
      `;
    }
	
	let answerButtonHTML = '&nbsp;';
	if(canAnswer === '1') {
      answerButtonHTML = `
	    <button type="button" class="btn-default btnReplyAnswerLayout" data-replyNum="${vo.replyNum}">
	      답글 <span id="answerCount${vo.replyNum}">${vo.answerCount}</span>
	    </button>
      `;
	}
	
	let likedHTML = '&nbsp;';
	if(canLiked === '1') {
	  likedHTML = `
	    <button type="button" class="btn-default btnSendReplyLike me-1" data-replyNum="${vo.replyNum}" data-replyLike="1" title="좋아요">
	      <i class="bi bi-hand-thumbs-up" style="${likeColor}"></i> <span>${vo.likeCount}</span>
	    </button>
	    <button type="button" class="btn-default btnSendReplyLike" data-replyNum="${vo.replyNum}" data-replyLike="0" title="싫어요">
	      <i class="bi bi-hand-thumbs-down" style="${dislikeColor}"></i> <span>${vo.disLikeCount}</span>
	    </button>	  
	  `;
	}	

	  return `
	    <div class="d-flex flex-column mb-2">
	      <div class="border bg-light d-flex justify-content-between align-items-center p-2 reply-item-header">
	        <div class="d-flex align-items-center reply-writer">
	          <img src="${cp}/dist/images/person.png" class="avatar-icon">
	          <div class="ms-2">
	            <div class="name">${vo.name}</div>
	            <div class="date">${vo.reg_date}</div>
	          </div>
	        </div>
	        <div class="reply-dropdown">
	          <span class="dropdown-button"><i class="bi bi-three-dots-vertical"></i></span>
	          <div class="reply-menu d-none">
	            ${menuHTML}
	          </div>
	        </div>
	      </div>
	      <div class="p-2 ${contentClass} reply-item-content">${vo.content}</div>
	      <div class="d-flex justify-content-between align-items-center px-2 reply-item-footer">
	        <div>
	          ${answerButtonHTML}
	        </div>		  
	        <div class="d-flex align-items-center reply-like-item" data-userLiked="${vo.userReplyLiked}">
	          ${likedHTML}
	        </div>
	      </div>
	      <div class="reply-answer d-none p-2">
	        <div class="border rounded p-2">
	          <div id="listReplyAnswer${vo.replyNum}" class="answer-list"></div>
	          <div class="mt-2">
	            <textarea class="form-control"></textarea>
	          </div>
	          <div class="text-end mt-1">
	            <button type="button" class="btn-default btn-md btnSendReplyAnswer" data-replyNum="${vo.replyNum}">답글 등록</button>
	          </div>
	        </div>
	      </div>
	    </div>
	  `;
	}).join('');
}

function renderReplyAnswers(listReplyAnswer) {
  const replyEL = document.querySelector('div.reply-session');
  const cp = replyEL ? (replyEL.getAttribute('data-contextPath') || '').replace(/\/$/, '') : '';

  return listReplyAnswer.map(vo => {
    const showReplyText = vo.showReply == 1 ? "숨김" : "표시";
    const contentClass = vo.showReply == 0 ? 'text-primary text-opacity-50' : '';

    let menuHTML = '';
    if (vo.hasOwner) {
      menuHTML = `
        <div class="deleteReplyAnswer reply-menu-item" data-replyNum="${vo.replyNum}" data-parentNum="${vo.parentNum}">삭제</div>
        <div class="hideReplyAnswer reply-menu-item" data-replyNum="${vo.replyNum}" data-showReply="${vo.showReply}">${showReplyText}</div>
      `;
    } else if (vo.hasAdmin) {
      menuHTML = `
        <div class="deleteReplyAnswer reply-menu-item" data-replyNum="${vo.replyNum}" data-parentNum="${vo.parentNum}">삭제</div>
        <div class="blockReplyAnswer reply-menu-item" data-replyNum="${vo.replyNum}" data-parentNum="${vo.parentNum}">차단</div>
      `;
    } else {
      menuHTML = `
        <div class="notifyReplyAnswer reply-menu-item" data-replyNum="${vo.replyNum}">신고</div>
        <div class="blockReplyAnswer reply-menu-item" data-replyNum="${vo.replyNum}" data-parentNum="${vo.parentNum}">차단</div>
      `;
    }

    return `
      <div class="border-bottom m-1">
        <div class="row p-1">
          <div class="col-md-6">
              <div class="d-flex align-items-center reply-writer">
                  <img src="${cp}/dist/images/person.png" class="avatar-icon">
                  <div class="ms-2">
                      <div class="name">${vo.name}</div>
                      <div class="date">${vo.reg_date}</div>
                  </div>
              </div>
          </div>
          <div class="col align-self-center text-end">
            <div class="reply-dropdown">
              <span class="dropdown-button"><i class="bi bi-three-dots-vertical"></i></span>
              <div class="reply-menu d-none">
                ${menuHTML}
              </div>
            </div>
          </div>
        </div>
        <div class="p-2 ${contentClass}">
          ${vo.content}
        </div>
      </div>
    `;
  }).join('');
}