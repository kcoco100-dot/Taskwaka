/* ==========================================================================
   [JS] 간트차트 렌더링 & 일정 클릭 시 우측 상세 패널 연동 (async / await 기반)
   ========================================================================== */
const DAY_CELL_WIDTH = 36; // 하루당 픽셀 너비

const ganttStatusMap = [
  { code: 0, text: '● 요청', class: 'badge-req' },
  { code: 1, text: '● 진행', class: 'badge-done' },
  { code: 2, text: '● 피드백', class: 'badge-feedback' },
  { code: 3, text: '● 완료', class: 'badge-done' },
  { code: 4, text: '● 보류', class: 'badge-hold' }
];

let ganttMinDate = null;
let ganttMaxDate = null;
let ganttTotalDays = 0;

// 현재 상세 패널에 열려 있는 일정 정보 ({ type: 'task' | 'subtask', id: number })
let activeGanttContext = {
  type: null,
  id: null
};

document.addEventListener('DOMContentLoaded', async function() {
  await initializeGanttChart();
  initScrollSync();
  initGanttResizer();
});

// ================= 1. 간트차트 초기화 및 데이터 로드 =================
async function initializeGanttChart() {
  const leftBody = document.getElementById('ganttLeftBody');
  const existingRows = leftBody.querySelectorAll('.gantt-tree-row');

  // SSR로 렌더링된 행이 없다면 API에서 업무 목록을 가져와 렌더링 시도
  if (existingRows.length === 0) {
    await loadTasksFromApiOrFallback();
  }

  calculateTimelineRange();
  renderTimelineHeader();
  renderTimelineBars();
  scrollToToday();
}

async function loadTasksFromApiOrFallback() {
  const leftBody = document.getElementById('ganttLeftBody');
  let tasks = [];

  try {
    const res = await fetch(`/api/projects/${currentProjectId}/tasks`);
    if (res.ok) {
      tasks = await res.json();
    }
  } catch (err) {
    console.warn('API 목록 조회 실패 또는 데이터 없음. 샘플 일정으로 초기화합니다.', err);
  }

  // DB 데이터가 비어있을 경우 확인용 기본 일정 세팅
  if (!tasks || tasks.length === 0) {
    const todayStr = formatDateYMD(new Date());
    const plus5Str = formatDateYMD(addDays(new Date(), 5));
    const plus10Str = formatDateYMD(addDays(new Date(), 10));
    tasks = [
      {
        taskId: 1,
        title: '기획 및 요구사항 정의',
        status: 1,
        progress: 60,
        startAt: todayStr,
        endAt: plus10Str,
        subTasks: [
          { subTaskId: 101, taskId: 1, title: '화면 설계서 작성', status: 3, progress: 100, startAt: todayStr, endAt: plus5Str },
          { subTaskId: 102, taskId: 1, title: 'ERD 및 테이블 명세 확정', status: 1, progress: 40, startAt: plus5Str, endAt: plus10Str }
        ]
      }
    ];
  }

  leftBody.innerHTML = '';
  tasks.forEach(task => {
    const hasSub = task.subTasks && task.subTasks.length > 0;
    const statusObj = ganttStatusMap.find(s => s.code === task.status) || ganttStatusMap[0];

    const mainRow = document.createElement('div');
    mainRow.className = 'gantt-tree-row main-row';
    mainRow.dataset.type = 'task';
    mainRow.dataset.id = task.taskId;
    mainRow.dataset.parentId = '';
    mainRow.dataset.start = task.startAt || '';
    mainRow.dataset.end = task.endAt || '';
    mainRow.dataset.progress = task.progress || 0;
    mainRow.dataset.status = task.status || 0;
    mainRow.dataset.title = task.title || '';
    mainRow.onclick = function() { onGanttItemClick('task', task.taskId, mainRow); };

    mainRow.innerHTML = `
      <div class="col-g-name">
        ${hasSub 
          ? `<span class="btn-gantt-toggle" onclick="event.stopPropagation(); toggleGanttSubRows(this)">－</span>` 
          : `<span class="btn-gantt-toggle-empty"></span>`}
        <span class="gantt-row-title">${task.title}</span>
      </div>
      <div class="col-g-status">
        <span class="badge-status ${statusObj.class}">${statusObj.text}</span>
      </div>
      <div class="col-g-period">${task.startAt || '-'} ~ ${task.endAt || '-'}</div>
    `;
    leftBody.appendChild(mainRow);

    if (hasSub) {
      task.subTasks.forEach(sub => {
        const subStatusObj = ganttStatusMap.find(s => s.code === sub.status) || ganttStatusMap[0];
        const subRow = document.createElement('div');
        subRow.className = 'gantt-tree-row sub-row';
        subRow.dataset.type = 'subtask';
        subRow.dataset.id = sub.subTaskId;
        subRow.dataset.parentId = task.taskId;
        subRow.dataset.start = sub.startAt || '';
        subRow.dataset.end = sub.endAt || '';
        subRow.dataset.progress = sub.progress || 0;
        subRow.dataset.status = sub.status || 0;
        subRow.dataset.title = sub.title || '';
        subRow.onclick = function() { onGanttItemClick('subtask', sub.subTaskId, subRow); };

        subRow.innerHTML = `
          <div class="col-g-name indent-sub">
            <span class="sub-branch-icon">ㄴ</span>
            <span class="gantt-row-title">${sub.title}</span>
          </div>
          <div class="col-g-status">
            <span class="badge-status ${subStatusObj.class}">${subStatusObj.text}</span>
          </div>
          <div class="col-g-period">${sub.startAt || '-'} ~ ${sub.endAt || '-'}</div>
        `;
        leftBody.appendChild(subRow);
      });
    }
  });
}

