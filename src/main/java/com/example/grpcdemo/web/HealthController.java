package com.example.grpcdemo.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> health() {
        var response = healthGatewayService.check("");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", response.ok());
        if (response.statusText() != null) {
            body.put("status", response.statusText());
        }
        if (response.channelReady() != null) {
            body.put("channelReady", response.channelReady());
        }
        if (response.error() != null) {
            body.put("error", response.error());
        }
        HttpStatus status = response.ok() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(body);
    }
}
