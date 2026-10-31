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
	const serverShopExp = Number(shop.dataset.shopExp);
	let shopExp = serverShopExp;
	let spentExp = 0;

	// 회원, 워크별로 화면 저장 상태 구분
	const memberId = shop.dataset.memberId;
	const workspaceId = shop.dataset.workspaceId;
	const storageKey = memberId && workspaceId
	    ? `growthTreeShop:v1:${memberId}:${workspaceId}`
	    : null;

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
	
	// 상점 확인창 호출과 중복 요청 차단
	async function confirmShopAction(message) {
	    if (purchasePending) {
	        return false;
	    }

	    purchasePending = true;
	    try {
	        return await showConfirm(message);
	    } finally {
	        purchasePending = false;
	    }
	}
	
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
	async function purchaseShopItem({
	    product,
	    item,
	    purchasedItems,
	    equipped,
	    getMessage,
	    applyItem,
		removeItem,
		getUnequipMessage,
	    getInsufficientMessage = () => '경험치가 부족합니다.',
	    unavailableMessage = '상점은 성장나무 5단계에 도달한 후 이용할 수 있습니다.'
	}) {
	    if (!shopEnabled) {
	        alert(unavailableMessage);
	        return;
	    }

		if (purchasePending) {
		    return;
		}

		if (equipped) {
		    if (typeof removeItem === 'function') {
		        await unequipShopItem({
		            product,
		            item,
		            removeItem,
		            getMessage: getUnequipMessage
		        });
		    }
		    return;
		}

	    const purchasePrice = purchasedItems.has(item)
	        ? 0 : Number(product.dataset.price);

	    if (shopExp < purchasePrice) {
	        alert(getInsufficientMessage(purchasePrice));
	        return;
	    }

	    const itemName = product.querySelector('.tree-shop-item-name')
	        .textContent.trim();
	    const confirmed = await confirmShopAction(
	        getMessage(itemName, purchasePrice)
	    );

	    if (!confirmed) {
	        return;
	    }

	    spentExp += purchasePrice;
	    purchasedItems.add(item);
	    applyItem(item);
	    renderShopState();
	    saveShopState();
	}
	
	// 확인 후 착용 및 배치 상태만 해제하고 저장
	async function unequipShopItem({
	    product,
	    item,
	    removeItem,
	    getMessage = itemName => itemName + ' 착용을 해제하시겠습니까?'
	}) {
	    if (!shopEnabled || purchasePending) {
	        return;
	    }

	    const itemName = product.querySelector('.tree-shop-item-name')
	        .textContent.trim();
	    const confirmed = await confirmShopAction(getMessage(itemName));

	    if (!confirmed) {
	        return;
	    }

	    removeItem(item);
	    renderShopState();
	    saveShopState();
	}
	
	function readShopState() {
	    if (!storageKey) {
	        return null;
	    }

	    try {
	        const saved = JSON.parse(localStorage.getItem(storageKey));
	        const allowedBackgrounds = new Set(
	            Array.from(backgroundProducts, product => product.dataset.background)
	        );

	        if (!saved ||
	            !Number.isSafeInteger(saved.spentExp) ||
	            saved.spentExp < 0 ||
	            !Array.isArray(saved.purchasedBackgrounds) ||
	            !saved.purchasedBackgrounds.every(
	                background => allowedBackgrounds.has(background)
	            ) ||
	            !allowedBackgrounds.has(saved.selectedBackground) ||
	            (saved.selectedBackground !== defaultBackground &&
	                !saved.purchasedBackgrounds.includes(saved.selectedBackground))) {
	            return null;
	        }

			// 열매 데이터 읽기
			const allowedFruits = new Set(
			    Array.from(fruitProducts, product => product.dataset.fruit)
			);

			// 열매 항목이 없는 기존 배경 저장 데이터도 지원
			const backgroundOnly = saved.purchasedFruits === undefined &&
			    saved.selectedFruit === undefined;
			const savedFruits = backgroundOnly ? [] : saved.purchasedFruits;
			const savedFruit = backgroundOnly ? null : saved.selectedFruit;

			if (!Array.isArray(savedFruits) ||
			    !savedFruits.every(fruit => allowedFruits.has(fruit)) ||
			    (savedFruit !== null &&
			        (!allowedFruits.has(savedFruit) || !savedFruits.includes(savedFruit)))) {
			    return null;
			}

			// 기존의 배경, 열매 기록을 유지하면서 모자 저장 데이터 검증
			const withoutHat = saved.purchasedHats === undefined &&
				saved.selectedHat === undefined;
			
			const savedHats = withoutHat ? [] : saved.purchasedHats;
			const savedHat = withoutHat ? null : saved.selectedHat;
			const hat = hatProduct.dataset.hat;
			
			if(!Array.isArray(savedHats) ||
				!savedHats.every(item => item === hat) ||
				(savedHat !== null && (savedHat !== hat || !savedHats.includes(savedHat)))) {
					return null;
				}
			
				// 상점에 등록된 장식 경로만 허용
				const allowedDecorations = new Set(
				    Array.from(decorationProducts, product => product.dataset.decoration)
				);

				// 장식 항목이 없는 기존 저장 데이터는 미구매, 미착용으로 처리
				const withoutDecorations = saved.purchasedDecorations === undefined &&
				    saved.equippedDecorations === undefined;
				const savedDecorations = withoutDecorations ? [] : saved.purchasedDecorations;
				const savedEquippedDecorations = withoutDecorations ? [] : saved.equippedDecorations;

				if (!Array.isArray(savedDecorations) ||
				    !savedDecorations.every(item => allowedDecorations.has(item)) ||
				    !Array.isArray(savedEquippedDecorations) ||
				    !savedEquippedDecorations.every(
				        item => allowedDecorations.has(item) && savedDecorations.includes(item)
				    )) {
				    return null;
				}

				// 상점에 등록된 펫 경로만 허용
				const allowedPets = new Set(
				    Array.from(petProducts, product => product.dataset.pet)
				);

				// 펫 항목이 없는 기존 저장 데이터도 지원
				const withoutPets = saved.purchasedPets === undefined &&
				    saved.equippedPets === undefined;
				const savedPets = withoutPets ? [] : saved.purchasedPets;
				const savedEquippedPets = withoutPets ? [] : saved.equippedPets;

				if (!Array.isArray(savedPets) ||
				    !savedPets.every(pet => allowedPets.has(pet)) ||
				    !Array.isArray(savedEquippedPets) ||
				    !savedEquippedPets.every(
				        pet => allowedPets.has(pet) && savedPets.includes(pet)
				    )) {
				    return null;
				}

				// 기존 항목과 검증된 펫 데이터를 함께 반환
				return {
				    ...saved,
				    purchasedFruits: savedFruits,
				    selectedFruit: savedFruit,
				    purchasedHats: savedHats,
				    selectedHat: savedHat,
				    purchasedDecorations: savedDecorations,
				    equippedDecorations: savedEquippedDecorations,
				    purchasedPets: savedPets,
				    equippedPets: savedEquippedPets
				};
			
	    } catch (error) {
	        return null;
	    }
	}
	
	// 잔액, 배경, 카드 표시
	function renderShopState() {
	    shopExp = Math.max(0, serverShopExp - spentExp);
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
	function saveShopState() {
		if(!storageKey) {
			return;
		}
		
		try {
			localStorage.setItem(storageKey, JSON.stringify({
				spentExp,
				purchasedBackgrounds: Array.from(purchasedBackgrounds),
				selectedBackground,
				purchasedFruits: Array.from(purchasedFruits),
				selectedFruit,
				purchasedHats: Array.from(purchasedHats),
				selectedHat,
				purchasedDecorations: Array.from(purchasedDecorations),
				equippedDecorations: Array.from(equippedDecorations),
				purchasedPets: Array.from(purchasedPets),
				equippedPets: Array.from(equippedPets)
			}));
		} catch(err) {
			console.log(err);
		}
		
	}
	
	// 페이지를 열 때 저장 상태를 복원
	const savedState = readShopState();

	if (savedState) {
	    spentExp = savedState.spentExp;
	    selectedBackground = savedState.selectedBackground;

	    for (const background of savedState.purchasedBackgrounds) {
	        purchasedBackgrounds.add(background);
	    }
		
		// 읽어온 열매 상태를 변수에 복원
		selectedFruit = savedState.selectedFruit;
		
		// 저장된 열매를 나무 및 카드에 표시
		for(const fruit of savedState.purchasedFruits) {
			purchasedFruits.add(fruit);
		}
		
		// 읽어온 모자 구매 기록 & 착용 상태 복원
		selectedHat = savedState.selectedHat;
		
		for(const hat of savedState.purchasedHats) {
			purchasedHats.add(hat);
		}
		
		// 저장된 장식 구매 기록 복원
		for (const decoration of savedState.purchasedDecorations) {
		    purchasedDecorations.add(decoration);
		}

		// 저장된 장식 동시 착용 상태 복원
		for (const decoration of savedState.equippedDecorations) {
		    equippedDecorations.add(decoration);
		}
		
		// 저장된 펫 구매 기록 복원
		for (const pet of savedState.purchasedPets) {
		    purchasedPets.add(pet);
		}

		// 저장된 펫 동시 배치 상태 복원
		for (const pet of savedState.equippedPets) {
		    equippedPets.add(pet);
		}
	}

	renderShopState();

	for (const product of backgroundProducts) {
		product.addEventListener('click', async function () {
			const background = product.dataset.background;

			await purchaseShopItem({
			    product,
			    item: background,
			    purchasedItems: purchasedBackgrounds,
			    equipped: product.classList.contains('is-equipped'),
			    getMessage: (itemName, purchasePrice) => purchasePrice === 0
			        ? itemName + ' 배경을 무료로 적용하시겠습니까?'
			        : itemName + ' 배경을 ' + purchasePrice +
			            ' EXP로 구매하시겠습니까?',
			    getInsufficientMessage: purchasePrice =>
			        '경험치가 부족합니다.\n' +
			        '필요 경험치: ' + purchasePrice + ' EXP\n' +
			        '사용 가능한 경험치: ' + shopExp + ' EXP',
			    applyItem: item => {
			        selectedBackground = item;
			    }
			});
		});
	}
	
	// 열매 클릭 시 구매, 착용, 저장
	for(const product of fruitProducts) {
		product.addEventListener('click', async function() {
			const fruit = product.dataset.fruit;

			await purchaseShopItem({
			    product,
			    item: fruit,
			    purchasedItems: purchasedFruits,
			    equipped: product.classList.contains('is-equipped'),
			    unavailableMessage: '상점은 성장나무가 5단계에 도달한 후 이용할 수 있습니다.',
			    getMessage: (itemName, purchasePrice) => purchasePrice === 0
			        ? itemName + '열매 5알을 무료로 착용하시겠습니까?'
			        : itemName + '열매 5알을' + purchasePrice +
			            ' EXP 로 구매하고 착용하시겠습니까?',
			    applyItem: item => {
			        selectedFruit = item;
			    },
			    removeItem: () => {
			        selectedFruit = null;
			    },
			    getUnequipMessage: itemName =>
			        itemName + ' 열매 5알의 착용을 해제하시겠습니까?'
			});
		});
	}
	
	// 장식 2개 한 세트 구매 & 착용
	for (const product of decorationProducts) {
	    product.addEventListener('click', async function () {
			const decoration = product.dataset.decoration;

			await purchaseShopItem({
			    product,
			    item: decoration,
			    purchasedItems: purchasedDecorations,
			    equipped: equippedDecorations.has(decoration),
			    getMessage: (itemName, purchasePrice) => purchasePrice === 0
			        ? itemName + ' 2개 한 세트를 무료로 착용하시겠습니까?'
			        : itemName + ' 2개 한 세트를 ' + purchasePrice +
			            ' EXP로 구매하고 착용하시겠습니까?',
			    applyItem: item => equippedDecorations.add(item),
			    removeItem: item => equippedDecorations.delete(item),
			    getUnequipMessage: itemName =>
			        itemName + ' 2개 한 세트의 착용을 해제하시겠습니까?'
			});
	    });
	}
	
	// 펫 구매, 배치
	for (const product of petProducts) {
	    product.addEventListener('click', async function () {
		const pet = product.dataset.pet;

			await purchaseShopItem({
			    product,
			    item: pet,
			    purchasedItems: purchasedPets,
			    equipped: equippedPets.has(pet),
			    getMessage: (itemName, purchasePrice) => purchasePrice === 0
			        ? itemName + ' 펫을 무료로 배치하시겠습니까?'
			        : itemName + ' 펫을 ' + purchasePrice +
			            ' EXP로 구매하고 배치하시겠습니까?',
			    applyItem: item => equippedPets.add(item),
			    removeItem: item => equippedPets.delete(item),
			    getUnequipMessage: itemName =>
			        itemName + ' 펫의 배치를 해제하시겠습니까?'
			}); 
	    });
	}
	
	// 모자 클릭 이벤트
	hatProduct.addEventListener('click', async function() {
		const hat = hatProduct.dataset.hat;

		await purchaseShopItem({
		    product: hatProduct,
		    item: hat,
		    purchasedItems: purchasedHats,
		    equipped: hatProduct.classList.contains('is-equipped'),
		    unavailableMessage: '상점은 성장나무 5단계 도달 후 이용하실 수 있습니다.',
		    getMessage: (itemName, purchasePrice) => purchasePrice === 0
		        ? itemName + '를 무료로 착용하시겠습니까?'
		        : itemName + '를' + purchasePrice +
		            ' EXP 로 구매하고 착용하시겠습니까?',
		    applyItem: item => {
		        selectedHat = item;
		    },
		    removeItem: () => {
		        selectedHat = null;
		    },
		    getUnequipMessage: itemName =>
		        itemName + ' 착용을 해제하시겠습니까?'
		});
	})
});
