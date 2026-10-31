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

	// 서버 기준 EXP와 화면에 표시할 잔액을 분리
	let shopExp = 0;
    let catalog = new Map();
    const shopUrl = shop.dataset.shopUrl;
    const message = document.getElementById('shopMessage');

    // 잔액 표시 영역과 배경을 적용할 미리보기 영역
	const availableExpElement = shop.querySelector('#shopAvailableExp');
	const summaryAvailableExpElement = document.querySelector('#summaryAvailableExp');
	const treePreview = document.querySelector('.tree-preview');

	// 배경 상품만 선택
	const backgroundProducts = shop.querySelectorAll(
		'#shop-background .tree-shop-product'
	);
	// 배경 구매 기록과 선택 상태를 관리하며 브라우저 저장 데이터로 복원
	const purchasedBackgrounds = new Set();
	const defaultBackground = Array.from(backgroundProducts).find(
		product => Number(product.dataset.price) === 0
	).dataset.background;
	let selectedBackground = defaultBackground;
	let purchasePending = false;
	
	// 열매 상품과 나무 위 표시 영역
	const fruitProducts = shop.querySelectorAll(
	    '#shop-fruit .tree-shop-product'
	);
	const fruitLayer = document.getElementById('treeFruitLayer');
	const fruitImages = fruitLayer.querySelectorAll('.tree-fruit');

	// 열매 구매 기록과 선택 상태
	const purchasedFruits = new Set();
	let selectedFruit = null;
	
	// 착용템 : 모자 표시 이미지 및 구매, 착용 상태
	const hatProduct = shop.querySelector(
		'#shop-accessory .tree-shop-product[data-hat]');
	
	const hatImage = document.getElementById('treeHatImage');
	
	// 모자 구매 기록 및 착용 상태
	const purchasedHats = new Set();
	let selectedHat = null;
	
	// 트리, 은, 금 장식
	const decorationProducts = shop.querySelectorAll(
		'#shop-accessory .tree-shop-product[data-decoration]'
	);
	const decorationLayers = document.querySelectorAll(
		'.tree-image-area .tree-decoration-layer'
	);
	
	// 장식 구매 기록 + 착용 장식 종류 관리
	const purchasedDecorations = new Set();
	const equippedDecorations  = new Set();
	
	// 펫 상품과 나무 주변 표시 영역
	const petProducts = shop.querySelectorAll(
	    '#shop-pet .tree-shop-product[data-pet]'
	);
	const petLayer = document.getElementById('treePetLayer');
	const petImages = petLayer.querySelectorAll('.tree-pet');

	// 펫 구매 기록과 동시에 배치한 펫 종류 관리
	const purchasedPets = new Set();
	const equippedPets = new Set();
	
	// 상품 카드의 클래스와 상태 문구 갱신
	function renderShopCard(product, {
	    equipped,
	    purchased,
	    equippedText,
	    actionText,
	    priceText = Number(product.dataset.price) + ' EXP'
	}) {
	    const priceElement = product.querySelector('.tree-shop-price');

	    product.classList.toggle('is-equipped', equipped);
	    priceElement.classList.toggle('is-default', equipped);

	    if (equipped) {
	        priceElement.textContent = equippedText;
	    } else if (purchased) {
	        priceElement.textContent = actionText;
	    } else {
	        priceElement.textContent = priceText;
	    }
	}
	
	// 상점 이용 조건 확인부터 구매·적용·저장까지 공통 처리
	function renderShopState() {

	    availableExpElement.textContent = shopExp + ' EXP';
	    summaryAvailableExpElement.textContent = shopExp + ' EXP';
	    shop.dataset.shopExp = String(shopExp);
	    treePreview.style.backgroundImage = `url("${selectedBackground}")`;

	    for (const backgroundProduct of backgroundProducts) {
	        const background = backgroundProduct.dataset.background;
	        const equipped = background === selectedBackground;
			renderShopCard(backgroundProduct, {
			    equipped,
			    purchased: Number(backgroundProduct.dataset.price) === 0 ||
			        purchasedBackgrounds.has(background),
			    equippedText: '✓ 적용 중',
			    actionText: '적용하기'
			});
	    }
		
		// 선택한 열매 5개와 카드 상태를 화면에 반영
		// 선택한 열매 5개 표시
		fruitLayer.hidden = selectedFruit === null;

		if (selectedFruit !== null) {
		    for (const fruitImage of fruitImages) {
		        fruitImage.src = selectedFruit;
		    }
		}

		// 열매 카드의 착용 상태와 가격 표시
		for (const fruitProduct of fruitProducts) {
		    const fruit = fruitProduct.dataset.fruit;
			// 현재 카드의 열매가 지금 착용한 열매인지 비교
		    const equipped = fruit === selectedFruit;
			renderShopCard(fruitProduct, {
			    equipped,
			    purchased: purchasedFruits.has(fruit),
			    equippedText: '✓ 착용 중',
			    actionText: '착용하기'
			});
		}
		
		// 착용한 모자 표시
		hatImage.hidden = selectedHat === null;
		
		if(selectedHat !== null) {
			hatImage.src = selectedHat;
		}
		
		// 모자 카드의 착용 상태와 가격 표시
		const hat = hatProduct.dataset.hat;
		// hatEquipped : 모자 카드가 현재 착용중인 모자인지 판단한 결과
		const hatEquipped = hat === selectedHat;
		renderShopCard(hatProduct, {
		    equipped: hatEquipped,
		    purchased: purchasedHats.has(hat),
		    equippedText: '✓ 착용 중',
		    actionText: '착용하기',
		    priceText: Number(hatProduct.dataset.price) + 'EXP'
		});
		
		// 착용한 종류마다 장식 2개 표시
		for (const layer of decorationLayers) {
		    const decoration = layer.dataset.decoration;
		    layer.hidden = !(shopEnabled &&
		        purchasedDecorations.has(decoration) &&
		        equippedDecorations.has(decoration));
		}

		// 장식 카드의 착용 상태와 가격 표시
		for (const product of decorationProducts) {
		    const decoration = product.dataset.decoration;
		    const equipped = shopEnabled &&
		        purchasedDecorations.has(decoration) &&
		        equippedDecorations.has(decoration);
				renderShopCard(product, {
				    equipped,
				    purchased: purchasedDecorations.has(decoration),
				    equippedText: '✓ 착용 중',
				    actionText: '착용하기'
				});
		}
		
		// 상점 이용 조건과 펫 배치 상태에 따라 표시
		petLayer.hidden = !shopEnabled || equippedPets.size === 0;

		for (const petImage of petImages) {
		    const pet = petImage.dataset.pet;
		    petImage.hidden = !(shopEnabled &&
		        purchasedPets.has(pet) &&
		        equippedPets.has(pet));
		}

		// 펫 카드의 배치 상태와 가격 표시
		for (const product of petProducts) {
		    const pet = product.dataset.pet;
		    const equipped = shopEnabled &&
		        purchasedPets.has(pet) &&
		        equippedPets.has(pet);
				renderShopCard(product, {
				    equipped,
				    purchased: purchasedPets.has(pet),
				    equippedText: '✓ 배치 중',
				    actionText: '배치하기'
				});
		}
	}
	// 배경, 열매 상태를 함께 저장하는 함수 준비
	function applyState(state) {
        shopExp = state.balance;
        catalog = new Map(state.items.map(item => [item.path.replace(/^\/dist\//, shop.dataset.assetRoot), item]));
        for (const set of [purchasedBackgrounds, purchasedFruits, purchasedHats, purchasedDecorations,
                           equippedDecorations, purchasedPets, equippedPets]) set.clear();
        selectedBackground = defaultBackground;
        selectedFruit = selectedHat = null;
        for (const [path, item] of catalog) {
            if (item.ownedId == null) continue;
            const active = Boolean(item.equipped);
            switch (item.slot) {
                case 'BACKGROUND': purchasedBackgrounds.add(path); if (active) selectedBackground = path; break;
                case 'FRUIT': purchasedFruits.add(path); if (active) selectedFruit = path; break;
                case 'HAT': purchasedHats.add(path); if (active) selectedHat = path; break;
                case 'DECORATION': purchasedDecorations.add(path); if (active) equippedDecorations.add(path); break;
                case 'PET': purchasedPets.add(path); if (active) equippedPets.add(path); break;
            }
        }
        document.querySelectorAll('.tree-shop-product').forEach(product => {
            const item = catalog.get(productPath(product));
            if (item) product.dataset.price = String(item.price);
            product.setAttribute('aria-disabled', String(!item || !shopEnabled));
        });
        renderShopState();
        message.textContent = catalog.size ? '' : '아직 등록된 상점 상품이 없습니다.';
    }

    function productPath(product) {
        return product.dataset.background || product.dataset.fruit || product.dataset.hat ||
                product.dataset.decoration || product.dataset.pet;
    }

    async function reloadShop() {
        const res = await fetch(shopUrl, {headers: {Accept: 'application/json'}});
        if (!res.ok) throw new Error('상점 정보를 불러오지 못했습니다.');
        applyState(await res.json());
    }

    for (const product of shop.querySelectorAll('.tree-shop-product')) {
        product.addEventListener('click', async () => {
            if (purchasePending) return;
            const item = catalog.get(productPath(product));
            if (!shopEnabled || !item) {
                message.textContent = shopEnabled ? '판매 중인 상품이 아닙니다.' : '5단계부터 상점을 이용할 수 있습니다.';
                return;
            }
            purchasePending = true;
            try {
                const action = item.equipped ? '장착을 해제' : item.ownedId == null ? item.price + ' EXP로 구매하고 장착' : '장착';
                if (!await showConfirm(product.querySelector('.tree-shop-item-name').textContent.trim() + ' · ' + action + '하시겠습니까?')) return;
                const res = await fetch(shopUrl + '/' + item.itemId + '?equipped=' + !item.equipped, {method:'PUT'});
                if (!res.ok) throw new Error(res.status === 409 ? '잔액 또는 소유 상태를 확인하세요.' : '상점을 변경하지 못했습니다.');
                applyState(await res.json());
            } catch (error) {
                await reloadShop().catch(() => {});
                message.textContent = error.message;
            } finally { purchasePending = false; }
        });
    }
    const refresh = () => reloadShop().catch(() => { message.textContent = '상점 정보를 불러오지 못했습니다.'; });
    window.addEventListener('focus', refresh);
    refresh();
});
