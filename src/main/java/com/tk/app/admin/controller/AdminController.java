package com.tk.app.admin.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tk.app.admin.domain.dto.AdminMemberDto;
import com.tk.app.admin.domain.dto.AdminMonthlyRevenueDto;
import com.tk.app.admin.service.AdminMemberService;
import com.tk.app.admin.service.AdminStatsService;
import com.tk.app.common.PaginateUtil;
import com.tk.app.service.LoginHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin")
public class AdminController {
	private final AdminMemberService service;
	private final PaginateUtil paginateUtil;
	private final LoginHistoryService loginHistoryService;
	private final AdminStatsService adminStatsService;

    @GetMapping
    public String adminMain() {
        return "redirect:/admin/user";
    }

    @GetMapping("/user")
    public String user(Model model,
    		@RequestParam (name="keyword", required = false) String keyword,
    		@RequestParam (name="status", required = false) Integer status,
    		@RequestParam (name="page", defaultValue = "1") int current_page) {
    	
    	if(keyword == null) {
    		keyword = "";
    	}
    	
    	keyword = keyword.trim();
    	
    	int size = 7;
    	
    	Map<String, Object> map = new HashMap<>();
    	map.put("keyword", keyword);
    	map.put("status", status);
    	
    	int dataCount = service.dataCountMembers(map);
    	
    	Map<String, Object> countMap = new HashMap<>();

    	countMap.put("keyword", "");
    	countMap.put("status", null);
    	int totalMembers = service.dataCountMembers(countMap);

    	countMap.put("status", 0);
    	int activeMembers = service.dataCountMembers(countMap);

    	countMap.put("status", 1);
    	int inactiveMembers = service.dataCountMembers(countMap);
    	
    	int totalPage = paginateUtil.pageCount(dataCount, size);
    	
    	current_page = Math.max(1, current_page);
    	if(totalPage > 0) {
    		current_page = Math.min(current_page, totalPage);
    	}
    	
    	int offset = (current_page - 1) * size;

    	map.put("offset", offset);
    	map.put("size", size);

    	List<AdminMemberDto> members =
    	        service.listMembersByPage(map);

    	for (AdminMemberDto member : members) {
    	    member.setWorkspaces(
    	        service.findMemberWorkspaces(member.getMember_id())
    	    );
    	}
    	
    	String listUrl = "/admin/user?keyword="
    	        + URLEncoder.encode(keyword, StandardCharsets.UTF_8);

    	if (status != null) {
    	    listUrl += "&status=" + status;
    	}

    	String paging = paginateUtil.paging(current_page, totalPage, listUrl);

    	model.addAttribute("members", members);
    	model.addAttribute("paging", paging);
    	model.addAttribute("dataCount", dataCount);
    	model.addAttribute("totalPage", totalPage);
    	model.addAttribute("page", current_page);
    	model.addAttribute("keyword", keyword);
    	model.addAttribute("status", status);
    	model.addAttribute("totalMembers", totalMembers);
    	model.addAttribute("activeMembers", activeMembers);
    	model.addAttribute("inactiveMembers", inactiveMembers);
    	
    return adminPage("user", model);
        
    }
    
    @GetMapping("/user/{member_id}")
    public String userDetail(@PathVariable long member_id,
    		Model model) {
    	  System.out.println("회원 상세 컨트롤러 실행: " + member_id);
    	
    	model.addAttribute("workspaces", service.findMemberWorkspaces(member_id));
    	model.addAttribute("member", service.findMemberById(member_id));
    	
    	return adminPage("userDetail", model);
    }

    
    @PostMapping("/user/status")
    public String updateMemberStatus(
    		@RequestParam ("member_id") long member_id,
    		@RequestParam ("status") int status) {
    	
    	if(status != 0 && status != 1) {
    		return "redirect:/admin/user";
    	}
    	
    	service.updateMemberStatus(member_id, status);
    	
    	return "redirect:/admin/user";
    }
    
    @GetMapping("/domain")
    public String domain(Model model) {
        return adminPage("domain", model);
    }

//    @GetMapping("/notice")
//    public String notice(Model model) {
//        return adminPage("notice", model);
//    }

//    @GetMapping("/inquiry")
//    public String inquiry(Model model) {
//    	model.addAttribute("activeSection", "inquiry");
//        model.addAttribute("adminName", "관리자 (ADMIN)");
//
//        return "admin/admin_layout";
//    }

