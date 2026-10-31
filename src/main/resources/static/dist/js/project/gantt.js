/* ==========================================================================
   [JS] 간트차트 렌더링 & 일정 클릭 시 공통 상세 패널(detail_panel) 연동
   ========================================================================== */
const DAY_CELL_WIDTH = 36; // 하루당 픽셀 너비

// detail_panel.js의 DETAIL_STATUS_MAP과 상태 코드 통일 (0:요청, 1:진행, 2:완료, 3:보류)
const ganttStatusMap = [
  { code: 0, text: '● 요청', class: 'badge-req' },
  { code: 1, text: '● 진행', class: 'badge-prog' },
  { code: 2, text: '● 완료', class: 'badge-done' },
  { code: 3, text: '● 보류', class: 'badge-hold' }
];

let ganttMinDate = null;
let ganttMaxDate = null;
let ganttTotalDays = 0;

document.addEventListener('DOMContentLoaded', async function() {
  await initializeGanttChart();
  initScrollSync();
});

// ================= 1. 간트차트 초기화 및 데이터 로드 =================
async function initializeGanttChart() {
  const leftBody = document.getElementById('ganttLeftBody');
  const existingRows = leftBody.querySelectorAll('.gantt-tree-row');

  // SSR로 렌더링된 행이 없다면 API에서 업무 목록을 가져와 렌더링 시도
  if (existingRows.length === 0) {
    await loadTasksFromApi();
  }

  calculateTimelineRange();
  renderTimelineHeader();
  renderTimelineBars();
  scrollToToday();
}

async function loadTasksFromApi() {
  const leftBody = document.getElementById('ganttLeftBody');
  let tasks;
  try {
    const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/api/tasks`);
    if (!res.ok) throw new Error('업무 목록 조회 실패');
    tasks = await res.json();
    if (!Array.isArray(tasks)) throw new Error('잘못된 업무 목록 응답');
  } catch (err) {
    console.warn('간트차트 업무 목록 조회 실패', err);
    leftBody.textContent = '업무 목록을 불러오지 못했습니다. 새로고침해 다시 시도하세요.';
    return;
  }

  if (tasks.length === 0) {
    leftBody.textContent = '등록된 업무가 없습니다.';
    return;
  }

  leftBody.innerHTML = '';
  tasks.forEach(task => {
    const hasSub = task.subTasks && task.subTasks.length > 0;
    const statusObj = ganttStatusMap.find(s => s.code === Number(task.status)) || ganttStatusMap[0];

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
        <span class="gantt-row-title"></span>
      </div>
      <div class="col-g-status">
        <span class="badge-status ${statusObj.class}">${statusObj.text}</span>
      </div>
      <div class="col-g-period"></div>
    `;
    mainRow.querySelector('.gantt-row-title').textContent = mainRow.dataset.title;
    mainRow.querySelector('.col-g-period').textContent = `${task.startAt || '-'} ~ ${task.endAt || '-'}`;
    leftBody.appendChild(mainRow);

    if (hasSub) {
      task.subTasks.forEach(sub => {
        const subStatusObj = ganttStatusMap.find(s => s.code === Number(sub.status)) || ganttStatusMap[0];
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
            <span class="gantt-row-title"></span>
          </div>
          <div class="col-g-status">
            <span class="badge-status ${subStatusObj.class}">${subStatusObj.text}</span>
          </div>
          <div class="col-g-period"></div>
        `;
        subRow.querySelector('.gantt-row-title').textContent = subRow.dataset.title;
        subRow.querySelector('.col-g-period').textContent = `${sub.startAt || '-'} ~ ${sub.endAt || '-'}`;
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
    if (row.classList.contains('selected-gantt-row')) {
      gridRow.classList.add('selected-gantt-row');
    }
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

    // 일정 막대(Bar) 클릭 시 공통 상세 패널(detail_panel) 오픈
    bar.onclick = function(e) {
      e.stopPropagation();
      onGanttItemClick(type, id, row);
    };

    const fill = document.createElement('div');
    fill.className = 'gantt-bar-progress';
    fill.style.width = Math.min(100, Math.max(0, progress)) + '%';
    const label = document.createElement('span');
    label.className = 'gantt-bar-label';
    label.textContent = `${title} (${Math.round(progress)}%)`;
    bar.append(fill, label);

    gridRow.appendChild(bar);
    barsContainer.appendChild(gridRow);
  });
}

// ================= 3. 일정 클릭 시 공통 상세 패널(detail_panel.js) 오픈 =================
async function onGanttItemClick(type, id, leftRowElem) {
  if (!id) return;

  const targetLeftRow = leftRowElem || document.querySelector(`.gantt-tree-row[data-type="${type}"][data-id="${id}"]`);

  // 간트차트 좌측 트리 행 및 우측 타임라인 행 선택 하이라이트 처리
  document.querySelectorAll('.gantt-tree-row, .gantt-grid-row').forEach(el => el.classList.remove('selected-gantt-row'));
  if (targetLeftRow) targetLeftRow.classList.add('selected-gantt-row');

  const gridRow = document.querySelector(`.gantt-grid-row[data-type="${type}"][data-id="${id}"]`);
  if (gridRow) gridRow.classList.add('selected-gantt-row');

  // 공통 상세 패널(detail_panel.js) 함수 호출로 연결
  if (type === 'task') {
    if (typeof openDetailPanel === 'function') {
      await openDetailPanel(id, targetLeftRow);
    }
  } else if (type === 'subtask') {
    if (typeof openSubTaskDetailPanel === 'function') {
      await openSubTaskDetailPanel(id, targetLeftRow);
    }
  }
}

// ================= 4. 그룹 접기/펼치기, 스크롤 동기화 =================
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

// ================= 5. 날짜 유틸리티 함수 =================
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
