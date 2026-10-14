/* ==========================================================================
   [JS] 단일 업무 목록 기반 비동기 통신 로직 (async / await + 상세형 등록 모달)
   ========================================================================== */
const statusMap = [
  { code: 0, text: '● 요청', class: 'badge-req' },
  { code: 1, text: '● 진행', class: 'badge-done' },
  { code: 2, text: '● 피드백', class: 'badge-feedback' },
  { code: 3, text: '● 완료', class: 'badge-done' },
  { code: 4, text: '● 보류', class: 'badge-hold' }
];

let currentActiveRow = null;
let currentActiveTaskId = null;
let pendingDeleteContext = null;

// 등록 모달 컨텍스트 ({ mode: 'main' | 'sub', parentTaskId: null | number, statusCode: 0 })
let createModalContext = {
  mode: 'main',
  parentTaskId: null,
  statusCode: 0
};

// ================= 1. 업무 / 하위업무 등록 모달 오픈 & 제출 =================
function requestCreateMainTask() {
  openCreateTaskModal('main', null);
}

function requestCreateSubTask(taskId) {
  openCreateTaskModal('sub', taskId);
}

function detailAddSubtaskClick() {
  if (currentActiveTaskId) openCreateTaskModal('sub', currentActiveTaskId);
}

function openCreateTaskModal(mode, parentTaskId = null) {
  createModalContext = {
    mode: mode,
    parentTaskId: parentTaskId,
    statusCode: 0
  };

  const modal = document.getElementById('taskCreateModal');
  const headerTitle = document.getElementById('createModalHeaderTitle');
  const titleInput = document.getElementById('createTaskTitleInput');
  const contentInput = document.getElementById('createTaskContentInput');
  const assigneeSelect = document.getElementById('createAssigneeSelect');
  const startDateInput = document.getElementById('createStartDateInput');
  const endDateInput = document.getElementById('createEndDateInput');
  const statusBadge = document.getElementById('createStatusBadge');
  const statusText = document.getElementById('createStatusText');

  headerTitle.textContent = mode === 'sub' ? '새 하위업무 등록' : '새 업무 등록';
  titleInput.placeholder = mode === 'sub' ? '하위업무 제목을 입력하세요 *' : '업무 제목을 입력하세요 *';

  // 입력값 초기화
  titleInput.value = '';
  contentInput.value = '';
  assigneeSelect.value = '1';

  // 기본 날짜 세팅 (오늘 ~ +7일)
  const today = new Date();
  const nextWeek = new Date();
  nextWeek.setDate(today.getDate() + 7);
  startDateInput.value = today.toISOString().split('T')[0];
  endDateInput.value = nextWeek.toISOString().split('T')[0];

  // 상태 초기값 (0: 요청)
  statusBadge.className = 'badge-status badge-req';
  statusText.textContent = '● 요청';

  modal.classList.add('show');
  setTimeout(() => titleInput.focus(), 50);
}

function closeCreateTaskModal() {
  const modal = document.getElementById('taskCreateModal');
  if (modal) modal.classList.remove('show');
}

function handleCreateBackdropClick(e) {
  if (e.target === document.getElementById('taskCreateModal')) {
    closeCreateTaskModal();
  }
}

function cycleCreateModalStatus() {
  createModalContext.statusCode = (createModalContext.statusCode + 1) % statusMap.length;
  const next = statusMap[createModalContext.statusCode];
  const badge = document.getElementById('createStatusBadge');
  const textSpan = document.getElementById('createStatusText');

  badge.className = 'badge-status ' + next.class;
  textSpan.textContent = next.text;
}

