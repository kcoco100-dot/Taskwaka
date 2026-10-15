package com.tk.app.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.tk.app.domain.dto.DailyReportDto;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.domain.dto.TaskMemberDto;

@Mapper
public interface TaskMapper {

    // 1. 메인 업무 및 하위업무 조회
    List<TaskDto> selectTasksByProjectId(@Param("projectId") Long projectId);
    TaskDto selectTaskById(@Param("taskId") Long taskId);
    List<SubTaskDto> selectSubTasksByTaskId(@Param("taskId") Long taskId);
    SubTaskDto selectSubTaskById(@Param("subTaskId") Long subTaskId);

    // 2. 메인 업무 수정
    int updateTaskDetail(TaskDto dto);
    int updateTaskField(@Param("taskId") Long taskId, @Param("fieldName") String fieldName, @Param("fieldValue") Object fieldValue);

    // 3. 하위 업무 CRUD
    int insertSubTask(SubTaskDto dto);
    int updateSubTaskDetail(SubTaskDto dto);
    int updateSubTaskField(@Param("subTaskId") Long subTaskId, @Param("fieldName") String fieldName, @Param("fieldValue") Object fieldValue);
    int softDeleteSubTask(@Param("subTaskId") Long subTaskId);

    // 4. 업무 참여 인원(task_members) & 하위업무 참여 인원(sub_task_members)
    List<TaskMemberDto> selectTaskMembers(@Param("taskId") Long taskId);
    List<TaskMemberDto> selectSubTaskMembers(@Param("subTaskId") Long subTaskId);
    List<TaskMemberDto> selectProjectMembers(@Param("projectId") Long projectId);
    int insertTaskMember(@Param("taskId") Long taskId, @Param("projectMemberId") Long projectMemberId);
    int insertSubTaskMember(@Param("subTaskId") Long subTaskId, @Param("taskId") Long taskId, @Param("projectMemberId") Long projectMemberId);

    // 5. 일일 보고서(daily_reports) CRUD
    List<DailyReportDto> selectDailyReportsBySubTaskId(@Param("subTaskId") Long subTaskId);
    DailyReportDto selectDailyReportById(@Param("dailyReportId") Long dailyReportId);
    int insertDailyReport(DailyReportDto dto);
    int updateDailyReport(DailyReportDto dto);
    int softDeleteDailyReport(@Param("dailyReportId") Long dailyReportId);
}