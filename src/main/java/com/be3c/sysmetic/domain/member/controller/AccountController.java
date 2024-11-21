package com.be3c.sysmetic.domain.member.controller;

import com.be3c.sysmetic.domain.member.dto.FindEmailRequestDto;
import com.be3c.sysmetic.domain.member.dto.ResetPasswordRequestDto;
import com.be3c.sysmetic.domain.member.service.AccountService;
import com.be3c.sysmetic.domain.member.service.RegisterService;
import com.be3c.sysmetic.global.common.response.APIResponse;
import com.be3c.sysmetic.global.common.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final RegisterService registerService;

    // 1. 이메일 찾기 API
    @PostMapping("/auth/find-email")
    public ResponseEntity<APIResponse<String>> findEmail(@Valid @RequestBody FindEmailRequestDto findEmailRequestDto, HttpServletRequest request) {
        String result = accountService.findEmail(findEmailRequestDto.getName(), findEmailRequestDto.getPhoneNumber());
        if(result == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.fail(ErrorCode.BAD_REQUEST, "일치하는 회원 정보를 찾을 수 없습니다."));
        }
        return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(result));
        // todo: 2차 개발 고려(request는 FindEmailLog 구현 시 사용될 예정)
    }

    // 2. 이메일 확인 및 인증코드 발송 API
    @GetMapping("/auth/reset-password")
    public ResponseEntity<APIResponse<String>> checkEmailAndSendCode(@Email(message = "유효한 이메일 형식이 아닙니다.") @RequestParam String email) {
        /*
            이메일 확인
            인증코드 발송 및 저장
         */
        if(!accountService.isPresentEmail(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.fail(ErrorCode.BAD_REQUEST));
        }
        if(!registerService.sendVerifyEmailCode(email)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success());
    }


    // 3. 비밀번호 재설정 API
    @PostMapping("/auth/reset-password")
    public ResponseEntity<APIResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto requestDto, HttpServletRequest request) {
        if(!accountService.isPasswordMatch(requestDto.getPassword(), requestDto.getRewritePassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.fail(ErrorCode.BAD_REQUEST, "비밀번호 불일치"));
        }
        if(!accountService.resetPassword(requestDto.getEmail(), requestDto.getPassword())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success());
        // todo: 2차 개발 고려(request는 ResetPasswordLg 구현 시 사용될 예정)
    }

}