async function submitCreateTaskModal() {
  const titleInput = document.getElementById('createTaskTitleInput');
  const contentInput = document.getElementById('createTaskContentInput');
  const assigneeSelect = document.getElementById('createAssigneeSelect');
  const startDateInput = document.getElementById('createStartDateInput');
  const endDateInput = document.getElementById('createEndDateInput');

  const title = titleInput.value.trim();
  const content = contentInput.value.trim();
  const projectMemberId = assigneeSelect.value;
  const startAt = startDateInput.value;
  const endAt = endDateInput.value;

  // 필수 입력값 검증 (제목, 담당자, 상세 내용)
  if (!title) {
    alert('업무 제목을 입력해 주세요.');
    titleInput.focus();
    return;
  }
  if (!projectMemberId) {
    alert('담당자를 선택해 주세요.');
    assigneeSelect.focus();
    return;
  }
  if (!content) {
    alert('상세 내용을 입력해 주세요.');
    contentInput.focus();
    return;
  }

  const formData = new URLSearchParams();
  formData.append('title', title);
  formData.append('content', content);
  formData.append('projectMemberId', projectMemberId);
  formData.append('status', createModalContext.statusCode);
  if (startAt) formData.append('startAt', startAt);
  if (endAt) formData.append('endAt', endAt);

  try {
    if (createModalContext.mode === 'main') {
      formData.append('taskLevel', 1);

      const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/tasks/fragment/create-main`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData
      });

      if (!res.ok) throw new Error('메인 업무 생성 요청 실패');

      const htmlFragment = await res.text();
      const taskList = document.getElementById('mainTaskList');
      taskList.insertAdjacentHTML('beforeend', htmlFragment);
    } else {
      const taskId = createModalContext.parentTaskId;
      const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/tasks/${taskId}/fragment/create-sub`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData
      });

      if (!res.ok) throw new Error('하위 업무 생성 요청 실패');

      const htmlFragment = await res.text();
      const branchLi = document.querySelector(`[data-task-id="${taskId}"]`);
      if (branchLi) {
        const subTaskList = branchLi.querySelector('.sub-task-list');
        subTaskList.insertAdjacentHTML('beforeend', htmlFragment);

        // 토글 버튼 및 하위 개수 카운트 갱신
        const countSpan = branchLi.querySelector('.sub-count');
        if (countSpan) {
          countSpan.textContent = parseInt(countSpan.textContent || '0') + 1;
        } else {
          const titleSpan = branchLi.querySelector('.task-title-text');
          titleSpan.insertAdjacentHTML('afterend', '<span style="font-size:12px; color:var(--text-muted);">(<span class="sub-count">1</span>)</span>');
          const colName = branchLi.querySelector('.col-task-name');
          if (!colName.querySelector('.task-toggle')) {
            colName.firstElementChild.outerHTML = '<span class="btn-toggle-box task-toggle" onclick="event.stopPropagation(); toggleSubTasks(this)">－</span>';
          }
        }
      }

      if (currentActiveTaskId === taskId) {
        await refreshSubTasksInDetailPanel(taskId);
      }
    }

    closeCreateTaskModal();
  } catch (err) {
    console.error('업무 등록 오류:', err);
    alert('업무 등록 중 오류가 발생했습니다. 입력값 및 DB 제약조건을 확인해 주세요.');
  }
}

// ================= 2. 인라인 즉시 수정 (TaskApiController PATCH 연동) =================
function editTitle(cell, id, isSub) {
  if (cell.querySelector('input')) return;
  const oldText = cell.textContent.trim();
  const input = document.createElement('input');
  input.type = 'text';
  input.className = 'inline-edit-input';
  input.value = oldText;

  cell.textContent = '';
  cell.appendChild(input);
  input.focus();
  input.select();

  let isSaving = false;

  async function finish() {
    if (isSaving) return;
    isSaving = true;

    const newText = input.value.trim() || oldText;
    cell.textContent = newText;

    if (newText !== oldText) {
      const url = isSub 
        ? `/api/projects/${currentProjectId}/tasks/subtasks/${id}/field`
        : `/api/projects/${currentProjectId}/tasks/${id}/field`;

      try {
        await fetch(url, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ fieldName: 'title', fieldValue: newText })
        });

        if (currentActiveTaskId === id) {
          document.getElementById('detailTaskTitle').value = newText;
        }
      } catch (err) {
        console.error('제목 수정 오류:', err);
        cell.textContent = oldText;
      }
    }
  }

  input.onblur = finish;
  input.onkeydown = e => {
    if (e.key === 'Enter') {
      input.blur();
    } else if (e.key === 'Escape') {
      isSaving = true;
      cell.textContent = oldText;
    }
  };
}

