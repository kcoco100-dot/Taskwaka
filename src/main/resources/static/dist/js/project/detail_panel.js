/* ==========================================================================
   [JS] 공통 우측 오버레이 상세 패널 제어 (메인 업무 조회 + 하위업무 실시간 수정 + 일일보고서 CRUD)
   ========================================================================== */
const panelStatusMap = [
  { code: 0, text: '● 요청', class: 'badge-req' },
  { code: 1, text: '● 진행', class: 'badge-done' },
  { code: 2, text: '● 피드백', class: 'badge-feedback' },
  { code: 3, text: '● 완료', class: 'badge-done' },
  { code: 4, text: '● 보류', class: 'badge-hold' }
];

let activePanelContext = {
  type: null,
  id: null,
  parentTaskId: null,
  assigneeId: null,
  rowElem: null
};

function getPanelApiBaseUrl() {
  return `/${currentDomainKey}/project/${currentProjectId}/api/tasks`;
}

// 1. 메인 업무 클릭 시 상세 패널 오픈 (프로젝트 계층에서는 메인 업무 조회 전용)
async function openDetailPanel(taskId, rowElem) {
  await openCommonDetailPanel('task', taskId, rowElem);
}

// 2. 하위 업무 클릭 시 상세 패널 오픈 (하위업무 실시간 수정 + 담당자 일일보고서)
async function openSubTaskDetailPanel(subTaskId, rowElem) {
  await openCommonDetailPanel('subtask', subTaskId, rowElem);
}

