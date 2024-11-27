package com.be3c.sysmetic.domain.strategy.controller;

import com.be3c.sysmetic.domain.strategy.dto.StrategyAlgorithmOption;
import com.be3c.sysmetic.domain.strategy.dto.StrategyAlgorithmResponseDto;
import com.be3c.sysmetic.domain.strategy.dto.StrategySearchRequestDto;
import com.be3c.sysmetic.domain.strategy.dto.StrategySearchResponseDto;
import com.be3c.sysmetic.domain.strategy.service.StrategySearchService;
import com.be3c.sysmetic.global.common.response.APIResponse;
import com.be3c.sysmetic.global.common.response.ErrorCode;
import com.be3c.sysmetic.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/v1/strategy/search")
@RequiredArgsConstructor(onConstructor_ = @__(@Autowired))
public class StrategySearchController implements StrategySearchControllerDocs {

    private final StrategySearchService strategySearchService;

    /*
        conditionSearch : 항목별 상세 검색
        URL 경로 : localhost:8080/v1/strategy/search/conditions?pageNum=0
        HTTP Method : POST
        Request Body : JSON
        {
             "methods": ["Manual"],
             "cycle": ["D"],
             "stockNames": [],
             "accumulatedProfitLossRateRangeStart": 10,
             "accumulatedProfitLossRateRangeEnd": 100
        }
    */
    @Override
    @PostMapping("/conditions")
    public APIResponse<PageResponse<StrategySearchResponseDto>> conditionSearch(
            @RequestHeader(value = "pageNum", defaultValue = "0") Integer pageNum,
            @RequestBody StrategySearchRequestDto strategySearchRequestDto) {

        PageResponse<StrategySearchResponseDto> results = strategySearchService.searchConditions(pageNum, strategySearchRequestDto);

        if (results.getContent().isEmpty())
            return APIResponse.fail(ErrorCode.NOT_FOUND, "상세 조건에 해당하는 검색 결과가 없습니다.");

        return APIResponse.success(results);
    }


    /*
        algorithmSearch : 알고리즘별 정렬 - 계산 후 정렬 & 페이징
    */
    @Override
    @GetMapping("/algorithm")
    public APIResponse<PageResponse<StrategyAlgorithmResponseDto>> algorithmSearch(
            @RequestParam(name = "pageNum", defaultValue = "0") Integer pageNum,
            @RequestParam(name = "algorithm", defaultValue = "EFFICIENCY") StrategyAlgorithmOption algorithm) {

        PageResponse<StrategyAlgorithmResponseDto> results = strategySearchService.searchAlgorithm(pageNum, algorithm);

        if (results.getContent().isEmpty())
            return APIResponse.fail(ErrorCode.NOT_FOUND, algorithm + "에 해당하는 전략이 없습니다.");

        return APIResponse.success(results);
    }


}