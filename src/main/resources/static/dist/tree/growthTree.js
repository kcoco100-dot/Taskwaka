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

	for (const product of backgroundProducts) {
		product.addEventListener('click', async function () {
			// 클릭한 상품의 가격, 이미지 주소, 이름 읽기
			const price = Number(product.dataset.price);
			const background = product.dataset.background;
			const itemName = product.querySelector('.tree-shop-item-name')
				.textContent.trim();

			// 다음 단계에서 이용 조건 검사와 구매 확인 처리 추가
			// 5단계에 도달하지 않았으면 종료
			if (!shopEnabled) {
				alert('상점은 성장나무 5단계에 도달한 후 이용할 수 있습니다.');
				return;
			}
			
			// 이미 적용 중인 배경이면 종료
			if (product.classList.contains('is-equipped')) {
				return;
			}

			// 사용 가능한 경험치가 상품 가격보다 적으면 종료
			if (shopExp < price) {
				alert(
					'경험치가 부족합니다.\n' +
					'필요 경험치: ' + price + ' EXP\n' +
					'사용 가능한 경험치: ' + shopExp + ' EXP'
				);
				return;
			}

			// 공통 확인창에서 확인·취소 응답 기다리기
			const message = price === 0
				? itemName + ' 배경을 적용하시겠습니까?'
				: itemName + ' 배경을 ' + price + ' EXP로 구매하시겠습니까?';

			const confirmed = await showConfirm(message);

			// 취소하면 잔액과 배경 유지
			if (!confirmed) {
				return;
			}

			// 화면에서 사용하는 잔액만 차감
			shopExp -= price;
			availableExpElement.textContent = shopExp + ' EXP';
			summaryAvailableExpElement.textContent = shopExp + ' EXP';
			shop.dataset.shopExp = String(shopExp);

			// 왼쪽 미리보기 배경 변경
			treePreview.style.backgroundImage = `url("${background}")`;

			// 선택한 배경 카드에 적용 중 테두리 표시
			for (const backgroundProduct of backgroundProducts) {
				backgroundProduct.classList.toggle(
					'is-equipped',
					backgroundProduct === product
				);
			}
		});
	}
});
