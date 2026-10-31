(() => {
    'use strict';

    document.addEventListener('DOMContentLoaded', async () => {
        const ticker = document.getElementById('twSiteNoticeTicker');
        if (!ticker) return;

        const current = document.getElementById('twSiteNoticeCurrent');
        const expand = document.getElementById('twSiteNoticeExpand');
        const dropdown = document.getElementById('twSiteNoticeDropdown');
        const items = document.getElementById('twSiteNoticeItems');

        let notices;
        try {
            const response = await fetch(ticker.dataset.listUrl, {
                credentials: 'same-origin',
                headers: { Accept: 'application/json' }
            });
            if (!response.ok) return;
            const result = await response.json();
            if (!Array.isArray(result)) return;
            notices = result.filter(n => Number.isSafeInteger(Number(n.noticeId)) && Number(n.noticeId) > 0 && typeof n.title === 'string');
        } catch (error) {
            return;
        }
        if (!notices.length) return;

        const detailLink = notice => ticker.dataset.detailBase + encodeURIComponent(String(notice.noticeId));
        let index = 0;
        const showCurrent = () => {
            const notice = notices[index];
            current.textContent = 'NEW · ' + notice.title;
            current.href = detailLink(notice);
            current.title = notice.title;
        };
        const setExpanded = expanded => {
            dropdown.hidden = !expanded;
            expand.setAttribute('aria-expanded', String(expanded));
            expand.setAttribute('aria-label', expanded ? '새 공지사항 목록 닫기' : '새 공지사항 목록 열기');
        };

        notices.forEach(notice => {
            const li = document.createElement('li');
            const link = document.createElement('a');
            const tag = document.createElement('span');
            const title = document.createElement('span');
            link.href = detailLink(notice);
            tag.className = 'tw-site-notice-new';
            tag.textContent = 'NEW';
            title.className = 'tw-site-notice-title';
            title.textContent = notice.title;
            link.append(tag, title);
            li.appendChild(link);
            items.appendChild(li);
        });

        showCurrent();
        ticker.hidden = false;
        ticker.addEventListener('mouseenter', () => setExpanded(true));
        ticker.addEventListener('mouseleave', () => setExpanded(false));
        ticker.addEventListener('focusout', event => {
            if (!ticker.contains(event.relatedTarget)) setExpanded(false);
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape' && !dropdown.hidden) {
                setExpanded(false);
                expand.focus();
            }
        });
        expand.addEventListener('click', () => setExpanded(dropdown.hidden));

        if (notices.length > 1) {
            window.setInterval(() => {
                if (!dropdown.hidden || document.hidden) return;
                index = (index + 1) % notices.length;
                showCurrent();
            }, 4000);
        }
    });
})();
