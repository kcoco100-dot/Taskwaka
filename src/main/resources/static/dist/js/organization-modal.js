document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("organizationModal");
    const openButton = document.getElementById("organizationToggle");

    if (! modal || ! openButton) return;

    const closeButton = document.getElementById("twOrgClose");

    const projectSelect = document.getElementById("twOrgProjectSelect");
    const projectName = document.getElementById("twOrgProjectName");
    const memberCount = document.getElementById("twOrgMemberCount");
    const memberStatus = document.getElementById("twOrgMemberStatus");
    const memberList = document.getElementById("twOrgMembers");
    const memberScroll = document.getElementById("twOrgMemberScroll");
    const taskCount = document.getElementById("twOrgTaskCount");
    const tree = document.getElementById("twOrgTree");


    const domainKey = modal.dataset.domainKey;
    if (! domainKey) return;

    const apiUrl = "/" + encodeURIComponent(domainKey) + "/api/organization";

    let refreshTreeLayout = null;
    let projects = [];
    let projectRequest = 0;
    let lastFocused = null;
    let previousBodyOverflow = null;
    let projectMembers = [];
    let renderedMemberCount = 0;
    const memberPageSize = 20;

    if (modal.parentElement !== document.body) {
        document.body.appendChild(modal);
    }

    function createElement(tag, className, text) {
        const element = document.createElement(tag);

        if (className) element.className = className;
        if (text != null) element.textContent = String(text);

        return element;
    }

    async function request(url) {
        const response = await fetch(url, {
            headers: { "Accept": "application/json" }
        });

        const contentType = response.headers.get("content-type") || "";

        if (! contentType.includes("application/json")) {
            throw new Error("서버 응답을 확인할 수 없습니다. (HTTP " + response.status + ")");
        }

        if (! response.ok) {
            if (response.status === 401) throw new Error("로그인이 필요합니다.");
            if (response.status === 403) throw new Error("조회 권한이 없습니다.");
            if (response.status === 404) throw new Error("조회 API를 찾을 수 없습니다.");

            throw new Error("조회 중 오류가 발생했습니다. (HTTP " + response.status + ")");
        }

        return await response.json();
    }

    async function loadProjects() {
        projectSelect.disabled = true;
        projectSelect.replaceChildren(createElement("option", "", "프로젝트 불러오는 중..."));
        projectMembers = [];
        renderedMemberCount = 0;
        memberList.replaceChildren();
        tree.replaceChildren();

        try {
            const result = await request(apiUrl + "/projects");
            projects = Array.isArray(result) ? result : [];

            projectSelect.replaceChildren();

            for (const project of projects) {
                const option = createElement("option", "", project.subject || "제목 없는 프로젝트");
                option.value = String(project.projectId);
                projectSelect.appendChild(option);
            }

            if (projects.length === 0) {
                projectSelect.appendChild(createElement("option", "", "참여 중인 프로젝트가 없습니다."));
                projectName.textContent = "프로젝트 업무 조직도";
                memberStatus.textContent = "참여 중인 프로젝트가 없습니다.";
                memberCount.textContent = "";
                taskCount.textContent = "";
                return;
            }

            projectSelect.value = String(projects[0].projectId);
            await chooseProject();

        } catch (e) {
            projectSelect.replaceChildren(createElement("option", "", "프로젝트 조회 실패"));
            memberStatus.textContent = e.message;
            console.error("프로젝트 목록 조회 오류 : ", e);

        } finally {
            projectSelect.disabled = projects.length === 0;
        }
    }

    function createTreeCard(type, title, assignee, detail, status) {
        const card = createElement("div", "tw-org-tree-card tw-org-tree-card--" + type);
        const label = type === "project" ? "프로젝트" : type === "task" ? "업무" : "하위업무";

        const top = createElement("div", "tw-org-tree-card-top");
        top.appendChild(createElement("small", "tw-org-tree-label", label));

        if (type !== "project" && status != null) {
            const names = { 0: "예정", 1: "진행중", 2: "검토중", 3: "완료" };
            const state = Object.prototype.hasOwnProperty.call(names, status) ? status : "unknown";
            top.appendChild(createElement("span", "tw-org-state tw-org-state--" + state,
                    names[status] || "상태 미정"));
        }

        card.appendChild(top);
        card.appendChild(createElement("strong", "tw-org-tree-title", title || "제목 없음"));
        card.appendChild(createElement("span", "tw-org-tree-assignee", assignee || "담당자 정보 없음"));

        if (detail) {
            card.appendChild(createElement("small", "tw-org-tree-info", detail));
        }

        return card;
    }

    function drawTree(project, members, tasks, subTasks) {
        tree.replaceChildren();

        const root = createElement("div", "tw-org-tree-root");
        root.appendChild(createTreeCard("project", project.subject, members.length + "명 참여"));
        tree.appendChild(root);

        if (tasks.length === 0) {
            tree.appendChild(createElement("p", "tw-org-tree-empty", "등록된 업무가 없습니다."));
            return;
        }

        const branches = createElement("div", "tw-org-task-branches");

        tasks.forEach(function (task) {
            const branch = createElement("div", "tw-org-task-branch");
            const children = subTasks.filter(function (subTask) {
                return Number(subTask.parentTaskId) === Number(task.taskId);
            });

            const card = createTreeCard("task", task.title, task.assigneeName,
                    children.length + "개 하위업무", task.status);

            if (children.length > 0) {
                const button = createElement("button", "tw-org-task-toggle");
                button.type = "button";
                button.setAttribute("aria-expanded", "false");
                button.setAttribute("aria-label", (task.title || "업무") + " 하위업무 펼치기");
                button.appendChild(card);

                let childList = null;

                button.addEventListener("click", function () {
                    const isOpen = button.getAttribute("aria-expanded") === "true";

                    if (isOpen) {
                        childList.hidden = true;
                        button.setAttribute("aria-expanded", "false");
                        button.setAttribute("aria-label", (task.title || "업무") + " 하위업무 펼치기");
                        if (refreshTreeLayout) requestAnimationFrame(refreshTreeLayout);
                        return;
                    }

                    if (! childList) {
                        childList = createElement("div", "tw-org-subtask-list");

                        children.forEach(function (subTask) {
                            const child = createElement("div", "tw-org-subtask-item");
                            child.appendChild(createTreeCard("subtask", subTask.title, subTask.assigneeName, null, subTask.status));
                            childList.appendChild(child);
                        });

                        branch.appendChild(childList);
                    }

                    childList.hidden = false;
                    button.setAttribute("aria-expanded", "true");
                    button.setAttribute("aria-label", (task.title || "업무") + " 하위업무 접기");
                    if (refreshTreeLayout) requestAnimationFrame(refreshTreeLayout);
                });
                branch.appendChild(button);
            } else {
                branch.appendChild(card);
            }

            branches.appendChild(branch);
        });

        tree.appendChild(branches);

        // 화면 너비에 따라 업무가 줄바꿈되면 첫 줄까지만 연결선 표시
        function updateTreeConnections() {
            const cards = Array.from(branches.querySelectorAll(".tw-org-task-branch"));
            if (! cards.length) return;

            const firstTop = cards[0].offsetTop;
            const firstRow = cards.filter(function (card) {
                const isFirstRow = card.offsetTop === firstTop;
                card.classList.toggle("is-first-row", isFirstRow);
                return isFirstRow;
            });

            const wrapped = firstRow.length !== cards.length;
            branches.classList.toggle("is-wrapped", wrapped);
            branches.classList.toggle("is-one-top", firstRow.length === 1);
            tree.classList.toggle("is-wrapped", wrapped);

            if (firstRow.length > 1) {
                const left = firstRow[0].offsetLeft + firstRow[0].offsetWidth / 2;
                const last = firstRow[firstRow.length - 1];
                const right = branches.clientWidth - (last.offsetLeft + last.offsetWidth / 2);
                branches.style.setProperty("--tw-line-left", left + "px");
                branches.style.setProperty("--tw-line-right", right + "px");
            }
        }

        requestAnimationFrame(updateTreeConnections);
        refreshTreeLayout = updateTreeConnections;
    }

    function appendMembers() {
        const roles = { 70: "PM", 50: "DT", 30: "TL", 10: "AA" };
        const end = Math.min(renderedMemberCount + memberPageSize, projectMembers.length);

        for (let i = renderedMemberCount; i < end; i++) {
            const member = projectMembers[i];
            const item = createElement("div", "tw-org-person");
            const heading = createElement("div", "tw-org-person-line");

            heading.appendChild(createElement("strong", "", member.name || "이름 정보 없음"));
            heading.appendChild(createElement("span", "tw-org-role", roles[member.role] || "직책 없음"));
            item.appendChild(heading);

            if (member.taskTitles) {
                const task = createElement("small", "tw-org-person-tasks", "업무: " + member.taskTitles);
                task.title = member.taskTitles;
                item.appendChild(task);
            }

            memberList.appendChild(item);
        }

        renderedMemberCount = end;
    }

    function showMembers(members) {
        projectMembers = members;
        renderedMemberCount = 0;
        memberList.replaceChildren();
        memberScroll.scrollTop = 0;
        memberCount.textContent = members.length + "명";

        if (members.length === 0) {
            memberList.appendChild(createElement("p", "tw-org-help", "현재 참여자가 없습니다."));
            return;
        }

        appendMembers();

        // 목록 높이가 남을 경우 다음 인원을 추가해 스크롤 영역을 채움
        requestAnimationFrame(function () {
            while (renderedMemberCount < projectMembers.length &&
                    memberScroll.scrollHeight <= memberScroll.clientHeight + 5) {
                appendMembers();
            }
        });
    }

    memberScroll.addEventListener("scroll", function () {
        if (memberScroll.scrollTop + memberScroll.clientHeight >= memberScroll.scrollHeight - 100) {
            appendMembers();
        }
    });

    async function chooseProject() {
        const projectId = projectSelect.value;
        const version = ++projectRequest;

        const project = projects.find(function (item) {
            return String(item.projectId) === projectId;
        });

        projectMembers = [];
        renderedMemberCount = 0;
        memberScroll.scrollTop = 0;
        memberList.replaceChildren();
        tree.replaceChildren();
        memberCount.textContent = "";
        taskCount.textContent = "";

        if (! project) {
            projectName.textContent = "프로젝트 업무 조직도";
            memberStatus.textContent = "프로젝트를 선택해 주세요.";
            return;
        }

        projectName.textContent = project.subject;
        memberStatus.textContent = "프로젝트 정보를 불러오는 중...";

        try {
            const result = await request(apiUrl + "/projects/" + encodeURIComponent(projectId) + "/tree");

            if (version !== projectRequest) return;

            const members = Array.isArray(result.members) ? result.members : [];
            const tasks = Array.isArray(result.tasks) ? result.tasks : [];
            const subTasks = Array.isArray(result.subTasks) ? result.subTasks : [];

            memberStatus.textContent = "";
            taskCount.textContent = "업무 " + tasks.length + "개";

            showMembers(members);
            drawTree(project, members, tasks, subTasks);

        } catch (e) {
            if (version === projectRequest) {
                memberStatus.textContent = e.message;
            }
            console.error("프로젝트 업무 조직도 조회 오류 : ", e);
        }
    }

    function closeModal() {
        modal.hidden = true;
        document.body.style.overflow = previousBodyOverflow == null ? "" : previousBodyOverflow;
        previousBodyOverflow = null;
        openButton.setAttribute("aria-expanded", "false");

        if (lastFocused && lastFocused.isConnected) {
            lastFocused.focus();
        }
    }

    openButton.addEventListener("click", function () {
        if (! modal.hidden) {
            closeModal();
            return;
        }

        lastFocused = document.activeElement;

        const workchat = document.getElementById("workchatModal");
        if (workchat && ! workchat.hidden) {
            document.getElementById("workchatClose")?.click();
        }

        previousBodyOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";
        modal.hidden = false;
        openButton.setAttribute("aria-expanded", "true");
        loadProjects();
        closeButton.focus();
    });

    closeButton.addEventListener("click", closeModal);
    projectSelect.addEventListener("change", chooseProject);

    window.addEventListener("resize", function () {
        if (! modal.hidden && refreshTreeLayout) {
            requestAnimationFrame(refreshTreeLayout);
        }
    });

    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape" && ! modal.hidden) closeModal();
    });
});