// ================= 2. 타임라인 날짜 범위 계산 및 헤더/바 렌더링 =================
function calculateTimelineRange() {
  const rows = document.querySelectorAll('.gantt-tree-row');
  let minD = new Date();
  let maxD = new Date();

  rows.forEach(r => {
    const s = parseDateYMD(r.dataset.start);
    const e = parseDateYMD(r.dataset.end);
    if (s && s < minD) minD = new Date(s);
    if (e && e > maxD) maxD = new Date(e);
  });

  // 좌우 여유 일수 확보 (시작 7일 전 ~ 종료 21일 후)
  ganttMinDate = addDays(minD, -7);
  ganttMaxDate = addDays(maxD, 21);
  ganttTotalDays = diffDays(ganttMinDate, ganttMaxDate) + 1;
  if (ganttTotalDays < 35) {
    ganttMaxDate = addDays(ganttMinDate, 35);
    ganttTotalDays = 36;
  }
}

function renderTimelineHeader() {
  const monthRow = document.getElementById('ganttMonthRow');
  const dayRow = document.getElementById('ganttDayRow');
  monthRow.innerHTML = '';
  dayRow.innerHTML = '';

  const todayStr = formatDateYMD(new Date());
  let currentMonthKey = '';
  let monthDayCount = 0;
  let monthCells = [];

  for (let i = 0; i < ganttTotalDays; i++) {
    const curDate = addDays(ganttMinDate, i);
    const ymKey = `${curDate.getFullYear()}년 ${String(curDate.getMonth() + 1).padStart(2, '0')}월`;

    if (ymKey !== currentMonthKey) {
      if (currentMonthKey !== '') {
        monthCells.push({ label: currentMonthKey, count: monthDayCount });
      }
      currentMonthKey = ymKey;
      monthDayCount = 1;
    } else {
      monthDayCount++;
    }

    const dayCell = document.createElement('div');
    dayCell.className = 'gantt-day-cell';
    const dayOfWeek = curDate.getDay();
    if (dayOfWeek === 0 || dayOfWeek === 6) dayCell.classList.add('weekend');
    if (formatDateYMD(curDate) === todayStr) dayCell.classList.add('today');

    dayCell.textContent = curDate.getDate();
    dayRow.appendChild(dayCell);
  }

  if (monthDayCount > 0) {
    monthCells.push({ label: currentMonthKey, count: monthDayCount });
  }

  monthCells.forEach(m => {
    const mCell = document.createElement('div');
    mCell.className = 'gantt-month-cell';
    mCell.style.width = (m.count * DAY_CELL_WIDTH) + 'px';
    mCell.textContent = m.label;
    monthRow.appendChild(mCell);
  });
}

