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
	// shop 안에서 요소를 찾아 변수에 저장
	const selectPrice = shop.querySelector('#shopSelectedPrice');
	// shop 안의 모든 요소 찾아 for~of 문으로 하나씩 꺼내기
	for(const products of shop.querySelectorAll('.tree-shop-product')) {
		products.addEventListener('click', function () {
			const price = products.querySelector('.tree-shop-price');
			
			if(selectPrice !== null && price !== null) {
				selectPrice.textContent = price.textContent.trim();
			}
		});
	}
});
