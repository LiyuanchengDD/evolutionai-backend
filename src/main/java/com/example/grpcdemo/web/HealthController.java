package com.example.grpcdemo.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grpcdemo.gateway.HealthGatewayService;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthGatewayService healthGatewayService;

    public HealthController(HealthGatewayService healthGatewayService) {
        this.healthGatewayService = healthGatewayService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        // 仍然触发一次 gRPC 通道检查，便于及时发现后台通信异常
        healthGatewayService.check("");
        return Map.of("ok", true);
    }
}
