const replySessionEL = document.querySelector('div.reply-session');

// const cp = replySessionEL ? (replySessionEL.getAttribute('data-contextPath') || '').replace(/\/$/, '') : '';
const num = replySessionEL?.getAttribute('data-num');
const postsUrl = replySessionEL?.getAttribute('data-postsUrl');

// fetch
function initReplySystem() {
	if(! postsUrl || ! num) {
		alert('댓글 기능이 비활성화되어 있습니다.');
		return false;
	}
	
	return true;
}

window.addEventListener('DOMContentLoaded', () => loadContent(1));

function loadContent(page) {
	if(! initReplySystem()) {
		return;
	}
	
	const url = `${postsUrl}/${num}`;
	const params = {pageNo:page};
	
	fetchRequest(url, 'get', params, 'form', 'json')
		.then(data => {
			addNewReply(data);
		})
		.catch(error => console.log(error));
}

function addNewReply(data) {
	const listReply = data.listReply;
	const totalCount = Number(data.totalCount) || 0;
	const pageNo = Number(data.pageNo) || 0;
	const totalPage = Number(data.totalPage) || 0;
	const paging = data.paging;
	
	const listReplyEL = document.querySelector('div#listReply');
	const listContentEL = document.querySelector('#listReply .list-content');
	const replyCountEL = document.querySelector('#listReply .reply-count');
	const replyPageEL = document.querySelector('#listReply .reply-page');
	const pageNavigationEL = document.querySelector('#listReply .page-navigation');
	
	const htmlText = renderReplies(listReply, pageNo);
	
	replyCountEL.textContent = `댓글 ${totalCount}개`;
	replyPageEL.textContent = `[목록, ${pageNo}/${totalPage} 페이지]`;

	listContentEL.setAttribute('data-pageNo', pageNo);
	listContentEL.setAttribute('data-totalPage', totalPage);	
	
	if(totalCount === 0) {
		listReplyEL.style.display = 'none';
		listContentEL.textContent = '';
		
		return;
	} 
	
	listReplyEL.style.display = '';
	listContentEL.innerHTML = htmlText;
	pageNavigationEL.innerHTML = paging;
}

// 댓글 등록
window.addEventListener('DOMContentLoaded', () => {
	const btnSendEL = replySessionEL.querySelector('button.btnSendReply');
	
	btnSendEL.addEventListener('click', function () {
		if(! initReplySystem()) {
			return;
		}
				
		const divEL = this.closest('div.reply-form');
		const taEL = divEL.querySelector('textarea');

		let content = taEL.value.trim();
		if(! content) {
			taEL.focus();
			return;
		}
		
		const url = `${postsUrl}/insert`;
		// const params = 'num=' + num + '&content=' + encodeURIComponent(content) + '&parentNum=0';
		const params = {num:num, content:content, parentNum:0};
		
		const fn = function(data){
			taEL.value = '';
			
			loadContent(1);
		};
		
		fetchRequest(url, 'post', params, 'form', 'json')	
			.then(data => {
				fn(data);
			})
			.catch(error => console.log(error));
	});
});

window.addEventListener('DOMContentLoaded', () => {
	// 삭제, 신고 메뉴
	const listReplyEL = document.querySelector('div#listReply');
	
	listReplyEL.addEventListener('click', ev => {
		const btnEL = ev.target.closest('.dropdown-button');
		if (! btnEL) return;
		
		const menuEL = btnEL.nextElementSibling;
		if (! menuEL || ! menuEL.classList.contains('reply-menu')) return;

		document.querySelectorAll('.reply-menu').forEach(function (el) {
			if (el !== menuEL) {
				el.classList.add('d-none');
			}
		});
		
		menuEL.classList.toggle('d-none');
		
		// body 클릭 이벤트로 전파되지 않게
		ev.stopPropagation();
	});

	document.body.addEventListener('click', ev => {
		const parent = ev.target.parentNode;
		const isMatch = parent && parent.tagName === 'SPAN' &&
				parent.classList.contains('dropdown-button');
		
		if(isMatch) {
			return;
		}
		
		document.querySelectorAll('div.reply-menu:not(.d-none)').forEach(function (menu) {
			menu.classList.add('d-none');
		});
	});

	// 댓글 삭제
	listReplyEL.addEventListener('click', ev => {
		if(! initReplySystem()) {
			return;
		}
		
		const target = ev.target.closest('.deleteReply');
		if (! target) return;
						
		if(! confirm('게시물을 삭제하시겠습니까 ? ')) {
		    return;
		}
		
		const replyNum = target.getAttribute('data-replyNum');
		const page = target.getAttribute('data-pageNo');
		
		const url = `${postsUrl}/${replyNum}`;
		const params = {mode:'reply'};
		
		fetchRequest(url, 'delete', params, 'form', 'json')	
			.then(data => {
				loadContent(page);
			})
			.catch(error => console.log(error));		
	});
	
});

