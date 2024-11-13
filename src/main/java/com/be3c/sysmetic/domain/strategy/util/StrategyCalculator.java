package com.be3c.sysmetic.domain.strategy.util;

import com.be3c.sysmetic.global.util.doublehandler.DoubleHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 전략 계산 util
@Component
@RequiredArgsConstructor(onConstructor_ = @__(@Autowired))
public class StrategyCalculator {

    private final DoubleHandler doubleHandler;

    /*
    원금
    첫데이터: 입출금
    이후데이터: 전 원금 + (입출금/(전 잔고/전 원금))
     */
    public Double getPrincipal(boolean isFirst, Double depositWithdrawalAmount, Double beforePrincipal, Double beforeBalance) {
        if(isFirst) {
            return doubleHandler.cutDouble(depositWithdrawalAmount);
        } else {
            return doubleHandler.cutDouble(beforePrincipal + (depositWithdrawalAmount/(beforeBalance/beforePrincipal)));
        }
    }

    /*
    기준가
    => 잔고/원금*1000
    첫데이터: (입출금 + 일손익) / 입출금 * 1000
    이후데이터: (전 잔고 + 입출금 + 일손익) / (전 원금 + (입출금/(전 잔고/전 원금))) * 1000
     */
    public Double getStandardAmount(boolean isFirst, Double depositWithdrawalAmount, Double dailyProfitLossAmount, Double beforeBalance, Double beforePrincipal) {
        Double balance = getCurrentBalance(isFirst, beforeBalance, depositWithdrawalAmount, dailyProfitLossAmount);
        Double principal = getPrincipal(isFirst, depositWithdrawalAmount, beforePrincipal, beforeBalance);

        return doubleHandler.cutDouble(balance / principal * 1000);
    }

    /*
    잔고
    첫데이터: 원금 + 일손익 => 입출금 + 일손익
    이후데이터: 전 잔고 + 입출금 + 일손익
     */
    public Double getCurrentBalance(boolean isFirst, Double beforeBalance, Double depositWithdrawalAmount, Double dailyProfitLossAmount) {
        if(isFirst) {
            return doubleHandler.cutDouble(depositWithdrawalAmount + dailyProfitLossAmount);
        } else {
            return doubleHandler.cutDouble(beforeBalance + depositWithdrawalAmount + dailyProfitLossAmount);
        }
    }

    /*
    일손익률
    첫데이터: (기준가 - 1000) * 1000
    이후데이터: (현재 기준가 - 이전 데이터 기준가) / 이전 데이터 기준가
     */
    public Double getDailyProfitLossRate(boolean isFirst, Double depositWithdrawalAmount, Double dailyProfitLossAmount, Double beforeBalance, Double beforePrincipal, Double beforeStandardAmount) {
        Double standardAmount = getStandardAmount(isFirst, depositWithdrawalAmount, dailyProfitLossAmount, beforeBalance, beforePrincipal);
        if(isFirst) {
            return doubleHandler.cutDouble((standardAmount - 1000) * 1000);
        } else {
            return doubleHandler.cutDouble((standardAmount - beforeStandardAmount) / beforeStandardAmount);
        }
    }

}