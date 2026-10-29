/* ==========================================================================
   [JS] 업무 목록 탭 스크립트 (인라인 수정 제거 + 하위업무 전용 생성/삭제 + task_members 연동)
   ========================================================================== */
let pendingDeleteContext = null;

let createSubModalContext = {
  parentTaskId: null,
  statusCode: 0
};

// 1. 하위업무 등록 모달 오픈 (상위 업무의 task_members 동적 로드)
async function requestCreateSubTask(taskId) {
  createSubModalContext = {
    parentTaskId: taskId,
    statusCode: 0
  };

  const modal = document.getElementById('taskCreateModal');
  const titleInput = document.getElementById('createTaskTitleInput');
  const contentInput = document.getElementById('createTaskContentInput');
  const assigneeSelect = document.getElementById('createAssigneeSelect');
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

  // 상위 업무에 배정된 참여 인원(task_members) 목록 조회하여 드롭다운 구성
  assigneeSelect.innerHTML = '<option value="">불러오는 중...</option>';
  try {
    const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/api/tasks/${taskId}/members`);
	console.log(res);
    if (res.ok) {
      const members = await res.json();
      assigneeSelect.innerHTML = '';
      if (members && members.length > 0) {
        members.forEach(m => {
          const opt = document.createElement('option');
          opt.value = m.projectMemberId;
          opt.textContent = m.memberName;
          assigneeSelect.appendChild(opt);
        });
      } else {
        assigneeSelect.innerHTML = '<option value="1">기본 담당자 (#1)</option>';
      }
    }
  } catch (err) {
    console.error('참여 인원 조회 실패:', err);
    assigneeSelect.innerHTML = '<option value="1">기본 담당자 (#1)</option>';
  }

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
  createSubModalContext.statusCode = (createSubModalContext.statusCode + 1) % panelStatusMap.length;
  const next = panelStatusMap[createSubModalContext.statusCode];
  document.getElementById('createStatusBadge').className = 'badge-status ' + next.class;
  document.getElementById('createStatusText').textContent = next.text;
}//panelStatusMap 정의 필요 ... 

async function submitCreateSubTaskModal() {
  const taskId = createSubModalContext.parentTaskId;
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
  if (!projectMemberId) { alert('하위업무 담당자를 선택해 주세요.'); assigneeSelect.focus(); return; }
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
    const branchLi = document.querySelector(`[data-task-id="${taskId}"]`);
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
  if (e.target === document.getElementById('deleteConfirmModal')) closeDeleteModal();
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
async function requestCreateReport(taskId, isTask) {
	
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
  progressInput.value = '';

  const today = new Date();
  dateInput.value = today.toISOString().split('T')[0];

  modal.classList.add('show');
  setTimeout(() => titleInput.focus(), 50);
}

function closeCreateReportModal() {
  const modal = document.getElementById('reportCreateModal');
  if (modal) modal.classList.remove('show');
}

function handleCreateBackdropClick(e) {
  if (e.target === document.getElementById('taskCreateModal')) {
    closeCreateTaskModal();
  }else if(e.target === document.getElementById('reportCreateModal')){
	closeCreateReportModal();
  }
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
  } catch (err) {
    console.error('보고서 작성 실패:', err);
    alert('보고서 작성 중 오류가 발생했습니다.');
  }
}

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
	closeCreateReportModal();
    closeCreateTaskModal();
    closeDeleteModal();
    closeDetailPanel();
  }
});