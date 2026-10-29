/* ==========================================================================
   [JS] 업무공지 탭 제어 (조회 드로어 & 팀 리더 전용 작성/수정/삭제 드로어 전환)
   ========================================================================== */
let currentNoticeFilter = 'ALL';
let activeNoticeData = null;
let activeNoticeRow = null;

function getNoticeApiBaseUrl() {
  return `/${currentDomainKey}/project/${currentProjectId}/api/tasks/notices`;
}

// ================= 1. 공지 목록 필터링 & 검색 =================
function filterNoticeList(filterType) {
  currentNoticeFilter = filterType;
}

function applyNoticeFilterAndSearch() {
	const keyword = (document.getElementById('noticeSearchInput')?.value || '').trim();
	
	const params = new URLSearchParams({
		tab: 'notice',
		schType: currentNoticeFilter || 'all',
		kwd: keyword,
		page: 1 // 검색 시 1페이지부터 조회
	});

	// 페이지 이동
	location.href = `/${currentDomainKey}/project/${currentProjectId}?${params.toString()}`;

}

// ================= 2. 공지 행 클릭 -> 우측 드로어 [상세 조회 모드] 오픈 =================
async function openNoticeDetailDrawer(noticeId, rowElem) {
  document.querySelectorAll('.notice-item-row').forEach(r => r.classList.remove('selected-notice-row'));
  if (rowElem) rowElem.classList.add('selected-notice-row');
  activeNoticeRow = rowElem;

  const drawer = document.getElementById('noticeSideDrawer');
  const readView = document.getElementById('noticeReadView');
  const formView = document.getElementById('noticeFormView');
  const leaderBtns = document.getElementById('noticeLeaderActionBtns');

  document.getElementById('noticeDrawerTopTitle').textContent = '업무 공지 상세';
  document.getElementById('noticeDrawerModeChip').textContent = '상세 조회';

  if (readView) readView.style.display = 'flex';
  if (formView) formView.style.display = 'none';
  if (leaderBtns) leaderBtns.style.display = isTeamLeader ? 'flex' : 'none';

  drawer.classList.add('active');

  try {
    const res = await fetch(`${getNoticeApiBaseUrl()}/${noticeId}?incView=true`);
    if (!res.ok) throw new Error('공지 상세 조회 실패');
    const data = await res.json();
    activeNoticeData = data;

    bindNoticeReadView(data);

    // 목록의 조회수 셀 실시간 +1 반영
    if (rowElem && data.viewCount != null) {
      const viewCell = rowElem.querySelector('.col-n-views');
      if (viewCell) viewCell.textContent = data.viewCount;
    }
  } catch (err) {
    console.error('공지 상세 조회 오류:', err);
  }
}

function bindNoticeReadView(data) {
  const typeBadge = document.getElementById('readNoticeTypeBadge');
  const isTaskType = (data.noticeType === 'task');
  typeBadge.textContent = isTaskType ? '업무공지' : '프로젝트 공지';
  typeBadge.className = 'n-type-badge ' + (isTaskType ? 'type-task' : 'type-project');

  const taskChip = document.getElementById('readNoticeTaskChip');
  if (data.taskTitle) {
    taskChip.style.display = 'inline-block';
    taskChip.textContent = data.taskTitle;
  } else {
    taskChip.style.display = 'none';
  }

  const statusBadge = document.getElementById('readNoticeStatusBadge');
  const isPrivate = String(data.status) === '0';
  statusBadge.textContent = isPrivate ? '🔒 비공개' : '🌐 공개';
  statusBadge.className = 'n-status-badge ' + (isPrivate ? 'status-private' : 'status-public');

  document.getElementById('readNoticeViewCount').textContent = data.viewCount != null ? data.viewCount : 0;
  document.getElementById('readNoticeTitle').textContent = data.title || '';

  const writer = data.writerName || '팀 리더';
  document.getElementById('readNoticeWriter').textContent = writer;
  document.getElementById('readNoticeAvatar').textContent = writer.length >= 2 ? writer.slice(-2) : writer;

  const dateText = `작성일: ${data.created || '-'}` + (data.updated ? ` · 수정일: ${data.updated}` : '');
  document.getElementById('readNoticeDates').textContent = dateText;
  document.getElementById('readNoticeContent').innerHTML = data.content || '';
}

