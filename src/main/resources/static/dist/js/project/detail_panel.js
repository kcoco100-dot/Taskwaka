/**
 * [Taskwaka] 공통 우측 오버레이 상세 드로어 패널 스크립트 (detail_panel.js)
 * - 인라인 실시간 수정 제거 완료
 * - 기본 조회 모드(일반 텍스트 형태) + [수정] 버튼 클릭 시에만 수정 모드(.is-editing) 전환
 * - 하위업무 담당자 전용 일일 보고서(Daily Report) CRUD 지원
 */

let currentEntityType = 'TASK'; // 'TASK' | 'SUB_TASK'
let currentOpenTaskId = null;
let currentOpenSubTaskId = null;
let currentDetailRowElem = null;
let currentDetailStatus = 0;
let canEditCurrentSubTask = false;

// 수정 취소 시 원래 값 복원을 위한 스냅샷 객체
let originalDetailSnapshot = null;

const DETAIL_STATUS_MAP = {
	0: { text: '● 요청', cls: 'badge-req' },
	1: { text: '● 진행', cls: 'badge-prog' },
	2: { text: '● 완료', cls: 'badge-done' },
	3: { text: '● 보류', cls: 'badge-hold' }
};

function getCsrfHeaders() {
	const headers = { 'Content-Type': 'application/json' };
	const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
	if (csrfToken && csrfHeader) {
		headers[csrfHeader] = csrfToken;
	}
	return headers;
}

function getTaskApiBaseUrl() {
	const pathParts = window.location.pathname.split('/').filter(Boolean);
	if (pathParts.length >= 3 && pathParts[1] === 'project') {
		return `/${pathParts[0]}/project/${pathParts[2]}/api/tasks`;
	}
	return '/api/tasks';
}

function getCurrentProjectMemberId() {
	const panel = document.getElementById('taskDetailSidePanel');
		return Number(panel.dataset.currentPmId);
}

/* ==========================================================================
   1. 메인 업무 상세 패널 열기 (항상 조회 전용)
   ========================================================================== */
async function openDetailPanel(taskId, rowElem) {
	if (!taskId) return;
	
	currentEntityType = 'TASK';
	currentOpenTaskId = Number(taskId);
	currentOpenSubTaskId = null;
	currentDetailRowElem = rowElem || document.querySelector(`.task-tr[data-task-id="${taskId}"]`);

	highlightSelectedRow(currentDetailRowElem);

	const panel = document.getElementById('taskDetailSidePanel');
	if (!panel) return;

	// 수정 모드 강제 종료 및 조회 전용 세팅
	exitDetailEditModeUI();
	setPanelReadonlyMode(true);

	const typeBadge = document.getElementById('detailEntityTypeBadge');
	if (typeBadge) {
		typeBadge.textContent = '메인 업무';
		typeBadge.classList.remove('subtask-mode');
	}

	const subtasksSection = document.getElementById('detailSubtasksSection');
	if (subtasksSection) subtasksSection.style.display = 'block';

	const dailyReportSection = document.getElementById('detailDailyReportSection');
	if (dailyReportSection) dailyReportSection.style.display = 'none';

	panel.classList.add('open');

	try {
		const baseUrl = getTaskApiBaseUrl();
		const [detailRes, membersRes] = await Promise.all([
			fetch(`${baseUrl}/${taskId}`),
			fetch(`${baseUrl}/${taskId}/members`)
		]);

		if (!detailRes.ok) throw new Error('메인 업무 상세 조회 실패');
		const data = await detailRes.json();
		const task = data.task || data;
		const subTasks = data.subTasks || task.subTasks || [];
		const taskMembers = membersRes.ok ? await membersRes.json() : [];

		/*populateAssigneeOptions(taskMembers, task.projectMemberId, task.assigneeName);*/
		bindDetailCommonFields(task);
		renderDetailSubTasksList(subTasks, task);
	} catch (err) {
		console.error('openDetailPanel 에러:', err);
		if (currentDetailRowElem) {
			fallbackBindFromRow(currentDetailRowElem, 'TASK');
		}
	}
}

