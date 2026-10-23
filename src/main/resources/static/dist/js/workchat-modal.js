document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("workchatModal");
    if (!modal) return;
	
	// ========================================
	  //  워크 멤버 관련 기능
	  // ========================================

	  // selectMember()
	  // memberVm
	  // loadMembers()
	  // searchMembers()


	  // ========================================
	  //  채팅방 관련 기능
	  // ========================================

	  // roomsVm
	  // showDirectRoom()
	  // openRoom()
	  // loadMyDirectRooms()


	  // ========================================
	  //  메시지 관련 기능
	  // ========================================

	  // messageVm
	  // loadMessages()


	  // ========================================
	  //  모달 및 탭 관리
	  // ========================================

	  // changeTab()
	  // openWorkchat()
	  // closeWorkchat()


    const openButton = document.getElementById("workchatToggle");
    const closeButton = document.getElementById("workchatClose");
    const createButton = document.getElementById("workchatCreateToggle");
    const typeMenu = document.getElementById("workchatTypeMenu");
    const tabs = document.querySelectorAll("[data-workchat-tab]");
    const panels = document.querySelectorAll("[data-workchat-panel]");
    const views = document.querySelectorAll("[data-workchat-view]");
    const createViews = document.querySelectorAll("[data-workchat-create-view]");
    const searchInput = document.getElementById("workchatSearch");
    const personSearch = document.getElementById("workchatPersonSearch");
    const personList = document.getElementById("workchatPersonList");
    function getPersonChecks() {
        return Array.from(modal.querySelectorAll("[data-workchat-participant]"));
    }
    const personCounter = document.getElementById("workchatPersonCounter");
    const personChips = document.getElementById("workchatPersonChips");
    let chatKind = "team";
	let activeChatRoomId = null;

    if (!openButton || !closeButton || !createButton || !typeMenu ||
        !personList || !personSearch || !personCounter || !personChips) return;

		

		function limitAvatarText(avatar) {
		    if (!avatar || avatar.querySelector("img, svg")) return;
		    const fullName = avatar.textContent.trim();

		    // Vue가 처리하기 전의 {{ }} 표현식은 건드리지 않음
		    if (fullName.includes("{{") || fullName.includes("}}")) {
		        return;
		    }

		    if (Array.from(fullName).length > 2) {
		        avatar.textContent = Array.from(fullName).slice(0, 2).join("");
		        avatar.title = fullName;
		    }
		}

    modal.querySelectorAll(".workchat-avatar").forEach(limitAvatarText);


    if (modal.parentElement !== document.body) {
        document.body.appendChild(modal);
    }

    const placeholders = {
        members: "워크 멤버 검색",
        chats: "채팅방 검색",
        meetings: "회의방 검색"
    };

    function showTypeMenu(visible) {
        typeMenu.hidden = !visible;
        createButton.setAttribute("aria-expanded", String(visible));
        if (visible) typeMenu.querySelector("[data-workchat-kind]").focus();
    }
	
	const memberList = modal.querySelector(".workchat-member-groups");
	
	
	
	function selectMember(member) {
	    const memberView = modal.querySelector('[data-workchat-view="members"]');
	    if (!memberView) return;
	    const avatar = memberView.querySelector(".workchat-chat-header .workchat-avatar");
	    const title = memberView.querySelector(".workchat-chat-heading strong");
	    const subtitle = memberView.querySelector(".workchat-chat-heading small");
	    const name = memberView.querySelector(".workchat-overview h3");
	    const description = memberView.querySelector(".workchat-overview p");

	    if (avatar) avatar.textContent = member.name.slice(-2);
	    if (title) title.textContent = member.name;
	    if (subtitle) subtitle.textContent = member.roleName;
	    if (name) name.textContent = member.name;

	    if (description) { 
			description.textContent = "직급: " + member.roleName;
		}
		
		const directChatButton = modal.querySelector("#workchatStartDirectChat");

		if (directChatButton) {
		    directChatButton.disabled = false;
		}
	}

	const memberVm = Vue.createApp({
	    setup() {
			const members = Vue.ref([]);
			const memberGroups = Vue.ref([]);
			const selectedMemberId = Vue.ref(null);
			
	        return {
	            members, memberGroups, selectMember, selectedMemberId
	        };
	    }

	}).mount(memberList);
	
	
	async function loadMembers() {
		
		const domainKey = window.location.pathname.split("/")[1];
		
		try {
			const res = await axios.get("/" + domainKey + "/api/workchat/members");
			
			memberVm.members = res.data;
			
			const roles = ["사장", "과장", "대리", "사원"];
			const groups = [];

			roles.forEach(function(role) {
			    const roleMembers = memberVm.members.filter(function(member) {
			        return member.roleName === role;
			    });

			    if (roleMembers.length > 0) {
			        groups.push({
						roleName: role,
			            members: roleMembers
			        });
			    }
			});

			memberVm.memberGroups = groups;
			
		} catch(e) {
			console.error("워크 멤버 조회 실패:", e);
		}
		
	}
	
	
	function searchMembers() {
		const kwd = searchInput.value.trim().toLowerCase();
		
		const roles = ["사장", "과장", "대리", "사원"];
		const groups = [];
		
		roles.forEach(function(role) {
		    const roleMembers = memberVm.members.filter(function(member) {

		    const name = (member.name || "").toLowerCase();
		    const roleName = (member.roleName || "").toLowerCase();

		    return member.roleName === role &&
		           (name.includes(kwd) || roleName.includes(kwd));
			});

		    if (roleMembers.length > 0) {
		        groups.push({
					roleName: role,
		            members: roleMembers
		        });
		    }
		});

		memberVm.memberGroups = groups;
	}
	
	
		    
	const roomList = document.getElementById("workchatRoomList");
	const roomsVm = Vue.createApp({
	    setup() { 
			const rooms = Vue.ref([]);
			
	        return {
	            rooms,
	            openRoom,
				formatLastSent
	        };
	    }
		
	}).mount(roomList);
	
	function formatLastSent(lastSent) {

	           if (!lastSent) return "";

	           const sentDate = lastSent.slice(0, 10);

	           const now = new Date();

	           const today =
	               now.getFullYear() + "-" +
	               String(now.getMonth() + 1).padStart(2, "0") + "-" +
	               String(now.getDate()).padStart(2, "0");

	           if (sentDate === today) {
	               return lastSent.slice(11, 16);
	           }

	           return sentDate;
	       }
	
	
	function showDirectRoom(chatRoomId, targetName) {

	    activeChatRoomId = chatRoomId;

	    document.getElementById("workchatActiveRoomTitle")
	        .textContent = targetName;

	    document.getElementById("workchatActiveRoomSubtitle")
	        .textContent = "1:1 채팅 · 방 번호 " + chatRoomId;

	    document.getElementById("workchatActiveRoomAvatar")
	        .textContent = targetName.slice(-2);

	    views.forEach(function(view) {
	        view.hidden =
	            view.dataset.workchatView !== "chats";
	    });

	    createViews.forEach(function(view) {
	        view.hidden = true;
	    });
		
		loadMessages(chatRoomId, true);

	}

	function openRoom(room) {showDirectRoom(room.chatRoomId, room.targetName);}
	
	let roomListVersion = 0;

	async function loadMyDirectRooms() {
	    const domainKey = window.location.pathname.split("/")[1];
	    // 가장 최근에 시작한 조회 요청인지 구분
	    const version = ++roomListVersion;

	    try {
	        const res = await axios.get(
	            "/" + domainKey + "/api/workchat/rooms/direct"
	        );

	        if (version !== roomListVersion) return;


			const roomList = res.data;

			roomList.forEach((room) => {
			    if (isReadingRoom(room.chatRoomId)) {
			        room.unreadCount = 0;
			    }
			});

			roomsVm.rooms = roomList;


	    } catch (e) {
			
	        console.error("채팅방 목록 조회 실패:", e);
	    }
	}

	
	
	const messageList = document.querySelector('.workchat-messages')
	const messageVm = Vue.createApp({

	    setup() {
	        const messages = Vue.ref([]);
	        const loading = Vue.ref(false);
	        const failed = Vue.ref(false);

	        return {
	            messages,
	            loading,
	            failed
	        };
	    }
	}).mount(messageList);
	
	// 채팅방을 보고 있는지 체크
	function isReadingRoom(chatRoomId) {
	    if (modal.hidden) return false;

	    if (document.hidden) return false;

	    if (Number(activeChatRoomId) !== Number(chatRoomId)) {
	        return false;
	    }

	    const chatsPanel = modal.querySelector('[data-workchat-panel="chats"]');
	    const chatsView = modal.querySelector('[data-workchat-view="chats"]');

	    if (! chatsPanel || chatsPanel.hidden) return false;
	    if (! chatsView || chatsView.hidden) return false;

	    return true;
	}

	
	let messageLoadVersion = 0;

	async function loadMessages(chatRoomId, silent = false) {

	    const domainKey = window.location.pathname.split("/")[1];

	    // 요청마다 번호를 부여해 이전 요청 결과가 뒤늦게
	    // 도착하더라도 최신 화면을 덮어쓰지 못하게 함
	    const version = ++messageLoadVersion;

	    let loadedMessages = null;

		if (! silent) {
		    messageVm.messages = [];
		    messageVm.loading = true;
		    messageVm.failed = false;
		}

	    try {
	        const res = await axios.get(
	            "/" + domainKey + "/api/workchat/rooms/" + chatRoomId + "/messages"
	        );

	        if (version !== messageLoadVersion || Number(activeChatRoomId) !== Number(chatRoomId)) {
	            return;
	        }
			
			messageVm.failed = false;
	        messageVm.messages = res.data;
	        // 읽음 처리할 메시지 목록 보관
	        loadedMessages = res.data;

	    } catch (e) {

	        console.error("메시지 조회 실패:", e);
	        messageVm.failed = true;

	    } finally {
	        if (version === messageLoadVersion && Number(activeChatRoomId) === Number(chatRoomId)) {
	            // 로딩 상태를 먼저 종료해야 메시지가 화면에 나타남
	            messageVm.loading = false;
	            // Vue가 화면을 갱신할 때까지 기다림
	            await Vue.nextTick();
	            // 메시지 목록을 맨 아래로 스크롤
	            messageList.scrollTop = messageList.scrollHeight;
	            // 정상 조회했고 실제 대화 화면을 보고 있다면 읽음 처리
	            if (loadedMessages !== null && isReadingRoom(chatRoomId)) {
	                await markRoomAsRead(chatRoomId, loadedMessages);
	            }
	        }
	    }
	}


	// 메시지 읽음 처리
	async function markRoomAsRead(chatRoomId, messages) {
	    if (! isReadingRoom(chatRoomId)) return;

	    if (! Array.isArray(messages) || messages.length === 0) {
	        return;
	    }

	    const lastMessage = messages[messages.length - 1];
	    const messageId = lastMessage.messageId;

	    if (messageId == null) return;

	    const domainKey = window.location.pathname.split("/")[1];

	    try {
	        await axios.post(
	            "/" + domainKey + "/api/workchat/rooms/" + chatRoomId + "/read",
	            {
	                messageId: messageId
	            }
	        );
			
	        await loadMyDirectRooms();

	    } catch (e) {
	        console.error("메시지 읽음 처리 실패:", e);
	    }
	}

	
	const messageInput = modal.querySelector(".workchat-input");
	const sendButton = modal.querySelector(".workchat-send");
	
	async function sendMessage() {

	    if (activeChatRoomId == null) {
	        alert("채팅방을 먼저 선택해주세요."); return;
	    }

	    const content = messageInput.value.trim();

	    if (content === "") {
	        return;
	    }

	    const chatRoomId = activeChatRoomId;

	    const domainKey = window.location.pathname.split("/")[1];

	    try {
	        sendButton.disabled = true;

	        await axios.post(
				"/" + domainKey + "/api/workchat/rooms/" + chatRoomId + "/messages",
	            {
	                content: content
	            }
	        );

	        if (activeChatRoomId === chatRoomId) {
	            messageInput.value = "";

	            await loadMessages(chatRoomId);
	        }
	
			await loadMyDirectRooms();

	    } catch (e) {
	        console.error("메시지 전송 실패:", e);
	        alert("메시지를 전송하지 못했습니다.");

	    } finally {
	        sendButton.disabled = false;
	    }
	}
	
	sendButton.addEventListener("click", sendMessage);

	messageInput.addEventListener("keydown", (e) => {

	    if (e.key === "Enter" && ! e.isComposing) {
	        e.preventDefault();
	        sendMessage();
	    }

	});
	


	// WebSocket 연결
	// ========================================

	let stompClient = null;
	
	function connect() {
	    const socket = new SockJS("/ws-workchat");

	    stompClient = Stomp.over(socket);
		stompClient.debug = function() {};
		
	    stompClient.connect({}, () => {
	        // 로그인한 사용자 전용 알림 구독
			stompClient.subscribe("/user/queue/workchat", function(res) {

			    const chatRoomId = Number(res.body);

			    loadMyDirectRooms();

			    if (isReadingRoom(chatRoomId)) loadMessages(chatRoomId, true);
				
			});
	    }, (e) => {
	        console.error("워크챗 WebSocket 연결 실패:", e);
	    });
	}
	
	// WebSocket 연결과 별도로 브라우저 탭 복귀 감지
	document.addEventListener("visibilitychange", () => {

	    if (document.hidden) return;

	    if (modal.hidden) return;

	    loadMyDirectRooms();

	    if (activeChatRoomId != null && isReadingRoom(activeChatRoomId)) {
	        loadMessages(activeChatRoomId, true);
	    }
	});




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
        createViews.forEach(function (view) { view.hidden = true; });
		
			if (searchInput) {
			    searchInput.placeholder = placeholders[name];
			    searchInput.value = "";
			}
	
			if (name === "members" && memberVm) {
			    searchMembers();
			    loadMembers();
			}
	
			showTypeMenu(false);
			
			if (name === "chats" && ! modal.hidden) {
			    loadMyDirectRooms();
			}
    }

    function showCreateView(name) {
        views.forEach(function (view) { view.hidden = true; });
        createViews.forEach(function (view) {
            view.hidden = view.dataset.workchatCreateView !== name;
        });
    }


    function updateSelectedPeople() {
        const selected = getPersonChecks().filter(function (check) { return check.checked; });
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
        getPersonChecks().forEach(function (check) { check.checked = false; });
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
        modal.querySelectorAll("[data-workchat-person-row]").forEach(function (row) {
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
		if (searchInput) {
		    searchInput.addEventListener("input", function() {
	
		        const memberPanel = modal.querySelector(
		            '[data-workchat-panel="members"]'
		        );
	
		        if (memberPanel && !memberPanel.hidden) {
		            searchMembers();
		        }
		    });
		}

    personList.addEventListener("change", function (event) {
        const check = event.target;
        if (!check.matches("[data-workchat-participant]")) return;

        if (chatKind === "general" && check.checked) {
            getPersonChecks().forEach(function (other) {
                if (other !== check) other.checked = false;
            });
        }
        updateSelectedPeople();
    });
    // 회의방 참석자 목록도 추후 동적으로 채워질 것을 고려해 이벤트 위임
    const meetingInviteList = document.getElementById("workchatMeetingInviteList");
    if (meetingInviteList) {
        meetingInviteList.addEventListener("change", function (event) {
            if (!event.target.matches('[data-workchat-invite="meetings"]')) return;
            const selected = meetingInviteList.querySelectorAll(
                '[data-workchat-invite="meetings"]:checked'
            ).length;
            const selectedCount = modal.querySelector('[data-workchat-selected-count="meetings"]');
            if (selectedCount) selectedCount.textContent = selected + "명 선택";
        });
    }
	

	const directChatButton = document.getElementById("workchatStartDirectChat");
	directChatButton.addEventListener("click", async function() {

	    const targetMemberId = memberVm.selectedMemberId;

	    if (targetMemberId == null) {
	        alert("채팅할 회원을 선택해주세요.");
	        return;
	    }

	    const selectedMember = memberVm.members.find((member) => {
	        return member.memberId === targetMemberId;
	    });
		
	    if (! selectedMember || selectedMember.workId == null) {
	        alert("회원 또는 워크 정보를 찾을 수 없습니다.");
	        return;
	    }

	    const domainKey = window.location.pathname.split("/")[1];

	    try {
	        directChatButton.disabled = true;

	        const res = await axios.post(
	            "/" + domainKey + "/api/workchat/rooms/direct",
	            {
	                memberId: targetMemberId,
	                workId: selectedMember.workId
	            }
	        );
			
			const chatRoomId = res.data;

			showDirectRoom(chatRoomId, selectedMember.name);

	    } catch (e) {
	        console.error("1:1 채팅방 처리 실패:", e);

	        alert("채팅방을 시작하지 못했습니다.");

	    } finally {
	        // 요청 완료 후 버튼 다시 활성화
	        directChatButton.disabled = false;
	    }
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

	connect();


});