async function cycleStatus(badge, id, isSub) {
  const text = badge.textContent.trim();
  const idx = statusMap.findIndex(s => s.text === text);
  const next = statusMap[(idx + 1) % statusMap.length];

  badge.className = 'badge-status ' + next.class;
  badge.textContent = next.text;

  const url = isSub 
    ? `/api/projects/${currentProjectId}/tasks/subtasks/${id}/field`
    : `/api/projects/${currentProjectId}/tasks/${id}/field`;

  try {
    await fetch(url, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fieldName: 'status', fieldValue: next.code })
    });

    if (currentActiveTaskId === id) {
      document.getElementById('detailStatusBadge').className = 'badge-status ' + next.class;
      document.getElementById('detailStatusText').textContent = next.text;
    }
  } catch (err) {
    console.error('상태 변경 오류:', err);
  }
}

function editDate(cell, id, fieldName, isSub) {
  if (cell.querySelector('input')) return;
  const currentVal = cell.textContent.trim() === '-' ? '' : cell.textContent.trim();
  const input = document.createElement('input');
  input.type = 'date';
  input.className = 'inline-edit-date';
  if (currentVal) input.value = currentVal;

  cell.textContent = '';
  cell.appendChild(input);
  input.focus();

  let isSaving = false;

  async function finish() {
    if (isSaving) return;
    isSaving = true;

    const picked = input.value || '';
    cell.textContent = picked || '-';
    if (fieldName === 'endAt' && picked) cell.classList.add('text-danger');
    else if (fieldName === 'endAt') cell.classList.remove('text-danger');

    const url = isSub 
      ? `/api/projects/${currentProjectId}/tasks/subtasks/${id}/field`
      : `/api/projects/${currentProjectId}/tasks/${id}/field`;

    try {
      await fetch(url, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fieldName: fieldName, fieldValue: picked })
      });
    } catch (err) {
      console.error('날짜 수정 오류:', err);
    }
  }

  input.onblur = finish;
  input.onchange = () => input.blur();
}

// ================= 3. 상세 패널 데이터 바인딩 =================
async function openDetailPanel(taskId, rowElem) {
  document.querySelectorAll('.task-item-row').forEach(r => r.classList.remove('selected-task-row'));
  rowElem.classList.add('selected-task-row');

  currentActiveRow = rowElem;
  currentActiveTaskId = taskId;

  try {
    const res = await fetch(`/api/projects/${currentProjectId}/tasks/${taskId}`);
    if (!res.ok) throw new Error('상세 조회 실패');

    const task = await res.json();

    document.getElementById('taskDetailSidePanel').classList.add('active');
    document.getElementById('detailTaskTitle').value = task.title || '';
    document.getElementById('detailCreatedDate').textContent = task.created || '';
    document.getElementById('detailContentText').value = task.content || '';
    document.getElementById('detailStartDate').value = task.startAt || '';
    document.getElementById('detailEndDate').value = task.endAt || '';

    const statusObj = statusMap.find(s => s.code === task.status) || statusMap[0];
    document.getElementById('detailStatusBadge').className = 'badge-status ' + statusObj.class;
    document.getElementById('detailStatusText').textContent = statusObj.text;

    renderSubTasksToDetailBox(task.subTasks);
  } catch (err) {
    console.error('상세 패널 로딩 오류:', err);
  }
}

async function refreshSubTasksInDetailPanel(taskId) {
  try {
    const res = await fetch(`/api/projects/${currentProjectId}/tasks/${taskId}`);
    if (!res.ok) throw new Error('하위업무 갱신 실패');

    const task = await res.json();
    renderSubTasksToDetailBox(task.subTasks);
  } catch (err) {
    console.error('하위업무 갱신 오류:', err);
  }
}