/* ==========================================================================
   2. 하위업무 상세 패널 열기 (기본 조회 모드 + [수정] 버튼 노출)
   ========================================================================== */
   async function openSubTaskDetailPanel(subTaskId, rowElem) {
   	if (!subTaskId) return;

   	currentEntityType = 'SUB_TASK';
   	currentOpenSubTaskId = Number(subTaskId);
   	currentDetailRowElem = rowElem || document.querySelector(`.subtask-tr[data-subtask-id="${subTaskId}"]`);

   	highlightSelectedRow(currentDetailRowElem);

   	const panel = document.getElementById('taskDetailSidePanel');
   	if (!panel) return;

   	// 데이터 로드 전에는 일단 수정 버튼을 숨겨둠 (조회 전용 상태로 초기화)
   	canEditCurrentSubTask = false;
   	exitDetailEditModeUI();
   	setPanelReadonlyMode(true);

   	const typeBadge = document.getElementById('detailEntityTypeBadge');
   	if (typeBadge) {
   		typeBadge.textContent = '하위 업무';
   		typeBadge.classList.add('subtask-mode');
   	}

   	const subtasksSection = document.getElementById('detailSubtasksSection');
   	if (subtasksSection) subtasksSection.style.display = 'none';

   	const dailyReportSection = document.getElementById('detailDailyReportSection');
   	if (dailyReportSection) dailyReportSection.style.display = 'block';

   	panel.classList.add('open');

   	try {
   		const baseUrl = getTaskApiBaseUrl();
   		const res = await fetch(`${baseUrl}/subtasks/${subTaskId}`);
   		if (!res.ok) throw new Error('하위업무 상세 조회 실패');

   		const data = await res.json();
   		const subTask = data.subTask || data;
   		currentOpenTaskId = subTask.taskId || currentOpenTaskId;
		console.log(subTask);

   		let candidateMembers = data.taskMembers || [];
   		if (candidateMembers.length === 0 && currentOpenTaskId) {
   			const memRes = await fetch(`${baseUrl}/${currentOpenTaskId}/members`);
   			if (memRes.ok) candidateMembers = await memRes.json();
   		}

   		/*populateAssigneeOptions(candidateMembers, subTask.projectMemberId, subTask.assigneeName);*/
   		bindDetailCommonFields(subTask);

   		// [핵심] 로그인한 사용자가 해당 하위업무 담당자일 때만 수정 버튼 및 일일보고서 작성 폼 노출
   		const loginPmId = getCurrentProjectMemberId();
   		const isSubTaskAssignee = Number(subTask.projectMemberId) === Number(loginPmId);
   		canEditCurrentSubTask = isSubTaskAssignee;

   		// 담당자 본인이면 조회 전용 해제(수정 버튼 노출), 아니면 조회 전용 배지 노출
   		setPanelReadonlyMode(!isSubTaskAssignee);

   		const reportWriteBox = document.getElementById('dailyReportWriteFormBox');
   		if (reportWriteBox) {
   			reportWriteBox.style.display = isSubTaskAssignee ? 'flex' : 'none';
   		}
   		resetDailyReportForm();
   		await loadDailyReports(subTaskId);
   	} catch (err) {
   		console.error('openSubTaskDetailPanel 에러:', err);
   		if (currentDetailRowElem) {
   			fallbackBindFromRow(currentDetailRowElem, 'SUB_TASK');
   		}
   	}
   }

/* ==========================================================================
   3. 조회 전용 / 수정 모드 전환 및 일괄 저장 로직
   ========================================================================== */
function setPanelReadonlyMode(isReadonly) {
	// 항상 필드는 잠금 상태(일반 텍스트 스타일)로 시작
	toggleFieldLock(true);

	const readonlyBadge = document.getElementById('detailReadonlyBadge');
	if (readonlyBadge) {
		readonlyBadge.style.display = isReadonly ? 'inline-flex' : 'none';
	}

	const editBtn = document.getElementById('btnDetailEditMode');
	if (editBtn) {
		editBtn.style.display = isReadonly ? 'none' : 'inline-block';
	}

	const delBtn = document.getElementById('btnDetailDeleteSubtask');
	if (delBtn) {
		delBtn.style.display = isReadonly ? 'none' : 'inline-block';
	}

	const saveBtn = document.getElementById('btnDetailSaveEdit');
	if (saveBtn) saveBtn.style.display = 'none';

	const cancelBtn = document.getElementById('btnDetailCancelEdit');
	if (cancelBtn) cancelBtn.style.display = 'none';
}

function toggleFieldLock(lock) {
	const titleEl = document.getElementById('detailTaskTitle');
	const assigneeEl = document.getElementById('detailAssigneeSelect');
	const startEl = document.getElementById('detailStartDate');
	const endEl = document.getElementById('detailEndDate');
	const progressEl = document.getElementById('detailProgressRange');
	const contentEl = document.getElementById('detailContentText');

	if (titleEl) titleEl.readOnly = lock;
	if (assigneeEl) assigneeEl.disabled = lock;
	if (startEl) startEl.readOnly = lock;
	if (endEl) endEl.readOnly = lock;
	if (progressEl) progressEl.disabled = lock;
	if (contentEl) contentEl.readOnly = lock;
}