// 댓글 좋아요 / 싫어요
window.addEventListener('DOMContentLoaded', () => {
	const listReplyEL = document.querySelector('div#listReply');
	
	// 댓글 좋아요 / 싫어요 등록
	listReplyEL.addEventListener('click', ev => {
		if(! initReplySystem()) {
			return;
		}
		
		const btnEL = ev.target.closest('button.btnSendReplyLike');
		if (! btnEL) return;
		
		const replyLikeItemEL = btnEL.closest('.reply-like-item');
		const isUserLiked = replyLikeItemEL && replyLikeItemEL.getAttribute('data-userLiked') !== '-1';
		if(isUserLiked) {
			alert('게시글 공감여부가 등록되어 있습니다.');
			return;
		}
		
		const replyNum = btnEL.getAttribute('data-replyNum');
		const replyLike = btnEL.getAttribute('data-replyLike');
		let msg = '게시글이 마음에 들지 않으십니까 ?';
		if(replyLike === '1') {
			msg = '게시글에 공감하십니까 ?';
		}
		
		if(! confirm(msg)) {
			return;
		}
		
		const url = `${postsUrl}/like`;
		const params = {replyNum:replyNum, replyLike:replyLike};

		const fn = function(data){
			let state = data.state;
			if(state === 'true') {
				let likeCount = data.likeCount;
				let disLikeCount = data.disLikeCount;
				
				replyLikeItemEL.setAttribute('data-userLiked', replyLike);
				
				const iconEL = btnEL.querySelector('i');
				if (iconEL)  iconEL.style.color = 'red';
				
				const likeSpanEL = replyLikeItemEL.children[0]?.querySelector('span');
				const disLikeSpanEL = replyLikeItemEL.children[1]?.querySelector('span');

				if (likeSpanEL) likeSpanEL.textContent = likeCount;
				if (disLikeSpanEL) disLikeSpanEL.textContent = disLikeCount;
				
			} else if(state === 'liked') {
				alert('게시글 공감 여부는 한번만 가능합니다. !!!');
			} else {
				alert('게시글물 공감 여부 처리가 실패했습니다. !!!');
			}
		};
		
		fetchRequest(url, 'post', params, 'form', 'json')	
			.then(data => {
				fn(data);
			})
			.catch(error => console.log(error));
	});
});

// 댓글별 답글 리스트
function listReplyAnswer(parentNum) {
	if(! initReplySystem()) {
		return;
	}
		
	const url = `${postsUrl}/answer`;
	const params = 'parentNum=' + parentNum;

	fetchRequest(url, 'get', params, 'form', 'json')	
		.then(data => {
			addNewAnswer(data, parentNum);
		})
		.catch(error => console.log(error));
}

function addNewAnswer(data, parentNum) {
	const listAnswers = data.listAnswers;
	const answerCount = data.answerCount;

	const htmlText = renderReplyAnswers(listAnswers);
	
	const answerListEL = document.querySelector('div#listReplyAnswer' + parentNum);
	const answerCountEL = document.querySelector('span#answerCount' + parentNum);
	
	answerListEL.innerHTML = htmlText;
	answerCountEL.textContent = answerCount;
}

