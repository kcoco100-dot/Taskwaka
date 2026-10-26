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
	const groupNameWrap = document.getElementById("workchatGroupNameWrap");
	const groupNameInput = document.getElementById("workchatGroupRoomName");
	const pickerConfirm = document.getElementById("workchatPickerConfirm");
	const pickerHint = document.getElementById("workchatPickerFooterHint");
    let chatKind = "team";
	let activeChatRoomId = null;
	let activeChatKind = null;
	let activeRoomOwner = false;

	const renameToggle = document.getElementById("workchatRenameToggle");
	const renamePanel = document.getElementById("workchatRenamePanel");
	const renameInput = document.getElementById("workchatRenameInput");
	const renameSave = document.getElementById("workchatRenameSave");
	const renameCancel = document.getElementById("workchatRenameCancel");

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
	

	function renderPickerMembers(members) {

	    personList.replaceChildren();

	    const groups = [];

	    members.forEach((member) => {

	        const roleName = member.roleName || "기타";

	        let group = groups.find((item) => {
	            return item.roleName === roleName;
	        });

	        if (! group) {
	            group = {
	                roleName: roleName,
	                members: []
	            };

	            groups.push(group);
	        }

	        group.members.push(member);
	    });

	    groups.forEach((group) =>  {

	        const groupDiv = document.createElement("div");
	        groupDiv.className = "workchat-picker-group";

	        const heading = document.createElement("div");
	        heading.className = "workchat-picker-heading";

	        const roleTitle = document.createElement("strong");
	        roleTitle.textContent = group.roleName;

	        const count = document.createElement("small");
	        count.textContent = group.members.length + "명";

	        heading.append(roleTitle, count);
	        groupDiv.appendChild(heading);

	        group.members.forEach(function(member) {

	            const label = document.createElement("label");
	            label.className = "workchat-picker-person";

	            label.setAttribute("data-workchat-person-row", "");
	            label.dataset.name = member.name;
	            label.dataset.role = member.roleName || "";

	            const avatar = document.createElement("span");
	            avatar.className =
	                "workchat-avatar workchat-avatar--sand workchat-avatar--mini";
	            avatar.textContent = member.name.slice(-2);

	            const name = document.createElement("span");
	            name.className = "workchat-picker-person-label";

	            const strong = document.createElement("strong");
	            strong.textContent = member.name;

	            const small = document.createElement("small");
	            small.textContent = member.roleName || "";

	            name.append(strong, small);

	            const checkbox = document.createElement("input");
	            checkbox.type = "checkbox";

	            checkbox.setAttribute("data-workchat-participant", "");

	            // 화면에는 이름을 표시하고 서버에는 회원번호를 전달
	            checkbox.value = member.name;
	            checkbox.dataset.memberId = member.memberId;

	            const checkMark = document.createElement("span");
	            checkMark.className = "workchat-picker-check";
	            checkMark.textContent = "✓";
	            checkMark.setAttribute("aria-hidden", "true");

	            label.append(avatar, name, checkbox, checkMark);
	            groupDiv.appendChild(label);
	        });

	        personList.appendChild(groupDiv);
	    });
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
	
	

		   function showChatRoom(chatRoomId, roomName, kind, groupCount, canManage = false) {

		       activeChatRoomId = chatRoomId;
		       activeChatKind = kind;
		       activeRoomOwner = kind === "GROUP" && canManage;

		       document.getElementById("workchatActiveRoomTitle")
		           .textContent = roomName;

		       const subtitle = kind === "GROUP" ? "그룹 채팅 · " + groupCount + "명" : "1:1 채팅 · 방 번호 " + chatRoomId;

		       document.getElementById("workchatActiveRoomSubtitle")
		           .textContent = subtitle;

		       document.getElementById("workchatActiveRoomAvatar")
		           .textContent = roomName.slice(-2);

		       renameToggle.hidden = ! activeRoomOwner;

		       renamePanel.hidden = true;

		       views.forEach((view) => {
		           view.hidden = view.dataset.workchatView !== "chats";
		       });

		       createViews.forEach((view) => {
		           view.hidden = true;
		       });

		       loadMessages(chatRoomId);
		   }


	   function showDirectRoom(chatRoomId, targetName) {
	       showChatRoom(chatRoomId, targetName, "PERSONAL", 2);
	   }


	   function openRoom(room) {

	       const roomName = room.chatKind === "GROUP"
	           ? (room.roomName || "그룹 채팅")
	           : (room.targetName || "1:1 채팅");

	       showChatRoom(
	           room.chatRoomId,
	           roomName,
	           room.chatKind,
	           room.groupCount,
			   room.canManage === 1
	       );
	   }
	   

	   renameToggle.addEventListener("click", function() {

	       if (activeChatKind !== "GROUP" || ! activeRoomOwner) {
	           return;
	       }

	       renameInput.value = document.getElementById("workchatActiveRoomTitle").textContent;
	       renamePanel.hidden = false;
	       renameInput.focus();
	   	});


	   renameCancel.addEventListener("click", () => {
	       renamePanel.hidden = true;
	   });


	   renameSave.addEventListener("click", async () => {

	       if (activeChatKind !== "GROUP" || ! activeRoomOwner || activeChatRoomId == null) {
	           return;
	       }

	       const roomName = renameInput.value.trim();

	       if (roomName.length === 0 || roomName.length > 15) {
	           alert("그룹 이름은 1~15자로 입력해 주세요.");
	           return;
	       }

	       const chatRoomId = activeChatRoomId;
	       const domainKey = window.location.pathname.split("/")[1];

	       try {
	           renameSave.disabled = true;

	           const res = await axios.put(
	               "/" + domainKey + "/api/workchat/rooms/" + chatRoomId + "/name",
	               {
	                   roomName: roomName
	               }
	           );

	           if (Number(activeChatRoomId) === Number(chatRoomId)) {

	               const newRoomName = res.data;

	               document.getElementById("workchatActiveRoomTitle")
	                   .textContent = newRoomName;

	               document.getElementById("workchatActiveRoomAvatar")
	                   .textContent = newRoomName.slice(-2);

	               renamePanel.hidden = true;
	           }

	           await loadMyDirectRooms();

	       } catch (e) {
	           console.error("그룹 이름 변경 실패:", e);

	           alert(e.response?.data || "그룹 이름을 변경하지 못했습니다.");
	       } finally {
	           renameSave.disabled = false;
	       }
	   });
	   
	   
 	 	let roomListVersion = 0;

	   async function loadMyDirectRooms() {

	       const domainKey = window.location.pathname.split("/")[1];
	       const version = ++roomListVersion;

	       try {

	           const [directRes, groupRes] = await Promise.all([
	               axios.get("/" + domainKey + "/api/workchat/rooms/direct"),
	               axios.get("/" + domainKey + "/api/workchat/rooms/group")
	           ]);

	           // 이전 요청 결과가 뒤늦게 도착하면 무시
	           if (version !== roomListVersion) return;

	           const directRooms = directRes.data.map((room) => {
	               return {
	                   ...room,
	                   chatKind: "PERSONAL"
	               };
	           });

	           const groupRooms = groupRes.data.map((room) => {
	               return {
	                   ...room,
	                   chatKind: "GROUP"
	               };
	           });
				
			   const rooms = [...directRooms, ...groupRooms];

			   // 1:1과 그룹 채팅방을 합친 후 전체 최신순 정렬
			   rooms.sort((a, b) => {

			       const dateA = a.lastSent;
			       const dateB = b.lastSent;

			       if (dateA == null && dateB == null) return 0;
			       if (dateA == null) return 1;
			       if (dateB == null) return -1;

			       return dateB.localeCompare(dateA);
			   });
			   
	           // 현재 보고 있는 채팅방의 읽지 않음 마크 숨기기
	           rooms.forEach((room) => {
	               if (isReadingRoom(room.chatRoomId)) {
	                   room.unreadCount = 0;
	               }
	           });

	           roomsVm.rooms = rooms;
			   
			   // 현재 열어둔 그룹 채팅방 정보도 갱신
			   if (activeChatKind === "GROUP" && activeChatRoomId != null) {

			       const activeRoom = rooms.find((room) => {
			           return Number(room.chatRoomId) === Number(activeChatRoomId)
			               && room.chatKind === "GROUP";
			       });

			       if (activeRoom) {
			           document.getElementById("workchatActiveRoomTitle")
			               .textContent = activeRoom.roomName;

			           document.getElementById("workchatActiveRoomAvatar")
			               .textContent = activeRoom.roomName.slice(-2);

			           activeRoomOwner = activeRoom.canManage === 1;
			           renameToggle.hidden = ! activeRoomOwner;
			       }
			   }

	           const roomCount = modal.querySelector('[data-workchat-panel="chats"] .workchat-room-count');

	           if (roomCount) {roomCount.textContent = rooms.length + "개";}
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
			
			if (version !== messageLoadVersion ||
			    Number(activeChatRoomId) !== Number(chatRoomId)) {
			    return;
			}
			
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

	            await loadMessages(chatRoomId, true);
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
        views.forEach((view) => { view.hidden = true; });
        createViews.forEach((view) => {
            view.hidden = view.dataset.workchatCreateView !== name;
        });
    }
	

	function updatePickerConfirm() {

	    const selectedCount = getPersonChecks().filter((check) => {
	        return check.checked;}).length;

	    if (chatKind === "team") {

	        pickerConfirm.disabled =
	            selectedCount < 2 || groupNameInput.value.trim() === "";
	    } else {
	        // 일반채팅은 상대방 1명만 선택
	        pickerConfirm.disabled = selectedCount !== 1;
	    }
	}



    function updateSelectedPeople() {
        const selected = getPersonChecks().filter(function (check) { return check.checked; });
        personCounter.textContent = selected.length + "명";
		
		updatePickerConfirm();
		
        personChips.replaceChildren();
		
        if (!selected.length) {
            const empty = document.createElement("span");
            empty.className = "workchat-picker-empty";
            empty.textContent = "멤버를 선택하면 여기에 표시돼요.";
            personChips.appendChild(empty);
            return;
        }
		
        selected.forEach((check) => {
            const chip = document.createElement("span");
            chip.className = "workchat-picker-chip";
            chip.textContent = check.value;
            personChips.appendChild(chip);
        });
        personChips.scrollLeft = personChips.scrollWidth;
    }

	groupNameInput.addEventListener("input", updatePickerConfirm);
	
    async function openKind(kind) {
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
		
        getPersonChecks().forEach((check) => { check.checked = false; });
		
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
		

		groupNameWrap.hidden = kind !== "team";
		groupNameInput.value = "";

		pickerConfirm.textContent =
		    kind === "team" ? "그룹 채팅방 만들기" : "1:1 채팅 시작";

		pickerHint.textContent =
		    kind === "team"
		        ? "본인 외 2명 이상을 선택해 주세요."
		        : "대화할 회원 1명을 선택해 주세요.";

		updatePickerConfirm();

		// 회원 데이터를 불러오는 동안 기존 선택 목록 초기화
		personList.replaceChildren();

		// 기존 회원 조회 함수 재사용
		await loadMembers();

		// 사용자가 다른 화면으로 이동하지 않았을 때만 표시
		const pickerView = modal.querySelector(
		    '[data-workchat-create-view="chats"]'
		);

		if (chatKind === kind && pickerView && !pickerView.hidden) {
		    renderPickerMembers(memberVm.members);
		}

		
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
	

	pickerConfirm.addEventListener("click", async function() {

	    const selectedIds = getPersonChecks()
	        .filter(function(check) {
	            return check.checked;
	        })
	        .map(function(check) {
	            return Number(check.dataset.memberId);
	        });

	    const domainKey = window.location.pathname.split("/")[1];

	    try {

	        pickerConfirm.disabled = true;

	        if (chatKind === "team") {

	            const roomName = groupNameInput.value.trim();

	            if (!roomName || selectedIds.length < 2) {
	                alert("그룹 이름과 참여자 2명 이상을 선택해 주세요.");
	                return;
	            }

	            // 그룹 채팅방 생성
	            const res = await axios.post(
	                "/" + domainKey + "/api/workchat/rooms/group",
	                {
	                    roomName: roomName,
	                    memberIds: selectedIds
	                }
	            );

	            const chatRoomId = res.data;

	            showChatRoom(
	                chatRoomId,
	                roomName,
	                "GROUP",
	                selectedIds.length + 1,
					true
	            );

	        } else {

	            if (selectedIds.length !== 1) {
	                alert("대화할 회원 1명을 선택해 주세요.");
	                return;
	            }

	            const member = memberVm.members.find(function(item) {
	                return Number(item.memberId) === selectedIds[0];
	            });

	            if (!member || member.workId == null) {
	                alert("회원 정보를 확인할 수 없습니다.");
	                return;
	            }

	            // 기존 1:1 채팅방 생성 또는 재사용
	            const res = await axios.post(
	                "/" + domainKey + "/api/workchat/rooms/direct",
	                {
	                    memberId: member.memberId,
	                    workId: member.workId
	                }
	            );

	            showDirectRoom(res.data, member.name);
	        }

	        // 생성된 채팅방을 목록에 반영
	        await loadMyDirectRooms();

	    } catch (e) {

	        console.error("채팅방 생성 실패:", e);
	        alert("채팅방을 만들지 못했습니다.");

	    } finally {

	        updatePickerConfirm();
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
