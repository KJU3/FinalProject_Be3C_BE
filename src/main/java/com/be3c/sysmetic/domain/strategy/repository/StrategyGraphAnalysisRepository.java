package com.be3c.sysmetic.domain.strategy.repository;

import com.be3c.sysmetic.domain.strategy.entity.StrategyGraphAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StrategyGraphAnalysisRepository extends JpaRepository<StrategyGraphAnalysis, Long> {

    void deleteByDailyId(Long dailyId);

    @Query("SELECT s FROM StrategyGraphAnalysis s WHERE s.strategy.id = :strategyId AND s.date > :date")
    List<StrategyGraphAnalysis> findByIdAndAfterDate(@Param("strategyId") Long strategyId, @Param("date") LocalDate date);

    Double findTop1MaximumCapitalReductionAmountByStrategyId(Long strategyId);

    @Query("SELECT COUNT(s) FROM StrategyGraphAnalysis s WHERE s.strategy.id = :strategyId AND s.date <= :date")
    Long countAllByStrategyIdBeforeDate(@Param("strategyId") Long strategyId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM StrategyGraphAnalysis s WHERE s.strategy.id = :strategyId AND s.profitLossAmount >= 0 AND s.date <= :date")
    Long countProfitDaysByStrategyIdBeforeDate(@Param("strategyId") Long strategyId, @Param("date") LocalDate date);

    @Query("SELECT MAX(s.profitLossAmount), MIN(s.profitLossAmount) FROM StrategyGraphAnalysis s WHERE s.strategy.id = :strategyId AND s.date <= :date")
    Object[] findProfitLossAmount(@Param("strategyId") Long strategyId, @Param("date") LocalDate date);

    @Query("SELECT MAX(s.maximumCapitalReductionAmount) FROM StrategyGraphAnalysis s WHERE s.strategy.id = :strategyId AND s.date <= :date")
    Double findMaximumCapitalReductionAmountBeforeDate(@Param("strategyId") Long strategyId, @Param("date") LocalDate date);

    // 전략 삭제 시 전체 삭제
    void deleteAllByStrategyId(Long strategyId);

    // 시작 날짜 찾기
    @Query("SELECT s.date FROM StrategyGraphAnalysis s WHERE s.date >= : startDate")
    List<String> findDates(@Param("startDate") LocalDate startDate);
}
