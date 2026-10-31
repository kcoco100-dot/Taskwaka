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
			
			// 배경, 열매, 모자 정보를 하나의 객체로 묶어서 반환
			return {
				...saved,
				purchasedFruits: savedFruits,
				selectedFruit: savedFruit,
				purchasedHats: savedHats,
				selectedHat: savedHat
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
	        const priceElement = backgroundProduct.querySelector('.tree-shop-price');
	        const originalPrice = Number(backgroundProduct.dataset.price);

	        backgroundProduct.classList.toggle('is-equipped', equipped);
	        priceElement.classList.toggle('is-default', equipped);

	        if (equipped) {
	            priceElement.textContent = '✓ 적용 중';
	        } else if (originalPrice === 0 || purchasedBackgrounds.has(background)) {
	            priceElement.textContent = '적용하기';
	        } else {
	            priceElement.textContent = originalPrice + ' EXP';
	        }
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
		    const priceElement = fruitProduct.querySelector('.tree-shop-price');
		    const originalPrice = Number(fruitProduct.dataset.price);

		    fruitProduct.classList.toggle('is-equipped', equipped);
		    priceElement.classList.toggle('is-default', equipped);

		    if (equipped) {
		        priceElement.textContent = '✓ 착용 중';
		    } else if (purchasedFruits.has(fruit)) {
		        priceElement.textContent = '착용하기';
		    } else {
		        priceElement.textContent = originalPrice + ' EXP';
		    }
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
		const hatPriceElement = hatProduct.querySelector('.tree-shop-price');
		const hatPrice = Number(hatProduct.dataset.price);
		
		hatProduct.classList.toggle('is-equipped', hatEquipped);
		hatPriceElement.classList.toggle('is-default', hatEquipped);
	
		if(hatEquipped) {
			hatPriceElement.textContent = '✓ 착용 중';
		} else if (purchasedHats.has(hat)) {
			hatPriceElement.textContent = '착용하기';
		} else {
			hatPriceElement.textContent = hatPrice + 'EXP';
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
				selectedHat
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
	}

	renderShopState();

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

			// 구매, 적용 확정 후 상태와 화면 갱신
			spentExp += purchasePrice;
			purchasedBackgrounds.add(background);
			selectedBackground = background;
			renderShopState();
			
			// 배경 구매 처리도 새 저장 함수를 사용함
			saveShopState();
		});
	}
	
	// 열매 클릭 시 구매, 착용, 저장
	for(const product of fruitProducts) {
		product.addEventListener('click', async function() {
			if(!shopEnabled) {
				alert('상점은 성장나무가 5단계에 도달한 후 이용할 수 있습니다.');
				return;
			}
			// 이미 착용중인 상품이거나, 확인창의 응답을 기다리는 중이면 클릭 처리 종료
			if(product.classList.contains('is-equipped') || purchasePending) {
				return;
			}
			
			const fruit = product.dataset.fruit;
			const itemName = product.querySelector('.tree-shop-item-name')
				.textContent.trim();
				
			const purchasePrice = purchasedFruits.has(fruit)
				? 0 : Number(product.dataset.price);
				
			if(shopExp < purchasePrice) {
				alert('경험치가 부족합니다.');
				return;
			}
			
			const message = purchasePrice === 0 
				? itemName + '열매 5알을 무료로 착용하시겠습니까?'
				: itemName + '열매 5알을' + purchasePrice + ' EXP 로 구매하고 착용하시겠습니까?';
				
			let confirmed;
			purchasePending = true;
			
			try {
				confirmed = await showConfirm(message);
			} finally {
				purchasePending = false;
			}
			
			if(!confirmed) {
				return;
			}
			
			spentExp += purchasePrice;
			purchasedFruits.add(fruit);
			selectedFruit = fruit;
			renderShopState();
			saveShopState();
		});
	}
	
	// 모자 클릭 이벤트
	hatProduct.addEventListener('click', async function() {
		if(!shopEnabled) {
			alert('상점은 성장나무 5단계 도달 후 이용하실 수 있습니다.');
			return;
		}
		
		// is-equipped 가 있으면 이미 착용중이므로 종료
		// purchasePending 가 true 면 확인창 응답을 기다리는 중이므로 종료
		if(hatProduct.classList.contains('is-equipped') || purchasePending) {
			return;
		}
		
		// 모자 카드의 착용 상태와 가격 표시
		const hat = hatProduct.dataset.hat;
		const itemName = hatProduct.querySelector('.tree-shop-item-name')
			.textContent.trim();
		const purchasePrice = purchasedHats.has(hat) 
			? 0 : Number(hatProduct.dataset.price);
			
		if(shopExp < purchasePrice) {
			alert('경험치가 부족합니다.');
			return;
		}
		const message = purchasePrice === 0 
			? itemName + '를 무료로 착용하시겠습니까?'
			: itemName + '를' + purchasePrice + ' EXP 로 구매하고 착용하시겠습니까?';
			
		let confirmed;
		purchasePending = true;

		try {
			confirmed = await showConfirm(message);
		} finally {
			purchasePending = false;
		}

		if(!confirmed) {
			return;
		}

		spentExp += purchasePrice;
		purchasedHats.add(hat);
		selectedHat = hat;
		renderShopState();
		saveShopState();
		
	})
});
