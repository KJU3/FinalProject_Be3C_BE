package com.be3c.sysmetic.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@Schema(description = "관심 전략 조회 응답 DTO")
public class InterestStrategyGetResponseDto {

    @Schema(description = "전략 ID", example = "1")
    private Long id;

    @Schema(description = "전략 이름", example = "효율적인 트레이딩 전략")
    private String strategyName;

    @Schema(description = "트레이더 이름", example = "Jane Doe")
    private String traderName;

    @Schema(description = "사용자 프로필 이미지")
    private String traderProfilePath;

    @Schema(description = "아이콘 경로 리스트", example = "[\"/path/to/icon1.png\", \"/path/to/icon2.png\"]")
    private String methodIconPath;

    @Schema(description = "종목 아이콘 경로")
    private List<String> stockIconPath;

    @Schema(description = "주기")
    private Character cycle;

    @Schema(description = "팔로워 수", example = "1200")
    private Long followerCount;

    @Schema(description = "누적 수익률", example = "25.75")
    private Double accumulatedProfitRatio;

    @Schema(description = "SM 점수", example = "88.5")
    private Double SMScore;

    @Schema(description = "최대 낙폭(MDD)", example = "-12.34")
    private Double MDD;

    public InterestStrategyGetResponseDto(Long id, String strategyName, String traderName, String traderProfilePath, String methodIconPath, List<String> stockIconPath, Character cycle, Long followerCount, Double accumulatedProfitRatio, Double SMScore, Double MDD) {
        this.id = id;
        this.strategyName = strategyName;
        this.traderName = traderName;
        this.traderProfilePath = traderProfilePath;
        this.methodIconPath = methodIconPath;
        this.stockIconPath = stockIconPath;
        this.cycle = cycle;
        this.followerCount = followerCount;
        this.accumulatedProfitRatio = accumulatedProfitRatio;
        this.SMScore = SMScore;
        this.MDD = MDD;
    }
}