/**
 * [수정] 버튼 클릭 시 수정 모드 진입
 */
function enterDetailEditMode() {
	// 담당자 권한(canEditCurrentSubTask)이 없으면 수정 모드 진입 차단
	if (currentEntityType !== 'SUB_TASK' || !currentOpenSubTaskId || !canEditCurrentSubTask) return;

	const panel = document.getElementById('taskDetailSidePanel');
	if (!panel) return;

	originalDetailSnapshot = {
		title: document.getElementById('detailTaskTitle')?.value || '',
		status: String(currentDetailStatus),
		projectMemberId: document.getElementById('detailAssigneeSelect')?.value || '',
		startAt: document.getElementById('detailStartDate')?.value || '',
		endAt: document.getElementById('detailEndDate')?.value || '',
		progress: document.getElementById('detailProgressRange')?.value || '0',
		content: document.getElementById('detailContentText')?.value || ''
	};

	const statusSelect = document.getElementById('detailStatusSelect');
	if (statusSelect) {
		statusSelect.value = String(currentDetailStatus);
	}

	panel.classList.add('is-editing');
	toggleFieldLock(false);

	const editBtn = document.getElementById('btnDetailEditMode');
	const delBtn = document.getElementById('btnDetailDeleteSubtask');
	const saveBtn = document.getElementById('btnDetailSaveEdit');
	const cancelBtn = document.getElementById('btnDetailCancelEdit');

	if (editBtn) editBtn.style.display = 'none';
	if (delBtn) delBtn.style.display = 'none';
	if (saveBtn) saveBtn.style.display = 'inline-block';
	if (cancelBtn) cancelBtn.style.display = 'inline-block';

	document.getElementById('detailTaskTitle')?.focus();
}

function exitDetailEditModeUI() {
	const panel = document.getElementById('taskDetailSidePanel');
	if (panel) panel.classList.remove('is-editing');

	toggleFieldLock(true);

	// 하위업무이면서 동시에 담당자 본인(canEditCurrentSubTask)일 때만 수정/삭제 버튼 노출
	const showEditBtns = (currentEntityType === 'SUB_TASK' && canEditCurrentSubTask);

	const editBtn = document.getElementById('btnDetailEditMode');
	const delBtn = document.getElementById('btnDetailDeleteSubtask');
	const saveBtn = document.getElementById('btnDetailSaveEdit');
	const cancelBtn = document.getElementById('btnDetailCancelEdit');
	const readonlyBadge = document.getElementById('detailReadonlyBadge');

	if (editBtn) editBtn.style.display = showEditBtns ? 'inline-block' : 'none';
	if (delBtn) delBtn.style.display = showEditBtns ? 'inline-block' : 'none';
	if (saveBtn) saveBtn.style.display = 'none';
	if (cancelBtn) cancelBtn.style.display = 'none';
	if (readonlyBadge) readonlyBadge.style.display = showEditBtns ? 'none' : 'inline-flex';
}

/**
 * [취소] 버튼 클릭 시 원래 값 복원 후 조회 모드 복귀
 */
function cancelDetailEditMode() {
	if (originalDetailSnapshot) {
		const titleEl = document.getElementById('detailTaskTitle');
		const assigneeEl = document.getElementById('detailAssigneeSelect');
		const startEl = document.getElementById('detailStartDate');
		const endEl = document.getElementById('detailEndDate');
		const progressEl = document.getElementById('detailProgressRange');
		const progressLabel = document.getElementById('detailProgressLabel');
		const contentEl = document.getElementById('detailContentText');

		if (titleEl) titleEl.value = originalDetailSnapshot.title;
		if (assigneeEl) assigneeEl.value = originalDetailSnapshot.projectMemberId;
		if (startEl) startEl.value = originalDetailSnapshot.startAt;
		if (endEl) endEl.value = originalDetailSnapshot.endAt;
		if (progressEl) progressEl.value = originalDetailSnapshot.progress;
		if (progressLabel) progressLabel.textContent = originalDetailSnapshot.progress + '%';
		if (contentEl) contentEl.value = originalDetailSnapshot.content;

		currentDetailStatus = parseInt(originalDetailSnapshot.status, 10) || 0;
		updateDetailStatusBadgeUI(currentDetailStatus);
	}
	exitDetailEditModeUI();
}

