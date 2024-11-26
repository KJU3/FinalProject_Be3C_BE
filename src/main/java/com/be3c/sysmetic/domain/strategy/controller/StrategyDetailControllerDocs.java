package com.be3c.sysmetic.domain.strategy.controller;

import com.be3c.sysmetic.domain.strategy.dto.StrategyDetailDto;
import com.be3c.sysmetic.global.common.response.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "전략 상세 페이지 API", description = "전략 상세 페이지 조회")
public interface StrategyDetailControllerDocs {

    @Operation(
            summary = "전략 상세 페이지 조회 - 테스트 필요",
            description = "전략 상세 페이지",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "401")
            }
    )
    @GetMapping("/strategy/detail")
    APIResponse<StrategyDetailDto> getDetailPage(@RequestParam("id") Long id);
}
