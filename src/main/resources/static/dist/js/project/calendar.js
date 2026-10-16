/* ==========================================================================
   [JS] 캘린더 탭 스크립트 (주 단위 연속 막대 렌더링 & 상세 드로어 연동)
   ========================================================================== */
let calCurrentYear = new Date().getFullYear();
let calCurrentMonth = new Date().getMonth(); // 0 ~ 11
let allCalendarEvents = [];
let activePersonalScheduleId = null;

function getCalendarApiBaseUrl() {
  return `/${currentDomainKey}/project/${currentProjectId}/api/tasks`;
}

document.addEventListener('DOMContentLoaded', async function() {
  await fetchCalendarEvents();
  renderCalendarGrid();
});

// ================= 1. 서버에서 전체 일정(업무 + 하위업무 + 개인일정) 조회 =================
async function fetchCalendarEvents() {
  try {
    const res = await fetch(`${getCalendarApiBaseUrl()}/calendar-events`);
    if (res.ok) {
      allCalendarEvents = await res.json();
    }
  } catch (err) {
    console.error('캘린더 일정 로드 실패:', err);
    allCalendarEvents = [];
  }
}

// ================= 2. 월간 달력 그리드 렌더링 (주 단위 연속 바 방식) =================
function renderCalendarGrid() {
  const ymTitle = document.getElementById('calCurrentYearMonth');
  const grid = document.getElementById('calDaysGrid');
  if (!grid) return;

  ymTitle.textContent = `${calCurrentYear}년 ${String(calCurrentMonth + 1).padStart(2, '0')}월`;
  grid.innerHTML = '';

  const showMain = document.getElementById('filterMainTask')?.checked ?? true;
  const showSub = document.getElementById('filterSubTask')?.checked ?? true;
  const showPersonal = document.getElementById('filterPersonal')?.checked ?? true;

  // 필터링된 일정 목록
  const visibleEvents = allCalendarEvents.filter(ev => {
    if (ev.scheduleType === 'TASK' && !showMain) return false;
    if (ev.scheduleType === 'SUB_TASK' && !showSub) return false;
    if (ev.scheduleType === 'PERSONAL' && !showPersonal) return false;
    return !!(ev.startAt || ev.endAt);
  });

  const firstDayOfMonth = new Date(calCurrentYear, calCurrentMonth, 1);
  const startDayOfWeek = firstDayOfMonth.getDay(); // 0(일) ~ 6(토)
  const calendarStartDate = new Date(calCurrentYear, calCurrentMonth, 1 - startDayOfWeek);
  const todayStr = formatCalDate(new Date());

  // 총 6주(Week) 렌더링
  for (let weekIdx = 0; weekIdx < 6; weekIdx++) {
    const weekDates = [];
    for (let d = 0; d < 7; d++) {
      const cellDate = new Date(
        calendarStartDate.getFullYear(),
        calendarStartDate.getMonth(),
        calendarStartDate.getDate() + (weekIdx * 7 + d)
      );
      weekDates.push({
        dateObj: cellDate,
        dateStr: formatCalDate(cellDate),
        isCurrentMonth: cellDate.getMonth() === calCurrentMonth,
        dayOfWeek: cellDate.getDay()
      });
    }

    const weekStartStr = weekDates[0].dateStr;
    const weekEndStr = weekDates[6].dateStr;

    const weekRow = document.createElement('div');
    weekRow.className = 'cal-week-row';

    // 1) 배경 7칸 (날짜 클릭 시 개인 일정 추가 모달 오픈)
    const bgGrid = document.createElement('div');
    bgGrid.className = 'cal-week-bg-grid';

    // 2) 상단 날짜 숫자 줄
    const datesRow = document.createElement('div');
    datesRow.className = 'cal-week-dates-row';

    weekDates.forEach(wd => {
      // 배경 셀
      const bgCell = document.createElement('div');
      bgCell.className = 'cal-day-bg-cell';
      if (!wd.isCurrentMonth) bgCell.classList.add('other-month');
      if (wd.dateStr === todayStr) bgCell.classList.add('today-cell');
      bgCell.onclick = () => openPersonalEventModal(wd.dateStr);
      bgGrid.appendChild(bgCell);

      // 날짜 라벨
      const labelBox = document.createElement('div');
      labelBox.className = 'cal-date-label-box';
      if (!wd.isCurrentMonth) labelBox.classList.add('other-month');
      if (wd.dateStr === todayStr) labelBox.classList.add('today-cell');
      if (wd.dayOfWeek === 0) labelBox.classList.add('sun-cell');
      if (wd.dayOfWeek === 6) labelBox.classList.add('sat-cell');
      labelBox.innerHTML = `<span class="cal-date-num">${wd.dateObj.getDate()}</span>`;
      datesRow.appendChild(labelBox);
    });

    weekRow.appendChild(bgGrid);
    weekRow.appendChild(datesRow);

    // 3) 이번 주(weekStartStr ~ weekEndStr)에 걸친 일정 추출 및 정렬
    const weekSegments = [];
    visibleEvents.forEach(ev => {
      const evStart = (ev.startAt || ev.endAt).substring(0, 10);
      const evEnd = (ev.endAt || ev.startAt).substring(0, 10);
      if (evEnd < weekStartStr || evStart > weekEndStr) return;

      // 이번 주 내에서의 시작 열(0~6)과 종료 열(0~6) 계산
      let startCol = 0;
      let endCol = 6;
      for (let c = 0; c < 7; c++) {
        if (weekDates[c].dateStr === evStart) startCol = c;
        if (weekDates[c].dateStr === evEnd) endCol = c;
      }
      if (evStart < weekStartStr) startCol = 0;
      if (evEnd > weekEndStr) endCol = 6;

      weekSegments.push({
        ev: ev,
        startCol: startCol,
        endCol: endCol,
        span: endCol - startCol + 1,
        continuesLeft: evStart < weekStartStr,
        continuesRight: evEnd > weekEndStr
      });
    });

    // 시작 열이 빠른 순 -> 기간(span)이 긴 순 -> 메인업무 우선 순으로 정렬
    const typePriority = { 'TASK': 1, 'SUB_TASK': 2, 'PERSONAL': 3 };
    weekSegments.sort((a, b) => {
      if (a.startCol !== b.startCol) return a.startCol - b.startCol;
      if (b.span !== a.span) return b.span - a.span;
      return (typePriority[a.ev.scheduleType] || 9) - (typePriority[b.ev.scheduleType] || 9);
    });

    // 4) 슬롯(행 트랙) 배정: 겹치는 일정끼리 자동으로 아랫줄로 내려가며 한 줄로 쭉 이어짐
    const tracks = []; // 각 트랙은 [false, false, false, false, false, false, false]
    const eventsGrid = document.createElement('div');
    eventsGrid.className = 'cal-week-events-grid';

    weekSegments.forEach(seg => {
      let assignedTrack = -1;
      for (let t = 0; t < tracks.length; t++) {
        let canPlace = true;
        for (let c = seg.startCol; c <= seg.endCol; c++) {
          if (tracks[t][c]) {
            canPlace = false;
            break;
          }
        }
        if (canPlace) {
          assignedTrack = t;
          break;
        }
      }

      if (assignedTrack === -1) {
        assignedTrack = tracks.length;
        tracks.push([false, false, false, false, false, false, false]);
      }

      for (let c = seg.startCol; c <= seg.endCol; c++) {
        tracks[assignedTrack][c] = true;
      }

      const bar = document.createElement('div');
      let typeClass = 'type-personal';
      let prefixIcon = '📌 ';

      if (seg.ev.scheduleType === 'TASK') {
        typeClass = 'type-task';
        prefixIcon = '🌿 ';
      } else if (seg.ev.scheduleType === 'SUB_TASK') {
        typeClass = 'type-subtask';
        prefixIcon = 'ㄴ ';
      }

      bar.className = `cal-span-bar ${typeClass}`;
      if (seg.continuesLeft) bar.classList.add('continues-left');
      if (seg.continuesRight) bar.classList.add('continues-right');

      // CSS Grid 위치 지정 (1-based index)
      bar.style.gridColumn = `${seg.startCol + 1} / ${seg.endCol + 2}`;
      bar.style.gridRow = `${assignedTrack + 1}`;

      bar.textContent = prefixIcon + (seg.ev.title || '');
      bar.title = `${seg.ev.title} (${seg.ev.startAt || ''} ~ ${seg.ev.endAt || ''})`;

      bar.onclick = function(e) {
        e.stopPropagation();
        onCalendarEventClick(seg.ev);
      };

      eventsGrid.appendChild(bar);
    });

    weekRow.appendChild(eventsGrid);
    grid.appendChild(weekRow);
  }
}