    @GetMapping("/stats")
    public String stats(Model model,
    		@RequestParam(value = "period", defaultValue = "all") String period) {
    	try {
    		// 접속자 통계
    		int loginCount = loginHistoryService.countLoginHistory();
    		int memberCount = loginHistoryService.countDistinctMembers();
    		int dailyLoginCount = loginHistoryService.countDailyLoginHistory();
    		int dailyMemberCount = loginHistoryService.countDailyDistinctMembers();
    		int weekLoginCount = loginHistoryService.countWeekLogin();
    		int weekMemberCount	= loginHistoryService.countWeekMember();
    		int monthLoginCount = loginHistoryService.countMonthLogin();
    		int monthMemberCount = loginHistoryService.countMonthMember();
    		int yearLoginCount = loginHistoryService.countYearLogin();
    		int yearMemberCount = loginHistoryService.countYearMember();
    		// 수익 통계
    		int paymentCount = adminStatsService.countPayment();
    		long paymentAmount = adminStatsService.sumPayment();
    		long refundAmount = adminStatsService.sumRefund();
    		long netAmount = paymentAmount - refundAmount;
    		int monthPaymentCount = adminStatsService.countMonthPayment();
    		long monthPaymentAmount = adminStatsService.sumMonthPayment();
    		long monthRefundAmount =  adminStatsService.sumMonthRefund();
    		long monthNetAmount = monthPaymentAmount - monthRefundAmount;
    		int quarterPaymentCount = adminStatsService.countQuarterPayment();
    		long quarterPaymentAmount = adminStatsService.sumQuarterPayment();
    		long quarterRefundAmount = adminStatsService.sumQuarterRefund();
    		long quarterNetAmount = quarterPaymentAmount - quarterRefundAmount;
    		
    		List<AdminMonthlyRevenueDto> monthlyRevenueList = adminStatsService.listMonthlyRevenue();
    		System.out.println("월별 매출 데이터 개수 = " + monthlyRevenueList.size());
    		
    		// 접속자 통계
    		LocalDate today = LocalDate.now();
    		LocalDate weekStart = today.with(DayOfWeek.MONDAY);
    		LocalDate weekEnd = weekStart.plusDays(6);
    		String weekPeriod = weekStart + " ~ " + weekEnd;
    		LocalDate monthStart = today.withDayOfMonth(1);
    		LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
    		String monthPeriod = monthStart + " ~ " + monthEnd;
    		LocalDate yearStart = today.withDayOfYear(1);
    		LocalDate yearEnd = today.withDayOfYear(today.lengthOfYear());
    		String yearPeriod = yearStart + " ~ " + yearEnd;
    		
    		System.out.println("선택한 기간 = " + period);
    		
    		int quarter = (today.getMonthValue() - 1) / 3 + 1;
    		String quarterPeriod = today.getYear() + "년 " + quarter + "분기 (당분기 실적)";

    		if ("daily".equals(period)) {
    		    loginCount = dailyLoginCount;
    		    memberCount = dailyMemberCount;
    		} else if ("weekly".equals(period)) {
    			loginCount = weekLoginCount;
    		    memberCount = weekMemberCount;
    		} else if ("monthly".equals(period)) {
    			loginCount = monthLoginCount;
    		    memberCount = monthMemberCount;
    		} else if ("yearly".equals(period)) {
    		    loginCount = yearLoginCount;
    		    memberCount = yearMemberCount;
    		}
    		
    		// 접속자 통계
    		model.addAttribute("loginCount", loginCount);
    		model.addAttribute("memberCount", memberCount);
    		model.addAttribute("dailyLoginCount", dailyLoginCount);
    		model.addAttribute("dailyMemberCount", dailyMemberCount);
    		model.addAttribute("weekLoginCount", weekLoginCount);
    		model.addAttribute("weekMemberCount", weekMemberCount);
    		model.addAttribute("monthLoginCount", monthLoginCount);
    		model.addAttribute("monthMemberCount", monthMemberCount);
    		model.addAttribute("yearLoginCount", yearLoginCount);
    		model.addAttribute("yearMemberCount", yearMemberCount);
    		
    		
    		model.addAttribute("today", today);
    		model.addAttribute("period", period);
    		model.addAttribute("weekPeriod", weekPeriod);
    		model.addAttribute("monthPeriod", monthPeriod);
    		model.addAttribute("yearPeriod", yearPeriod);
    		
    		//수익 통계
    		model.addAttribute("paymentCount", paymentCount);
    		model.addAttribute("paymentAmount", paymentAmount);
    		model.addAttribute("refundAmount", refundAmount);
    		model.addAttribute("netAmount", netAmount);
    		model.addAttribute("monthPaymentCount", monthPaymentCount);
    		model.addAttribute("monthPaymentAmount", monthPaymentAmount);
    		model.addAttribute("monthRefundAmount", monthRefundAmount);
    		model.addAttribute("monthNetAmount", monthNetAmount);
    		model.addAttribute("quarterPaymentCount", quarterPaymentCount);
    		model.addAttribute("quarterPaymentAmount", quarterPaymentAmount);
    		model.addAttribute("quarterRefundAmount", quarterRefundAmount);
    		model.addAttribute("quarterNetAmount", quarterNetAmount);
    		model.addAttribute("quarterPeriod", quarterPeriod);
    		model.addAttribute("monthlyRevenueList", monthlyRevenueList);
    		
    		
		} catch (Exception e) {
			log.error("stats : ", e);
			throw e;
		}
        return adminPage("stats", model);
    }

    @GetMapping("/security")
    public String security(Model model) {
        return adminPage("security", model);
    }

    private String adminPage(String section, Model model) {

        model.addAttribute("activeSection", section);
        model.addAttribute("adminName", "관리자 (ADMIN)");

        return "admin/admin_layout";
    }
}