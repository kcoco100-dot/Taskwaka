package com.tk.app.service;

import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;

import java.util.List;

public interface TaskService {
    List<TaskDto> getTaskList(Long projectId);
    TaskDto getTaskDetail(Long taskId);
    
    TaskDto createMainTask(TaskDto dto);
    void updateTaskField(Long taskId, String fieldName, Object fieldValue);
    void updateTaskDetail(TaskDto dto);
    void deleteTask(Long taskId);

    SubTaskDto createSubTask(SubTaskDto dto);
    void updateSubTaskField(Long subTaskId, String fieldName, Object fieldValue);
    void deleteSubTask(Long subTaskId);
}
