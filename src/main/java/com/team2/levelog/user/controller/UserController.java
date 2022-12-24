package com.team2.levelog.user.controller;

import com.team2.levelog.global.security.UserDetailsImpl;
import com.team2.levelog.user.dto.DupRequestCheck;
import com.team2.levelog.user.dto.SigninRequestDto;
import com.team2.levelog.user.dto.SignUpRequestDto;
import com.team2.levelog.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

// 1. 기능   : 회원 관련 종합 컨트롤러
// 2. 작성자 : 서혁수
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {
    private final UserService userService;

    @PostMapping("/signUp")
    public ResponseEntity<?> signUp(@RequestBody @Valid SignUpRequestDto signUpRequestDto) {
        userService.signUp(signUpRequestDto);
        return null;
    }

    @PostMapping("/signIn")
    public ResponseEntity<?> login(@RequestBody SigninRequestDto signinRequestDto, HttpServletResponse response) {
        userService.login(signinRequestDto, response);
        return null;
    }

    @PostMapping("/dupEmail")
    public ResponseEntity<?> dupEmailCheck(@RequestBody DupRequestCheck requestCheck) {
        return userService.dupCheckEmail(requestCheck);
    }

    @PostMapping("/dupNick")
    public ResponseEntity<?> dupNickCheck(@RequestBody DupRequestCheck requestCheck) {
        return userService.dupCheckNick(requestCheck);
    }

}