function changeCalendarMonth(delta) {
  calCurrentMonth += delta;
  if (calCurrentMonth < 0) {
    calCurrentMonth = 11;
    calCurrentYear--;
  } else if (calCurrentMonth > 11) {
    calCurrentMonth = 0;
    calCurrentYear++;
  }
  renderCalendarGrid();
}

function goCalendarToday() {
  const now = new Date();
  calCurrentYear = now.getFullYear();
  calCurrentMonth = now.getMonth();
  renderCalendarGrid();
}

// ================= 3. 일정 클릭 시 분기 처리 (업무 드로어 패널 vs 개인 일정 모달) =================
async function onCalendarEventClick(ev) {
  if (ev.scheduleType === 'TASK') {
    if (typeof openDetailPanel === 'function') {
      await openDetailPanel(ev.taskId || ev.scheduleId, null);
    }
  } else if (ev.scheduleType === 'SUB_TASK') {
    if (typeof openSubTaskDetailPanel === 'function') {
      await openSubTaskDetailPanel(ev.subTaskId || ev.scheduleId, null);
    }
  } else {
    activePersonalScheduleId = ev.scheduleId;
    document.getElementById('viewPersonalTitle').textContent = ev.title || '';
    document.getElementById('viewPersonalPeriod').textContent = `📅 ${ev.startAt || '-'} ~ ${ev.endAt || '-'}`;
    document.getElementById('viewPersonalContent').textContent = ev.content || '등록된 메모가 없습니다.';
    document.getElementById('personalEventDetailModal').classList.add('show');
  }
}

