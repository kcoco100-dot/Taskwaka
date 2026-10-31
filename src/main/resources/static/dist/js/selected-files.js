// 선택된 파일을 텍스트로 표시하고 제출할 FileList와 함께 갱신한다.
document.querySelectorAll('[data-file-input]').forEach(list => {
    const input = document.getElementById(list.dataset.fileInput);
    function render() {
        const files = Array.from(input.files);
        list.replaceChildren();
        files.forEach((file, index) => {
            const row = document.createElement('div');
            row.className = list.dataset.rowClass;
            const name = document.createElement('span');
            name.className = list.dataset.nameClass;
            name.textContent = file.name;
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = list.dataset.removeClass;
            remove.textContent = '삭제';
            remove.addEventListener('click', () => {
                const remaining = new DataTransfer();
                files.forEach((item, i) => {
                    if (i !== index) remaining.items.add(item);
                });
                input.files = remaining.files;
                render();
            });
            row.append(name, remove);
            list.appendChild(row);
        });
    }
    input.addEventListener('change', render);
    render();
});
