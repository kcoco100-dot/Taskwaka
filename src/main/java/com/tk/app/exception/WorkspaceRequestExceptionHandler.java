package com.tk.app.exception;

import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.tk.app.controller.HomeGrowthTreeController;
import com.tk.app.controller.InviteAcceptanceController;
import com.tk.app.controller.ProjectController;
import com.tk.app.controller.TaskApiController;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {ProjectController.class, TaskApiController.class,
        InviteAcceptanceController.class, HomeGrowthTreeController.class})
public class WorkspaceRequestExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> rejected(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "요청을 처리할 수 없습니다." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("success",false,"message",message));
    }

    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    public ResponseEntity<Map<String,Object>> failed(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of("success",false,"message","요청을 저장하거나 조회하지 못했습니다."));
    }
}