// ================= 3. [+ 새 공지 작성] 클릭 -> 우측 드로어 [작성 폼 모드] 오픈 (팀 리더 전용) =================
function openNoticeWriteDrawer() {
  if (! isTeamLeader) {
    alert('팀 리더만 공지를 작성할 수 있습니다.'+ isTeamLeader);
    return;
  }

  document.querySelectorAll('.notice-item-row').forEach(r => r.classList.remove('selected-notice-row'));
  activeNoticeData = null;
  activeNoticeRow = null;

  const drawer = document.getElementById('noticeSideDrawer');
  const readView = document.getElementById('noticeReadView');
  const formView = document.getElementById('noticeFormView');
  const leaderBtns = document.getElementById('noticeLeaderActionBtns');

  document.getElementById('noticeDrawerTopTitle').textContent = '새 업무 공지 작성';
  document.getElementById('noticeDrawerModeChip').textContent = '작성 중';
  document.getElementById('btnSubmitNoticeForm').textContent = '공지 등록';

  if (readView) readView.style.display = 'none';
  if (formView) formView.style.display = 'flex';
  if (leaderBtns) leaderBtns.style.display = 'none';

  // 폼 필드 초기화
  document.getElementById('formNoticeId').value = '';
  document.getElementById('formNoticeTitle').value = '';
  document.getElementById('formNoticeTaskId').value = '';
  document.getElementById('formTaskSelectRow').style.display = 'flex';
  document.getElementById('editor').querySelector('.ql-editor').innerHTML = '';
  const publicRadio = document.querySelector('input[name="formNoticeStatus"][value="1"]');
  if (publicRadio) publicRadio.checked = true;

  drawer.classList.add('active');
  setTimeout(() => document.getElementById('formNoticeTitle')?.focus(), 50);
}

// ================= 4. 상세 조회 중 [수정] 버튼 클릭 -> 우측 드로어를 [수정 폼 모드]로 전환 =================
function switchDrawerToEditMode() {
  if (! isTeamLeader || !activeNoticeData) return;

  const readView = document.getElementById('noticeReadView');
  const formView = document.getElementById('noticeFormView');
  const leaderBtns = document.getElementById('noticeLeaderActionBtns');

  document.getElementById('noticeDrawerTopTitle').textContent = '업무 공지 수정';
  document.getElementById('noticeDrawerModeChip').textContent = '수정 중';
  document.getElementById('btnSubmitNoticeForm').textContent = '수정 저장';

  if (readView) readView.style.display = 'none';
  if (formView) formView.style.display = 'flex';
  if (leaderBtns) leaderBtns.style.display = 'none';

  document.getElementById('formNoticeId').value = activeNoticeData.noticeId;
  document.getElementById('formNoticeTitle').value = activeNoticeData.title || '';

  const nType = 'task';

  document.getElementById('formNoticeTaskId').value = activeNoticeData.taskId != null ? String(activeNoticeData.taskId) : '';
  
  document.getElementById('editor').querySelector('.ql-editor').innerHTML = activeNoticeData.content || '';

  const statusVal = activeNoticeData.status != null ? String(activeNoticeData.status) : '1';
  const targetRadio = document.querySelector(`input[name="formNoticeStatus"][value="${statusVal}"]`);
  if (targetRadio) targetRadio.checked = true;
}

function onNoticeTypeChange(value) {
  const row = document.getElementById('formTaskSelectRow');
  if (!row) return;
  if (value === 'project' || value === 'workspace') {
    row.style.display = 'none';
    document.getElementById('formNoticeTaskId').value = '';
  } else {
    row.style.display = 'flex';
  }
}

