/* ==========================================================================
   [JS] 업무 목록 탭 스크립트 (인라인 수정 제거 + 하위업무 전용 생성/삭제 + task_members 연동)
   ========================================================================== */
let pendingDeleteContext = null;

let createSubModalContext = null;

// 1. 하위업무 등록 모달 오픈 (상위 업무의 task_members 동적 로드)
async function requestCreateSubTask(taskId) {
  createSubModalContext = {
    parentTaskId: taskId,
    statusCode: 0
  };

  const modal = document.getElementById('taskCreateModal');
  const titleInput = document.getElementById('createTaskTitleInput');
  const contentInput = document.getElementById('createTaskContentInput');
  const startDateInput = document.getElementById('createStartDateInput');
  const endDateInput = document.getElementById('createEndDateInput');
  const statusBadge = document.getElementById('createStatusBadge');
  const statusText = document.getElementById('createStatusText');

  titleInput.value = '';
  contentInput.value = '';

  const today = new Date();
  const nextWeek = new Date();
  nextWeek.setDate(today.getDate() + 7);
  startDateInput.value = today.toISOString().split('T')[0];
  endDateInput.value = nextWeek.toISOString().split('T')[0];

  statusBadge.className = 'badge-status badge-req';
  statusText.textContent = '● 예정';

  modal.classList.add('show');
  titleInput.focus();
  await loadSubTaskAssignees();
}

async function loadSubTaskAssignees() {
  const context = createSubModalContext;
  if (!context) return;
  const select = document.getElementById('createAssigneeSelect');
  const submit = document.getElementById('createSubTaskSubmit');
  const retry = document.getElementById('retryCreateAssignees');
  select.disabled = submit.disabled = true;
  retry.hidden = true;
  select.replaceChildren(new Option('불러오는 중...', ''));
  try {
    const res = await fetch(`${getTaskApiBaseUrl()}/${context.parentTaskId}/members`);
    if (!res.ok) throw new Error('담당자 조회 실패');
    const members = await res.json();
    if (context !== createSubModalContext) return;
    if (!members.length) {
      select.replaceChildren(new Option('배정 가능한 담당자가 없습니다.', ''));
      retry.hidden = false;
      return;
    }
    select.replaceChildren(...members.map(m => new Option(m.memberName, m.projectMemberId)));
    select.disabled = submit.disabled = false;
  } catch (err) {
    if (context !== createSubModalContext) return;
    console.error('참여 인원 조회 실패:', err);
    select.replaceChildren(new Option('담당자를 불러오지 못했습니다.', ''));
    retry.hidden = false;
  }
}

function closeCreateTaskModal() {
  createSubModalContext = null;
  const modal = document.getElementById('taskCreateModal');
  if (modal) modal.classList.remove('show');
}

function cycleCreateModalStatus() {
  createSubModalContext.statusCode = (createSubModalContext.statusCode + 1) % Object.keys(DETAIL_STATUS_MAP).length;
  const next = DETAIL_STATUS_MAP[createSubModalContext.statusCode];
  document.getElementById('createStatusBadge').className = 'badge-status ' + next.cls;
  document.getElementById('createStatusText').textContent = next.text;
}

async function submitCreateSubTaskModal() {
  const taskId = createSubModalContext?.parentTaskId;
  if (!taskId) return;

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

  if (!title) { alert('하위업무 제목을 입력해 주세요.'); titleInput.focus(); return; }
  if (assigneeSelect.disabled || !projectMemberId) { alert('하위업무 담당자를 선택해 주세요.'); assigneeSelect.focus(); return; }
  if (!content) { alert('상세 내용을 입력해 주세요.'); contentInput.focus(); return; }

  const formData = new URLSearchParams();
  formData.append('title', title);
  formData.append('content', content);
  formData.append('projectMemberId', projectMemberId);
  formData.append('status', createSubModalContext.statusCode);
  if (startAt) formData.append('startAt', startAt);
  if (endAt) formData.append('endAt', endAt);

  try {
    const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/tasks/${taskId}/fragment/create-sub`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: formData
    });

    if (!res.ok) throw new Error('하위 업무 생성 요청 실패');

    const htmlFragment = await res.text();
    if (!document.getElementById('taskTreeMainPage')) {
      location.reload();
      return;
    }
    const branchLi = document.querySelector(`.task-branch[data-task-id="${taskId}"]`);
    if (branchLi) {
      const subTaskList = branchLi.querySelector('.sub-task-list');
      subTaskList.insertAdjacentHTML('beforeend', htmlFragment);

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
    closeCreateTaskModal();
    if (currentEntityType === 'TASK' && currentOpenTaskId === Number(taskId)) {
      await openDetailPanel(taskId, currentDetailRowElem);
    }
  } catch (err) {
    console.error('하위업무 등록 오류:', err);
    alert('하위업무 등록 중 오류가 발생했습니다.');
  }
}

// 2. 하위업무 삭제 모달 & API 연동
function openDeleteModal(type, targetId, btnElement) {
  const modal = document.getElementById('deleteConfirmModal');
  pendingDeleteContext = { type, targetId, btnElement };
  modal.classList.add('show');
}

function closeDeleteModal() {
  const modal = document.getElementById('deleteConfirmModal');
  if (modal) modal.classList.remove('show');
  pendingDeleteContext = null;
}

function handleBackdropClick(e) {
  if (e.target === e.currentTarget) e.stopPropagation();
}

async function executeConfirmedDelete() {
  if (!pendingDeleteContext) return;
  const { targetId, btnElement } = pendingDeleteContext;

  try {
    const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/api/tasks/subtasks/${targetId}`, {
      method: 'DELETE'
    });
    if (!res.ok) throw new Error('하위업무 삭제 실패');
    await res.json();

    if (!document.getElementById('taskTreeMainPage')) {
      location.reload();
      return;
    }
    const subLi = btnElement
      ? btnElement.closest('.task-item-row.sub-task')
      : document.querySelector(`.task-item-row.sub-task[data-subtask-id="${targetId}"]`);

    if (subLi) {
      const mainBranch = subLi.closest('.task-branch');
      subLi.remove();
      if (mainBranch) {
        const countSpan = mainBranch.querySelector('.sub-count');
        if (countSpan) {
          countSpan.textContent = Math.max(0, parseInt(countSpan.textContent || '0') - 1);
        }
      }
    }

      closeDetailPanel();

    closeDeleteModal();
  } catch (err) {
    console.error('하위업무 삭제 오류:', err);
    alert('하위업무를 삭제하지 못했습니다.');
  }
}

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