function renderSubTasksToDetailBox(subTasks) {
  const subBox = document.getElementById('detailSubtaskBox');
  subBox.innerHTML = '';
  const subCount = subTasks ? subTasks.length : 0;
  document.getElementById('detailSubtaskCount').textContent = subCount;

  if (subTasks && subTasks.length > 0) {
    subTasks.forEach(st => {
      const stStatus = statusMap.find(s => s.code === st.status) || statusMap[0];
      const div = document.createElement('div');
      div.className = 'detail-subtask-row';
      div.innerHTML = `<span class="badge-status ${stStatus.class}">${stStatus.text}</span><span>${st.title}</span>`;
      subBox.appendChild(div);
    });
  } else {
    subBox.innerHTML = '<div style="padding:10px 14px; font-size:12.5px; color:var(--text-muted);">등록된 하위업무가 없습니다.</div>';
  }
}

function closeDetailPanel() {
  document.getElementById('taskDetailSidePanel').classList.remove('active');
  if (currentActiveRow) {
    currentActiveRow.classList.remove('selected-task-row');
    currentActiveRow = null;
  }
}

async function onDetailTitleChange(val) {
  if (!currentActiveTaskId) return;
  try {
    await fetch(`/api/projects/${currentProjectId}/tasks/${currentActiveTaskId}/field`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fieldName: 'title', fieldValue: val })
    });

    if (currentActiveRow) {
      currentActiveRow.querySelector('.task-title-text').textContent = val;
    }
  } catch (err) {
    console.error('상세창 제목 수정 오류:', err);
  }
}

