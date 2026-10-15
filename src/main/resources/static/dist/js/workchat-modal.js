// TASKWAKA WORKCHAT V5: 디자인 시안 전용 화면 전환입니다.
// 실제 DB/메시지/초대/회의 생성 기능은 포함되지 않습니다.
document.addEventListener("DOMContentLoaded", function () {
    const openButton = document.getElementById("workchatToggle");
    const modal = document.getElementById("workchatModal");
    const closeButton = document.getElementById("workchatClose");
    const createButton = document.getElementById("workchatCreateToggle");
    const typeMenu = document.getElementById("workchatTypeMenu");
    const tabs = document.querySelectorAll("[data-workchat-tab]");
    const panels = document.querySelectorAll("[data-workchat-panel]");
    const views = document.querySelectorAll("[data-workchat-view]");
    const createViews = document.querySelectorAll("[data-workchat-create-view]");
    const searchInput = document.getElementById("workchatSearch");
    const personSearch = document.getElementById("workchatPersonSearch");
    const personChecks = Array.from(document.querySelectorAll("[data-workchat-participant]"));
    const personRows = Array.from(document.querySelectorAll("[data-workchat-person-row]"));
    const personCounter = document.getElementById("workchatPersonCounter");
    const personChips = document.getElementById("workchatPersonChips");
    const roomRows = Array.from(modal.querySelectorAll("[data-workchat-room]"));
    const activeRoomTitle = document.getElementById("workchatActiveRoomTitle");
    const activeRoomSubtitle = document.getElementById("workchatActiveRoomSubtitle");
    const activeRoomAvatar = document.getElementById("workchatActiveRoomAvatar");
    const messageList = modal.querySelector(".workchat-messages");
    let chatKind = "team";

    if (!openButton || !modal || !closeButton || !createButton || !typeMenu) return;

    // 디자인 시안: 프로필 박스에는 최대 두 글자만 표시.
    // 나중에 사진(<img>)이 들어오면 텍스트 처리를 건너뜁니다.
    function limitAvatarText(avatar) {
        if (!avatar || avatar.querySelector("img, svg")) return;
        const fullName = avatar.textContent.trim();
        if (Array.from(fullName).length > 2) {
            avatar.textContent = Array.from(fullName).slice(0, 2).join("");
            avatar.title = fullName; // 원래 이름은 마우스를 올리면 확인 가능
        }
    }
    modal.querySelectorAll(".workchat-avatar").forEach(limitAvatarText);

    // 헤더 레이아웃(transform/overflow)의 영향을 받지 않도록 모달을 body로 분리.
    // 실제 워크챗 디자인/탭 기능은 그대로 유지합니다.
    if (modal.parentElement !== document.body) {
        document.body.appendChild(modal);
    }


    const placeholders = {
        members: "워크 멤버 검색 (디자인 미리보기)",
        chats: "채팅방 검색 (디자인 미리보기)",
        meetings: "회의방 검색 (디자인 미리보기)"
    };

    function showTypeMenu(visible) {
        typeMenu.hidden = !visible;
        createButton.setAttribute("aria-expanded", String(visible));
        if (visible) typeMenu.querySelector("[data-workchat-kind]").focus();
    }

    function changeTab(name) {
        tabs.forEach(function (tab) {
            const active = tab.dataset.workchatTab === name;
            tab.setAttribute("aria-selected", String(active));
            tab.classList.toggle("workchat-tab--active", active);
            tab.tabIndex = active ? 0 : -1;
        });
        panels.forEach(function (panel) {
            panel.hidden = panel.dataset.workchatPanel !== name;
        });
        views.forEach(function (view) {
            const targetView = name === "chats" ? "chats-empty" : name;
            view.hidden = view.dataset.workchatView !== targetView;
        });
        // 채팅방 탭에 들어올 때는 항상 아무 방도 선택하지 않은 상태
        if (name === "chats") {
            roomRows.forEach(function (row) {
                row.classList.remove("workchat-list-item--selected");
                row.setAttribute("aria-pressed", "false");
            });
        }
        createViews.forEach(function (view) { view.hidden = true; });
        if (searchInput) searchInput.placeholder = placeholders[name];
        showTypeMenu(false);
    }

    function showCreateView(name) {
        views.forEach(function (view) { view.hidden = true; });
        createViews.forEach(function (view) {
            view.hidden = view.dataset.workchatCreateView !== name;
        });
    }


    // 디자인 미리보기: 왼쪽 방을 누르면 샘플 대화 화면을 표시합니다.
    const roomPreviews = {
        development: {
            avatar: "개발", lines: [
                ["김지민", "안녕하세요! 워크챗 화면 시안 확인 부탁드려요 😊", "14:25"],
                ["박민수", "왼쪽에서 대화방을 보고 오른쪽에서 메시지를 확인하네요!", "14:27"],
                ["나", "네! 디자인부터 완성하고 실제 채팅은 나중에 연결할게요.", "14:29"],
                ["김지민", "좋아요! 오늘 화면 작업 확인 부탁드려요!", "14:31"]
            ]
        },
        design: {
            avatar: "디자인", lines: [
                ["정하늘", "수정한 컬러 팔레트 공유했습니다.", "어제"],
                ["나", "확인 후 피드백 전달드릴게요!", "어제"]
            ]
        },
        notice: {
            avatar: "전체", lines: [
                ["김지민", "이번 주 회의 일정 안내드립니다.", "월요일"],
                ["나", "공지 확인했습니다.", "월요일"]
            ]
        }
    };

    function selectRoom(row) {
        roomRows.forEach(function (item) {
            const active = item === row;
            item.classList.toggle("workchat-list-item--selected", active);
            item.setAttribute("aria-pressed", String(active));
        });
        views.forEach(function (view) {
            view.hidden = view.dataset.workchatView !== "chats";
        });
        createViews.forEach(function (view) { view.hidden = true; });
        showTypeMenu(false);
        activeRoomTitle.textContent = row.dataset.roomName;
        activeRoomSubtitle.textContent = row.dataset.roomDescription;
        const sample = roomPreviews[row.dataset.workchatRoom];
        activeRoomAvatar.textContent = sample.avatar;
        limitAvatarText(activeRoomAvatar);
        messageList.replaceChildren();
        const day = document.createElement("span");
        day.className = "workchat-day";
        day.textContent = "샘플 대화";
        messageList.appendChild(day);
        sample.lines.forEach(function (line) {
            const mine = line[0] === "나";
            const message = document.createElement("div");
            message.className = "workchat-message" + (mine ? " workchat-message--mine" : "");
            if (!mine) {
                const avatar = document.createElement("div");
                avatar.className = "workchat-avatar workchat-avatar--sand workchat-avatar--mini";
                avatar.textContent = line[0].slice(-2);
                message.appendChild(avatar);
            }
            const content = document.createElement("div");
            content.className = "workchat-message-content";
            if (!mine) {
                const sender = document.createElement("span");
                sender.className = "workchat-sender";
                sender.textContent = line[0];
                content.appendChild(sender);
            }
            const bubbleRow = document.createElement("div");
            bubbleRow.className = "workchat-bubble-row";
            const bubble = document.createElement("div");
            bubble.className = "workchat-bubble";
            bubble.textContent = line[1];
            const time = document.createElement("span");
            time.className = "workchat-message-time";
            time.textContent = line[2];
            bubbleRow.append(bubble, time);
            content.appendChild(bubbleRow);
            message.appendChild(content);
            messageList.appendChild(message);
        });
    }

    function updateSelectedPeople() {
        const selected = personChecks.filter(function (check) { return check.checked; });
        personCounter.textContent = selected.length + "명";
        personChips.replaceChildren();
        if (!selected.length) {
            const empty = document.createElement("span");
            empty.className = "workchat-picker-empty";
            empty.textContent = "멤버를 선택하면 여기에 표시돼요.";
            personChips.appendChild(empty);
            return;
        }
        selected.forEach(function (check) {
            const chip = document.createElement("span");
            chip.className = "workchat-picker-chip";
            chip.textContent = check.value;
            personChips.appendChild(chip);
        });
        // 이름표가 길어져도 여러 줄로 내려오지 않고 오른쪽으로 스크롤
        personChips.scrollLeft = personChips.scrollWidth;
    }

    function openKind(kind) {
        showTypeMenu(false);
        if (kind === "meeting") {
            changeTab("meetings");
            showCreateView("meetings");
            document.getElementById("workchatMeetingName").focus();
            return;
        }
        chatKind = kind;
        changeTab("chats");
        showCreateView("chats");
        personChecks.forEach(function (check) { check.checked = false; });
        personSearch.value = "";
        filterPeople();
        updateSelectedPeople();
        document.getElementById("workchatPickerSubtitle").textContent = kind === "general"
            ? "일반채팅 · 한 명만 선택" : "팀채팅 · 여러 명 선택";
        document.getElementById("workchatPickerHeading").textContent = kind === "general"
            ? "1:1로 대화할 상대를 선택하세요" : "함께 대화할 멤버를 선택하세요";
        document.getElementById("workchatPickerDescription").textContent = kind === "general"
            ? "워크 멤버 중 한 명을 선택할 수 있어요." : "워크 멤버를 여러 명 선택할 수 있어요.";
        document.getElementById("workchatPickerTitle").textContent = "대화상대 선택";
        personSearch.focus();
    }

    function filterPeople() {
        const query = personSearch.value.trim().toLocaleLowerCase();
        personRows.forEach(function (row) {
            row.hidden = !((row.dataset.name + " " + row.dataset.role).toLocaleLowerCase().includes(query));
        });
        modal.querySelectorAll(".workchat-picker-group").forEach(function (group) {
            group.hidden = !group.querySelector("[data-workchat-person-row]:not([hidden])");
        });
    }

    function openWorkchat() {
        modal.hidden = false;
        document.body.classList.add("workchat-open");
        openButton.setAttribute("aria-expanded", "true");
        changeTab("chats");
        closeButton.focus();
    }

    function closeWorkchat() {
        showTypeMenu(false);
        modal.hidden = true;
        document.body.classList.remove("workchat-open");
        openButton.setAttribute("aria-expanded", "false");
        openButton.focus();
    }

    tabs.forEach(function (tab, index) {
        tab.addEventListener("click", function () { changeTab(tab.dataset.workchatTab); });
        tab.addEventListener("keydown", function (event) {
            if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
            event.preventDefault();
            const next = (index + (event.key === "ArrowRight" ? 1 : -1) + tabs.length) % tabs.length;
            changeTab(tabs[next].dataset.workchatTab);
            tabs[next].focus();
        });
    });

    createButton.addEventListener("click", function () {
        showTypeMenu(typeMenu.hidden);
    });
    // 채팅방 미선택 화면의 두 버튼은 상대 선택 화면으로 이동
    modal.querySelectorAll("[data-workchat-start-kind]").forEach(function (button) {
        button.addEventListener("click", function () {
            openKind(button.dataset.workchatStartKind);
        });
    });
    roomRows.forEach(function (row) {
        row.addEventListener("click", function () { selectRoom(row); });
        row.addEventListener("keydown", function (event) {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                selectRoom(row);
            }
        });
    });
    typeMenu.querySelectorAll("[data-workchat-kind]").forEach(function (button) {
        button.addEventListener("click", function () { openKind(button.dataset.workchatKind); });
    });
    modal.querySelectorAll("[data-workchat-return-types]").forEach(function (button) {
        button.addEventListener("click", function (event) {
            event.stopPropagation();
            changeTab("chats");
            showTypeMenu(true);
        });
    });
    personSearch.addEventListener("input", filterPeople);
    personChecks.forEach(function (check) {
        check.addEventListener("change", function () {
            if (chatKind === "general" && check.checked) {
                personChecks.forEach(function (other) {
                    if (other !== check) other.checked = false;
                });
            }
            updateSelectedPeople();
        });
    });
    // 회의방 참석자 체크는 화면에 선택된 인원 수만 표시합니다.
    modal.querySelectorAll('[data-workchat-invite="meetings"]').forEach(function (checkbox) {
        checkbox.addEventListener("change", function () {
            const selected = modal.querySelectorAll('[data-workchat-invite="meetings"]:checked').length;
            modal.querySelector('[data-workchat-selected-count="meetings"]').textContent = selected + "명 선택";
        });
    });
    openButton.addEventListener("click", openWorkchat);
    closeButton.addEventListener("click", closeWorkchat);
    modal.addEventListener("click", function (event) {
        if (event.target === modal) closeWorkchat();
        else if (!typeMenu.hidden && !typeMenu.contains(event.target) && !createButton.contains(event.target)) showTypeMenu(false);
    });
    document.addEventListener("keydown", function (event) {
        if (event.key !== "Escape" || modal.hidden) return;
        if (!typeMenu.hidden) {
            showTypeMenu(false);
            createButton.focus();
        } else closeWorkchat();
    });
    changeTab("chats");
});