window.addEventListener('DOMContentLoaded', () => {
	const listReplyEL = document.querySelector('div#listReply');
	
	// 답글 버튼(댓글별 답글 등록폼 및 답글리스트)
	listReplyEL.addEventListener('click', ev => {
		const btnEL = ev.target.closest('button.btnReplyAnswerLayout');
		if (! btnEL) return;

		const footerEL = btnEL.closest('div.reply-item-footer');
		if (! footerEL) return;
		
		const el = footerEL.nextElementSibling;
		
		let isHidden = el.classList.contains('d-none');
		let replyNum = btnEL.getAttribute('data-replyNum');
		
		if(isHidden) {
			// 답글 리스트 및 답글 개수
			listReplyAnswer(replyNum);
		}
		
		el.classList.toggle('d-none');
	});

	// 댓글별 답글 등록
	listReplyEL.addEventListener('click', ev => {
		if(! initReplySystem()) {
			return;
		}

		const btnEL = ev.target.closest('button.btnSendReplyAnswer');
		if (! btnEL) return;

		const el = btnEL.closest('div.reply-answer');
		if (! el) return;
		
		const replyNum = btnEL.getAttribute('data-replyNum');
		const taEL = el.querySelector('textarea');
		let content = taEL.value.trim();
		
		if(! content) {
			taEL.focus();
			return;
		}
		
		const url = `${postsUrl}/insert`;
		const params = {num:num, content:content, parentNum:replyNum};
		
		const fn = function(data){
			taEL.value = '';
			
			listReplyAnswer(replyNum);
		};
		
		fetchRequest(url, 'post', params, 'form', 'json')	
			.then(data => {
				fn(data);
			})
			.catch(error => console.log(error));
		
	});

	// 댓글별 답글 삭제
	listReplyEL.addEventListener('click', ev => {
		if(! initReplySystem()) {
			return;
		}
		
		const btnEL = ev.target.closest('.deleteReplyAnswer');
		if (! btnEL) return;
	
		if(! confirm('게시물을 삭제하시겠습니까 ? ')) {
		    return;
		}
		
		const replyNum = btnEL.getAttribute('data-replyNum');
		const parentNum = btnEL.getAttribute('data-parentNum');
		
		const url = `${postsUrl}/${replyNum}`;
		const params = {mode:'answer'};
		
		fetchRequest(url, 'delete', params, 'form', 'json')	
			.then(data => {
				listReplyAnswer(parentNum);
			})
			.catch(error => console.log(error));
		
	});
});

// 댓글 / 답글 숨김기능
window.addEventListener('DOMContentLoaded', () => {
	const listReplyEL = document.querySelector('div#listReply');
	
	// 댓글 숨김
	listReplyEL.addEventListener('click', ev => {
		if(! initReplySystem()) {
			return;
		}

		const menuEL = ev.target.closest('.hideReply');
		if (! menuEL) return;
			
		let replyNum = menuEL.getAttribute('data-replyNum');
		let showReply = menuEL.getAttribute('data-showReply');
			
		let msg = '댓글을 숨김 하시겠습니까 ? ';
		if(showReply === '0') {
			msg = '댓글 숨김을 해제 하시겠습니까 ? ';
		}
		if(! confirm(msg)) {
			return;
		}
			
		showReply = showReply === '1' ? '0' : '1';
			
		const url = `${postsUrl}/replyShowHide`;
		const params = {replyNum:replyNum, showReply:showReply};
			
		const fn = function(data){
			const itemEL = menuEL.closest('.reply-item-header')?.nextElementSibling;
								
			if(showReply === '1') {
				itemEL.classList.remove('text-primary', 'text-opacity-50');
				menuEL.setAttribute('data-showReply', '1');
				menuEL.textContent = '숨김';
			} else {
				itemEL.classList.add('text-primary', 'text-opacity-50');
				menuEL.setAttribute('data-showReply', '0');
				menuEL.textContent = '표시';
			}
		};
			
		fetchRequest(url, 'post', params, 'form', 'json')	
			.then(data => {
				fn(data);
			})
			.catch(error => console.log(error));
	});

	// 답글 숨김
	listReplyEL.addEventListener('click', ev => {
		if(! initReplySystem()) {
			return;
		}
		
		const menuEL = ev.target.closest('.hideReplyAnswer');
		if (! menuEL) return;		
		
		let replyNum = menuEL.getAttribute('data-replyNum');
		let showReply = menuEL.getAttribute('data-showReply');
		
		let msg = '댓글을 숨김 하시겠습니까 ? ';
		if(showReply === '0') {
			msg = '댓글 숨김을 해제 하시겠습니까 ? ';
		}
		if(! confirm(msg)) {
			return;
		}
		
		showReply = showReply === '1' ? '0' : '1';
		
		const url = `${postsUrl}/replyShowHide`;
		const params = {replyNum:replyNum, showReply:showReply};
		
		const fn = function(data) {
			const itemEL = menuEL.closest('.row')?.nextElementSibling;
			if (! itemEL) return;

			if(showReply === '1') {
				itemEL.classList.remove('text-primary', 'text-opacity-50');
				menuEL.setAttribute('data-showReply', '1');
				menuEL.textContent = '숨김';
			} else {
				itemEL.classList.add('text-primary', 'text-opacity-50');
				menuEL.setAttribute('data-showReply', '0');
				menuEL.textContent = '표시';
			}
		};

		fetchRequest(url, 'post', params, 'form', 'json')	
			.then(data => {
				fn(data);
			})
			.catch(error => console.log(error));
	});
});
