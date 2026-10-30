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


        photoFile.addEventListener("change", () => {
            const file = photoFile.files[0];

            if (! file) return;

            const reader = new FileReader();

            reader.onload = () => {
                photoPreview.src = reader.result;
                photoPreview.style.display = "block";
                photoPlaceholder.style.display = "none";
            };

            reader.readAsDataURL(file);

            photoSave.disabled = false;
        });


        photoSave.addEventListener("click", async () => {
            const file = photoFile.files[0];

            if (! file) return;

            const formData = new FormData();
            formData.append("selectFile", file);

            photoSave.disabled = true;

            try {
                const response = await fetch(profileUrl, {
                    method: "POST",
                    body: formData
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    alert(result.message || "프로필 사진이 변경되었습니다.");
                    location.reload();
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


        photoDelete.addEventListener("click", async () => {
            if (! confirm("프로필 사진을 삭제하시겠습니까?")) return;

            photoDelete.disabled = true;

            try {
                const response = await fetch(profileUrl, {
                    method: "DELETE"
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    alert(result.message || "프로필 사진이 삭제되었습니다.");
                    location.reload();
                } else {
                    alert(result.message || "프로필 사진 삭제에 실패했습니다.");
                }

            } catch (e) {
                console.error("프로필 사진 삭제 오류", e);
                alert("서버 통신 중 오류가 발생했습니다.");

            } finally {
                photoDelete.disabled = false;
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
                    location.reload();
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



});
