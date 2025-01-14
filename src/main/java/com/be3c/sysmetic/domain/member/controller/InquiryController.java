package com.be3c.sysmetic.domain.member.controller;

import com.be3c.sysmetic.domain.member.dto.*;
import com.be3c.sysmetic.domain.member.entity.Inquiry;
import com.be3c.sysmetic.domain.member.entity.InquiryAnswer;
import com.be3c.sysmetic.domain.member.entity.InquiryStatus;
import com.be3c.sysmetic.domain.member.service.InquiryAnswerService;
import com.be3c.sysmetic.domain.member.service.InquiryService;
import com.be3c.sysmetic.domain.strategy.entity.Strategy;
import com.be3c.sysmetic.global.common.response.APIResponse;
import com.be3c.sysmetic.global.common.response.ErrorCode;
import com.be3c.sysmetic.global.common.response.PageResponse;
import com.be3c.sysmetic.global.common.response.SuccessCode;
import com.be3c.sysmetic.global.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
public class InquiryController implements InquiryControllerDocs {

    private final SecurityUtils securityUtils;

    private final InquiryService inquiryService;
    private final InquiryAnswerService inquiryAnswerService;

    private final Integer pageSize = 10; // 한 페이지 크기

    /*
        관리자 문의 조회 / 검색 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의 데이터 조회에 성공했을 때 : OK
        3. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/qna")
    public ResponseEntity<APIResponse<PageResponse<InquiryAdminListOneShowResponseDto>>> showAdminInquiry (
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "closed", required = false, defaultValue = "all") String closed,
            @RequestParam(value = "searchType", required = false, defaultValue = "strategy") String searchType,
            @RequestParam(value = "searchText", required = false) String searchText) {
        InquiryStatus inquiryStatus = InquiryStatus.valueOf(closed);

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        if (!(closed.equals("all") || closed.equals("closed") || closed.equals("unclosed"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 closed가 올바르지 않습니다."));
        }

        if (!(searchType.equals("strategy") || searchType.equals("trader") || searchType.equals("inquirer"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 searchType이 올바르지 않습니다."));
        }

        InquiryAdminListShowRequestDto inquiryAdminListShowRequestDto = new InquiryAdminListShowRequestDto();
        inquiryAdminListShowRequestDto.setTab(inquiryStatus);
        inquiryAdminListShowRequestDto.setSearchType(searchType);
        inquiryAdminListShowRequestDto.setSearchText(searchText);

        Page<Inquiry> inquiryList = inquiryService.findInquiriesAdmin(inquiryAdminListShowRequestDto, page-1);

        List<InquiryAdminListOneShowResponseDto> inquiryDtoList = inquiryList.stream()
                .map(InquiryController::inquiryToInquiryAdminOneResponseDto).collect(Collectors.toList());

        PageResponse<InquiryAdminListOneShowResponseDto> adminInquiryPage = PageResponse.<InquiryAdminListOneShowResponseDto>builder()
                .currentPage(page)
                .pageSize(pageSize)
                .totalElement(inquiryList.getTotalElements())
                .totalPages(inquiryList.getTotalPages())
                .content(inquiryDtoList)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(adminInquiryPage));
    }

    public static InquiryAdminListOneShowResponseDto inquiryToInquiryAdminOneResponseDto(Inquiry inquiry) {

        return InquiryAdminListOneShowResponseDto.builder()
                .inquiryId(inquiry.getId())
                .traderNickname(inquiry.getTraderNickname())
                .strategyName(inquiry.getStrategyName())
                .inquiryRegistrationDate(inquiry.getInquiryRegistrationDate())
                .inquirerNickname(inquiry.getInquirerNickname())
                .inquiryStatus(inquiry.getInquiryStatus())
                .build();
    }


    /*
        관리자 문의 상세 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의의 상세 데이터 조회에 성공했을 때 : OK
        3. 해당 문의를 찾지 못했을 때 : NOT_FOUND
        4. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/qna/{qnaId}")
    public ResponseEntity<APIResponse<InquiryAnswerAdminShowResponseDto>> showAdminInquiryDetail (
            @PathVariable(value = "qnaId") Long inquiryId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "closed", required = false, defaultValue = "all") String closed,
            @RequestParam(value = "searchType", required = false, defaultValue = "strategy") String searchType,
            @RequestParam(value = "searchText", required = false) String searchText) {
        InquiryStatus inquiryStatus = InquiryStatus.valueOf(closed);

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        if (!(closed.equals("all") || closed.equals("closed") || closed.equals("unclosed"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 closed가 올바르지 않습니다."));
        }

        if (!(searchType.equals("strategy") || searchType.equals("trader") || searchType.equals("inquirer"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 searchType이 올바르지 않습니다."));
        }

        try {
            Inquiry inquiry = inquiryService.findOneInquiry(inquiryId);
            String previousInquiryTitle = inquiryService.adminFindPreviousInquiryTitle(inquiryId);
            LocalDateTime previousInquiryWriteDate = inquiryService.adminFindPreviousInquiryWriteDate(inquiryId);
            String nextInquiryTitle = inquiryService.adminFindNextInquiryTitle(inquiryId);
            LocalDateTime nextInquiryWriteDate = inquiryService.adminFindNextInquiryWriteDate(inquiryId);

            Long inquiryAnswerId;
            String answerTitle;
            LocalDateTime answerRegistrationDate;
            String answerContent;
            if (inquiry.getInquiryStatus() == InquiryStatus.unclosed) {
                inquiryAnswerId = null;
                answerTitle = null;
                answerRegistrationDate = null;
                answerContent = null;
            } else {
                InquiryAnswer inquiryAnswer = inquiryAnswerService.findThatInquiryAnswer(inquiryId);
                inquiryAnswerId = inquiryAnswer.getId();
                answerTitle = inquiryAnswer.getAnswerTitle();
                answerRegistrationDate = inquiryAnswer.getAnswerRegistrationDate();
                answerContent = inquiryAnswer.getAnswerContent();
            }

            InquiryAnswerAdminShowResponseDto inquiryAnswerAdminShowResponseDto = InquiryAnswerAdminShowResponseDto.builder()
                    .page(page)
                    .closed(closed)
                    .searchType(searchType)
                    .searchText(searchText)

                    .inquiryId(inquiryId)
                    .inquiryAnswerId(inquiryAnswerId)

                    .inquiryTitle(inquiry.getInquiryTitle())
                    .inquiryRegistrationDate(inquiry.getInquiryRegistrationDate())
                    .inquirerNickname(inquiry.getInquirerNickname())
                    .inquiryStatus(inquiry.getInquiryStatus())

                    // 전략 위 아이콘들
                    .strategyName(inquiry.getStrategyName())
                    // 트레이더의 프로필 사진
                    .traderNickname(inquiry.getTraderNickname())

                    .inquiryContent(inquiry.getInquiryContent())

                    .answerTitle(answerTitle)
                    .answerRegistrationDate(answerRegistrationDate)
                    .answerContent(answerContent)

                    .previousTitle(previousInquiryTitle)
                    .previousWriteDate(previousInquiryWriteDate)
                    .nextTitle(nextInquiryTitle)
                    .nextWriteDate(nextInquiryWriteDate)
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(APIResponse.success(inquiryAnswerAdminShowResponseDto));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }


    /*
        관리자 문의 삭제 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의 삭제에 성공했을 때 : OK
        3. 문의 삭제에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 해당 문의를 찾지 못했을 때 : NOT_FOUND
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/admin/qna/{qnaId}")
    public ResponseEntity<APIResponse<Long>> deleteAdminInquiry (
            @PathVariable(value = "qnaId") Long inquiryId) {

        try {
            if (inquiryService.deleteAdminInquiry(inquiryId)) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }


    /*
        관리자 문의 목록 삭제 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의 목록 삭제에 성공했을 때 : OK
        3. 해당 문의를 찾지 못했을 때 : NOT_FOUND
        4. 문의 중 일부만 삭제에 실패했을 때 : MULTI_STATUS
        5. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/admin/qna")
    public ResponseEntity<APIResponse<Map<Long, String>>> deleteAdminInquiryList(
            @RequestBody @Valid InquiryAdminListDeleteRequestDto noticeListDeleteRequestDto) {

        List<Long> inquiryIdList = noticeListDeleteRequestDto.getInquiryIdList();

        try {
            Map<Long, String> deleteResult = inquiryService.deleteAdminInquiryList(inquiryIdList);

            if (deleteResult.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.MULTI_STATUS)
                    .body(APIResponse.success(SuccessCode.OK, deleteResult));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }


    /*
        질문자 문의 등록 화면 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 질문자 문의 등록 화면 조회에 성공했을 때 : OK
        3. 질문자 문의 등록 화면 조회에 실패했을 때 : NOT_FOUND
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_USER_MANAGER")
    @GetMapping("/strategy/{strategyId}/qna-page")
    public ResponseEntity<APIResponse<InquirySavePageShowResponseDto>> showInquirySavePage (
            @PathVariable(value = "strategyId") Long strategyId) {

        try {
            Strategy strategy = inquiryService.findStrategyForInquiryPage(strategyId);

            InquirySavePageShowResponseDto inquirySavePageShowResponseDto = InquirySavePageShowResponseDto.builder()
                    .strategyName(strategy.getName())
                    .traderNickname(strategy.getTrader().getNickname())
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(APIResponse.success(inquirySavePageShowResponseDto));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }


    /*
        질문자 문의 등록 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의가 등록에 성공했을 때 : OK
        3. 문의가 등록에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 데이터의 형식이 올바르지 않음 : BAD_REQUEST
        5. 등록하는 질문자나 해당 전략의 정보를 찾지 못했을 때 : NOT_FOUND
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_USER_MANAGER")
    @PostMapping("/strategy/{strategyId}/qna")
    public ResponseEntity<APIResponse<Long>> saveInquirerInquiry(
            @PathVariable(value = "strategyId") Long strategyId,
            @RequestBody @Valid InquirySaveRequestDto inquirySaveRequestDto) {

        Long userId = securityUtils.getUserIdInSecurityContext();

        try {
            if (inquiryService.registerInquiry(
                    userId,
                    strategyId,
                    inquirySaveRequestDto.getInquiryTitle(),
                    inquirySaveRequestDto.getInquiryContent())) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }


    /*
        질문자 문의 조회 / 검색 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의 데이터 조회에 성공했을 때 : OK
        3. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_USER_MANAGER")
    @GetMapping("/member/qna")
    public ResponseEntity<APIResponse<PageResponse<InquiryListOneShowResponseDto>>> showInquirerInquiry (
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "sort", defaultValue = "registrationDate") String sort,
            @RequestParam(value = "closed", defaultValue = "all") String closed) {
        InquiryStatus inquiryStatus = InquiryStatus.valueOf(closed);

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        if (!(closed.equals("all") || closed.equals("closed") || closed.equals("unclosed"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 closed가 올바르지 않습니다."));
        }

        if (!(sort.equals("registrationDate") || sort.equals("strategyName"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 sort가 올바르지 않습니다."));
        }

        Long userId = securityUtils.getUserIdInSecurityContext();

        InquiryListShowRequestDto inquiryListShowRequestDto = new InquiryListShowRequestDto();
        inquiryListShowRequestDto.setInquirerId(userId);
        inquiryListShowRequestDto.setSort(sort);
        inquiryListShowRequestDto.setTab(inquiryStatus);

        Page<Inquiry> inquiryList = inquiryService.findInquiries(inquiryListShowRequestDto, page-1);

        List<InquiryListOneShowResponseDto> inquiryDtoList = inquiryList.stream()
                .map(InquiryController::inquiryToInquiryOneResponseDto).collect(Collectors.toList());

        PageResponse<InquiryListOneShowResponseDto> inquiryPage = PageResponse.<InquiryListOneShowResponseDto>builder()
                .currentPage(page)
                .pageSize(pageSize)
                .totalElement(inquiryList.getTotalElements())
                .totalPages(inquiryList.getTotalPages())
                .content(inquiryDtoList)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(inquiryPage));
    }

    public static InquiryListOneShowResponseDto inquiryToInquiryOneResponseDto(Inquiry inquiry) {

        return InquiryListOneShowResponseDto.builder()
                .inquiryId(inquiry.getId())
                .inquiryTitle(inquiry.getInquiryTitle())
                .strategyName(inquiry.getStrategyName())
                .inquiryRegistrationDate(inquiry.getInquiryRegistrationDate())
                .inquiryStatus(inquiry.getInquiryStatus())
                .build();
    }


     /*
        질문자 문의 상세 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의의 상세 데이터 조회에 성공했을 때 : OK
        3. 해당 문의를 찾지 못했을 때 : NOT_FOUND
        4. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
     @Override
//     @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_USER_MANAGER")
     @GetMapping("/member/qna/{qnaId}")
    public ResponseEntity<APIResponse<InquiryAnswerShowResponseDto>> showInquirerInquiryDetail (
            @PathVariable(value = "qnaId") Long inquiryId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "sort", defaultValue = "registrationDate") String sort,
            @RequestParam(value = "closed", defaultValue = "all") String closed) {
         InquiryStatus inquiryStatus = InquiryStatus.valueOf(closed);

         if (page <= 0) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                     .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
         }

         if (!(closed.equals("all") || closed.equals("closed") || closed.equals("unclosed"))) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                     .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 closed가 올바르지 않습니다."));
         }

         if (!(sort.equals("registrationDate") || sort.equals("strategyName"))) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                     .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 sort가 올바르지 않습니다."));
         }

         try {
             Long userId = securityUtils.getUserIdInSecurityContext();
             Inquiry inquiry = inquiryService.findOneInquiry(inquiryId);

             if (!userId.equals(inquiry.getInquirer().getId())) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN)
                         .body(APIResponse.fail(ErrorCode.FORBIDDEN, "유효하지 않은 회원입니다."));
             }

             String previousInquiryTitle = inquiryService.inquirerFindPreviousInquiryTitle(inquiryId, userId);
             LocalDateTime previousInquiryWriteDate = inquiryService.inquirerFindPreviousInquiryWriteDate(inquiryId, userId);
             String nextInquiryTitle = inquiryService.inquirerFindNextInquiryTitle(inquiryId, userId);
             LocalDateTime nextInquiryWriteDate = inquiryService.inquirerFindNextInquiryWriteDate(inquiryId, userId);

             Long inquiryAnswerId;
             String answerTitle;
             LocalDateTime answerRegistrationDate;
             String answerContent;
             if (inquiry.getInquiryStatus() == InquiryStatus.unclosed) {
                 inquiryAnswerId = null;
                 answerTitle = null;
                 answerRegistrationDate = null;
                 answerContent = null;
             } else {
                 InquiryAnswer inquiryAnswer = inquiryAnswerService.findThatInquiryAnswer(inquiryId);
                 inquiryAnswerId = inquiryAnswer.getId();
                 answerTitle = inquiryAnswer.getAnswerTitle();
                 answerRegistrationDate = inquiryAnswer.getAnswerRegistrationDate();
                 answerContent = inquiryAnswer.getAnswerContent();
             }

             InquiryAnswerShowResponseDto inquiryAnswerShowResponseDto = InquiryAnswerShowResponseDto.builder()
                     .page(page)
                     .sort(sort)
                     .closed(closed)

                     .inquiryId(inquiryId)
                     .inquiryAnswerId(inquiryAnswerId)

                     .inquiryTitle(inquiry.getInquiryTitle())
                     .inquiryRegistrationDate(inquiry.getInquiryRegistrationDate())
                     .inquirerNickname(inquiry.getInquirerNickname())
                     .inquiryStatus(inquiry.getInquiryStatus())

                     // 전략 위 아이콘들
                     .strategyName(inquiry.getStrategyName())
                     // 트레이더의 프로필 사진
                     .traderNickname(inquiry.getTraderNickname())

                     .inquiryContent(inquiry.getInquiryContent())

                     .answerTitle(answerTitle)
                     .answerRegistrationDate(answerRegistrationDate)
                     .answerContent(answerContent)

                     .previousTitle(previousInquiryTitle)
                     .previousWriteDate(previousInquiryWriteDate)
                     .nextTitle(nextInquiryTitle)
                     .nextWriteDate(nextInquiryWriteDate)
                     .build();

             return ResponseEntity.status(HttpStatus.OK)
                     .body(APIResponse.success(inquiryAnswerShowResponseDto));
         }
         catch (EntityNotFoundException e) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                     .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
         }
    }


    /*
        질문자 문의 수정 화면 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 질문자 문의 수정 화면 조회에 성공했을 때 : OK
        3. 해당 문의를 찾지 못했을 때 : NOT_FOUND
        4. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_USER_MANAGER")
    @GetMapping("/member/qna/{qnaId}/modify-page")
    public ResponseEntity<APIResponse<InquiryModifyPageShowResponseDto>> showInquiryModifyPage (
            @PathVariable(value = "qnaId") Long inquiryId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "sort", defaultValue = "registrationDate") String sort,
            @RequestParam(value = "closed", defaultValue = "all") String closed) {
        InquiryStatus inquiryStatus = InquiryStatus.valueOf(closed);

        try {
            if (page <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
            }

            if (!(closed.equals("all") || closed.equals("closed") || closed.equals("unclosed"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 closed가 올바르지 않습니다."));
            }

            if (!(sort.equals("registrationDate") || sort.equals("strategyName"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 sort가 올바르지 않습니다."));
            }

            Inquiry inquiry = inquiryService.findOneInquiry(inquiryId);

            if(!Objects.equals(securityUtils.getUserIdInSecurityContext(), inquiry.getInquirer().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(APIResponse.fail(ErrorCode.FORBIDDEN, "유효하지 않은 회원입니다."));
            }

            InquiryModifyPageShowResponseDto inquiryModifyPageShowResponseDto = InquiryModifyPageShowResponseDto.builder()
                    .page(page)
                    .sort(sort)
                    .closed(closed)
                    .inquiryTitle(inquiry.getInquiryTitle())
                    .inquiryContent(inquiry.getInquiryContent())
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(APIResponse.success(inquiryModifyPageShowResponseDto));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }


     /*
        질문자 문의 수정 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의 수정에 성공했을 때 : OK
        3. 문의 수정에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 해당 문의를 찾지 못했을 때 : NOT_FOUND
        5. 데이터의 형식이 올바르지 않음 : BAD_REQUEST
            +) 답변이 등록된 문의를 수정 시도함
     */
     @Override
//     @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_USER_MANAGER")
     @PutMapping("/member/qna/{qnaId}")
    public ResponseEntity<APIResponse<Long>> modifyInquirerInquiry (
            @PathVariable(value = "qnaId") Long inquiryId,
            @RequestBody @Valid InquiryModifyRequestDto inquiryModifyRequestDto) {

        try {
            Inquiry inquiry = inquiryService.findOneInquiry(inquiryId);

            if(!Objects.equals(securityUtils.getUserIdInSecurityContext(), inquiry.getInquirer().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(APIResponse.fail(ErrorCode.FORBIDDEN, "유효하지 않은 회원입니다."));
            }

            if (inquiryService.modifyInquiry(
                    inquiryId,
                    inquiryModifyRequestDto.getInquiryTitle(),
                    inquiryModifyRequestDto.getInquiryContent())) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        catch (IllegalStateException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                     .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "답변이 등록된 문의는 수정할 수 없습니다."));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }


    /*
        질문자 문의 삭제 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의 삭제에 성공했을 때 : OK
        3. 문의 삭제에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 해당 문의를 찾지 못했을 때 : NOT_FOUND
        5. 답변이 등록된 문의를 수정 시도함 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_USER_MANAGER")
    @DeleteMapping("/member/qna/{qnaId}")
    public ResponseEntity<APIResponse<Long>> deleteInquirerInquiry (
            @PathVariable(value = "qnaId") Long inquiryId) {

        try {
            Inquiry inquiry = inquiryService.findOneInquiry(inquiryId);

            if(!Objects.equals(securityUtils.getUserIdInSecurityContext(), inquiry.getInquirer().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(APIResponse.fail(ErrorCode.FORBIDDEN, "유효하지 않은 회원입니다."));
            }

            if (inquiryService.deleteInquiry(inquiryId)) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
        catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "답변이 등록된 문의는 수정할 수 없습니다."));
        }
    }


    /*
        트레이더 문의 답변 등록 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의 답변이 등록에 성공했을 때 : OK
        3. 문의 답변이 등록에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 데이터의 형식이 올바르지 않음 : BAD_REQUEST
        5. 등록하는 문의 정보를 찾지 못했을 때 : NOT_FOUND
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_TRADER') or hasRole('ROLE_TRADER_MANAGER')")
    @PostMapping("/trader/qna/{qnaId}")
    public ResponseEntity<APIResponse<Long>> saveTraderInquiryAnswer (
            @PathVariable(value = "qnaId") Long inquiryId,
            @RequestBody @Valid InquiryDetailSaveRequestDto inquiryDetailSaveRequestDto) {

        try {
            Inquiry inquiry = inquiryService.findOneInquiry(inquiryId);

            if(!Objects.equals(securityUtils.getUserIdInSecurityContext(), inquiry.getTraderId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(APIResponse.fail(ErrorCode.FORBIDDEN, "유효하지 않은 회원입니다."));
            }

            if (inquiryAnswerService.registerInquiryAnswer(
                    inquiryId,
                    inquiryDetailSaveRequestDto.getAnswerTitle(),
                    inquiryDetailSaveRequestDto.getAnswerContent())) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }


    /*
        트레이더 문의 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의 데이터 조회에 성공했을 때 : OK
        3. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_TRADER') or hasRole('ROLE_TRADER_MANAGER')")
    @GetMapping("/trader/qna")
    public ResponseEntity<APIResponse<PageResponse<InquiryListOneShowResponseDto>>> showTraderInquiry (
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "sort", defaultValue = "registrationDate") String sort,
            @RequestParam(value = "closed", defaultValue = "all") String closed) {
        InquiryStatus inquiryStatus = InquiryStatus.valueOf(closed);

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        if (!(closed.equals("all") || closed.equals("closed") || closed.equals("unclosed"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 closed가 올바르지 않습니다."));
        }

        if (!(sort.equals("registrationDate") || sort.equals("strategyName"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 sort가 올바르지 않습니다."));
        }

        Long userId = securityUtils.getUserIdInSecurityContext();

        InquiryListShowRequestDto inquiryListShowRequestDto = new InquiryListShowRequestDto();
        inquiryListShowRequestDto.setTraderId(userId);
        inquiryListShowRequestDto.setSort(sort);
        inquiryListShowRequestDto.setTab(inquiryStatus);

        Page<Inquiry> inquiryList = inquiryService.findInquiries(inquiryListShowRequestDto, page-1);

        List<InquiryListOneShowResponseDto> inquiryDtoList = inquiryList.stream()
                .map(InquiryController::inquiryToInquiryOneResponseDto).collect(Collectors.toList());

        PageResponse<InquiryListOneShowResponseDto> inquiryPage = PageResponse.<InquiryListOneShowResponseDto>builder()
                .currentPage(page)
                .pageSize(pageSize)
                .totalElement(inquiryList.getTotalElements())
                .totalPages(inquiryList.getTotalPages())
                .content(inquiryDtoList)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(inquiryPage));
    }


    /*
        트레이더 문의 상세 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 문의의 상세 데이터 조회에 성공했을 때 : OK
        3. 해당 문의를 찾지 못했을 때 : NOT_FOUND
        4. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_TRADER') or hasRole('ROLE_TRADER_MANAGER')")
    @GetMapping("/trader/qna/{qnaId}")
    public ResponseEntity<APIResponse<InquiryAnswerShowResponseDto>> showTraderInquiryDetail (
            @PathVariable(value = "qnaId") Long inquiryId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "sort", defaultValue = "registrationDate") String sort,
            @RequestParam(value = "closed", defaultValue = "all") String closed) {
        InquiryStatus inquiryStatus = InquiryStatus.valueOf(closed);

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        if (!(closed.equals("all") || closed.equals("closed") || closed.equals("unclosed"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 closed가 올바르지 않습니다."));
        }

        if (!(sort.equals("registrationDate") || sort.equals("strategyName"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 sort가 올바르지 않습니다."));
        }

        try {
            Long userId = securityUtils.getUserIdInSecurityContext();
            Inquiry inquiry = inquiryService.findOneInquiry(inquiryId);

            if(!Objects.equals(userId, inquiry.getTraderId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(APIResponse.fail(ErrorCode.FORBIDDEN, "유효하지 않은 회원입니다."));
            }

            String previousInquiryTitle = inquiryService.traderFindPreviousInquiryTitle(inquiryId, userId);
            LocalDateTime previousInquiryWriteDate = inquiryService.traderFindPreviousInquiryWriteDate(inquiryId, userId);
            String nextInquiryTitle = inquiryService.traderFindNextInquiryTitle(inquiryId, userId);
            LocalDateTime nextInquiryWriteDate = inquiryService.traderFindNextInquiryWriteDate(inquiryId, userId);

            Long inquiryAnswerId;
            String answerTitle;
            LocalDateTime answerRegistrationDate;
            String answerContent;
            if (inquiry.getInquiryStatus() == InquiryStatus.unclosed) {
                inquiryAnswerId = null;
                answerTitle = null;
                answerRegistrationDate = null;
                answerContent = null;
            } else {
                InquiryAnswer inquiryAnswer = inquiryAnswerService.findThatInquiryAnswer(inquiryId);
                inquiryAnswerId = inquiryAnswer.getId();
                answerTitle = inquiryAnswer.getAnswerTitle();
                answerRegistrationDate = inquiryAnswer.getAnswerRegistrationDate();
                answerContent = inquiryAnswer.getAnswerContent();
            }

            InquiryAnswerShowResponseDto inquiryAnswerShowResponseDto = InquiryAnswerShowResponseDto.builder()
                    .page(page)
                    .sort(sort)
                    .closed(closed)

                    .inquiryId(inquiryId)
                    .inquiryAnswerId(inquiryAnswerId)

                    .inquiryTitle(inquiry.getInquiryTitle())
                    .inquiryRegistrationDate(inquiry.getInquiryRegistrationDate())
                    .inquirerNickname(inquiry.getInquirerNickname())
                    .inquiryStatus(inquiry.getInquiryStatus())

                    // 전략 위 아이콘들
                    .strategyName(inquiry.getStrategyName())
                    // 트레이더의 프로필 사진
                    .traderNickname(inquiry.getTraderNickname())

                    .inquiryContent(inquiry.getInquiryContent())

                    .answerTitle(answerTitle)
                    .answerRegistrationDate(answerRegistrationDate)
                    .answerContent(answerContent)

                    .previousTitle(previousInquiryTitle)
                    .previousWriteDate(previousInquiryWriteDate)
                    .nextTitle(nextInquiryTitle)
                    .nextWriteDate(nextInquiryWriteDate)
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(APIResponse.success(inquiryAnswerShowResponseDto));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
    }
}