/**
 * [저장] 버튼 클릭 시 변경된 항목들을 서버에 PATCH 후 조회 모드 복귀
 */
async function saveDetailEditMode() {
	if (currentEntityType !== 'SUB_TASK' || !currentOpenSubTaskId) return;

	const newValues = {
		title: document.getElementById('detailTaskTitle')?.value.trim() || '',
		status: document.getElementById('detailStatusSelect')?.value || String(currentDetailStatus),
		projectMemberId: document.getElementById('detailAssigneeSelect')?.value || '',
		startAt: document.getElementById('detailStartDate')?.value || '',
		endAt: document.getElementById('detailEndDate')?.value || '',
		progress: document.getElementById('detailProgressRange')?.value || '0',
		content: document.getElementById('detailContentText')?.value || ''
	};

	if (!newValues.title) {
		alert('하위업무 제목을 입력해 주세요.');
		document.getElementById('detailTaskTitle')?.focus();
		return;
	}

	const baseUrl = getTaskApiBaseUrl();
	const fieldsToCheck = ['title', 'status', 'projectMemberId', 'startAt', 'endAt', 'progress', 'content'];

	try {
		for (const field of fieldsToCheck) {
			if (!originalDetailSnapshot || String(originalDetailSnapshot[field]) !== String(newValues[field])) {
				const res = await fetch(`${baseUrl}/subtasks/${currentOpenSubTaskId}/field`, {
					method: 'PATCH',
					headers: getCsrfHeaders(),
					body: JSON.stringify({
						field: field,
						value: String(newValues[field])
					})
				});
				if (!res.ok) {
					throw new Error(`${field} 업데이트 실패`);
				}
			}
		}

		currentDetailStatus = parseInt(newValues.status, 10) || 0;
		updateDetailStatusBadgeUI(currentDetailStatus);

		// 담당자 변경 시 상단 아바타/작성자명 갱신 및 일일보고서 폼 권한 갱신
		const assigneeSelect = document.getElementById('detailAssigneeSelect');
		const selectedAssigneeName = assigneeSelect?.options[assigneeSelect.selectedIndex]?.text || '-';
		if (selectedAssigneeName && selectedAssigneeName !== '-') {
			const authorNameEl = document.getElementById('detailAuthorName');
			const avatarEl = document.getElementById('detailAuthorAvatar');
			if (authorNameEl) authorNameEl.textContent = selectedAssigneeName;
			if (avatarEl) avatarEl.textContent = selectedAssigneeName.substring(0, 2);
		}

		const loginPmId = getCurrentProjectMemberId();
		const isSubTaskAssignee = Number(newValues.projectMemberId) === Number(loginPmId);
		canEditCurrentSubTask = isSubTaskAssignee; // [추가] 담당자가 다른 사람으로 바뀌었으면 수정 권한도 즉시 회수
		const reportWriteBox = document.getElementById('dailyReportWriteFormBox');
		if (reportWriteBox) {
			reportWriteBox.style.display = isSubTaskAssignee ? 'flex' : 'none';
		}

		syncUpdatedSubTaskToRow(newValues, selectedAssigneeName);
		exitDetailEditModeUI();
	} catch (err) {
		console.error('saveDetailEditMode 오류:', err);
		alert('수정 내용을 저장하는 중 오류가 발생했습니다.');
	}
}


/* ==========================================================================
   4. 공통 데이터 바인딩 및 좌측 목록 행 동기화
   ========================================================================== */
/*function populateAssigneeOptions(members, selectedPmId, fallbackName) {
	const selectEl = document.getElementById('detailAssigneeSelect');
	if (!selectEl) return;

	selectEl.innerHTML = '';

	if (Array.isArray(members) && members.length > 0) {
		members.forEach(m => {
			const opt = document.createElement('option');
			opt.value = m.projectMemberId;
			opt.textContent = m.name || m.memberName || `멤버 #${m.projectMemberId}`;
			if (Number(m.projectMemberId) === Number(selectedPmId)) {
				opt.selected = true;
			}
			selectEl.appendChild(opt);
		});
	} else if (selectedPmId) {
		const opt = document.createElement('option');
		opt.value = selectedPmId;
		opt.textContent = fallbackName || `담당자 #${selectedPmId}`;
		opt.selected = true;
		selectEl.appendChild(opt);
	} else {
		const opt = document.createElement('option');
		opt.value = '';
		opt.textContent = '미배정';
		selectEl.appendChild(opt);
	}
}
*/

