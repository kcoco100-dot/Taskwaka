(function () {
    'use strict';

    // 프로젝트 및 업무 간트차트 조회
    document.addEventListener('DOMContentLoaded', function () {
        const page = document.getElementById('manageGanttPage');
        if (!page) {
            return;
        }

        const data = window.manageGanttData || {};
        const board = document.getElementById('mgBoard');
        const scroll = document.getElementById('mgScroll');
        const panel = document.getElementById('mgDetail');
        const form = document.getElementById('mgSearchForm');
        const projectFilter = document.getElementById('mgProjectFilter');
        const schType = document.getElementById('mgSchType');
        const kwd = document.getElementById('mgKwd');
        const dayMs = 86400000;
        const views = {
            day: { days: 42, width: 36, tick: 1 },
            week: { days: 84, width: 14, tick: 7 },
            month: { days: 180, width: 7, tick: 0 }
        };
        const projectStatuses = { 0: '예정', 1: '진행 중', 2: '완료' };
        const taskStatuses = { 0: '할 일', 1: '진행 중', 2: '검토 중', 3: '완료', 4: '보류' };
        const items = new Map();
        const tasksByProject = new Map();
        const sourceProjects = Array.isArray(data.projects) ? data.projects : [];
        const sourceTasks = Array.isArray(data.tasks) ? data.tasks : [];
        let view = 'day';
        let rangeStart;
        let selectedKey = null;
        let lastFocusKey = null;
        let collapsed = new Set();
        let filter = { project: 'all', schType: 'all', kwd: '' };

        function text(value, fallback) {
            return value === null || value === undefined || value === ''
                ? (fallback === undefined ? '-' : fallback) : String(value);
        }

        function node(tag, className, value) {
            const element = document.createElement(tag);
            if (className) {
                element.className = className;
            }
            if (value !== undefined) {
                element.textContent = value;
            }
            return element;
        }

        function parseDay(value) {
            if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
                return null;
            }
            const date = new Date(value + 'T00:00:00Z');
            if (!Number.isFinite(date.getTime()) || date.toISOString().slice(0, 10) !== value) {
                return null;
            }
            return date.getTime() / dayMs;
        }

        function iso(day) {
            return new Date(day * dayMs).toISOString().slice(0, 10);
        }

        function shortDate(day) {
            return iso(day).replaceAll('-', '.');
        }

        function monday(day) {
            const weekday = new Date(day * dayMs).getUTCDay();
            return day - (weekday + 6) % 7;
        }

        function today() {
            const now = new Date();
            return parseDay(now.getFullYear() + '-'
                + String(now.getMonth() + 1).padStart(2, '0') + '-'
                + String(now.getDate()).padStart(2, '0'));
        }

        function percent(value) {
            const number = Number(value);
            return Number.isFinite(number) ? Math.max(0, Math.min(100, number)) : 0;
        }

        function normalize(source, type) {
            const item = Object.assign({}, source);
            item.type = type;
            item.key = type + ':' + String(type === 'project' ? item.projectId : item.taskId);
            item.name = text(type === 'project' ? item.projectName : item.title, '이름 미정');
            item.start = parseDay(item.startAt);
            item.end = parseDay(item.endAt);
            item.validPeriod = item.start !== null && item.end !== null && item.end >= item.start;
            item.progress = percent(item.progress);
            item.statusName = text((type === 'project' ? projectStatuses : taskStatuses)[item.status], '상태 미정');
            items.set(item.key, item);
            return item;
        }

        const projects = sourceProjects.map(function (source) {
            return normalize(source, 'project');
        });
        const projectIds = new Set(projects.map(function (item) {
            return String(item.projectId);
        }));
        sourceTasks.forEach(function (source) {
            const id = String(source.projectId);
            if (!projectIds.has(id)) {
                return;
            }
            if (!tasksByProject.has(id)) {
                tasksByProject.set(id, []);
            }
            tasksByProject.get(id).push(normalize(source, 'task'));
        });
        projects.forEach(function (project) {
            const option = node('option', '', project.name);
            option.value = String(project.projectId);
            projectFilter.append(option);
        });

        function grouped() {
            const keyword = filter.kwd.toLocaleLowerCase();
            const result = [];
            projects.forEach(function (project) {
                if (filter.project !== 'all' && String(project.projectId) !== filter.project) {
                    return;
                }
                const tasks = tasksByProject.get(String(project.projectId)) || [];
                const projectMatch = (filter.schType === 'all' || filter.schType === 'project')
                    && project.name.toLocaleLowerCase().includes(keyword);
                const matches = tasks.filter(function (task) {
                    if (!keyword || projectMatch) {
                        return true;
                    }
                    const taskMatch = (filter.schType === 'all' || filter.schType === 'task')
                        && task.name.toLocaleLowerCase().includes(keyword);
                    const personMatch = (filter.schType === 'all' || filter.schType === 'assignee')
                        && text(task.assignee, '').toLocaleLowerCase().includes(keyword);
                    return taskMatch || personMatch;
                });
                if (!keyword || projectMatch || matches.length > 0) {
                    result.push({ project: project, tasks: matches });
                }
            });
            return result;
        }

        function positionAround(day) {
            if (view === 'month') {
                rangeStart = parseDay(iso(day).slice(0, 7) + '-01');
            } else {
                rangeStart = monday(day) - 7;
            }
        }

        function locateData(groups) {
            const dates = [];
            groups.forEach(function (group) {
                [group.project].concat(group.tasks).forEach(function (item) {
                    if (item.validPeriod) {
                        dates.push(item.start, item.end);
                    }
                });
            });
            let target = today();
            if (dates.length > 0) {
                const first = dates.reduce(function (min, day) { return Math.min(min, day); }, dates[0]);
                const last = dates.reduce(function (max, day) { return Math.max(max, day); }, dates[0]);
                target = Math.min(last, Math.max(first, target));
            }
            positionAround(target);
        }

        function monthParts(start, count) {
            const parts = [];
            for (let index = 0; index < count; index++) {
                const key = iso(start + index).slice(0, 7);
                const previous = parts[parts.length - 1];
                if (previous && previous.key === key) {
                    previous.days++;
                } else {
                    parts.push({ key: key, days: 1 });
                }
            }
            return parts;
        }

        function makeHeader() {
            const settings = views[view];
            const header = node('div', 'mg-board-header');
            const label = node('div', 'mg-label-heading');
            label.append(node('span', '', '프로젝트 / 업무'), node('small', '', '상태'));
            const timeline = node('div', 'mg-time-heading');
            const months = node('div', 'mg-months');
            const ticks = node('div', 'mg-ticks');
            months.setAttribute('aria-hidden', 'true');
            ticks.setAttribute('aria-hidden', 'true');
            const parts = monthParts(rangeStart, settings.days);
            parts.forEach(function (part) {
                const cell = node('span', '', part.key.replace('-', '년 ') + '월');
                cell.style.width = part.days * settings.width + 'px';
                months.append(cell);
                if (view === 'month') {
                    const tick = node('span', '', part.key.slice(5) + '월');
                    tick.style.width = part.days * settings.width + 'px';
                    ticks.append(tick);
                }
            });
            if (view !== 'month') {
                for (let index = 0; index < settings.days; index += settings.tick) {
                    const day = rangeStart + index;
                    const date = new Date(day * dayMs);
                    const dayText = view === 'day' ? String(date.getUTCDate()) : iso(day).slice(5).replace('-', '/');
                    const tick = node('span', '', dayText);
                    tick.style.width = settings.width * settings.tick + 'px';
                    if (view === 'day') {
                        tick.append(node('small', '', ['일', '월', '화', '수', '목', '금', '토'][date.getUTCDay()]));
                        tick.classList.toggle('mg-weekend', date.getUTCDay() === 0 || date.getUTCDay() === 6);
                    }
                    if (today() >= day && today() < day + settings.tick) {
                        tick.classList.add('mg-is-today');
                    }
                    ticks.append(tick);
                }
            }
            timeline.append(months, ticks);
            header.append(label, timeline);
            return header;
        }

        function makeTrack(item) {
            const settings = views[view];
            const end = rangeStart + settings.days - 1;
            const track = node('div', 'mg-track');
            if (view === 'day') {
                for (let offset = 0; offset < settings.days; offset++) {
                    const weekday = new Date((rangeStart + offset) * dayMs).getUTCDay();
                    if (weekday === 0 || weekday === 6) {
                        const shade = node('span', 'mg-weekend-shade');
                        shade.style.left = offset * settings.width + 'px';
                        track.append(shade);
                    }
                }
            }
            if (today() >= rangeStart && today() <= end) {
                const line = node('span', 'mg-today-line');
                line.style.left = ((today() - rangeStart) + .5) * settings.width + 'px';
                track.append(line);
            }
            if (!item.validPeriod) {
                track.append(node('span', 'mg-no-date', '기간 확인 필요'));
                return track;
            }
            if (item.start > end || item.end < rangeStart) {
                track.append(node('span', 'mg-no-date', '표시 기간 밖'));
                return track;
            }
            const visibleStart = Math.max(rangeStart, item.start);
            const visibleEnd = Math.min(end, item.end);
            const visibleDays = visibleEnd - visibleStart + 1;
            const completeDays = (item.end - item.start + 1) * item.progress / 100;
            const visibleComplete = Math.max(0, Math.min(visibleDays, completeDays - (visibleStart - item.start)));
            const bar = node('button', 'mg-bar');
            bar.type = 'button';
            bar.dataset.action = 'detail';
            bar.dataset.key = item.key;
            bar.dataset.focusKey = 'bar-' + item.key;
            bar.style.left = (visibleStart - rangeStart) * settings.width + 4 + 'px';
            bar.style.width = Math.max(4, visibleDays * settings.width - 8) + 'px';
            const description = item.name + ', ' + item.statusName + ', '
                + text(item.startAt) + ' ~ ' + text(item.endAt) + ', 진행률 ' + item.progress + '%';
            bar.title = description;
            bar.setAttribute('aria-label', description);
            const fill = node('span', 'mg-bar-fill');
            fill.style.width = visibleComplete / visibleDays * 100 + '%';
            fill.setAttribute('aria-hidden', 'true');
            bar.append(fill, node('span', 'mg-bar-text', item.progress + '%'));
            track.append(bar);
            return track;
        }

        function makeRow(item, taskCount) {
            const project = item.type === 'project';
            const row = node('div', 'mg-row ' + (project ? 'mg-project-row' : 'mg-task-row'));
            row.dataset.key = item.key;
            row.classList.toggle('mg-selected', item.key === selectedKey);
            const label = node('div', 'mg-label');
            if (project) {
                const toggle = node('button', 'mg-toggle', collapsed.has(item.key) ? '▸' : '▾');
                toggle.type = 'button';
                toggle.dataset.action = 'toggle';
                toggle.dataset.key = item.key;
                toggle.dataset.focusKey = 'toggle-' + item.key;
                toggle.setAttribute('aria-expanded', String(!collapsed.has(item.key)));
                toggle.setAttribute('aria-label', item.name + ' 업무 ' + (collapsed.has(item.key) ? '펼치기' : '접기'));
                toggle.disabled = taskCount === 0;
                label.append(toggle);
            }
            const name = node('button', 'mg-name');
            name.type = 'button';
            name.dataset.action = 'detail';
            name.dataset.key = item.key;
            name.dataset.focusKey = 'name-' + item.key;
            name.title = item.name;
            name.setAttribute('aria-label', item.name + ' 상세 정보');
            name.append(node('strong', '', item.name));
            const period = text(item.startAt) + ' ~ ' + text(item.endAt);
            name.append(node('small', '', project ? '업무 ' + taskCount + '개 · ' + period : text(item.assignee, '담당자 미정') + ' · ' + period));
            const status = node('span', 'mg-status', item.statusName);
            if (item.status === 1) {
                status.classList.add('mg-status-active');
            }
            if ((project && item.status === 2) || (!project && item.status === 3)) {
                status.classList.add('mg-status-done');
            }
            label.append(name, status);
            row.append(label, makeTrack(item));
            return row;
        }

        function restoreFocus(key) {
            if (!key) {
                return;
            }
            const button = Array.from(board.querySelectorAll('[data-focus-key]')).find(function (element) {
                return element.dataset.focusKey === key;
            });
            if (button) {
                button.focus({ preventScroll: true });
            }
        }

        function render() {
            const settings = views[view];
            const groups = grouped();
            const top = scroll.scrollTop;
            const left = scroll.scrollLeft;
            const focusKey = document.activeElement && document.activeElement.dataset.focusKey;
            const fragment = document.createDocumentFragment();
            const matchedKeys = new Set();
            let count = 0;
            let done = 0;
            board.style.setProperty('--mg-days', settings.days);
            board.style.setProperty('--mg-day-width', settings.width + 'px');
            fragment.append(makeHeader());
            groups.forEach(function (group) {
                matchedKeys.add(group.project.key);
                fragment.append(makeRow(group.project, group.tasks.length));
                group.tasks.forEach(function (task) {
                    count++;
                    if (task.status === 3) {
                        done++;
                    }
                    matchedKeys.add(task.key);
                    if (!collapsed.has(group.project.key)) {
                        fragment.append(makeRow(task));
                    }
                });
            });
            if (selectedKey && !matchedKeys.has(selectedKey)) {
                closeDetail(false);
            }
            board.replaceChildren(fragment);
            document.getElementById('mgEmpty').hidden = groups.length > 0;
            scroll.hidden = groups.length === 0;
            ['mgProjectCount', 'mgTaskCount', 'mgDoneCount'].forEach(function (id, index) {
                const strong = document.getElementById(id);
                strong.replaceChildren(document.createTextNode(String([groups.length, count, done][index])), node('small', '', '개'));
            });
            document.getElementById('mgResult').textContent = '프로젝트 ' + groups.length + '개 · 업무 ' + count + '개';
            document.getElementById('mgRange').textContent = shortDate(rangeStart) + ' – ' + shortDate(rangeStart + settings.days - 1);
            scroll.scrollTop = top;
            scroll.scrollLeft = left;
            restoreFocus(focusKey);
        }

        function openDetail(item, focusKey) {
            selectedKey = item.key;
            lastFocusKey = focusKey;
            const isProject = item.type === 'project';
            document.getElementById('mgDetailType').textContent = isProject ? '프로젝트' : '업무';
            document.getElementById('mgDetailTitle').textContent = item.name;
            document.getElementById('mgDetailProject').textContent = isProject ? '프로젝트 일정 및 진행 현황' : text(item.projectName);
            const info = document.getElementById('mgDetailInfo');
            info.replaceChildren();
            const fields = [['진행 상태', item.statusName], ['시작일', text(item.startAt)], ['종료일', text(item.endAt)]];
            if (isProject) {
                fields.push(['업무 수', (tasksByProject.get(String(item.projectId)) || []).length + '개']);
            } else {
                fields.push(['담당자', text(item.assignee, '담당자 미정')]);
            }
            fields.forEach(function (field) {
                info.append(node('dt', '', field[0]), node('dd', '', field[1]));
            });
            document.getElementById('mgDetailPercent').textContent = '진행률 ' + item.progress + '%';
            document.getElementById('mgDetailProgress').value = item.progress;
            document.getElementById('mgDetailContent').innerHTML = text(item.content, '등록된 내용이 없습니다.');
            document.getElementById('mgProjectLink').href = String(data.detailBase || '') + '/' + encodeURIComponent(item.projectId);
            board.querySelectorAll('.mg-row').forEach(function (row) {
                row.classList.toggle('mg-selected', row.dataset.key === item.key);
            });
            panel.inert = false;
            panel.setAttribute('aria-hidden', 'false');
            panel.classList.add('mg-open');
            document.getElementById('mgClose').focus({ preventScroll: true });
        }

        function closeDetail(focus) {
            panel.inert = true;
            panel.setAttribute('aria-hidden', 'true');
            panel.classList.remove('mg-open');
            selectedKey = null;
            board.querySelectorAll('.mg-selected').forEach(function (row) {
                row.classList.remove('mg-selected');
            });
            if (focus) {
                restoreFocus(lastFocusKey);
            }
        }

        function applyFilters() {
            filter = { project: projectFilter.value, schType: schType.value, kwd: kwd.value.trim() };
            kwd.value = filter.kwd;
            if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
                const url = new URL(window.location.href);
                url.searchParams.set('schType', filter.schType);
                url.searchParams.set('kwd', filter.kwd);
                if (filter.project === 'all') {
                    url.searchParams.delete('projectId');
                } else {
                    url.searchParams.set('projectId', filter.project);
                }
                window.history.replaceState(null, '', url);
            }
            collapsed.clear();
            locateData(grouped());
            scroll.scrollLeft = 0;
            render();
        }

        board.addEventListener('click', function (event) {
            const button = event.target.closest('button[data-action]');
            if (!button) {
                return;
            }
            const item = items.get(button.dataset.key);
            if (!item) {
                return;
            }
            if (button.dataset.action === 'toggle') {
                if (collapsed.has(item.key)) {
                    collapsed.delete(item.key);
                } else {
                    collapsed.add(item.key);
                }
                render();
            } else {
                openDetail(item, button.dataset.focusKey);
            }
        });
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            applyFilters();
        });
        projectFilter.addEventListener('change', applyFilters);
        document.getElementById('mgReset').addEventListener('click', function () {
            projectFilter.value = 'all';
            schType.value = 'all';
            kwd.value = '';
            applyFilters();
        });
        document.getElementById('mgPrev').addEventListener('click', function () {
            rangeStart -= views[view].days;
            render();
        });
        document.getElementById('mgNext').addEventListener('click', function () {
            rangeStart += views[view].days;
            render();
        });
        document.getElementById('mgToday').addEventListener('click', function () {
            positionAround(today());
            scroll.scrollLeft = 0;
            render();
        });
        document.getElementById('mgViews').addEventListener('click', function (event) {
            const button = event.target.closest('button[data-view]');
            if (!button) {
                return;
            }
            const anchor = rangeStart + 7;
            view = button.dataset.view;
            positionAround(anchor);
            document.querySelectorAll('#mgViews button').forEach(function (element) {
                element.setAttribute('aria-pressed', String(element === button));
            });
            scroll.scrollLeft = 0;
            render();
        });
        document.getElementById('mgExpand').addEventListener('click', function () {
            collapsed.clear();
            render();
        });
        document.getElementById('mgCollapse').addEventListener('click', function () {
            collapsed = new Set(projects.map(function (project) { return project.key; }));
            render();
        });
        document.getElementById('mgClose').addEventListener('click', function () {
            closeDetail(true);
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && panel.classList.contains('mg-open')) {
                closeDetail(true);
            }
        });
        const query = new URL(window.location.href).searchParams;
        const initialType = query.get('schType') || data.schType;
        const initialProject = query.get('projectId');
        schType.value = ['all', 'project', 'task', 'assignee'].includes(initialType) ? initialType : 'all';
        kwd.value = query.has('kwd') ? query.get('kwd') : text(data.kwd, '');
        if (initialProject && projectIds.has(initialProject)) {
            projectFilter.value = initialProject;
        }
        applyFilters();
    });
})();
