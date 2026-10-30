document.addEventListener('DOMContentLoaded', function () {
    const shop = document.querySelector('.tree-shop');

    if (!shop) {
        return;
    }

    const categoryButtons = shop.querySelectorAll('[data-shop-category]');
    const itemPanels = shop.querySelectorAll('[data-shop-items]');

    for (const button of categoryButtons) {
        button.addEventListener('click', function () {
            const category = button.dataset.shopCategory;

            for (const categoryButton of categoryButtons) {
                const selected = categoryButton === button;
                categoryButton.classList.toggle('is-active', selected);
                categoryButton.setAttribute('aria-pressed', String(selected));
            }

            for (const panel of itemPanels) {
                panel.hidden = panel.dataset.shopItems !== category;
            }

            shop.querySelector('.tree-shop-catalog').scrollTop = 0;
        });
    }
	// 상점 이용 가능 여부와 화면용 잔액 읽기
	const shopEnabled = shop.dataset.shopEnabled === 'true';
	let shopExp = Number(shop.dataset.shopExp);

	// 잔액 표시 영역과 배경을 적용할 미리보기 영역
	const availableExpElement = shop.querySelector('#shopAvailableExp');
	const summaryAvailableExpElement = document.querySelector('#summaryAvailableExp');
	const treePreview = document.querySelector('.tree-preview');

	// 배경 상품만 선택
	const backgroundProducts = shop.querySelectorAll(
		'#shop-background .tree-shop-product'
	);
	// 구매한 배경은 현재 페이지에서만 기억하며 새로고침하면 초기화
	const purchasedBackgrounds = new Set();
	let purchasePending = false;

	for (const product of backgroundProducts) {
		product.addEventListener('click', async function () {
			// 클릭한 상품의 가격, 이미지 주소, 이름 읽기
			const price = Number(product.dataset.price);
			const background = product.dataset.background;
			const itemName = product.querySelector('.tree-shop-item-name')
				.textContent.trim();

			// 5단계에 도달하지 않았으면 종료
			if (!shopEnabled) {
				alert('상점은 성장나무 5단계에 도달한 후 이용할 수 있습니다.');
				return;
			}
			
			// 이미 적용 중인 배경이면 종료
			if (product.classList.contains('is-equipped')) {
				return;
			}

			// 공통 확인창이 열려 있는 동안 중복 구매 요청 방지
			if (purchasePending) {
				return;
			}

			// 구매한 배경은 잔액과 관계없이 무료로 다시 적용
			const purchasePrice = purchasedBackgrounds.has(background) ? 0 : price;

			// 사용 가능한 경험치가 실제 차감 금액보다 적으면 종료
			if (shopExp < purchasePrice) {
				alert(
					'경험치가 부족합니다.\n' +
					'필요 경험치: ' + purchasePrice + ' EXP\n' +
					'사용 가능한 경험치: ' + shopExp + ' EXP'
				);
				return;
			}

			// 공통 확인창에서 확인·취소 응답 기다리기
			const message = purchasePrice === 0
				? itemName + ' 배경을 무료로 적용하시겠습니까?'
				: itemName + ' 배경을 ' + purchasePrice + ' EXP로 구매하시겠습니까?';

			let confirmed;
			purchasePending = true;
			try {
				confirmed = await showConfirm(message);
			} finally {
				purchasePending = false;
			}

			// 취소하면 잔액과 배경 유지
			if (!confirmed) {
				return;
			}

			// 화면에서 사용하는 잔액만 차감
			shopExp -= purchasePrice;
			purchasedBackgrounds.add(background);
			availableExpElement.textContent = shopExp + ' EXP';
			summaryAvailableExpElement.textContent = shopExp + ' EXP';
			shop.dataset.shopExp = String(shopExp);

			// 왼쪽 미리보기 배경 변경
			treePreview.style.backgroundImage = `url("${background}")`;

			// 배경 카드의 테두리와 적용 상태 문구 갱신
			for (const backgroundProduct of backgroundProducts) {
				const equipped = backgroundProduct === product;

				backgroundProduct.classList.toggle('is-equipped', equipped);

				const priceElement = backgroundProduct.querySelector('.tree-shop-price');
				const originalPrice = Number(backgroundProduct.dataset.price);

				priceElement.classList.toggle('is-default', equipped);

				if (equipped) {
					priceElement.textContent = '✓ 적용 중';
				} else if (originalPrice === 0 || purchasedBackgrounds.has(backgroundProduct.dataset.background)) {
					priceElement.textContent = '적용하기';
				} else {
					priceElement.textContent = originalPrice + ' EXP';
				}
			}
		});
	}
});
