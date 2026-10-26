async function submitCreateReportModal() {
  const taskId = createSubModalContext.parentTaskId;
  if (!taskId) return;

  const titleInput = document.getElementById('createTaskTitleInput');
  const contentInput = document.getElementById('createTaskContentInput');
  const assigneeSelect = document.getElementById('createAssigneeSelect');
  const startDateInput = document.getElementById('createStartDateInput');
  const endDateInput = document.getElementById('createEndDateInput');

  const title = titleInput.value.trim();
  const content = contentInput.value.trim();
  const projectMemberId = assigneeSelect.value;
  const startAt = startDateInput.value;
  const endAt = endDateInput.value;

  if (!title) { alert('하위업무 제목을 입력해 주세요.'); titleInput.focus(); return; }
  if (!projectMemberId) { alert('하위업무 담당자를 선택해 주세요.'); assigneeSelect.focus(); return; }
  if (!content) { alert('상세 내용을 입력해 주세요.'); contentInput.focus(); return; }

  const formData = new URLSearchParams();
  formData.append('title', title);
  formData.append('content', content);
  formData.append('projectMemberId', projectMemberId);
  formData.append('status', createSubModalContext.statusCode);
  if (startAt) formData.append('startAt', startAt);
  if (endAt) formData.append('endAt', endAt);

  try { 
    const res = await fetch(`/${currentDomainKey}/project/${currentProjectId}/tasks/${taskId}/reports`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: formData
    });

    if (!res.ok) throw new Error('하위 업무 생성 요청 실패');

    const htmlFragment = await res.text();
    const branchLi = document.querySelector(`[data-task-id="${taskId}"]`);
    if (branchLi) {
      const subTaskList = branchLi.querySelector('.sub-task-list');
      subTaskList.insertAdjacentHTML('beforeend', htmlFragment);

      const countSpan = branchLi.querySelector('.sub-count');
      if (countSpan) {
        countSpan.textContent = parseInt(countSpan.textContent || '0') + 1;
      } else {
        const titleSpan = branchLi.querySelector('.task-title-text');
        titleSpan.insertAdjacentHTML('afterend', '<span style="font-size:12px; color:var(--text-muted);">(<span class="sub-count">1</span>)</span>');
        const colName = branchLi.querySelector('.col-task-name');
        if (!colName.querySelector('.task-toggle')) {
          colName.firstElementChild.outerHTML = '<span class="btn-toggle-box task-toggle" onclick="event.stopPropagation(); toggleSubTasks(this)">－</span>';
        }
      }
    }
    closeCreateTaskModal();
  } catch (err) {
    console.error('하위업무 등록 오류:', err);
    alert('하위업무 등록 중 오류가 발생했습니다.');
  }
}