function closePersonalDetailModal() {
  document.getElementById('personalEventDetailModal').classList.remove('show');
  activePersonalScheduleId = null;
}

// ================= 4. 개인 일정 추가 & 삭제 연동 =================
function openPersonalEventModal(defaultDateStr) {
  const dateStr = defaultDateStr || formatCalDate(new Date());
  document.getElementById('personalEventTitleInput').value = '';
  document.getElementById('personalEventStartInput').value = dateStr;
  document.getElementById('personalEventEndInput').value = dateStr;
  document.getElementById('personalEventContentInput').value = '';

  document.getElementById('personalEventCreateModal').classList.add('show');
  setTimeout(() => document.getElementById('personalEventTitleInput').focus(), 50);
}

function closePersonalEventModal() {
  document.getElementById('personalEventCreateModal').classList.remove('show');
}

async function submitPersonalEventModal() {
  const titleInput = document.getElementById('personalEventTitleInput');
  const startInput = document.getElementById('personalEventStartInput');
  const endInput = document.getElementById('personalEventEndInput');
  const contentInput = document.getElementById('personalEventContentInput');

  const title = titleInput.value.trim();
  const startAt = startInput.value;
  const endAt = endInput.value || startAt;
  const content = contentInput.value.trim();

  if (!title) {
    alert('개인 일정 제목을 입력해 주세요.');
    titleInput.focus();
    return;
  }
  if (!startAt) {
    alert('시작일을 선택해 주세요.');
    startInput.focus();
    return;
  }

  try {
    const res = await fetch(`${getCalendarApiBaseUrl()}/calendar-events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        scheduleType: 'PERSONAL',
        projectId: currentProjectId,
        title: title,
        startAt: startAt,
        endAt: endAt,
        content: content
      })
    });

    if (!res.ok) throw new Error('개인 일정 등록 실패');

    closePersonalEventModal();
    await fetchCalendarEvents();
    renderCalendarGrid();
  } catch (err) {
    console.error('개인 일정 등록 오류:', err);
    alert('일정 등록 중 오류가 발생했습니다.');
  }
}

async function deleteActivePersonalEvent() {
  if (!activePersonalScheduleId) return;
  if (!confirm('이 개인 일정을 삭제하시겠습니까?')) return;

  try {
    const res = await fetch(`${getCalendarApiBaseUrl()}/calendar-events/${activePersonalScheduleId}`, {
      method: 'DELETE'
    });
    if (!res.ok) throw new Error('삭제 실패');

    closePersonalDetailModal();
    await fetchCalendarEvents();
    renderCalendarGrid();
  } catch (err) {
    console.error('개인 일정 삭제 오류:', err);
  }
}

function formatCalDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    closePersonalEventModal();
    closePersonalDetailModal();
    if (typeof closeDetailPanel === 'function') closeDetailPanel();
  }
});