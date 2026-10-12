// 드롭다운 메뉴
document.addEventListener("DOMContentLoaded", () => {
	const profileToggle = document.querySelector("#profileToggle");
	const profileMenu = document.querySelector("#profileMenu");
	
	if(! profileToggle || ! profileMenu) return;

	profileToggle.addEventListener("click", () => {
		
		const isOpen = ! profileMenu.hidden;

		profileMenu.hidden = isOpen;

		profileToggle.setAttribute("aria-expanded" , String(! isOpen));
	});





	document.addEventListener("click", e => {
		if(! e.target.closest("div.profile-dropdown")){
			profileMenu.hidden = true;
			profileToggle.setAttribute("aria-expanded" , "false");
		}
	});
	
	document.addEventListener("keydown", e => {
		if(e.key === "Escape"){
			profileMenu.hidden = true;
			profileToggle.setAttribute("aria-expanded", "false");
		}
	});
});