async function openCommonDetailPanel(type, id, rowElem) {
  document.querySelectorAll('.task-item-row, .gantt-tree-row, .gantt-grid-row').forEach(r => {
    r.classList.remove('selected-task-row', 'selected-gantt-row');
  });
  if (rowElem) {
    rowElem.classList.add('selected-task-row');
  }

  activePanelContext = {
    type: type,
    id: Number(id),
    parentTaskId: null,
    assigneeId: null,
    rowElem: rowElem
  };

  // 클릭 즉시 패널 오픈
  const sidePanel = document.getElementById('taskDetailSidePanel');
  if (sidePanel) sidePanel.classList.add('active');

  const typeBadge = document.getElementById('detailEntityTypeBadge');
  const readonlyBadge = document.getElementById('detailReadonlyBadge');
  const btnDeleteSub = document.getElementById('btnDetailDeleteSubtask');
  const subtasksSection = document.getElementById('detailSubtasksSection');
  const dailyReportSection = document.getElementById('detailDailyReportSection');

  const isMainTask = (type === 'task');
  typeBadge.textContent = isMainTask ? '메인 업무' : '하위 업무';

  readonlyBadge.style.display = isMainTask ? 'inline-block' : 'none';
  btnDeleteSub.style.display = isMainTask ? 'none' : 'inline-block';
  subtasksSection.style.display = isMainTask ? 'flex' : 'none';
  dailyReportSection.style.display = isMainTask ? 'none' : 'flex';

  setDetailInputsDisabled(isMainTask);

  try {
    const baseUrl = getPanelApiBaseUrl();
    const url = isMainTask ? `${baseUrl}/${id}` : `${baseUrl}/subtasks/${id}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error(`상세 조회 실패 (${res.status})`);

    const data = await res.json();
    activePanelContext.assigneeId = Number(data.projectMemberId);
    activePanelContext.parentTaskId = Number(data.taskId);

    bindCommonDetailFields(data, isMainTask);

    if (isMainTask) {
      // 상위 메인 업무의 담당자 본인일 때만 [+ 하위업무 추가] 버튼 노출
      const btnAddSub = document.getElementById('btnDetailAddSubtask');
      if (btnAddSub) {
        const isTaskOwner = Number(currentProjectMemberId) === Number(data.projectMemberId);
        btnAddSub.style.display = isTaskOwner ? 'inline-flex' : 'none';
      }
      renderSubTasksToDetailBox(data.subTasks || []);
    } else {
      // 하위업무인 경우: 일일보고서 목록 로드 & 담당자 본인에게만 보고서 작성창 노출
      const writeBox = document.getElementById('dailyReportWriteFormBox');
      const isSubTaskAssignee = Number(currentProjectMemberId) === Number(data.projectMemberId);
      if (writeBox) {
        writeBox.style.display = isSubTaskAssignee ? 'flex' : 'none';
      }
      resetDailyReportForm();
      await loadDailyReportsForSubTask(id);
    }
  } catch (err) {
    console.error('상세 패널 로딩 오류:', err);
  }
}

function setDetailInputsDisabled(disabled) {
  document.getElementById('detailTaskTitle').disabled = disabled;
  document.getElementById('detailAssigneeSelect').disabled = disabled;
  document.getElementById('detailStartDate').disabled = disabled;
  document.getElementById('detailEndDate').disabled = disabled;
  document.getElementById('detailProgressRange').disabled = disabled;
  document.getElementById('detailContentText').disabled = disabled;
  document.getElementById('detailStatusBadge').style.pointerEvents = disabled ? 'none' : 'auto';
}

function bindCommonDetailFields(data, isMainTask) {
  const assigneeName = data.assigneeName || ('담당자#' + (data.projectMemberId || ''));
  document.getElementById('detailAuthorName').textContent = assigneeName;
  document.getElementById('detailAuthorAvatar').textContent = assigneeName.length >= 2 ? assigneeName.slice(-2) : assigneeName;
  document.getElementById('detailCreatedDate').textContent = data.created || '-';

  document.getElementById('detailTaskTitle').value = data.title || '';
  document.getElementById('detailContentText').value = data.content || '';
  document.getElementById('detailStartDate').value = data.startAt || '';
  document.getElementById('detailEndDate').value = data.endAt || '';

  const prog = data.progress != null ? Math.round(data.progress) : 0;
  document.getElementById('detailProgressRange').value = prog;
  document.getElementById('detailProgressLabel').textContent = prog + '%';

  // 참여 인원(task_members / sub_task_members) 드롭다운 옵션 바인딩
  const select = document.getElementById('detailAssigneeSelect');
  select.innerHTML = '';
  const members = data.participants || [];
  if (members.length > 0) {
    members.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.projectMemberId;
      opt.textContent = m.memberName;
      if (Number(m.projectMemberId) === Number(data.projectMemberId)) opt.selected = true;
      select.appendChild(opt);
    });
  } else {
    const opt = document.createElement('option');
    opt.value = data.projectMemberId || 1;
    opt.textContent = assigneeName;
    opt.selected = true;
    select.appendChild(opt);
  }

  const statusObj = panelStatusMap.find(s => s.code === Number(data.status)) || panelStatusMap[0];
  document.getElementById('detailStatusBadge').className = 'badge-status ' + statusObj.class;
  document.getElementById('detailStatusText').textContent = statusObj.text;
}

function renderSubTasksToDetailBox(subTasks) {
  const subBox = document.getElementById('detailSubtaskBox');
  subBox.innerHTML = '';
  document.getElementById('detailSubtaskCount').textContent = subTasks.length;

  if (subTasks.length === 0) {
    subBox.innerHTML = '<div style="padding:12px 14px; font-size:12.5px; color:var(--text-muted);">등록된 하위업무가 없습니다.</div>';
    return;
  }

  subTasks.forEach(st => {
    const stStatus = panelStatusMap.find(s => s.code === st.status) || panelStatusMap[0];
    const div = document.createElement('div');
    div.className = 'detail-subtask-row';
    div.onclick = () => {
      const targetRow = document.querySelector(`.task-item-row.sub-task[data-subtask-id="${st.subTaskId}"]`);
      openSubTaskDetailPanel(st.subTaskId, targetRow);
    };
    div.innerHTML = `
      <div style="display:flex; align-items:center; gap:8px; overflow:hidden;">
        <span class="badge-status ${stStatus.class}">${stStatus.text}</span>
        <span style="font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${st.title}</span>
      </div>
      <span style="font-size:11.5px; color:var(--text-sub); flex-shrink:0;">${st.assigneeName || ''} · ${Math.round(st.progress || 0)}%</span>
    `;
    subBox.appendChild(div);
  });
}

function closeDetailPanel() {
  const panel = document.getElementById('taskDetailSidePanel');
  if (panel) panel.classList.remove('active');
  document.querySelectorAll('.task-item-row, .gantt-tree-row, .gantt-grid-row').forEach(r => {
    r.classList.remove('selected-task-row', 'selected-gantt-row');
  });
  activePanelContext = { type: null, id: null, parentTaskId: null, assigneeId: null, rowElem: null };
}

// 3. 우측 패널 실시간 수정 연동 (하위업무 전용)
async function onDetailFieldChange(fieldName, fieldValue) {
  if (!activePanelContext.id || activePanelContext.type !== 'subtask') return;
  const subTaskId = activePanelContext.id;

  try {
    const res = await fetch(`${getPanelApiBaseUrl()}/subtasks/${subTaskId}/field`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fieldName, fieldValue })
    });
    if (!res.ok) throw new Error('필드 수정 실패');

    if (fieldName === 'projectMemberId') {
      activePanelContext.assigneeId = Number(fieldValue);
      const writeBox = document.getElementById('dailyReportWriteFormBox');
      if (writeBox) {
        writeBox.style.display = (Number(currentProjectMemberId) === Number(fieldValue)) ? 'flex' : 'none';
      }
    }

    syncRowDomAfterUpdate(subTaskId, fieldName, fieldValue);
  } catch (err) {
    console.error('하위업무 실시간 수정 오류:', err);
  }
}

async function cycleDetailStatus() {
  if (!activePanelContext.id || activePanelContext.type !== 'subtask') return;
  const textSpan = document.getElementById('detailStatusText');
  const badge = document.getElementById('detailStatusBadge');
  const idx = panelStatusMap.findIndex(s => s.text === textSpan.textContent.trim());
  const next = panelStatusMap[(idx + 1) % panelStatusMap.length];

  badge.className = 'badge-status ' + next.class;
  textSpan.textContent = next.text;

  await onDetailFieldChange('status', next.code);
}

async function onDetailContentChange(val) {
  if (!activePanelContext.id || activePanelContext.type !== 'subtask') return;
  if (!val || !val.trim()) {
    alert('상세 내용은 비워둘 수 없습니다.');
    return;
  }
  try {
    await fetch(`${getPanelApiBaseUrl()}/subtasks/${activePanelContext.id}/field`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fieldName: 'content', fieldValue: val.trim() })
    });
  } catch (err) {
    console.error('상세 내용 저장 오류:', err);
  }
}

function syncRowDomAfterUpdate(subTaskId, fieldName, fieldValue) {
  const row = document.querySelector(`.task-item-row.sub-task[data-subtask-id="${subTaskId}"]`);
  if (!row) return;

  if (fieldName === 'title') {
    const t = row.querySelector('.task-title-text');
    if (t) t.textContent = fieldValue;
  } else if (fieldName === 'status') {
    const stObj = panelStatusMap.find(s => s.code === Number(fieldValue)) || panelStatusMap[0];
    const b = row.querySelector('.badge-status');
    if (b) { b.className = 'badge-status ' + stObj.class; b.textContent = stObj.text; }
  } else if (fieldName === 'projectMemberId') {
    const sel = document.getElementById('detailAssigneeSelect');
    const selectedName = sel.options[sel.selectedIndex]?.text || fieldValue;
    const assigneeCell = row.querySelector('.col-assignee');
    if (assigneeCell) assigneeCell.textContent = selectedName;
    document.getElementById('detailAuthorName').textContent = selectedName;
  } else if (fieldName === 'startAt') {
    const cell = row.querySelectorAll('.date-cell')[0];
    if (cell) cell.textContent = fieldValue || '-';
  } else if (fieldName === 'endAt') {
    const cell = row.querySelectorAll('.date-cell')[1];
    if (cell) cell.textContent = fieldValue || '-';
  } else if (fieldName === 'progress') {
    const fill = row.querySelector('.progress-bar-fill');
    const txt = row.querySelector('.progress-text');
    if (fill) fill.style.width = fieldValue + '%';
    if (txt) txt.textContent = fieldValue + '%';
  }
}

function requestDeleteCurrentSubTask() {
  if (activePanelContext.type === 'subtask' && activePanelContext.id) {
    const row = document.querySelector(`.task-item-row.sub-task[data-subtask-id="${activePanelContext.id}"]`);
    if (typeof openDeleteModal === 'function') {
      openDeleteModal('subtask', activePanelContext.id, row);
    }
  }
}

function detailAddSubtaskClick() {
  if (activePanelContext.type === 'task' && activePanelContext.id) {
    if (typeof requestCreateSubTask === 'function') {
      requestCreateSubTask(activePanelContext.id);
    }
  }
}

// 4. 일일 보고서 (Daily Report) CRUD 로직
function resetDailyReportForm() {
  document.getElementById('editingDailyReportId').value = '';
  document.getElementById('dailyReportFormTitle').textContent = '새 일일 보고서 작성';
  document.getElementById('btnSubmitDailyReport').textContent = '보고서 등록';
  document.getElementById('btnCancelReportEdit').style.display = 'none';
  document.getElementById('dailyReportDateInput').value = new Date().toISOString().split('T')[0];
  document.getElementById('dailyReportProgressInput').value = document.getElementById('detailProgressRange').value || 0;
  document.getElementById('dailyReportContentInput').value = '';
}

async function loadDailyReportsForSubTask(subTaskId) {
  const listBox = document.getElementById('dailyReportListBox');
  const countSpan = document.getElementById('dailyReportCount');
  listBox.innerHTML = '';

  try {
    const res = await fetch(`${getPanelApiBaseUrl()}/subtasks/${subTaskId}/daily-reports`);
    if (!res.ok) throw new Error('보고서 목록 조회 실패');
    const reports = await res.json();

    countSpan.textContent = `(${reports.length})`;
    if (reports.length === 0) {
      listBox.innerHTML = '<div style="padding:12px; font-size:12.5px; color:var(--text-muted); text-align:center;">작성된 일일 보고서가 없습니다.</div>';
      return;
    }

    const isOwner = Number(currentProjectMemberId) === Number(activePanelContext.assigneeId);

    reports.forEach(r => {
      const card = document.createElement('div');
      card.className = 'daily-report-item-card';

      const canModify = isOwner && r.modifiable;
      const reviewBadgeHtml = r.modifiable
        ? '<span class="report-review-badge review-pending">상급자 확인 전</span>'
        : '<span class="report-review-badge review-confirmed">상급자 확인 완료 (수정불가)</span>';

      const actionBtnsHtml = canModify ? `
        <div class="report-action-btns">
          <button type="button" class="btn-report-sm" onclick="startEditDailyReport(${r.dailyReportId}, '${r.reportDate}', ${r.progress}, this)" data-content="${escapeHtmlAttr(r.content)}">수정</button>
          <button type="button" class="btn-report-sm danger" onclick="deleteDailyReportItem(${r.dailyReportId})">삭제</button>
        </div>
      ` : '';

      card.innerHTML = `
        <div class="daily-report-item-top">
          <div class="report-meta-left">
            <span>📅 ${r.reportDate}</span>
            <span style="color:var(--primary-green);">진척도 ${Math.round(r.progress || 0)}%</span>
            ${reviewBadgeHtml}
          </div>
          ${actionBtnsHtml}
        </div>
        <div class="daily-report-item-body">${escapeHtmlText(r.content || '')}</div>
      `;
      listBox.appendChild(card);
    });
  } catch (err) {
    console.error('일일 보고서 조회 오류:', err);
  }
}

function startEditDailyReport(reportId, reportDate, progress, btnElem) {
  document.getElementById('editingDailyReportId').value = reportId;
  document.getElementById('dailyReportFormTitle').textContent = '일일 보고서 수정 (상급자 확인 전)';
  document.getElementById('btnSubmitDailyReport').textContent = '수정 저장';
  document.getElementById('btnCancelReportEdit').style.display = 'inline-block';

  document.getElementById('dailyReportDateInput').value = reportDate;
  document.getElementById('dailyReportProgressInput').value = Math.round(progress || 0);
  document.getElementById('dailyReportContentInput').value = btnElem.getAttribute('data-content') || '';
  document.getElementById('dailyReportContentInput').focus();
}

async function submitDailyReportForm() {
  if (activePanelContext.type !== 'subtask' || !activePanelContext.id) return;
  const subTaskId = activePanelContext.id;
  const editingId = document.getElementById('editingDailyReportId').value;
  const reportDate = document.getElementById('dailyReportDateInput').value;
  const progress = Number(document.getElementById('dailyReportProgressInput').value || 0);
  const content = document.getElementById('dailyReportContentInput').value.trim();

  if (!content) {
    alert('일일 보고서 내용을 입력해 주세요.');
    document.getElementById('dailyReportContentInput').focus();
    return;
  }

  try {
    const baseUrl = getPanelApiBaseUrl();
    if (!editingId) {
      const res = await fetch(`${baseUrl}/subtasks/${subTaskId}/daily-reports`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subTaskId: subTaskId,
          projectMemberId: currentProjectMemberId,
          reportDate: reportDate,
          progress: progress,
          content: content
        })
      });
      if (!res.ok) throw new Error('보고서 등록 실패');
    } else {
      const res = await fetch(`${baseUrl}/daily-reports/${editingId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          dailyReportId: Number(editingId),
          subTaskId: subTaskId,
          reportDate: reportDate,
          progress: progress,
          content: content
        })
      });
      const result = await res.json();
      if (!result.success) {
        alert(result.message || '수정할 수 없는 보고서입니다.');
        return;
      }
    }

    document.getElementById('detailProgressRange').value = progress;
    document.getElementById('detailProgressLabel').textContent = progress + '%';
    syncRowDomAfterUpdate(subTaskId, 'progress', progress);

    resetDailyReportForm();
    await loadDailyReportsForSubTask(subTaskId);
  } catch (err) {
    console.error('일일 보고서 저장 오류:', err);
  }
}