function bindDetailCommonFields(item) {
	const assigneeName = document.getElementById('detailAssigneeSelect'); 
	const avatarEl = document.getElementById('detailAuthorAvatar');
	const authorNameEl = document.getElementById('detailAuthorName');
	const createdDateEl = document.getElementById('detailCreatedDate');
	
	const assigneeNameText = item.assigneeName;
	
	if (avatarEl) avatarEl.textContent = assigneeNameText.substring(0, 2);
	if (authorNameEl) authorNameEl.textContent = assigneeNameText;
	if (createdDateEl) {
		createdDateEl.textContent = item.created ? `등록일 ${formatDateOnly(item.created)}` : '등록일 -';
	}
	
	if(assigneeName) assigneeName.textContent = assigneeNameText || item.writerName || '미배정';

	const titleEl = document.getElementById('detailTaskTitle');
	if (titleEl) titleEl.value = item.title || '';

	currentDetailStatus = Number(item.status || 0);
	updateDetailStatusBadgeUI(currentDetailStatus);

	const statusSelect = document.getElementById('detailStatusSelect');
	if (statusSelect) statusSelect.value = String(currentDetailStatus);

	const startEl = document.getElementById('detailStartDate');
	const endEl = document.getElementById('detailEndDate');
	if (startEl) startEl.value = formatDateOnly(item.startAt || item.start);
	if (endEl) endEl.value = formatDateOnly(item.endAt || item.end);

	const progVal = Number(item.progress || 0);
	const progRange = document.getElementById('detailProgressRange');
	const progLabel = document.getElementById('detailProgressLabel');
	if (progRange) progRange.value = progVal;
	if (progLabel) progLabel.textContent = `${progVal}%`;

	const contentEl = document.getElementById('detailContentText');
	if (contentEl) contentEl.value = item.content || '';
}

function updateDetailStatusBadgeUI(statusCode) {
	const badge = document.getElementById('detailStatusBadge');
	const textEl = document.getElementById('detailStatusText');
	const info = DETAIL_STATUS_MAP[statusCode] || DETAIL_STATUS_MAP[0];

	if (badge) {
		badge.className = `badge-status ${info.cls}`;
	}
	if (textEl) {
		textEl.textContent = info.text;
	}
}

function syncUpdatedSubTaskToRow(newValues, assigneeName) {
	const row = currentDetailRowElem || document.querySelector(`.subtask-tr[data-subtask-id="${currentOpenSubTaskId}"]`);
	if (!row) return;

	const titleSpan = row.querySelector('.task-name-text');
	if (titleSpan) titleSpan.textContent = newValues.title;

	const statusBadge = row.querySelector('.badge-status');
	if (statusBadge) {
		const info = DETAIL_STATUS_MAP[Number(newValues.status)] || DETAIL_STATUS_MAP[0];
		statusBadge.className = `badge-status ${info.cls}`;
		statusBadge.textContent = info.text;
	}

	const assigneeNameSpan = row.querySelector('.assignee-name-text');
	const avatarMini = row.querySelector('.avatar-mini');
	if (assigneeName && assigneeName !== '-') {
		if (assigneeNameSpan) assigneeNameSpan.textContent = assigneeName;
		if (avatarMini) avatarMini.textContent = assigneeName.substring(0, 2);
	}

	const dateCells = row.querySelectorAll('.date-cell');
	if (dateCells.length >= 1 && newValues.startAt) {
		dateCells[0].textContent = newValues.startAt;
	}
	if (dateCells.length >= 2 && newValues.endAt) {
		dateCells[1].textContent = newValues.endAt;
	}
}

function renderDetailSubTasksList(subTasks, parentTask) {
	const countEl = document.getElementById('detailSubtaskCount');
	const boxEl = document.getElementById('detailSubtaskBox');
	const addBtn = document.getElementById('btnDetailAddSubtask');

	if (countEl) countEl.textContent = subTasks.length;

	// 상위 메인 업무 담당자에게만 '+ 하위업무 추가' 버튼 노출
	if (addBtn && parentTask) {
		const isMainTaskOwner = Number(parentTask.projectMemberId) === Number(getCurrentProjectMemberId());
		addBtn.style.display = isMainTaskOwner ? 'inline-block' : 'none';
	}

	if (!boxEl) return;
	boxEl.innerHTML = '';

	if (subTasks.length === 0) {
		boxEl.innerHTML = `<div style="padding:18px; text-align:center; color:#9E9284; font-size:12.5px;">등록된 하위업무가 없습니다.</div>`;
		return;
	}

	subTasks.forEach(st => {
		const stId = st.subTaskId;
		const stStatus = Number(st.status || 0);
		const stInfo = DETAIL_STATUS_MAP[stStatus] || DETAIL_STATUS_MAP[0];
		const itemDiv = document.createElement('div');
		itemDiv.className = 'detail-subtask-item';
		itemDiv.style.cursor = 'pointer';
		itemDiv.onclick = () => openSubTaskDetailPanel(stId);

		itemDiv.innerHTML = `
			<div class="detail-subtask-left">
				<span class="badge-status ${stInfo.cls}" style="font-size:11px; padding:2px 7px;">${stInfo.text}</span>
				<span style="font-weight:600; color:#2B241D; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escapeHtml(st.title || '')}</span>
			</div>
			<div style="display:flex; align-items:center; gap:8px; font-size:12px; color:#786B5E;">
				<span>${escapeHtml(st.assigneeName || '미배정')}</span>
				<span style="font-weight:700; color:#4F6242;">${Number(st.progress || 0)}%</span>
			</div>
		`;
		boxEl.appendChild(itemDiv);
	});
}

