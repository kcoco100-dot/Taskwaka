package com.tk.app.service;

import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTaskList(Long projectId) {
        List<TaskDto> taskList = taskMapper.selectTaskListByProject(projectId);
        for (TaskDto task : taskList) {
            task.setSubTasks(taskMapper.selectSubTaskListByTask(task.getTaskId()));
        }
        return taskList;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskDetail(Long taskId) {
        TaskDto task = taskMapper.selectTaskById(taskId);
        if (task != null) {
            task.setSubTasks(taskMapper.selectSubTaskListByTask(taskId));
        }
        return task;
    }

    @Override
    @Transactional
    public TaskDto createMainTask(TaskDto dto) {
        if (dto.getContent() == null) dto.setContent("");
        if (dto.getStatus() == null) dto.setStatus(0);
        if (dto.getProgress() == null) dto.setProgress(0.0);
        taskMapper.insertTask(dto);
        return taskMapper.selectTaskById(dto.getTaskId());
    }

    @Override
    @Transactional
    public void updateTaskField(Long taskId, String fieldName, Object fieldValue) {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", taskId);
        map.put("fieldName", fieldName);
        map.put("fieldValue", fieldValue);
        taskMapper.updateTaskField(map);
    }

    @Override
    @Transactional
    public void updateTaskDetail(TaskDto dto) {
        taskMapper.updateTaskDetail(dto);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        taskMapper.deleteSubTasksByTaskId(taskId);
        taskMapper.deleteTask(taskId);
    }

    @Override
    @Transactional
    public SubTaskDto createSubTask(SubTaskDto dto) {
        if (dto.getContent() == null) dto.setContent("");
        if (dto.getStatus() == null) dto.setStatus(0);
        if (dto.getProgress() == null) dto.setProgress(0.0);
        taskMapper.insertSubTask(dto);
        return dto;
    }

    @Override
    @Transactional
    public void updateSubTaskField(Long subTaskId, String fieldName, Object fieldValue) {
        Map<String, Object> map = new HashMap<>();
        map.put("subTaskId", subTaskId);
        map.put("fieldName", fieldName);
        map.put("fieldValue", fieldValue);
        taskMapper.updateSubTaskField(map);
    }

    @Override
    @Transactional
    public void deleteSubTask(Long subTaskId) {
        taskMapper.deleteSubTask(subTaskId);
    }
}
