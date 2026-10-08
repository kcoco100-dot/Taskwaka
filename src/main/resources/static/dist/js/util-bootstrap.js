function showModal(selector) {
	const modalElement = document.querySelector(selector);
	// const modal = new bootstrap.Modal(modalElement);
	const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
	
	modal.show();
}

function hideModal(selector) {
	const modalElement = document.querySelector(selector);
	// const modal = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
	const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
	
	modal.hide();
}

function showOffcanvas(selector) {
	const offcanvasElement = document.querySelector(selector);
	
	const offcanvas = bootstrap.Offcanvas.getOrCreateInstance(offcanvasElement);
	offcanvas.show();
}

function hideOffcanvas(selector) {
	const offcanvasElement = document.querySelector(selector);
	
	const offcanvas = bootstrap.Offcanvas.getOrCreateInstance(offcanvasElement);
	offcanvas.hide();
}