/* ==========================================================================
   5. 일일 업무 보고서 (Daily Report) CRUD
   ========================================================================== */
async function loadDailyReports(subTaskId) {
	const listBox = document.getElementById('dailyReportListBox');
	const countEl = document.getElementById('dailyReportCount');
	if (!listBox) return;

	try {
		const baseUrl = getTaskApiBaseUrl();
		const res = await fetch(`${baseUrl}/subtasks/${subTaskId}/daily-reports`);
		if (!res.ok) throw new Error('일일 보고서 목록 조회 실패');

		const reports = await res.json();
		if (countEl) countEl.textContent = `(${reports.length})`;

		if (!Array.isArray(reports) || reports.length === 0) {
			listBox.innerHTML = `<div style="padding:14px; text-align:center; color:#9E9284; font-size:12.5px; background:#FFFFFF; border:1px solid #E6E0D6; border-radius:8px;">작성된 일일 업무 보고서가 없습니다.</div>`;
			return;
		}

		const loginPmId = getCurrentProjectMemberId();
		listBox.innerHTML = '';

		reports.forEach(r => {
			const statusNum = Number(r.status || 0);
			let statusLabel = '검토 대기';
			let statusCls = 'wait';
			if (statusNum === 1) {
				statusLabel = '승인 완료';
				statusCls = 'approved';
			} else if (statusNum === 2) {
				statusLabel = '반려';
				statusCls = 'rejected';
			}

			// 본인 작성이면서 상급자 확인 전(modifiable)일 때만 수정/삭제 노출
			const isWriter = Number(r.projectMemberId) === Number(loginPmId);
			const isModifiable = (r.modifiable !== undefined)
				? Boolean(r.modifiable)
				: (statusNum === 0 && !r.viewed && !r.reviewed);

			const card = document.createElement('div');
			card.className = 'daily-report-item-card';
			card.innerHTML = `
				<div class="daily-report-item-top">
					<div class="report-meta-left">
						<span class="report-date-badge">${formatDateOnly(r.reportDate)}</span>
						<span class="report-progress-chip">달성 ${Number(r.progress || 0)}%</span>
						<span class="report-review-status ${statusCls}">${statusLabel}</span>
					</div>
					${(isWriter && isModifiable) ? `
					<div class="daily-report-actions">
						<button type="button" class="btn-report-mini" onclick="startEditDailyReport(${r.dailyReportId}, '${formatDateOnly(r.reportDate)}',${Number(r.progress || 0)}, \`${escapeBacktick(r.content || '')}\`)">수정</button>
						<button type="button" class="btn-report-mini del" onclick="deleteDailyReport(${r.dailyReportId})">삭제</button>
					</div>
					` : ''}
				</div>
				<div class="daily-report-content-body">${escapeHtml(r.content || '')}</div>
			`;
			listBox.appendChild(card);
		});
	} catch (err) {
		console.error('loadDailyReports 오류:', err);
	}
}

function resetDailyReportForm() {
	const idInput = document.getElementById('editingDailyReportId');
	const dateInput = document.getElementById('dailyReportDateInput');
	const progInput = document.getElementById('dailyReportProgressInput');
	const contentInput = document.getElementById('dailyReportContentInput');
	const titleSpan = document.getElementById('dailyReportFormTitle');
	const cancelBtn = document.getElementById('btnCancelReportEdit');
	const submitBtn = document.getElementById('btnSubmitDailyReport');

	if (idInput) idInput.value = '';
	if (dateInput) dateInput.value = new Date().toISOString().slice(0, 10);
	if (progInput) progInput.value = document.getElementById('detailProgressRange')?.value || 0;
	if (contentInput) contentInput.value = '';
	if (titleSpan) titleSpan.textContent = '새 일일 보고서 작성';
	if (cancelBtn) cancelBtn.style.display = 'none';
	if (submitBtn) submitBtn.textContent = '보고서 등록';
}