function cancelNoticeForm() {
  if (activeNoticeData && activeNoticeData.noticeId) {
    // 수정 중 취소하면 다시 상세 조회 뷰로 복귀
    openNoticeDetailDrawer(activeNoticeData.noticeId, activeNoticeRow);
  } else {
    closeNoticeDrawer();
  }
}

function closeNoticeDrawer() {
  const drawer = document.getElementById('noticeSideDrawer');
  if (drawer) drawer.classList.remove('active');
  document.querySelectorAll('.notice-item-row').forEach(r => r.classList.remove('selected-notice-row'));
}

// ================= 5. 공지 등록 / 수정 제출 및 삭제 처리 (REST API 연동) =================
async function submitNoticeForm() {
  if (! isTeamLeader) return;

  const editingId = document.getElementById('formNoticeId').value;
  const title = document.getElementById('formNoticeTitle').value.trim();
  const taskId = document.getElementById('formNoticeTaskId').value;
  const content = document.getElementById('editor').querySelector('.ql-editor').innerHTML;
  const statusRadio = document.querySelector('input[name="formNoticeStatus"]:checked');
  const status = statusRadio ? Number(statusRadio.value) : 1;

  if (!title) {
    alert('공지 제목을 입력해 주세요.');
    document.getElementById('formNoticeTitle').focus();
    return;
  }
  if (!content) {
    alert('공지 상세 내용을 입력해 주세요.');
    return;
  }

  const payload = {
    currentProjectMemberId: currentProjectMemberId,
    noticeType: 'TASK',
    taskId: (noticeType === 'task' && taskId) ? Number(taskId) : null,
    title: title,
    content: content,
    status: status
  };
  
  console.log(payload);

  try {
    const baseUrl = getNoticeApiBaseUrl();
    const isEdit = !!editingId;
    const url = isEdit ? `${baseUrl}/${editingId}` : baseUrl;
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method: method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const result = await res.json();
    if (!res.ok || !result.success) {
      alert(result.message || '공지 저장에 실패했습니다.');
      return;
    }

    const saved = result.notice;
    if (isEdit) {
      updateNoticeRowInDom(saved);
      activeNoticeData = saved;
      await openNoticeDetailDrawer(saved.noticeId, activeNoticeRow);
    } else {
      prependNoticeRowToDom(saved);
      const newRow = document.querySelector(`.notice-item-row[data-notice-id="${saved.noticeId}"]`);
      await openNoticeDetailDrawer(saved.noticeId, newRow);
    }
  } catch (err) {
    console.error('공지 저장 오류:', err);
    alert('공지 저장 중 오류가 발생했습니다.');
  }
}

async function deleteCurrentNotice() {
  if (! isTeamLeader || !activeNoticeData || !activeNoticeData.noticeId) return;
  if (!confirm('이 업무 공지사항을 삭제하시겠습니까?')) return;

  const noticeId = activeNoticeData.noticeId;
  try {
    const res = await fetch(`${getNoticeApiBaseUrl()}/${noticeId}?currentProjectMemberId=${currentProjectMemberId}`, {
      method: 'DELETE'
    });
    const result = await res.json();
    if (!res.ok || !result.success) {
      alert(result.message || '삭제 권한이 없거나 실패했습니다.');
      return;
    }

    const row = document.querySelector(`.notice-item-row[data-notice-id="${noticeId}"]`);
    if (row) row.remove();

    closeNoticeDrawer();
    applyNoticeFilterAndSearch();
  } catch (err) {
    console.error('공지 삭제 오류:', err);
  }
}

