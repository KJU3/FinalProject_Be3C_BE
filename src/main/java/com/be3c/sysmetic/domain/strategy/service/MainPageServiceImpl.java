package com.be3c.sysmetic.domain.strategy.service;

import com.be3c.sysmetic.domain.member.repository.MemberRepository;
import com.be3c.sysmetic.domain.strategy.dto.*;
import com.be3c.sysmetic.domain.strategy.entity.Stock;
import com.be3c.sysmetic.domain.strategy.entity.Strategy;
import com.be3c.sysmetic.domain.strategy.entity.StrategyStockReference;
import com.be3c.sysmetic.domain.strategy.repository.MainPageRepository;
import com.be3c.sysmetic.domain.strategy.repository.StrategyGraphAnalysisRepository;
import com.be3c.sysmetic.domain.strategy.repository.StrategyRepository;
import com.be3c.sysmetic.domain.strategy.repository.StrategyStockReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @__(@Autowired))
@Service
public class MainPageServiceImpl implements MainPageService {

    private final MainPageRepository mainPageRepository;
    private final MemberRepository memberRepository;
    private final StrategyStockReferenceRepository strategyStockReferenceRepository;
    private final StrategyGraphAnalysisRepository strategyGraphAnalysisRepository;
    private final StrategyRepository strategyRepository;

    @Override
    @Transactional
    public MainPageDto getMain() {

        MainPageDto mainPageDto = MainPageDto.builder()
                .rankedTrader(setTop3FollowerTrader())
                .totalTraderCount(memberRepository.countAllByRoleCode("trader"))
                .totalStrategyCount(mainPageRepository.count())
                .smScoreTopFives(setTop5SmScore())
                .build();
        return mainPageDto;
    }


    // TODO
    @Override
    @Transactional
    public MainPageAnalysisDto getAnalysis(String period) {
    //
    //     LocalDate currentDate = LocalDate.now();
    //     LocalDate startDate = calculateStartDate(currentDate, period);
    //
    //     MainPageAnalysisDto analysisDto = MainPageAnalysisDto.builder()
    //             .smScoreTopStrategyName(strategyRepository.findTop1SmScore())
    //             .xAxis(strategyGraphAnalysisRepository.findDates(startDate))
    //             .averageStandardAmount()
    //             .accumProfitLossRate()
    //             .build();
    //
        return null;
    }

    private LocalDate calculateStartDate(LocalDate currentDate, String period) {
        switch (period) {
            case "ONE_MONTH":
                return currentDate.minusMonths(1);
            case "THREE_MONTH":
                return currentDate.minusMonths(3);
            case "SIX_MONTH":
                return currentDate.minusMonths(6);
            case "ONE_YEAR":
                return currentDate.minusYears(1);
            case "ALL":
            default:
                return LocalDate.of(2000, 1, 1);        // 2000년 1월 1일 이후의 기간
        }
    }


    // setTop3FollowerTrader : 트레이더 팔로우 수 Top 3
    private List<TraderRankingDto> setTop3FollowerTrader(){
        List<TraderRankingDto> traders = new ArrayList<>();

        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("followerCount")));

        Page<Strategy> strategyPage = mainPageRepository.findTop3ByFollowerCount(pageable);

        if (strategyPage.isEmpty())
            // 데이터가 없어도 메인 페이지 접속은 가능해야 함
            return new ArrayList<>();

        for (Strategy s : strategyPage.getContent()) {

            if (s.getContent() == null || s.getContent().isEmpty())
                return new ArrayList<>();

            traders.add(TraderRankingDto.builder()
                    .id(s.getId())
                    .nickname((s.getTrader().getNickname()))
                    .followerCount(s.getFollowerCount())
                    .accumProfitLossRate(s.getAccumulatedProfitLossRate())
                    .build()
            );
        }
        return traders;
    }

    // setTop5SmScore : SM Score Top 5 전략
    private List<SmScoreTopFive> setTop5SmScore(){

        Pageable pageable = PageRequest.of(0, 5);

        List<SmScoreTopFive> topFives = new ArrayList<>();
        Page<Strategy> strategyPage = mainPageRepository.findTop5SmScore(pageable);

        if (strategyPage.isEmpty())
            // 데이터가 없어도 메인 페이지 접속은 가능해야 함
            return new ArrayList<>();

        for (Strategy s : strategyPage) {
            HashSet<Long> idSet = new HashSet<>();
            HashSet<String> nameSet = new HashSet<>();

            List<StrategyStockReference> references = strategyStockReferenceRepository.findByStrategyId(s.getId());

            for (StrategyStockReference ref : references) {
                Stock stock = ref.getStock();
                idSet.add(stock.getId());
                nameSet.add(stock.getName());
            }

            StockListDto stockListDto = StockListDto.builder()
                    .stockIds(idSet)
                    .stockNames(nameSet)
                    .build();

            SmScoreTopFive element = SmScoreTopFive.builder()
                    .id(s.getId())
                    .traderId(s.getTrader().getId())
                    .nickname(s.getTrader().getNickname())
                    .name(s.getName())
                    .stocks(new HashSet<>(List.of(stockListDto)))
                    .smScore(s.getSmScore())
                    .build();

            topFives.add(element);
        }
        return topFives;
    }
}
