document.addEventListener("DOMContentLoaded", () => {

    const formatPhone = value =>
        value.replace(/^(\d{3})(\d{3,4})(\d{4})$/, "$1-$2-$3");

    const displayPhone = document.getElementById("mypagePhone");

    if (displayPhone) {
        displayPhone.textContent = formatPhone(displayPhone.textContent);
    }

    const profileSection = document.querySelector("[data-profile-url]");

    if (profileSection) {

        const photoChange = document.getElementById("mypagePhotoChange");
        const photoFile = document.getElementById("mypagePhotoFile");
        const photoPreview = document.getElementById("mypagePhotoPreview");
        const photoPlaceholder = document.getElementById("mypagePhotoPlaceholder");
        const photoSave = document.getElementById("mypagePhotoSave");
        const photoDelete = document.getElementById("mypagePhotoDelete");
		
		const profileUrl = profileSection.dataset.profileUrl;

        photoChange.addEventListener("click", () => {
            photoFile.click();
        });


        const hasSavedPhoto = !photoDelete.disabled;
        let deletePending = false;

        photoFile.addEventListener("change", () => {
            const file = photoFile.files[0];
            if (!file) return;

            deletePending = false;
            const reader = new FileReader();
            reader.onload = () => {
                if (deletePending || photoFile.files[0] !== file) return;
                photoPreview.src = reader.result;
                photoPreview.style.display = "block";
                photoPlaceholder.style.display = "none";
            };
            reader.readAsDataURL(file);

            photoSave.disabled = false;
            photoDelete.disabled = false;
        });

        // 삭제는 즉시 요청하지 않고 미리보기만 기본 이미지로 변경
        photoDelete.addEventListener("click", () => {
            if (!confirm("프로필 사진을 삭제하도록 선택하시겠습니까? 변경사항 저장 시 적용됩니다.")) return;

            photoFile.value = "";
            deletePending = hasSavedPhoto;
            photoPreview.removeAttribute("src");
            photoPreview.style.display = "none";
            photoPlaceholder.style.display = "";
            photoSave.disabled = !hasSavedPhoto;
            photoDelete.disabled = true;
        });

        photoSave.addEventListener("click", async () => {
            const file = photoFile.files[0];
            if (!file && !deletePending) return;

            photoSave.disabled = true;
            try {
                let response;
                if (deletePending) {
                    response = await fetch(profileUrl, { method: "DELETE" });
                } else {
                    const formData = new FormData();
                    formData.append("selectFile", file);
                    response = await fetch(profileUrl, { method: "POST", body: formData });
                }
                const result = await response.json();
                if (response.ok && result.success) {
                    alert(result.message || "프로필 사진이 변경되었습니다.");
                    window.location.assign("/member/mypage");
                } else {
                    alert(result.message || "프로필 사진 변경에 실패했습니다.");
                }
            } catch (e) {
                console.error("프로필 사진 저장 오류", e);
                alert("서버 통신 중 오류가 발생했습니다.");
            } finally {
                photoSave.disabled = false;
            }
        });

    }

    const section = document.querySelector("[data-update-url]");

    if (section) {

        const phone = document.getElementById("mypageEditPhone");
        const postcode = document.getElementById("mypagePostcode");
        const baseAddress = document.getElementById("mypageBaseAddress");
        const detailAddress = document.getElementById("mypageDetailAddress");
        const addressValue = document.getElementById("mypageAddressValue");
        const addressStatus = document.getElementById("mypageAddressStatus");
        const saveButton = document.getElementById("mypageContactSave");

        const searchAddress = document.getElementById("mypageAddressSearch");
        const cancelAddress = document.getElementById("mypageAddressCancel");
        const clearAddress = document.getElementById("mypageAddressClear");

        const originalAddress = addressValue.value;
		
		const addressUrl = section.dataset.updateUrl;

        phone.value = formatPhone(phone.value);

        searchAddress.addEventListener("click", () => {

            if (!window.kakao || !window.kakao.Postcode) {
                alert("주소 검색 서비스를 불러올 수 없습니다.");
                return;
            }

            new kakao.Postcode({
                oncomplete(data) {
                    postcode.value = data.zonecode;
                    baseAddress.value = data.address;
                    detailAddress.value = "";
                    detailAddress.disabled = false;

                    updateAddress();

                    addressStatus.textContent = "새 주소가 선택되었습니다. 저장 시 변경됩니다.";

                    detailAddress.focus();
                }
            }).open();
        });

        function updateAddress() {
            addressValue.value = baseAddress.value
                ? `[${postcode.value}] ${baseAddress.value} ${detailAddress.value.trim()}`.trim()
                : "";
        }

        detailAddress.addEventListener("input", updateAddress);

        function resetAddress() {
            postcode.value = "";
            baseAddress.value = "";
            detailAddress.value = "";
            detailAddress.disabled = true;
        }

        clearAddress.addEventListener("click", () => {

            if (! confirm("등록된 주소를 삭제하시겠습니까?")) return;

            resetAddress();
            addressValue.value = "";

            addressStatus.textContent = "주소 삭제가 선택되었습니다. 저장 시 반영됩니다.";
        });

        cancelAddress.addEventListener("click", () => {

            resetAddress();
            addressValue.value = originalAddress;

            addressStatus.textContent = "주소 변경을 취소했습니다. 기존 주소가 유지됩니다.";
        });


        saveButton.addEventListener("click", async () => {

            saveButton.disabled = true;

            try {
                const response = await fetch(addressUrl, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        phone: phone.value.trim(),
                        address: addressValue.value.trim()
                    })
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    alert(result.message || "회원정보가 수정되었습니다.");
                    window.location.assign("/member/mypage");
                } else {
                    alert(result.message || "회원정보 수정에 실패했습니다.");
                }

            } catch (e) {
                console.error("회원정보 수정 오류", e);
                alert("서버 통신 중 오류가 발생했습니다.");

            } finally {
                saveButton.disabled = false;
            }
        });

    }
	

	const pwdSection = document.querySelector("[data-pwd-url]");

	if (pwdSection) {

	    const currentPwd = document.getElementById("mypageCurrentPwd");
	    const newPwd = document.getElementById("mypageNewPwd");
	    const confirmPwd = document.getElementById("mypageConfirmPwd");
	    const pwdSave = document.getElementById("mypagePwdSave");

	    const pwdUrl = pwdSection.dataset.pwdUrl;

	    pwdSave.addEventListener("click", async () => {

	        pwdSave.disabled = true;

	        try {
	            const response = await fetch(pwdUrl, {
	                method: "POST",
	                headers: {
	                    "Content-Type": "application/json"
	                },
	                body: JSON.stringify({
	                    currentPwd: currentPwd.value,
	                    newPwd: newPwd.value,
	                    confirmPwd: confirmPwd.value
	                })
	            });

	            const result = await response.json();

	            if (response.ok && result.success) {

	                alert(result.message || "비밀번호가 변경되었습니다.");

	                currentPwd.value = "";
	                newPwd.value = "";
	                confirmPwd.value = "";

	            } else {
	                alert(result.message || "비밀번호 변경에 실패했습니다.");
	            }

	        } catch (e) {
	            console.error("비밀번호 변경 오류", e);
	            alert("서버 통신 중 오류가 발생했습니다.");

	        } finally {
	            pwdSave.disabled = false;
	        }

	    });
	}
	

	document.querySelectorAll(".mypage-work-leave").forEach(button => {

	    button.addEventListener("click", async () => {

	        button.disabled = true;

	        try {
	            const resInfo = await fetch(button.dataset.leaveInfoUrl);
	            const info = await resInfo.json();

	            if (! resInfo.ok || ! info.success) {
	                alert(info.message || "워크 탈퇴 정보를 확인할 수 없습니다.");
	                return;
	            }

	            const workName = button.closest(".mypage-info-box")?.querySelector("strong")?.textContent?.trim() || "해당 워크";

	            const message = info.hasOngoingWork
	                ? `"${workName}"에 진행 중이거나 검토 중인 업무가 있습니다.\n\n` +
	                  "탈퇴하면 해당 워크와 프로젝트·업무에 대한 참여 권한이 해제됩니다.\n" +
	                  "그래도 워크에서 탈퇴하시겠습니까?"
	                : `"${workName}"에서 탈퇴하시겠습니까?\n\n` +
	                  "탈퇴하면 해당 워크의 참여 권한이 해제됩니다.";

	            if (!confirm(message)) return;

	            const response = await fetch(button.dataset.leaveUrl, {
	                method: "POST"
	            });

	            const result = await response.json();

	            if (! response.ok || ! result.success) {
	                alert(result.message || "워크 탈퇴에 실패했습니다.");
	                return;
	            }

	            alert(result.message || "워크에서 탈퇴했습니다.");
	            window.location.reload();

	        } catch (e) {
	            console.error("워크 탈퇴 오류", e);
	            alert("워크 탈퇴 처리 중 오류가 발생했습니다.");

	        } finally {
	            button.disabled = false;
	        }
	    });
	});


    document.querySelectorAll(".mypage-work-delete").forEach(button => {
        button.addEventListener("click", async () => {
            button.disabled = true;

            try {
                const response = await fetch(button.dataset.deleteInfoUrl, {
                    headers: { "Accept": "application/json" }
                });

                if (response.redirected && response.url.includes("/member/login")) {
                    throw new Error("로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
                }

                if (!(response.headers.get("content-type") || "").includes("json")) {
                    console.error("워크 삭제 정보 API 응답", response.status, await response.text());
                    throw new Error(response.status === 404
                        ? "워크 삭제 API를 찾을 수 없습니다. 9차 Controller 소스 반영 여부를 확인해주세요."
                        : `워크 삭제 정보 API가 정상 응답하지 않았습니다. (HTTP ${response.status})`);
                }

                const info = await response.json();

                if (! response.ok || ! info.success) {
                    alert(info.message || "워크 삭제 정보를 확인할 수 없습니다.");
                    return;
                }

                const workName = button.closest(".mypage-info-box")?.querySelector("strong")?.textContent?.trim() || "해당 워크";

                if (info.hasOtherMembers) {
                    alert(`"${workName}"에 다른 워크 회원이 있습니다.\n사장 교체를 신청하고 관리자 승인을 받아야 합니다.`);

                    if (confirm("사장 교체 신청 페이지로 이동하시겠습니까?")) {
                        window.location.assign(button.dataset.rolesUrl);
                    }
                    return;
                }

                if (! confirm(`"${workName}" 워크를 삭제하시겠습니까?\n\n워크와 도메인이 비활성화되며 해당 워크에 더 이상 접근할 수 없습니다.`)) {
                    return;
                }

                const deleteResponse = await fetch(button.dataset.deleteUrl, {
                    method: "POST",
                    headers: { "Accept": "application/json" }
                });

                if (deleteResponse.redirected && deleteResponse.url.includes("/member/login")) {
                    throw new Error("로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
                }

                if (!(deleteResponse.headers.get("content-type") || "").includes("json")) {
                    console.error("워크 삭제 실행 API 응답", deleteResponse.status, await deleteResponse.text());
                    throw new Error(deleteResponse.status === 404 || deleteResponse.status === 405
                        ? "워크 삭제 요청 주소가 등록되지 않았습니다. 9차 Controller 소스와 서버 재시작을 확인해주세요."
                        : `워크 삭제 실행 API가 정상 응답하지 않았습니다. (HTTP ${deleteResponse.status})`);
                }

                const result = await deleteResponse.json();

                if (! deleteResponse.ok || ! result.success) {
                    alert(result.message || "워크 삭제에 실패했습니다.");
                    return;
                }

                alert(result.message || "워크가 삭제되었습니다.");
                window.location.assign("/member/mypage/works");

            } catch (e) {
                console.error("워크 삭제 오류", e);
                alert(e.message || "워크 삭제 처리 중 오류가 발생했습니다.");
            } finally {
                button.disabled = false;
            }
        });
    });

    const accountWithdrawal = document.getElementById("mypageAccountWithdrawal");
    const withdrawalDialog = document.getElementById("mypageWithdrawalDialog");
    const withdrawalCancel = document.getElementById("mypageWithdrawalCancel");
    const withdrawalGoWorks = document.getElementById("mypageWithdrawalGoWorks");

    const showWithdrawalDialog = () => {
        if (!withdrawalDialog) return;
        withdrawalDialog.hidden = false;
        withdrawalCancel.focus();
    };

    const hideWithdrawalDialog = () => {
        withdrawalDialog.hidden = true;
        accountWithdrawal.focus();
    };

    if (withdrawalDialog) {
        withdrawalCancel.addEventListener("click", hideWithdrawalDialog);
        withdrawalGoWorks.addEventListener("click", () => {
            window.location.assign(withdrawalGoWorks.dataset.worksUrl);
        });
        withdrawalDialog.addEventListener("click", e => {
            if (e.target === withdrawalDialog) hideWithdrawalDialog();
        });
        document.addEventListener("keydown", e => {
            if (withdrawalDialog.hidden) return;

            if (e.key === "Escape") {
                hideWithdrawalDialog();
            } else if (e.key === "Tab") {
                if (e.shiftKey && document.activeElement === withdrawalCancel) {
                    e.preventDefault();
                    withdrawalGoWorks.focus();
                } else if (!e.shiftKey && document.activeElement === withdrawalGoWorks) {
                    e.preventDefault();
                    withdrawalCancel.focus();
                }
            }
        });
    }

    if (accountWithdrawal) {
        accountWithdrawal.addEventListener("click", async () => {
            if (accountWithdrawal.dataset.hasActiveWorkspaces === "true") {
                showWithdrawalDialog();
                return;
            }

            if (!confirm("TASKWAKA 사이트에서 탈퇴하시겠습니까?\n탈퇴 후에는 기존 아이디로 재가입할 수 없습니다.")) return;

            accountWithdrawal.disabled = true;

            try {
                const response = await fetch(accountWithdrawal.dataset.withdrawUrl, { method: "POST" });
                const result = await response.json();

                if (! response.ok || ! result.success) {
                    if (response.status === 409 && result.message?.startsWith("활성 워크가 있습니다.")) {
                        showWithdrawalDialog();
                    } else {
                        alert(result.message || "사이트 탈퇴에 실패했습니다.");
                    }
                    return;
                }

                alert(result.message || "사이트 회원 탈퇴가 완료되었습니다.");
                window.location.assign("/member/login");

            } catch (e) {
                console.error("사이트 탈퇴 오류", e);
                alert("사이트 탈퇴 처리 중 오류가 발생했습니다.");
            } finally {
                accountWithdrawal.disabled = false;
            }
        });
    }


});