function renderTimelineBars() {
  const barsContainer = document.getElementById('ganttBarsContainer');
  const todayLine = document.getElementById('ganttTodayLine');
  const totalWidth = ganttTotalDays * DAY_CELL_WIDTH;

  barsContainer.innerHTML = '';
  barsContainer.style.width = totalWidth + 'px';

  // 오늘(Today) 수직선 위치 세팅
  const todayOffset = diffDays(ganttMinDate, new Date());
  if (todayOffset >= 0 && todayOffset < ganttTotalDays) {
    todayLine.style.display = 'block';
    todayLine.style.left = (todayOffset * DAY_CELL_WIDTH + DAY_CELL_WIDTH / 2) + 'px';
  } else {
    todayLine.style.display = 'none';
  }

  const leftRows = document.querySelectorAll('.gantt-tree-row');
  leftRows.forEach(row => {
    const type = row.dataset.type;
    const id = row.dataset.id;
    const parentId = row.dataset.parentId || '';
    const title = row.dataset.title || '';
    const progress = parseFloat(row.dataset.progress || '0');
    const startDate = parseDateYMD(row.dataset.start) || new Date();
    const endDate = parseDateYMD(row.dataset.end) || startDate;

    const gridRow = document.createElement('div');
    gridRow.className = 'gantt-grid-row ' + (type === 'subtask' ? 'sub-grid-row' : 'main-grid-row');
    gridRow.dataset.type = type;
    gridRow.dataset.id = id;
    gridRow.dataset.parentId = parentId;
    gridRow.style.display = row.style.display;

    let startIdx = diffDays(ganttMinDate, startDate);
    let duration = diffDays(startDate, endDate) + 1;
    if (duration < 1) duration = 1;

    const leftPx = Math.max(0, startIdx * DAY_CELL_WIDTH + 4);
    const widthPx = Math.max(28, duration * DAY_CELL_WIDTH - 8);

    const bar = document.createElement('div');
    bar.className = 'gantt-bar-item ' + (type === 'subtask' ? 'bar-sub' : 'bar-main');
    bar.style.left = leftPx + 'px';
    bar.style.width = widthPx + 'px';
    bar.title = `${title} (${row.dataset.start || '-'} ~ ${row.dataset.end || '-'})`;

    // 일정 막대(Bar) 클릭 시 상세 패널 오픈
    bar.onclick = function(e) {
      e.stopPropagation();
      onGanttItemClick(type, id, row);
    };

    bar.innerHTML = `
      <div class="gantt-bar-progress" style="width: ${Math.min(100, Math.max(0, progress))}%;"></div>
      <span class="gantt-bar-label">${title} (${Math.round(progress)}%)</span>
    `;

    gridRow.appendChild(bar);
    barsContainer.appendChild(gridRow);
  });
}

// ================= 3. 일정 클릭 시 우측 상세 패널 오픈 및 데이터 바인딩 =================
async function onGanttItemClick(type, id, leftRowElem) {
  // 선택 하이라이트 처리
  document.querySelectorAll('.gantt-tree-row, .gantt-grid-row').forEach(el => el.classList.remove('selected-gantt-row'));
  if (leftRowElem) leftRowElem.classList.add('selected-gantt-row');

  const gridRow = document.querySelector(`.gantt-grid-row[data-type="${type}"][data-id="${id}"]`);
  if (gridRow) gridRow.classList.add('selected-gantt-row');

  activeGanttContext = { type, id: Number(id) };

  const panel = document.getElementById('ganttDetailSidePanel');
  const typeBadge = document.getElementById('ganttDetailTypeBadge');
  const subtasksSection = document.getElementById('ganttDetailSubtasksSection');

  typeBadge.textContent = type === 'subtask' ? '하위 업무' : '메인 업무';
  subtasksSection.style.display = type === 'task' ? 'flex' : 'none';

  // 우선 DOM 데이터셋으로 즉시 패널 표시 (빠른 반응성)
  if (leftRowElem) {
    bindDetailPanelFields({
      title: leftRowElem.dataset.title,
      status: Number(leftRowElem.dataset.status || 0),
      startAt: leftRowElem.dataset.start,
      endAt: leftRowElem.dataset.end,
      progress: Number(leftRowElem.dataset.progress || 0),
      content: ''
    });
  }
  panel.classList.add('active');

  // 서버 API에서 최신 상세 데이터 비동기 조회
  try {
    if (type === 'task') {
      const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/api/tasks/${id}`);
      if (res.ok) {
        const task = await res.json();
        bindDetailPanelFields(task);
        renderDetailSubTasksList(task.subTasks || []);
      }
    } else if (type === 'subtask') {
      const parentId = leftRowElem ? leftRowElem.dataset.parentId : null;
      if (parentId) {
        const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/api/tasks/${parentId}`);
        if (res.ok) {
          const parentTask = await res.json();
          const sub = (parentTask.subTasks || []).find(st => Number(st.subTaskId) === Number(id));
          if (sub) bindDetailPanelFields(sub);
        }
      }
    }
  } catch (err) {
    console.error('일정 상세 조회 오류:', err);
  }
}

