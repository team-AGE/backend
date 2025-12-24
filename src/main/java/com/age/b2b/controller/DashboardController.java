package com.age.b2b.controller;

import com.age.b2b.dto.dashboard.DashboardRes;
import com.age.b2b.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    // 대시보드 한 번에 내려주기(추천)
    @GetMapping
    public DashboardRes dashboard() {
        return dashboardService.getDashboard();
    }
}