async function onDetailContentChange(val) {
  if (!currentActiveTaskId) return;
  if (!val || !val.trim()) {
    alert('상세 내용은 비워둘 수 없습니다.');
    return;
  }
  try {
    await fetch(`/api/projects/${currentProjectId}/tasks/${currentActiveTaskId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: val.trim() })
    });
  } catch (err) {
    console.error('상세창 내용 저장 오류:', err);
  }
}

async function onDetailDateChange(fieldName, val) {
  if (!currentActiveTaskId) return;
  try {
    await fetch(`/api/projects/${currentProjectId}/tasks/${currentActiveTaskId}/field`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fieldName: fieldName, fieldValue: val })
    });

    if (currentActiveRow) {
      const idx = fieldName === 'startAt' ? 0 : 1;
      const cell = currentActiveRow.querySelectorAll('.date-cell')[idx];
      cell.textContent = val || '-';
    }
  } catch (err) {
    console.error('상세창 날짜 수정 오류:', err);
  }
}

async function cycleDetailStatus(badge) {
  if (!currentActiveTaskId) return;
  const text = document.getElementById('detailStatusText').textContent.trim();
  const idx = statusMap.findIndex(s => s.text === text);
  const next = statusMap[(idx + 1) % statusMap.length];

  badge.className = 'badge-status ' + next.class;
  document.getElementById('detailStatusText').textContent = next.text;

  try {
    await fetch(`/api/projects/${currentProjectId}/tasks/${currentActiveTaskId}/field`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fieldName: 'status', fieldValue: next.code })
    });

    if (currentActiveRow) {
      const rowBadge = currentActiveRow.querySelector('.badge-status');
      rowBadge.className = 'badge-status ' + next.class;
      rowBadge.textContent = next.text;
    }
  } catch (err) {
    console.error('상세창 상태 수정 오류:', err);
  }
}

function toggleExtendedProps(btn) {
  const box = document.getElementById('extendedPropsContainer');
  const icon = document.getElementById('extendPropsIcon');
  if (box.classList.contains('show')) {
    box.classList.remove('show');
    icon.textContent = '＋';
  } else {
    box.classList.add('show');
    icon.textContent = '－';
  }
}

// ================= 4. 삭제 모달 & API 연동 =================
function openDeleteModal(type, targetId, btnElement) {
  const modal = document.getElementById('deleteConfirmModal');
  const title = document.getElementById('modalTargetTitle');
  const notice = document.getElementById('modalDescNotice');

  if (type === 'task') {
    title.textContent = '업무 삭제';
    notice.innerHTML = '선택한 업무와 포함된 하위업무가 모두 영구 삭제됩니다.';
  } else if (type === 'subtask') {
    title.textContent = '하위업무 삭제';
    notice.innerHTML = '선택한 하위업무를 영구 삭제하시겠습니까?';
  }

  pendingDeleteContext = { type, targetId, btnElement };
  modal.classList.add('show');
}

function closeDeleteModal() {
  document.getElementById('deleteConfirmModal').classList.remove('show');
  pendingDeleteContext = null;
}

function handleBackdropClick(e) {
  if (e.target === document.getElementById('deleteConfirmModal')) closeDeleteModal();
}

async function executeConfirmedDelete() {
  if (!pendingDeleteContext) return;
  const { type, targetId, btnElement } = pendingDeleteContext;

  const url = type === 'task'
    ? `/api/projects/${currentProjectId}/tasks/${targetId}`
    : `/api/projects/${currentProjectId}/tasks/subtasks/${targetId}`;

  try {
    const res = await fetch(url, { method: 'DELETE' });
    if (!res.ok) throw new Error('삭제 요청 실패');

    await res.json();

    if (type === 'task') {
      const li = document.querySelector(`[data-task-id="${targetId}"]`);
      if (li) li.remove();
      if (currentActiveTaskId === targetId) closeDetailPanel();
    } else if (type === 'subtask') {
      const subLi = btnElement.closest('.task-item-row.sub-task');
      const mainBranch = subLi.closest('.task-branch');
      if (subLi) subLi.remove();
      if (mainBranch) {
        const countSpan = mainBranch.querySelector('.sub-count');
        if (countSpan) {
          const cnt = Math.max(0, parseInt(countSpan.textContent || '0') - 1);
          countSpan.textContent = cnt;
        }
      }
      if (currentActiveTaskId) await refreshSubTasksInDetailPanel(currentActiveTaskId);
    }
    closeDeleteModal();
  } catch (err) {
    console.error('삭제 처리 오류:', err);
  }
}

// ================= 5. 너비 조절 리사이저 바 & 부가 기능 =================
document.addEventListener('DOMContentLoaded', function() {
  const resizerBar = document.getElementById('paneResizerBar');
  const sidePanel = document.getElementById('taskDetailSidePanel');
  const splitWrapper = document.getElementById('taskSplitViewWrapper');
  let isDragging = false;

  if (resizerBar && sidePanel && splitWrapper) {
    resizerBar.addEventListener('mousedown', () => { 
      isDragging = true; 
      document.body.style.cursor = 'col-resize'; 
    });
    document.addEventListener('mousemove', e => {
      if (!isDragging) return;
      let newWidth = splitWrapper.getBoundingClientRect().right - e.clientX;
      if (newWidth < 380) newWidth = 380;
      if (newWidth > 860) newWidth = 860;
      sidePanel.style.width = newWidth + 'px';
    });
    document.addEventListener('mouseup', () => { 
      if (isDragging) { 
        isDragging = false; 
        document.body.style.cursor = ''; 
      } 
    });
  }

  document.addEventListener('keydown', e => { 
    if (e.key === 'Escape') { 
      closeCreateTaskModal();
      closeDeleteModal(); 
      closeDetailPanel(); 
    } 
  });
});

function toggleSubTasks(btn) {
  const subList = btn.closest('.task-branch').querySelector('.sub-task-list');
  if (subList.style.display === 'none') { 
    subList.style.display = ''; 
    btn.textContent = '－'; 
  } else { 
    subList.style.display = 'none'; 
    btn.textContent = '＋'; 
  }
}

function handleFileUpload(input) {
  if (!input.files || input.files.length === 0) return;
  const file = input.files[0];
  const fileList = document.getElementById('attachedFileList');
  const div = document.createElement('div');
  div.className = 'attached-file-item';
  div.innerHTML = `
    <div class="attached-file-info">
      <span>📁</span>
      <span style="font-weight:600;">${file.name}</span>
      <span style="color:var(--text-muted); font-size:11px;">(${(file.size / (1024 * 1024)).toFixed(1)} MB)</span>
    </div>
    <button type="button" class="btn-del-file" onclick="this.closest('.attached-file-item').remove()" title="삭제">✕</button>
  `;
  fileList.appendChild(div);
  document.getElementById('attachedFileCount').textContent = `(${fileList.children.length})`;
}