function bindDetailPanelFields(data) {
  document.getElementById('ganttDetailTitleInput').value = data.title || '';
  document.getElementById('ganttDetailCreatedDate').textContent = data.created || '';
  document.getElementById('ganttDetailContentText').value = data.content || '';
  document.getElementById('ganttDetailStartDate').value = data.startAt || '';
  document.getElementById('ganttDetailEndDate').value = data.endAt || '';

  const prog = data.progress != null ? Math.round(data.progress) : 0;
  document.getElementById('ganttDetailProgressRange').value = prog;
  document.getElementById('ganttDetailProgressLabel').textContent = prog + '%';

  if (data.projectMemberId) {
    document.getElementById('ganttDetailAssigneeSelect').value = String(data.projectMemberId);
  }

  const statusObj = ganttStatusMap.find(s => s.code === Number(data.status)) || ganttStatusMap[0];
  const badge = document.getElementById('ganttDetailStatusBadge');
  badge.className = 'badge-status ' + statusObj.class;
  document.getElementById('ganttDetailStatusText').textContent = statusObj.text;
}

function renderDetailSubTasksList(subTasks) {
  const box = document.getElementById('ganttDetailSubtaskBox');
  const countSpan = document.getElementById('ganttDetailSubtaskCount');
  box.innerHTML = '';
  countSpan.textContent = subTasks.length;

  if (subTasks.length === 0) {
    box.innerHTML = '<div style="padding:10px 14px; font-size:12.5px; color:var(--text-muted);">등록된 하위업무 일정이 없습니다.</div>';
    return;
  }

  subTasks.forEach(st => {
    const stStatus = ganttStatusMap.find(s => s.code === st.status) || ganttStatusMap[0];
    const row = document.createElement('div');
    row.className = 'detail-subtask-row';
    row.onclick = function() {
      const targetLeftRow = document.querySelector(`.gantt-tree-row[data-type="subtask"][data-id="${st.subTaskId}"]`);
      onGanttItemClick('subtask', st.subTaskId, targetLeftRow);
    };
    row.innerHTML = `
      <div style="display:flex; align-items:center; gap:8px; overflow:hidden;">
        <span class="badge-status ${stStatus.class}">${stStatus.text}</span>
        <span style="font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${st.title}</span>
      </div>
      <span style="font-size:11.5px; color:var(--text-sub); flex-shrink:0;">${st.startAt || '-'} ~ ${st.endAt || '-'}</span>
    `;
    box.appendChild(row);
  });
}

function closeGanttDetailPanel() {
  document.getElementById('ganttDetailSidePanel').classList.remove('active');
  document.querySelectorAll('.gantt-tree-row, .gantt-grid-row').forEach(el => el.classList.remove('selected-gantt-row'));
  activeGanttContext = { type: null, id: null };
}

// ================= 4. 상세 패널 실시간 수정 연동 (PATCH / PUT) =================
async function onGanttDetailFieldChange(fieldName, fieldValue) {
  if (!activeGanttContext.id) return;
  const { type, id } = activeGanttContext;

  const url = type === 'subtask'
    ? `/${currentDomainKey}/project/${currentProjectId}/api/tasks/subtasks/${id}/field`
    : `/${currentDomainKey}/project/${currentProjectId}/api/tasks/${id}/field`;
	

  try {
    await fetch(url, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fieldName, fieldValue })
    });

    // 좌측 트리 행 데이터셋 및 간트 바 즉시 갱신
    const leftRow = document.querySelector(`.gantt-tree-row[data-type="${type}"][data-id="${id}"]`);
    if (leftRow) {
      if (fieldName === 'title') {
        leftRow.dataset.title = fieldValue;
        leftRow.querySelector('.gantt-row-title').textContent = fieldValue;
      } else if (fieldName === 'startAt') {
        leftRow.dataset.start = fieldValue;
        leftRow.querySelector('.col-g-period').textContent = `${fieldValue || '-'} ~ ${leftRow.dataset.end || '-'}`;
      } else if (fieldName === 'endAt') {
        leftRow.dataset.end = fieldValue;
        leftRow.querySelector('.col-g-period').textContent = `${leftRow.dataset.start || '-'} ~ ${fieldValue || '-'}`;
      } else if (fieldName === 'progress') {
        leftRow.dataset.progress = fieldValue;
      }

      calculateTimelineRange();
      renderTimelineHeader();
      renderTimelineBars();
    }
  } catch (err) {
    console.error('간트 상세 필드 수정 오류:', err);
  }
}

