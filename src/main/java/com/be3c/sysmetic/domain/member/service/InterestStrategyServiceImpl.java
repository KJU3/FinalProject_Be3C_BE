package com.be3c.sysmetic.domain.member.service;

import com.be3c.sysmetic.domain.member.dto.*;
import com.be3c.sysmetic.domain.member.entity.*;
import com.be3c.sysmetic.domain.member.repository.FolderRepository;
import com.be3c.sysmetic.domain.member.repository.InterestStrategyLogRepository;
import com.be3c.sysmetic.domain.member.repository.InterestStrategyRepository;
import com.be3c.sysmetic.domain.member.repository.MemberRepository;
import com.be3c.sysmetic.domain.strategy.entity.Strategy;
import com.be3c.sysmetic.domain.strategy.repository.StrategyRepository;
import com.be3c.sysmetic.domain.strategy.util.StockGetter;
import com.be3c.sysmetic.global.common.Code;
import com.be3c.sysmetic.global.common.response.PageResponse;
import com.be3c.sysmetic.global.util.SecurityUtils;
import com.be3c.sysmetic.global.util.email.dto.InterestRequest;
import com.be3c.sysmetic.global.util.email.service.EmailService;
import com.be3c.sysmetic.global.util.file.dto.FileReferenceType;
import com.be3c.sysmetic.global.util.file.dto.FileRequest;
import com.be3c.sysmetic.global.util.file.service.FileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.be3c.sysmetic.domain.member.message.FollowFailMessage.*;
import static com.be3c.sysmetic.global.common.Code.*;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
@Transactional
public class InterestStrategyServiceImpl implements InterestStrategyService {

    private final InterestStrategyRepository interestStrategyRepository;

    private final InterestStrategyLogRepository interestStrategyLogRepository;

    private final FolderRepository folderRepository;

    private final StrategyRepository strategyRepository;

    private final MemberRepository memberRepository;

    private final SecurityUtils securityUtils;

    private final EmailService emailService;

    private final FileService fileService;

    private final StockGetter stockGetter;

    /**
     * 추후에 수정 필요 (SELECT가 너무 많이 날아간다.)
     */
    /*
        1. SecurityContext에서 유저 아이디를 찾는다.
        2. 현재 페이지 + 페이지 사이즈 (10) + 마지막 수정 날짜를 통해 Pageable을 생성한다.
        3. Member Id + Folder Id + StatusCode + Pageable을 사용해 해당 폴더의 해당 페이지의
           데이터를 불러온다.
        4. 만약 해당 페이지에 데이터가 존재한다면, PageResponse를 생성해 반환한다.
        5. 만약 한 개의 데이터도 없다면, NoSuchElementException을 발생시킨다.
     */
    @Override
    public PageResponse<InterestStrategyGetResponseDto> getInterestStrategyPage(
            InterestStrategyGetRequestDto interestStrategyGetRequestDto
    ) {
        Long userId = securityUtils.getUserIdInSecurityContext();

        Pageable pageable = PageRequest.of(
                interestStrategyGetRequestDto.getPage() - 1,
                10,
                Sort.by("modifiedAt").descending());

        Page<InterestStrategyGetResponseDto> folderPage = interestStrategyRepository
                .findPageByIdAndStatusCode(
                        userId,
                        interestStrategyGetRequestDto.getFolderId(),
                        FOLLOW.getCode(),
                        pageable
                );

        if(folderPage.hasContent()) {
            folderPage.getContent().forEach(interestStrategy -> {
                interestStrategy.setTraderProfileImage(fileService.getFilePath(new FileRequest(FileReferenceType.MEMBER, interestStrategy.getTraderId())));
                interestStrategy.setMethodIconPath(fileService.getFilePath(new FileRequest(FileReferenceType.METHOD, interestStrategy.getMethodId())));
                stockGetter.getStocks(interestStrategy.getId()).getStockIds().forEach(stockId -> {
                            interestStrategy.getStockIconPath().add(fileService.getFilePath(new FileRequest(FileReferenceType.STOCK, stockId)));
                        }
                );
            });

            return PageResponse.<InterestStrategyGetResponseDto>builder()
                    .totalPages(folderPage.getTotalPages())
                    .totalElement(folderPage.getTotalElements())
                    .pageSize(folderPage.getNumberOfElements())
                    .currentPage(interestStrategyGetRequestDto.getPage())
                    .content(folderPage.getContent())
                    .build();
        }

        throw new NoSuchElementException();
    }

    @Override
    public boolean moveFolder(FollowPutRequestDto followPutRequestDto) {
        Long userId = securityUtils.getUserIdInSecurityContext();

        InterestStrategy interestStrategy = interestStrategyRepository
                .findByMemberIdAndFolderIdAndStrategyIdAndStatusCode(
                        userId,
                        followPutRequestDto.getOriginFolderId(),
                        followPutRequestDto.getStrategyId(),
                        FOLLOW.getCode()
                ).orElseThrow(EntityNotFoundException::new);

        interestStrategy.setFolder(folderRepository.findByIdAndStatusCode(
                followPutRequestDto.getToFolderId(),
                USING_STATE.getCode()
        ).orElseThrow(EntityNotFoundException::new));

        return true;
    }