function prependNoticeRowToDom(n) {
  const listBody = document.getElementById('noticeListBody');
  const emptyRow = document.getElementById('noticeEmptyRow');
  if (emptyRow) emptyRow.style.display = 'none';

  const li = document.createElement('li');
  li.className = 'notice-item-row notice-grid-cols';
  li.setAttribute('data-notice-id', n.noticeId);
  li.setAttribute('data-type', n.noticeType || 'TASK');
  li.setAttribute('data-title', n.title || '');
  li.setAttribute('data-writer', n.writerName || '팀 리더');
  li.onclick = function() { openNoticeDetailDrawer(n.noticeId, li); };

  li.innerHTML = buildNoticeRowInnerHtml(n);
  listBody.insertBefore(li, listBody.firstElementChild?.id === 'noticeEmptyRow' ? listBody.children[1] : listBody.firstElementChild);
}

function updateNoticeRowInDom(n) {
  const li = document.querySelector(`.notice-item-row[data-notice-id="${n.noticeId}"]`);
  if (!li) return;
  li.setAttribute('data-type', n.noticeType || 'TASK');
  li.setAttribute('data-title', n.title || '');
  li.innerHTML = buildNoticeRowInnerHtml(n);
}

function buildNoticeRowInnerHtml(n) {
  const isTask = (n.noticeType === 'task');
  const isPrivate = String(n.status) === '0';
  const taskChipHtml = n.taskTitle ? `<span class="n-task-chip">${escapeNoticeHtml(n.taskTitle)}</span>` : '';

  return `
    <div class="col-n-id">#${n.noticeId}</div>
    <div class="col-n-type">
      <span class="n-type-badge ${isTask ? 'type-task' : 'type-project'}">${isTask ? '업무공지' : '프로젝트'}</span>
      ${taskChipHtml}
    </div>
    <div class="col-n-title">
      <span class="notice-title-text">${escapeNoticeHtml(n.title || '')}</span>
    </div>
    <div>
      <span class="n-status-badge ${isPrivate ? 'status-private' : 'status-public'}">${isPrivate ? '🔒 비공개' : '🌐 공개'}</span>
    </div>
    <div class="col-n-writer">${escapeNoticeHtml(n.writerName || '팀 리더')}</div>
    <div class="col-n-views">${n.viewCount != null ? n.viewCount : 0}</div>
    <div class="col-n-date">${n.created || '-'}</div>
  `;
}

function handleNoticeFileSelect(input) {
  if (!input.files || input.files.length === 0) return;
  const file = input.files[0];
  const list = document.getElementById('noticeAttachedFileList');
  const item = document.createElement('div');
  item.className = 'attached-file-item';
  item.style.cssText = 'display:flex; justify-content:space-between; align-items:center; padding:6px 10px; background:#fff; border:1px solid #E8E2D9; border-radius:6px; font-size:12px;';
  item.innerHTML = `
    <span>📁 ${escapeNoticeHtml(file.name)} (${(file.size / 1024).toFixed(1)} KB)</span>
    <button type="button" style="border:none; background:none; cursor:pointer; color:#D9534F;" onclick="this.parentElement.remove()">✕</button>
  `;
  list.appendChild(item);
  document.getElementById('noticeFileCount').textContent = `(${list.children.length})`;
}

function escapeNoticeHtml(str) {
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// 드로어 너비 조절 리사이저 및 ESC 닫기
document.addEventListener('DOMContentLoaded', () => {
  const resizer = document.getElementById('noticeResizerBar');
  const drawer = document.getElementById('noticeSideDrawer');
  let isResizing = false;

  if (resizer && drawer) {
    resizer.addEventListener('mousedown', () => {
      isResizing = true;
      document.body.style.cursor = 'col-resize';
    });
    document.addEventListener('mousemove', e => {
      if (!isResizing) return;
      const rect = drawer.parentElement.getBoundingClientRect();
      let w = rect.right - e.clientX;
      if (w < 400) w = 400;
      if (w > 860) w = 860;
      drawer.style.width = w + 'px';
    });
    document.addEventListener('mouseup', () => {
      if (isResizing) {
        isResizing = false;
        document.body.style.cursor = '';
      }
    });
  }

  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeNoticeDrawer();
  });
});