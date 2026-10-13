/* ==========================================================================
   [JS] 단일 업무 목록 기반 비동기 통신 로직
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

// ================= 1. 메인 업무 생성 (Thymeleaf 프래그먼트 수신 후 삽입) =================
function requestCreateMainTask() {
  const title = prompt('새 업무명을 입력하세요:', '새 업무');
  if (!title) return;

  const taskList = document.getElementById('mainTaskList');
  const formData = new URLSearchParams();
  formData.append('title', title);
  formData.append('taskLevel', 1);

  fetch(`/${currentDomainKey}/project/${currentProjectId}/tasks/fragment/create-main`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: formData
  })
  .then(res => res.text()) // Thymeleaf 프래그먼트 HTML 조각 수신
  .then(htmlFragment => {
    taskList.insertAdjacentHTML('beforeend', htmlFragment);
  })
  .catch(err => console.error('업무 생성 오류:', err));
}

// ================= 2. 하위 업무 생성 (Thymeleaf 프래그먼트 수신 후 삽입) =================
function requestCreateSubTask(taskId) {
  const title = prompt('추가할 하위업무명을 입력하세요:', '새 하위업무');
  if (!title) return;

  const branchLi = document.querySelector(`[data-task-id="${taskId}"]`);
  const subTaskList = branchLi.querySelector('.sub-task-list');

  const formData = new URLSearchParams();
  formData.append('title', title);

  fetch(`/${currentDomainKey}/project/${currentProjectId}/tasks/${taskId}/fragment/create-sub`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: formData
  })
  .then(res => res.text()) // Thymeleaf 프래그먼트 HTML 조각 수신
  .then(htmlFragment => {
    subTaskList.insertAdjacentHTML('beforeend', htmlFragment);

    // 토글 버튼 및 하위 개수 카운트 갱신
    let countSpan = branchLi.querySelector('.sub-count');
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

    if (currentActiveTaskId === taskId) {
      refreshSubTasksInDetailPanel(taskId);
    }
  })
  .catch(err => console.error('하위업무 생성 오류:', err));
}

function detailAddSubtaskClick() {
  if (currentActiveTaskId) requestCreateSubTask(currentActiveTaskId);
}

// ================= 3. 인라인 즉시 수정 (TaskApiController PATCH 연동) =================
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

  function finish() {
    const newText = input.value.trim() || oldText;
    cell.textContent = newText;
    if (newText !== oldText) {
      const url = isSub 
        ? `/api/projects/${currentProjectId}/tasks/subtasks/${id}/field`
        : `/api/projects/${currentProjectId}/tasks/${id}/field`;
      fetch(url, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fieldName: 'title', fieldValue: newText })
      }).then(() => {
        if (currentActiveTaskId === id) document.getElementById('detailTaskTitle').value = newText;
      });
    }
  }
  input.onblur = finish;
  input.onkeydown = e => { if (e.key === 'Enter') finish(); else if (e.key === 'Escape') cell.textContent = oldText; };
}

function cycleStatus(badge, id, isSub) {
  const text = badge.textContent.trim();
  let idx = statusMap.findIndex(s => s.text === text);
  const next = statusMap[(idx + 1) % statusMap.length];

  badge.className = 'badge-status ' + next.class;
  badge.textContent = next.text;

  const url = isSub 
    ? `/api/projects/${currentProjectId}/tasks/subtasks/${id}/field`
    : `/api/projects/${currentProjectId}/tasks/${id}/field`;
  fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fieldName: 'status', fieldValue: next.code })
  }).then(() => {
    if (currentActiveTaskId === id) {
      document.getElementById('detailStatusBadge').className = 'badge-status ' + next.class;
      document.getElementById('detailStatusText').textContent = next.text;
    }
  });
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

  function finish() {
    const picked = input.value || '';
    cell.textContent = picked || '-';
    if (fieldName === 'endAt' && picked) cell.classList.add('text-danger');
    else if (fieldName === 'endAt') cell.classList.remove('text-danger');

    const url = isSub 
      ? `/api/projects/${currentProjectId}/tasks/subtasks/${id}/field`
      : `/api/projects/${currentProjectId}/tasks/${id}/field`;
    fetch(url, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fieldName: fieldName, fieldValue: picked })
    });
  }
  input.onblur = finish;
  input.onchange = finish;
}

// ================= 4. 상세 패널 데이터 바인딩 =================
function openDetailPanel(taskId, rowElem) {
  document.querySelectorAll('.task-item-row').forEach(r => r.classList.remove('selected-task-row'));
  rowElem.classList.add('selected-task-row');

  currentActiveRow = rowElem;
  currentActiveTaskId = taskId;

  fetch(`/api/projects/${currentProjectId}/tasks/${taskId}`)
    .then(res => res.json())
    .then(task => {
      document.getElementById('taskDetailSidePanel').classList.add('active');
      document.getElementById('detailTaskTitle').value = task.title || '';
      document.getElementById('detailCreatedDate').textContent = task.created || '';
      document.getElementById('detailContentText').value = task.content || '';
      document.getElementById('detailStartDate').value = task.startAt || '';
      document.getElementById('detailEndDate').value = task.endAt || '';

      const statusObj = statusMap.find(s => s.code === task.status) || statusMap[0];
      document.getElementById('detailStatusBadge').className = 'badge-status ' + statusObj.class;
      document.getElementById('detailStatusText').textContent = statusObj.text;

      refreshSubTasksInDetailPanel(taskId);
    });
}

function refreshSubTasksInDetailPanel(taskId) {
  fetch(`/api/projects/${currentProjectId}/tasks/${taskId}`)
    .then(res => res.json())
    .then(task => {
      const subBox = document.getElementById('detailSubtaskBox');
      subBox.innerHTML = '';
      const subCount = task.subTasks ? task.subTasks.length : 0;
      document.getElementById('detailSubtaskCount').textContent = subCount;

      if (task.subTasks && task.subTasks.length > 0) {
        task.subTasks.forEach(st => {
          const stStatus = statusMap.find(s => s.code === st.status) || statusMap[0];
          const div = document.createElement('div');
          div.className = 'detail-subtask-row';
          div.innerHTML = `<span class="badge-status ${stStatus.class}">${stStatus.text}</span><span>${st.title}</span>`;
          subBox.appendChild(div);
        });
      } else {
        subBox.innerHTML = '<div style="padding:10px 14px; font-size:12.5px; color:var(--text-muted);">등록된 하위업무가 없습니다.</div>';
      }
    });
}

function closeDetailPanel() {
  document.getElementById('taskDetailSidePanel').classList.remove('active');
  if (currentActiveRow) {
    currentActiveRow.classList.remove('selected-task-row');
    currentActiveRow = null;
  }
}

function onDetailTitleChange(val) {
  if (!currentActiveTaskId) return;
  fetch(`/api/projects/${currentProjectId}/tasks/${currentActiveTaskId}/field`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fieldName: 'title', fieldValue: val })
  }).then(() => {
    if (currentActiveRow) {
      currentActiveRow.querySelector('.task-title-text').textContent = val;
    }
  });
}

function onDetailContentChange(val) {
  if (!currentActiveTaskId) return;
  fetch(`/api/projects/${currentProjectId}/tasks/${currentActiveTaskId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content: val })
  });
}

function onDetailDateChange(fieldName, val) {
  if (!currentActiveTaskId) return;
  fetch(`/api/projects/${currentProjectId}/tasks/${currentActiveTaskId}/field`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fieldName: fieldName, fieldValue: val })
  }).then(() => {
    if (currentActiveRow) {
      const idx = fieldName === 'startAt' ? 0 : 1;
      const cell = currentActiveRow.querySelectorAll('.date-cell')[idx];
      cell.textContent = val || '-';
    }
  });
}

function cycleDetailStatus(badge) {
  if (!currentActiveTaskId) return;
  const text = document.getElementById('detailStatusText').textContent.trim();
  let idx = statusMap.findIndex(s => s.text === text);
  const next = statusMap[(idx + 1) % statusMap.length];

  badge.className = 'badge-status ' + next.class;
  document.getElementById('detailStatusText').textContent = next.text;

  fetch(`/api/projects/${currentProjectId}/tasks/${currentActiveTaskId}/field`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fieldName: 'status', fieldValue: next.code })
  }).then(() => {
    if (currentActiveRow) {
      const rowBadge = currentActiveRow.querySelector('.badge-status');
      rowBadge.className = 'badge-status ' + next.class;
      rowBadge.textContent = next.text;
    }
  });
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

// ================= 5. 삭제 모달 & API 연동 =================
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

function executeConfirmedDelete() {
  if (!pendingDeleteContext) return;
  const { type, targetId, btnElement } = pendingDeleteContext;

  const url = type === 'task'
    ? `/api/projects/${currentProjectId}/tasks/${targetId}`
    : `/api/projects/${currentProjectId}/tasks/subtasks/${targetId}`;

  fetch(url, { method: 'DELETE' })
    .then(res => res.json())
    .then(() => {
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
        if (currentActiveTaskId) refreshSubTasksInDetailPanel(currentActiveTaskId);
      }
      closeDeleteModal();
    });
}

// ================= 6. 너비 조절 리사이저 바 & 부가 기능 =================
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
