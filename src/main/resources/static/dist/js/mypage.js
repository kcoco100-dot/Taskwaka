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
			
			const profileUrl = profileSection.dataset.profileUrl

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
			const addressUrl = section.dataset.updateUrl

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

});
