package com.example.grpcdemo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.grpcdemo.gateway.HealthGatewayService;
import com.example.grpcdemo.web.dto.HealthStatusResponse;

@RestController
public class HealthController {

    private final HealthGatewayService healthGatewayService;

    public HealthController(HealthGatewayService healthGatewayService) {
        this.healthGatewayService = healthGatewayService;
    }

    @GetMapping("/api/health")
    public HealthStatusResponse health(@RequestParam(value = "service", required = false) String service) {
        return healthGatewayService.check(service);
    }
}