// 3. 보고서 등록 모달 오픈
async function requestCreateReport(taskId, isTask, progress = 0) {
	
  const modal = document.getElementById('reportCreateModal');
  const titleInput = document.getElementById('createReportTitleInput');
  const dateInput = document.getElementById('createReportDateInput');
  const progressInput = document.getElementById('createReportProgressInput');
  const contentInput = document.getElementById('createReportContentInput');
  const taskIdInput = document.getElementById('taskIdInput');
  const isTaskInput = document.getElementById('isTaskInput');
  taskIdInput.value = taskId;
  isTaskInput.value = isTask;
  titleInput.value = '';
  contentInput.value = '';
  const dailyReport = isTask === 'subTask';
  document.getElementById('createReportProgressLabel').textContent = dailyReport ? '오늘 증가분 (%)' : '진행률 (%)';
  progressInput.value = dailyReport ? 0 : (progress || 0);

  const today = new Date();
  dateInput.value = today.toISOString().split('T')[0];

  modal.classList.add('show');
  setTimeout(() => titleInput.focus(), 50);
}

function closeCreateReportModal() {
  const modal = document.getElementById('reportCreateModal');
  if (modal) modal.classList.remove('show');
}

// 4. 보고서 submit
async function submitCreateReportModal() {

  const titleInput = document.getElementById('createReportTitleInput');
  const contentInput = document.getElementById('createReportContentInput');
  const memberIdInput = document.getElementById('createReportWriter');
  const progress = document.getElementById('createReportProgressInput').value;
  const taskId = document.getElementById('taskIdInput').value
  const writerName = document.getElementById('createReportWriterName').value;
  const title = titleInput.value.trim();
  const content = contentInput.value.trim();
  const projectMemberId = memberIdInput.value;
  const isTask = document.getElementById('isTaskInput').value;
  const panelVersion = detailViewVersion;

  if (!title) { alert('보고서 제목을 입력해 주세요.'); titleInput.focus(); return; }
  if (!content) { alert('상세 내용을 입력해 주세요.'); contentInput.focus(); return; }
  
  console.log(isTask);
  
  try {
	if(isTask === 'task'){
		
        const payload = {
            taskId: Number(taskId),
            projectMemberId: Number(projectMemberId),
            projectId: Number(currentProjectId),
            reportType: 'REPORT',
            title: title,
            content: content,
            progress: Number(progress)
        };
		
		console.log(payload);
		console.log(`/${currentDomainKey}/project/${currentProjectId}/api/tasks/${taskId}/reports`);

		const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/api/tasks/${taskId}/reports`, 
		{
		  method: 'POST',
		  headers: { 'Content-Type': 'application/json' },
		  body: JSON.stringify(payload)
		});
		
		if (!res.ok) throw new Error('보고서 작성 실패');
	}else{
		
		const payload = {
		    subTaskId: Number(taskId),
		    projectMemberId: Number(projectMemberId),
			writerName: writerName,
		    title: title,
		    content: content,
		    progress: Number(progress)
		};
		
		console.log('subtask 보고서 제출', payload);
		const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/api/tasks/subtasks/${taskId}/daily-reports`, 
		{
		  method: 'POST',
		  headers: { 'Content-Type': 'application/json' },
		  body: JSON.stringify(payload)
		});

		if (!res.ok) throw new Error('보고서 작성 실패');
	}

    closeCreateReportModal();
    if (isTask === 'subTask') {
      await refreshDailyReportState(Number(taskId), panelVersion);
    }
  } catch (err) {
    console.error('보고서 작성 실패:', err);
    alert('보고서 작성 중 오류가 발생했습니다.');
  }
}

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    if (!document.querySelector('.project-task-dialogs .custom-modal-backdrop.show')) return;
    e.stopImmediatePropagation();
	closeCreateReportModal();
    closeCreateTaskModal();
    closeDeleteModal();
  }
}, true);
