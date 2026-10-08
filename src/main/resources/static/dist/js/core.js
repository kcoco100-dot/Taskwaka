(function() {
  /**
   * 페이지를 아래로 스크롤할 때 body에 scrolled 클래스를 추가
   */
  function toggleScrolled() {
    const selectBody = document.querySelector('body');
    const selectHeader = document.querySelector('#header');
	// header 가 없으면 함수 실행을 여기서 끝냄
	if (!selectHeader) return;
	
    if (!selectHeader.classList.contains('scroll-up-sticky') && !selectHeader.classList.contains('sticky-top') && !selectHeader.classList.contains('fixed-top')) return;
    window.scrollY > 100 ? selectBody.classList.add('scrolled') : selectBody.classList.remove('scrolled');
  }

  document.addEventListener('scroll', toggleScrolled);
  window.addEventListener('load', toggleScrolled);

  /**
   * Mobile nav toggle
   */
  const mobileNavToggleBtn = document.querySelector('.mobile-nav-toggle');

  function mobileNavToogle() {
    document.querySelector('body').classList.toggle('mobile-nav-active');
    mobileNavToggleBtn.classList.toggle('bi-list');
    mobileNavToggleBtn.classList.toggle('bi-x');
  }
  // 모바일 버튼이 있을때만 실행함
  if (mobileNavToggleBtn) {
      mobileNavToggleBtn.addEventListener('click', mobileNavToogle);
  }

  /**
   * Hide mobile nav on same-page/hash links
   */
  document.querySelectorAll('#navmenu a').forEach(navmenu => {
    navmenu.addEventListener('click', () => {
      if (document.querySelector('.mobile-nav-active')) {
        mobileNavToogle();
      }
    });
  });

  /**
   * Toggle mobile nav dropdowns
   */
  document.querySelectorAll('.navmenu .toggle-dropdown').forEach(navmenu => {
    navmenu.addEventListener('click', function(e) {
      e.preventDefault();
      this.parentNode.classList.toggle('active');
      this.parentNode.nextElementSibling.classList.toggle('dropdown-active');
      e.stopImmediatePropagation();
    });
  });

  /**
   * Preloader : 로딩중일 때 출력 
   */
  const preloader = document.querySelector('#preloader');
  /*
  if (preloader) {
    window.addEventListener('load', () => {
      preloader.remove();
    });
  }
  */
  const isHidden = el => {
  	 const styles = window.getComputedStyle(el);
  	 return styles.display === 'none' || styles.visibility === 'hidden';
  };  
  if (! isHidden(preloader)) {
    window.addEventListener('load', () => {
      preloader.style.display = 'none';
    });
  }
  
  /**
   * Scroll top button
   */
  let scrollTop = document.querySelector('.scroll-top');
  if(scrollTop) {
	  function toggleScrollTop() {
	    if (scrollTop) {
	      window.scrollY > 100 ? scrollTop.classList.add('active') : scrollTop.classList.remove('active');
	    }
	  }
	  scrollTop.addEventListener('click', (e) => {
	    e.preventDefault();
	    window.scrollTo({
	      top: 0,
	      behavior: 'smooth'
	    });
	  });
	
	  window.addEventListener('load', toggleScrollTop);
	  document.addEventListener('scroll', toggleScrollTop);
  }
  
  /**
   * Animation on scroll function and init : AOS 초기화
   */
  function aosInit() {
    AOS.init({
      duration: 600,
      easing: 'ease-in-out',
      once: true,
      mirror: false
    });
  }
  window.addEventListener('load', aosInit);
})();

// 메뉴 active
document.addEventListener("DOMContentLoaded", function () {
	const currentPath = window.location.pathname;
	const allLinks = document.querySelectorAll("#navmenu a");

    function getPathCandidates(path) {
        const parts = path.split("/");
        const candidates = [];
        for (let i = parts.length; i > 1; i--) {
            const subPath = parts.slice(0, i).join("/");
            candidates.push(subPath);
        }
        
        return candidates;
    }
    
    const pathCandidates = getPathCandidates(currentPath);

    let matchedLink = null;
    for (let i = 0; i < pathCandidates.length; i++) {
    	
    	for (let j = 0; j < allLinks.length; j++) {
    		let linkPath = allLinks[j].getAttribute('href');
    		if(i != 0) {
    			let linkCandidates = getPathCandidates(allLinks[j].getAttribute('href'));
    			if(linkCandidates[i]) {
    				linkPath = linkCandidates[i];
    			}
    		}
    		
    		if(linkPath === '#') continue;

    		// if (pathCandidates[i] === linkPath) {
    		if (linkPath.startsWith(pathCandidates[i])) {
    			matchedLink = allLinks[j];
                break;
    		}
    	}
    	
    	if (matchedLink) break;
    }
    
    // 가장 가까운 링크에 active 클래스 적용
    if (matchedLink) {
        const  li = matchedLink.closest("li");
		let parentMenu = matchedLink.closest("li.dropdown");
		
		if(parentMenu && parentMenu.closest("li.dropdown")) {
			parentMenu = parentMenu.closest("li.dropdown");
		}
        
        if (parentMenu) {
        	parentMenu.querySelector('a').classList.add("active");
        } else {
        	 li.querySelector('a').classList.add("active");
        }
    }
});
