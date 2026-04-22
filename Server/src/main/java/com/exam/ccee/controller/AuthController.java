package com.exam.ccee.controller;

import com.exam.ccee.entity.User;
import com.exam.ccee.jwt.JwtUtil;
import com.exam.ccee.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthService service;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> req) {
        return service.register(req.get("username"), req.get("password"));
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> req) {

        User user = service.login(req.get("username"), req.get("password"));

        String token = jwtUtil.generateToken(user.getUsername());

        return Map.of("token", token);
    }
}