function startEditDailyReport(reportId, reportDate, progress, content) {
	const idInput = document.getElementById('editingDailyReportId');
	const dateInput = document.getElementById('dailyReportDateInput');
	const progInput = document.getElementById('dailyReportProgressInput');
	const contentInput = document.getElementById('dailyReportContentInput');
	const titleSpan = document.getElementById('dailyReportFormTitle');
	const cancelBtn = document.getElementById('btnCancelReportEdit');
	const submitBtn = document.getElementById('btnSubmitDailyReport');

	if (idInput) idInput.value = reportId;
	if (dateInput) dateInput.value = reportDate;
	if (progInput) progInput.value = progress;
	if (contentInput) {
		contentInput.value = content;
		contentInput.focus();
	}
	if (titleSpan) titleSpan.textContent = '일일 보고서 수정';
	if (cancelBtn) cancelBtn.style.display = 'inline-block';
	if (submitBtn) submitBtn.textContent = '보고서 수정 저장';
}

async function submitDailyReportForm() {
	if (!currentOpenSubTaskId) return;

	const editingId = document.getElementById('editingDailyReportId')?.value;
	const reportDate = document.getElementById('dailyReportDateInput')?.value;
	const progress = Number(document.getElementById('dailyReportProgressInput')?.value || 0);
	const content = document.getElementById('dailyReportContentInput')?.value.trim() || '';

	if (!reportDate) {
		alert('보고일자를 선택해 주세요.');
		return;
	}
	if (!content) {
		alert('보고 내용을 입력해 주세요.');
		return;
	}

	const baseUrl = getTaskApiBaseUrl();
	const url = editingId
		? `${baseUrl}/daily-reports/${editingId}`
		: `${baseUrl}/subtasks/${currentOpenSubTaskId}/daily-reports`;
	const method = editingId ? 'PUT' : 'POST';

	try {
		const res = await fetch(url, {
			method: method,
			headers: getCsrfHeaders(),
			body: JSON.stringify({
				subTaskId: currentOpenSubTaskId,
				projectMemberId: getCurrentProjectMemberId(),
				reportDate: reportDate,
				progress: progress,
				content: content
			})
		});

		if (!res.ok) {
			const errData = await res.json().catch(() => ({}));
			throw new Error(errData.message || '보고서 저장에 실패했습니다.');
		}

		// 하위업무 진척도 표시 갱신
		const progRange = document.getElementById('detailProgressRange');
		const progLabel = document.getElementById('detailProgressLabel');
		if (progRange) progRange.value = progress;
		if (progLabel) progLabel.textContent = `${progress}%`;

		resetDailyReportForm();
		await loadDailyReports(currentOpenSubTaskId);
	} catch (err) {
		console.error('submitDailyReportForm 에러:', err);
		alert(err.message || '일일 보고서 처리 중 오류가 발생했습니다.');
	}
}

async function deleteDailyReport(dailyReportId) {
	if (!dailyReportId || !confirm('이 일일 보고서를 삭제하시겠습니까?\n(상급자가 이미 열람/검토한 보고서는 삭제할 수 없습니다.)')) {
		return;
	}

	try {
		const baseUrl = getTaskApiBaseUrl();
		const res = await fetch(`${baseUrl}/daily-reports/${dailyReportId}?projectMemberId=${getCurrentProjectMemberId()}`, {
			method: 'DELETE',
			headers: getCsrfHeaders()
		});

		if (!res.ok) {
			const errData = await res.json().catch(() => ({}));
			throw new Error(errData.message || '상급자가 이미 확인하여 삭제할 수 없습니다.');
		}

		await loadDailyReports(currentOpenSubTaskId);
	} catch (err) {
		console.error('deleteDailyReport 에러:', err);
		alert(err.message);
	}
}

/* ==========================================================================
   6. 패널 닫기 / 하위업무 모달 연동 / 유틸리티
   ========================================================================== */
function closeDetailPanel() {
	const panel = document.getElementById('taskDetailSidePanel');
	if (panel) {
		panel.classList.remove('open');
		panel.classList.remove('is-editing');
	}
	document.querySelectorAll('.task-tr.selected-row, .subtask-tr.selected-row').forEach(tr => {
		tr.classList.remove('selected-row');
	});
}