    /*
            1. SecurityContext에서 유저 아이디를 찾는다.
            2. userId + strategyId + statusCode를 사용해서 해당 유저의 관심 전략을 가져온다.
            3. 만약 관심 전략이 존재하지 않는다면,
                3-1. 관심 전략을 등록한다.
                3-2. 관심 전략 등록 로그를 등록한다.
                3-3. true를 반환해 성공 여부를 전달한다..
            4. 만약 관심 전략이 존재한다면,
                4-1. 찾은 관심 전략의 상태 코드를 확인한다.
                4-2. 관심 전략의 상태 코드가 삭제 상태라면 상태 코드를 팔로우 중으로 변경한다.
                4-3. 관심 전략 등록 로그를 등록한다.
                4-4. true를 반환해 성공 여부를 전달한다..
            5. 관심 전략을 팔로우 중이라면, IllegalArgumentException을 발생시킨다.
         */
    @Override
    public boolean follow(FollowPostRequestDto followPostRequestDto) {
        Long userId = securityUtils.getUserIdInSecurityContext();

        Optional<InterestStrategy> interestStrategy = interestStrategyRepository
                .findByMemberIdAndStrategyId(
                        userId,
                        followPostRequestDto.getStrategyId()
                );

        String traderEmail = memberRepository.findById(userId).orElseThrow(EntityNotFoundException::new).getEmail();

        if(interestStrategy.isEmpty()) {
            followStrategy(userId, followPostRequestDto.getFolderId(), followPostRequestDto.getStrategyId());
            emailService.notifyStrategyInterestRegistration(new InterestRequest(traderEmail)).subscribe();
            return true;
        } else if(interestStrategy.get().getStatusCode().equals(Code.UNFOLLOW.getCode())) {
            interestStrategy.get().setStatusCode(FOLLOW.getCode());
            followStrategyLog(
                    interestStrategy.get().getId(),
                    FOLLOW.getCode()
            );
            emailService.notifyStrategyInterestRegistration(new InterestRequest(traderEmail)).subscribe();
            return true;
        }

        throw new IllegalArgumentException();
    }

    @Override
    public boolean unfollow(Long strategyId) {
        Long userId = securityUtils.getUserIdInSecurityContext();

        unFollowStrategy(userId, strategyId);
        return true;
    }

    /*
        1. SecurityContext에서 유저 아이디를 찾는다.
        2. 언팔로우 요청 목록을 돌면서 해당 전략의 관심 전략 삭제를 진행한다.
            2-1. 만약 삭제할 관심 전략을 찾지 못했다면, 실패한 전략 Id와 실패 이유를 Map에 저장한다.
            2-2. 실패 목록을 반환한다.
     */
    @Override
    public Map<Long, String> unFollowList(FollowDeleteRequestDto followDeleteRequestDto) {
        Long userId = securityUtils.getUserIdInSecurityContext();

        Map<Long, String> failUnfollow = new HashMap<>();

        for(Long unfollowId : followDeleteRequestDto.getStrategyId()) {
            try {
                unFollowStrategy(userId, unfollowId);
            } catch (EntityNotFoundException e) {
                failUnfollow.put(unfollowId, e.getMessage());
            }
        }

        return failUnfollow;
    }

    /*
        1. userId + statusCode를 사용해 관심 전략을 가질 회원을 찾는다.
        2. userId + folderId + statusCode를 사용해 관심 전략을 등록할 폴더를 찾는다.
        3. strategyId + statusCode를 사용해 등록할 전략을 찾는다.
        4. 해당 관심 전략을 등록한다.
        5. 관심 전략 등록 로그를 저장한다.
     */
    private void followStrategy(Long userId, Long folderId, Long strategyId) {
        Strategy strategy = strategyRepository
                .findByIdAndOpenStatusCode(
                        strategyId
                ).orElseThrow(EntityNotFoundException::new);

        InterestStrategy interestStrategy = InterestStrategy.builder()
                .folder(folderRepository.findByMemberIdAndIdAndStatusCode(
                        userId,
                        folderId,
                        Code.USING_STATE.getCode()
                ).orElseThrow(EntityNotFoundException::new))
                .strategy(strategy)
                .statusCode(FOLLOW.getCode())
                .build();

        interestStrategyRepository.save(interestStrategy);

        strategy.increaseFollowerCount();

        followStrategyLog(
                interestStrategy.getId(),
                FOLLOW.getCode()
        );
    }

    /*
        1. userId + folderId + strategyId + statusCode를 사용해 해당 관심 전략을 찾는다.
        1-1. 해당 관심 전략을 찾지 못했다면, EntityNotFoundException을 발생시킨다.
        2. 해당 관심 전략의 상태를 UNFOLLOW 상태로 변경한다.
        3. 관심 전략 삭제 로그를 등록한다..
     */
    private void unFollowStrategy(Long userId, Long strategyId) {
        InterestStrategy interestStrategy = interestStrategyRepository
                .findByMemberIdAndStrategyIdAndStatusCode(
                        userId,
                        strategyId,
                        FOLLOW.getCode()
                ).orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_STRATEGY.getMessage()));

        interestStrategy.setStatusCode(UNFOLLOW.getCode());

        interestStrategyRepository.save(interestStrategy);

        interestStrategy.getStrategy().decreaseFollowerCount();

        followStrategyLog(
                interestStrategy.getId(),
                UNFOLLOW.getCode()
        );
    }

    /*
        1. 해당 관심 전략의 등록자를 찾는다.
        2. 해당 관심 전략이 등록된 폴더를 찾는다.
        3. 해당 관심 전략을 찾는다.
        4. 로그를 등록한다.
     */
    private void followStrategyLog(Long interestStrategyId, String LogCode) {
        interestStrategyLogRepository.save(
                InterestStrategyLog.builder()
                        .interestStrategy(
                                interestStrategyRepository
                                        .findById(
                                                interestStrategyId
                                        ).orElseThrow(EntityNotFoundException::new)
                        )
                        .LogCode(LogCode)
                        .isSendMail(Code.NOT_SEND_FOLLOW_MAIL.getCode())
                        .build()
        );
    }
}
