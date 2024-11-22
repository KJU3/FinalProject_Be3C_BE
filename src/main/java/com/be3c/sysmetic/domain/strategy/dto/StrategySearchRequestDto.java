package com.be3c.sysmetic.domain.strategy.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StrategySearchRequestDto {

    /*
        SearchConditionRequestDto : 전략 상세 조건 검색 요청 Dto

       methods : 매매방식
       cycle : 주기
       stockNames : 종목
       TODO periods : 기간
       accumProfitLossRate : 누적수익률
    */

    private List<String> methods;
    private List<Character> cycle;
    private List<String> stockNames;
    // TODO private List<String> periods;
    private String accumProfitLossRateRangeStart;
    private String accumProfitLossRateRangeEnd;


    @JsonCreator
    public StrategySearchRequestDto(
            @JsonProperty("methods") List<String> methods,
            @JsonProperty("cycle") List<Character> cycle,
            @JsonProperty("stockNames") List<String> stockNames,
            @JsonProperty("accumProfitLossRateRangeStart") String accumProfitLossRateRangeStart,
            @JsonProperty("accumProfitLossRateRangeEnd") String accumProfitLossRateRangeEnd) {
        this.methods = methods;
        this.cycle = cycle;
        this.stockNames = stockNames;
        this.accumProfitLossRateRangeStart = accumProfitLossRateRangeStart;
        this.accumProfitLossRateRangeEnd = accumProfitLossRateRangeEnd;
    }
}
