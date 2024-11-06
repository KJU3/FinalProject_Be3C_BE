package com.be3c.sysmetic.domain.strategy.controller;

import com.be3c.sysmetic.domain.strategy.dto.MethodGetResponseDto;
import com.be3c.sysmetic.domain.strategy.entity.Method;
import com.be3c.sysmetic.domain.strategy.service.MethodService;
import com.be3c.sysmetic.global.common.response.ApiResponse;
import com.be3c.sysmetic.global.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MethodController {

    private final MethodService methodService;

    /*
        매매 방식 조회 (개별)
        매매 방식 조회 (페이지)
        매매 방식 아이콘 조회
        매매 방식 등록
        매매 방식 수정
        매매 방식 삭제
     */

    @GetMapping("/admin/method/{name:^(?!\\d+$).+}")
    public ResponseEntity<ApiResponse<String>> duplCheck(
            @PathVariable String name
    ) throws Exception {
        if(methodService.duplCheck(name)) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ErrorCode.DUPLICATE_RESOURCE, "중복된 이름입니다."));
    }

    @GetMapping("/admin/method/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<MethodGetResponseDto>> getMethod(
            @PathVariable Long id
    ) throws Exception {
        try {
            Method find_method = methodService.findById(id);

            MethodGetResponseDto methodGetResponseDto = MethodGetResponseDto.builder()
                    .id(find_method.getId())
                    .name(find_method.getName())
                    // 아이콘 file path 찾는 코드 필요.
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(methodGetResponseDto));
        } catch (NullPointerException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(ErrorCode.BAD_REQUEST, "해당 데이터가 없습니다."));
        }
    }


}