function detailAddSubtaskClick() {
	if (!currentOpenTaskId) return;
	if (typeof openSubtaskModal === 'function') {
		openSubtaskModal(currentOpenTaskId);
	}
}

function requestDeleteCurrentSubTask() {
	if (!currentOpenSubTaskId) return;
	if (typeof openDeleteSubTaskModal === 'function') {
		const titleVal = document.getElementById('detailTaskTitle')?.value || '하위업무';
		openDeleteSubTaskModal(currentOpenSubTaskId, titleVal);
	}
}

function handleFileUpload(inputElem) {
	const file = inputElem.files && inputElem.files[0];
	if (!file) return;

	const listBox = document.getElementById('attachedFileList');
	const countSpan = document.getElementById('attachedFileCount');
	if (!listBox) return;

	const item = document.createElement('div');
	item.className = 'attached-file-item';
	item.innerHTML = `
		<span>📄 ${escapeHtml(file.name)} <small style="color:#9E9284;">(${(file.size / 1024).toFixed(1)} KB)</small></span>
		<button type="button" style="border:none; background:none; color:#B84A39; cursor:pointer; font-size:12px;" onclick="this.parentElement.remove(); updateAttachedFileCount();">삭제</button>
	`;
	listBox.appendChild(item);
	inputElem.value = '';
	updateAttachedFileCount();
}

function updateAttachedFileCount() {
	const listBox = document.getElementById('attachedFileList');
	const countSpan = document.getElementById('attachedFileCount');
	if (listBox && countSpan) {
		countSpan.textContent = `(${listBox.children.length})`;
	}
}

function highlightSelectedRow(rowElem) {
	document.querySelectorAll('.task-tr.selected-row, .subtask-tr.selected-row').forEach(tr => {
		tr.classList.remove('selected-row');
	});
	if (rowElem && rowElem.classList) {
		rowElem.classList.add('selected-row');
	}
}

function fallbackBindFromRow(rowElem, type) {
	const titleText = rowElem.querySelector('.task-name-text')?.textContent?.trim() || '';
	const assigneeText = rowElem.querySelector('.assignee-name-text')?.textContent?.trim() || '미배정';
	const dateCells = rowElem.querySelectorAll('.date-cell');
	const startText = dateCells[0]?.textContent?.trim() || '';
	const endText = dateCells[1]?.textContent?.trim() || '';

	bindDetailCommonFields({
		title: titleText,
		assigneeName: assigneeText,
		startAt: startText !== '-' ? startText : '',
		endAt: endText !== '-' ? endText : '',
		status: 0,
		progress: 0,
		content: ''
	});
}

function formatDateOnly(val) {
	if (!val) return '';
	const str = String(val).trim();
	if (str.length >= 10) return str.substring(0, 10);
	return str;
}

function escapeHtml(str) {
	return String(str)
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;')
		.replace(/'/g, '&#39;');
}

function escapeBacktick(str) {
	return String(str).replace(/\\/g, '\\\\').replace(/`/g, '\\`').replace(/\$/g, '\\$');
}

/* ==========================================================================
   7. 패널 너비 드래그 리사이저 & ESC 닫기 이벤트 바인딩
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
	const panel = document.getElementById('taskDetailSidePanel');
	const resizer = document.getElementById('paneResizerBar');

	if (panel && resizer) {
		let isResizing = false;

		resizer.addEventListener('mousedown', (e) => {
			isResizing = true;
			resizer.classList.add('resizing');
			document.body.style.cursor = 'col-resize';
			document.body.style.userSelect = 'none';
			e.preventDefault();
		});

		document.addEventListener('mousemove', (e) => {
			if (!isResizing) return;
			const newWidth = window.innerWidth - e.clientX;
			if (newWidth >= 400 && newWidth <= window.innerWidth * 0.82) {
				panel.style.width = `${newWidth}px`;
			}
		});

		document.addEventListener('mouseup', () => {
			if (isResizing) {
				isResizing = false;
				resizer.classList.remove('resizing');
				document.body.style.cursor = '';
				document.body.style.userSelect = '';
			}
		});
	}

	document.addEventListener('keydown', (e) => {
		if (e.key === 'Escape') {
			const panelEl = document.getElementById('taskDetailSidePanel');
			if (panelEl && panelEl.classList.contains('open')) {
				if (panelEl.classList.contains('is-editing')) {
					cancelDetailEditMode();
				} else {
					closeDetailPanel();
				}
			}
		}
	});
});