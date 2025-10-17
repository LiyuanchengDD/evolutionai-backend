package com.example.grpcdemo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grpcdemo.gateway.HealthGatewayService;
import com.example.grpcdemo.web.dto.HealthStatusResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthGatewayService healthGatewayService;

    public HealthController(HealthGatewayService healthGatewayService) {
        this.healthGatewayService = healthGatewayService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthStatusResponse> health() {
        var response = healthGatewayService.check("");
        if (response.ok()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }
}