async function cycleGanttDetailStatus() {
  if (!activeGanttContext.id) return;
  const { type, id } = activeGanttContext;

  const textSpan = document.getElementById('ganttDetailStatusText');
  const badge = document.getElementById('ganttDetailStatusBadge');
  const idx = ganttStatusMap.findIndex(s => s.text === textSpan.textContent.trim());
  const next = ganttStatusMap[(idx + 1) % ganttStatusMap.length];

  badge.className = 'badge-status ' + next.class;
  textSpan.textContent = next.text;

  await onGanttDetailFieldChange('status', next.code);

  const leftRow = document.querySelector(`.gantt-tree-row[data-type="${type}"][data-id="${id}"]`);
  if (leftRow) {
    leftRow.dataset.status = next.code;
    const rowBadge = leftRow.querySelector('.badge-status');
    if (rowBadge) {
      rowBadge.className = 'badge-status ' + next.class;
      rowBadge.textContent = next.text;
    }
  }
}

async function onGanttDetailContentChange(val) {
  if (!activeGanttContext.id) return;
  const { type, id } = activeGanttContext;

  if (type === 'task') {
    try {
      await fetch(`/${currentDomainKey}/project/${currentProjectId}/api/tasks/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: val.trim() })
      });
    } catch (err) {
      console.error('상세 내용 수정 오류:', err);
    }
  } else {
    await onGanttDetailFieldChange('content', val.trim());
  }
}

// ================= 5. 그룹 접기/펼치기, 스크롤 동기화, 리사이저 바 =================
function toggleGanttSubRows(btn) {
  const mainRow = btn.closest('.gantt-tree-row');
  const taskId = mainRow.dataset.id;
  const isCollapsed = btn.textContent.trim() === '＋';

  btn.textContent = isCollapsed ? '－' : '＋';

  document.querySelectorAll(`.gantt-tree-row.sub-row[data-parent-id="${taskId}"], .gantt-grid-row.sub-grid-row[data-parent-id="${taskId}"]`).forEach(el => {
    el.style.display = isCollapsed ? '' : 'none';
  });
}

function expandAllGanttGroups(expand) {
  document.querySelectorAll('.btn-gantt-toggle').forEach(btn => {
    btn.textContent = expand ? '－' : '＋';
  });
  document.querySelectorAll('.gantt-tree-row.sub-row, .gantt-grid-row.sub-grid-row').forEach(el => {
    el.style.display = expand ? '' : 'none';
  });
}

function scrollToToday() {
  const rightPane = document.getElementById('ganttRightPane');
  if (!rightPane || !ganttMinDate) return;

  const todayOffset = diffDays(ganttMinDate, new Date());
  const targetLeft = Math.max(0, todayOffset * DAY_CELL_WIDTH - rightPane.clientWidth / 3);
  rightPane.scrollTo({ left: targetLeft, behavior: 'smooth' });
}

function initScrollSync() {
  const leftBody = document.getElementById('ganttLeftBody');
  const rightPane = document.getElementById('ganttRightPane');
  if (!leftBody || !rightPane) return;

  rightPane.addEventListener('scroll', () => {
    leftBody.scrollTop = rightPane.scrollTop;
  });
}

function initGanttResizer() {
  const resizerBar = document.getElementById('ganttPaneResizerBar');
  const sidePanel = document.getElementById('ganttDetailSidePanel');
  const splitWrapper = document.getElementById('ganttSplitViewWrapper');
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
    if (e.key === 'Escape') closeGanttDetailPanel();
  });
}

function handleGanttFileUpload(input) {
  if (!input.files || input.files.length === 0) return;
  const file = input.files[0];
  const fileList = document.getElementById('ganttAttachedFileList');
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
  document.getElementById('ganttAttachedFileCount').textContent = `(${fileList.children.length})`;
}

// ================= 6. 날짜 유틸리티 함수 =================
function parseDateYMD(str) {
  if (!str || str === '-') return null;
  const parts = str.substring(0, 10).split('-');
  if (parts.length !== 3) return null;
  return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
}

function formatDateYMD(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function addDays(date, days) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  d.setDate(d.getDate() + days);
  return d;
}

function diffDays(d1, d2) {
  const utc1 = Date.UTC(d1.getFullYear(), d1.getMonth(), d1.getDate());
  const utc2 = Date.UTC(d2.getFullYear(), d2.getMonth(), d2.getDate());
  return Math.floor((utc2 - utc1) / (1000 * 60 * 60 * 24));
}