async function deleteDailyReportItem(dailyReportId) {
  if (!confirm('이 일일 보고서를 삭제하시겠습니까? (상급자 확인 전까지만 삭제 가능)')) return;
  try {
    const res = await fetch(`${getPanelApiBaseUrl()}/daily-reports/${dailyReportId}`, {
      method: 'DELETE'
    });
    const result = await res.json();
    if (!result.success) {
      alert(result.message || '상급자가 이미 확인한 보고서는 삭제할 수 없습니다.');
      return;
    }
    await loadDailyReportsForSubTask(activePanelContext.id);
  } catch (err) {
    console.error('일일 보고서 삭제 오류:', err);
  }
}

function escapeHtmlText(str) {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function escapeHtmlAttr(str) {
  return (str || '').replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// 5. 공통 패널 리사이저 바 초기화
document.addEventListener('DOMContentLoaded', function() {
  const resizerBar = document.getElementById('paneResizerBar');
  const sidePanel = document.getElementById('taskDetailSidePanel');
  let isDragging = false;

  if (resizerBar && sidePanel) {
    resizerBar.addEventListener('mousedown', () => {
      isDragging = true;
      document.body.style.cursor = 'col-resize';
    });
    document.addEventListener('mousemove', e => {
      if (!isDragging) return;
      const parentRect = sidePanel.parentElement.getBoundingClientRect();
      let newWidth = parentRect.right - e.clientX;
      if (newWidth < 390) newWidth = 390;
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
});

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