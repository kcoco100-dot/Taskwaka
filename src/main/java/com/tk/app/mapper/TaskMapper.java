package com.tk.app.mapper;

import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper {
    // 1. 목록 조회
    List<TaskDto> selectTaskListByProject(@Param("projectId") Long projectId);
    List<SubTaskDto> selectSubTaskListByTask(@Param("taskId") Long taskId);

    // 2. 단건 조회 (상세창용)
    TaskDto selectTaskById(@Param("taskId") Long taskId);

    // 3. 메인 업무 CRUD
    int insertTask(TaskDto dto);
    int updateTaskField(Map<String, Object> paramMap);
    int updateTaskDetail(TaskDto dto);
    int deleteTask(@Param("taskId") Long taskId);

    // 4. 하위 업무 CRUD
    int insertSubTask(SubTaskDto dto);
    int updateSubTaskField(Map<String, Object> paramMap);
    int deleteSubTask(@Param("subTaskId") Long subTaskId);
    int deleteSubTasksByTaskId(@Param("taskId") Long taskId);
}
