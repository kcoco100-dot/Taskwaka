package com.tk.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/error/*")
public class ErrorPageController {
	@GetMapping("downloadFailed")
	public String handleDownloadFailed() {
		return "error/downloadFailure";
	}

	@GetMapping("noAuthorized")
	public String handleError403() {
		// 권한이 없는 경우(403)
		return "error/noAuthorized";
	}
}
