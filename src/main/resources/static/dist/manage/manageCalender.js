(function () {
    'use strict';
    document.addEventListener('DOMContentLoaded', function () {
        const calendarEl = document.getElementById('myTaskCalendar');
        const panel = document.getElementById('mcDetailPanel');
        if (!calendarEl || !panel || typeof FullCalendar === 'undefined') return;

        const source = window.manageCalendarData || {};
        const projects = Array.isArray(source.projects) ? source.projects : [];
        const tasks = Array.isArray(source.tasks) ? source.tasks : [];
        const projectById = new Map(projects.map(function (item) { return [String(item.projectId), item]; }));
        const taskById = new Map(tasks.map(function (item) { return [String(item.taskId), item]; }));
        const typeEl = document.getElementById('mcPanelType');
        const titleEl = document.getElementById('mcPanelTitle');
        const subtitleEl = document.getElementById('mcPanelSubtitle');
        const infoEl = document.getElementById('mcPanelInfo');
        const contentEl = document.getElementById('mcPanelContent');
        const relatedEl = document.getElementById('mcRelatedList');
        const relatedTitle = document.getElementById('mcRelatedTitle');
        const relatedCount = document.getElementById('mcRelatedCount');
        const relatedSection = document.getElementById('mcRelatedSection');
        const closeBtn = document.getElementById('mcPanelClose');
        let lastCalendarFocus = null;

        function text(value, fallback) {
            return value === null || value === undefined || value === '' ? (fallback === undefined ? '-' : fallback) : String(value);
        }
        function percent(value) {
            const number = Number(value);
            return Number.isFinite(number) ? Math.max(0, Math.min(100, number)) + '%' : '0%';
        }
        function nextDate(dateText) {
            const date = new Date(String(dateText) + 'T00:00:00Z');
            if (Number.isNaN(date.getTime())) return null;
            date.setUTCDate(date.getUTCDate() + 1);
            return date.toISOString().slice(0, 10);
        }
        function period(item) { return text(item.startAt) + ' ~ ' + text(item.endAt); }
        function projectStatus(value) {
            return ({ 0: '예정', 1: '진행 중', 2: '완료' })[value] || '상태 미정';
        }
        function taskStatus(value) {
            return ({ 0: '할 일', 1: '진행 중', 2: '검토 중', 3: '완료' })[value] || '상태 미정';
        }
        function addInfo(label, value) {
            const key = document.createElement('span');
            key.className = 'mc-info-label';
            key.textContent = label;
            const data = document.createElement('strong');
            data.className = 'mc-info-value';
            data.textContent = text(value);
            infoEl.append(key, data);
        }
        function showPanel(type, title, subtitle, content, fields) {
            if (!panel.classList.contains('mc-open')) lastCalendarFocus = document.activeElement;
            typeEl.textContent = type;
            titleEl.textContent = text(title);
            subtitleEl.textContent = text(subtitle, '');
            contentEl.textContent = text(content, '등록된 내용이 없습니다.');
            infoEl.replaceChildren();
            fields.forEach(function (field) { addInfo(field[0], field[1]); });
            relatedEl.replaceChildren();
            panel.inert = false;
            panel.setAttribute('aria-hidden', 'false');
            panel.classList.add('mc-open');
            closeBtn.focus({ preventScroll: true });
        }
        function addRelated(title, description, onClick) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'mc-related-item';
            const heading = document.createElement('strong');
            heading.textContent = text(title);
            const detail = document.createElement('small');
            detail.textContent = text(description, '');
            button.append(heading, detail);
            button.addEventListener('click', onClick);
            relatedEl.append(button);
        }
        // 프로젝트 상세 아래에 업무별 담당자·상태·진행률을 함께 표시한다.
        function addProjectTask(task) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'mc-task-card';

            const top = document.createElement('div');
            top.className = 'mc-task-top';
            const name = document.createElement('strong');
            name.className = 'mc-task-name';
            name.textContent = text(task.title, '이름 없는 업무');
            const state = document.createElement('span');
            state.className = 'mc-task-state mc-task-state-' + text(task.status, '0');
            state.textContent = taskStatus(task.status);
            top.append(name, state);

            const detail = document.createElement('div');
            detail.className = 'mc-task-meta';
            const assignee = document.createElement('span');
            assignee.textContent = '담당자  ' + text(task.assignee, '미배정');
            const progress = document.createElement('strong');
            progress.textContent = percent(task.progress);
            detail.append(assignee, progress);

            const bar = document.createElement('div');
            bar.className = 'mc-task-progress';
            bar.setAttribute('role', 'progressbar');
            bar.setAttribute('aria-label', text(task.title) + ' 진행률');
            bar.setAttribute('aria-valuemin', '0');
            bar.setAttribute('aria-valuemax', '100');
            const number = Number(task.progress);
            const value = Number.isFinite(number) ? Math.max(0, Math.min(100, number)) : 0;
            bar.setAttribute('aria-valuenow', String(value));
            const fill = document.createElement('span');
            fill.style.width = value + '%';
            bar.append(fill);

            const dates = document.createElement('small');
            dates.className = 'mc-task-dates';
            dates.textContent = period(task);
            button.append(top, detail, bar, dates);
            button.addEventListener('click', function () { showTask(task.taskId); });
            relatedEl.append(button);
        }
        function showProject(id) {
            const project = projectById.get(String(id));
            if (!project) return;
            const relatedTasks = tasks.filter(function (task) {
                return String(task.projectId) === String(project.projectId);
            });
            showPanel('프로젝트', project.projectName, period(project), project.content, [
                ['상태', projectStatus(project.status)],
                ['시작일', project.startAt],
                ['종료일', project.endAt],
                ['진행률', percent(project.progress)],
                ['업무 수', relatedTasks.length + '개']
            ]);
            relatedSection.hidden = false;
            relatedTitle.textContent = '프로젝트 업무 목록';
            relatedCount.textContent = relatedTasks.length + '개';
            if (relatedTasks.length === 0) {
                const empty = document.createElement('p');
                empty.className = 'mc-related-empty';
                empty.textContent = '등록된 업무가 없습니다.';
                relatedEl.append(empty);
            }
            relatedTasks.sort(function (a, b) {
                return String(a.startAt || '').localeCompare(String(b.startAt || ''));
            });
            relatedTasks.forEach(function (task) {
                addProjectTask(task);
            });
        }
        function showTask(id) {
            const task = taskById.get(String(id));
            if (!task) return;
            const project = projectById.get(String(task.projectId));
            showPanel('업무', task.title, text(task.projectName, '') + ' · ' + period(task), task.content, [
                ['상태', taskStatus(task.status)],
                ['담당자', text(task.assignee, '미배정')],
                ['시작일', task.startAt],
                ['마감일', task.endAt],
                ['진행률', percent(task.progress)]
            ]);
            relatedSection.hidden = false;
            relatedTitle.textContent = '소속 프로젝트';
            relatedCount.textContent = project ? '1개' : '0개';
            if (project) {
                addRelated(project.projectName, '프로젝트 전체 기간 · ' + period(project),
                    function () { showProject(project.projectId); });
            } else {
                const empty = document.createElement('p');
                empty.className = 'mc-related-empty';
                empty.textContent = '소속 프로젝트 정보를 조회할 수 없습니다.';
                relatedEl.append(empty);
            }
        }
        function closePanel() {
            panel.classList.remove('mc-open');
            panel.setAttribute('aria-hidden', 'true');
            panel.inert = true;
            if (lastCalendarFocus && typeof lastCalendarFocus.focus === 'function') {
                lastCalendarFocus.focus({ preventScroll: true });
            }
        }
        closeBtn.addEventListener('click', closePanel);
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && panel.classList.contains('mc-open')) closePanel();
        });

        // 프로젝트별로 고유한 밝은 파스텔색을 사용한다. 같은 프로젝트의 모든 날짜는 같은 색상이다.
        // 단순 반복 팔레트가 아닌 프로젝트 ID 기반 색상이므로 월 이동 후에도 동일하게 표시된다.
        function projectColor(projectId) {
            const key = String(projectId);
            let hash = 0;
            for (let i = 0; i < key.length; i++) {
                hash = (Math.imul(hash, 31) + key.charCodeAt(i)) | 0;
            }
            const hue = Math.round(((hash >>> 0) * 137.508) % 360);
            return {
                background: 'hsl(' + hue + ', 76%, 87%)',
                border: 'hsl(' + hue + ', 58%, 69%)',
                text: '#203548',
                dot: 'hsl(' + hue + ', 62%, 62%)'
            };
        }
        function renderProjectLegend() {
            const legendEl = document.getElementById('mcProjectLegend');
            if (!legendEl) return;
            legendEl.replaceChildren();
            if (!projects.length) {
                const empty = document.createElement('span');
                empty.className = 'mc-project-legend-empty';
                empty.textContent = '표시할 프로젝트가 없습니다.';
                legendEl.append(empty);
                return;
            }
            projects.forEach(function (project) {
                const colors = projectColor(project.projectId);
                const item = document.createElement('span');
                item.className = 'mc-project-legend-item';
                item.style.backgroundColor = colors.background;
                item.style.borderColor = colors.border;
                const dot = document.createElement('span');
                dot.className = 'mc-project-legend-dot';
                dot.style.backgroundColor = colors.dot;
                const name = document.createElement('span');
                name.textContent = text(project.projectName, '이름 없는 프로젝트');
                item.append(dot, name);
                legendEl.append(item);
            });
        }
        renderProjectLegend();

        // 달력에는 프로젝트 기간만 표시하고, 업무는 프로젝트 클릭 후 우측 패널에서 확인한다.
        const projectEvents = [];
        projects.forEach(function (project) {
            if (!project.startAt || !project.endAt || project.startAt > project.endAt) return;
            const end = nextDate(project.endAt);
            if (!end) return;
            const colors = projectColor(project.projectId);
            projectEvents.push({
                id: 'project-' + project.projectId,
                title: text(project.projectName),
                start: project.startAt,
                end: end, // FullCalendar 종료일은 exclusive
                allDay: true,
                backgroundColor: colors.background,
                borderColor: colors.border,
                textColor: colors.text,
                classNames: ['mc-event-project'],
                extendedProps: { projectId: project.projectId, status: project.status }
            });
        });
        const calendar = new FullCalendar.Calendar(calendarEl, {
            initialView: 'dayGridMonth',
            locale: 'ko',
            height: 'auto',
            fixedWeekCount: false,
            expandRows: false,
            dayMaxEvents: false, // 일정이 5개를 넘어도 '+n개'로 숨기지 않는다.
            dayMaxEventRows: false, // 일정 개수에 맞춰 각 주간 행이 높아진다.
            headerToolbar: { left: 'prev,next today', center: 'title', right: '' },
            events: projectEvents,
            eventContent: function (info) {
                const wrapper = document.createElement('span');
                wrapper.className = 'mc-event-inner';
                const name = document.createElement('span');
                name.className = 'mc-event-name';
                name.textContent = info.event.title;
                const status = document.createElement('span');
                const statusValue = info.event.extendedProps.status;
                status.className = 'mc-event-status mc-event-status-' + text(statusValue, '0');
                status.textContent = projectStatus(statusValue);
                wrapper.append(name, status);
                return { domNodes: [wrapper] };
            },
            eventDidMount: function (info) {
                info.el.title = info.event.title + ' · ' + projectStatus(info.event.extendedProps.status)
                    + ' (클릭하면 담당 업무를 확인할 수 있습니다)';
            },
            eventClick: function (info) {
                info.jsEvent.preventDefault();
                showProject(info.event.extendedProps.projectId);
            }
        });
        calendar.render();
    });
})();
