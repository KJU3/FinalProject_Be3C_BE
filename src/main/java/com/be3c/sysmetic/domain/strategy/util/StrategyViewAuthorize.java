package com.be3c.sysmetic.domain.strategy.util;

import com.be3c.sysmetic.domain.strategy.entity.Strategy;
import com.be3c.sysmetic.domain.strategy.exception.StrategyBadRequestException;
import com.be3c.sysmetic.domain.strategy.exception.StrategyExceptionMessage;
import com.be3c.sysmetic.global.common.response.ErrorCode;
import com.be3c.sysmetic.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StrategyViewAuthorize {

    private final SecurityUtils securityUtils;

    public boolean Authorize(Strategy strategy) {
        String userRole = securityUtils.getUserRoleInSecurityContext();

        if (strategy.getStatusCode().equals("NOT_USING_STATE")) {
            // 1. 상태 코드가 NOT_USING_STATE인 경우 - 실패 (삭제된 전략)
            throw new StrategyBadRequestException(StrategyExceptionMessage.DATA_NOT_FOUND.getMessage(), ErrorCode.NOT_FOUND);
        } else if (strategy.getTrader().getId().equals(securityUtils.getUserIdInSecurityContext()) ||
                "USER_MANAGER".equals(userRole) ||
                "TRADER_MANAGER".equals(userRole) ||
                "ADMIN".equals(userRole)
        ) {
            return true;
            // 2. 상태 코드가 NOT_USING_STATE, PUBLIC이 아니면서 트레이더 ID가 일치하거나 사용자 역할이 MANAGER인 경우 - 성공
        } else if (strategy.getStatusCode().equals("PUBLIC")) {
            return true;
            // 3. 상태 코드가 PUBLIC이면 모든 요청에 대해 성공
        }  else {
            // 4. 나머지 경우 - 실패
            throw new StrategyBadRequestException(StrategyExceptionMessage.INVALID_MEMBER.getMessage(), ErrorCode.NOT_FOUND);
        }
    }
}
