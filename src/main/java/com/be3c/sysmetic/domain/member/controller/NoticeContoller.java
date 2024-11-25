package com.be3c.sysmetic.domain.member.controller;

import com.be3c.sysmetic.domain.member.dto.*;
import com.be3c.sysmetic.domain.member.entity.Inquiry;
import com.be3c.sysmetic.domain.member.entity.Notice;
import com.be3c.sysmetic.domain.member.service.NoticeService;
import com.be3c.sysmetic.global.common.response.APIResponse;
import com.be3c.sysmetic.global.common.response.ErrorCode;
import com.be3c.sysmetic.global.common.response.PageResponse;
import com.be3c.sysmetic.global.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
public class NoticeContoller implements NoticeControllerDocs {

    private final SecurityUtils securityUtils;

    private final NoticeService noticeService;

    private final Integer pageSize = 10; // 한 페이지 크기

    /*
        관리자 공지사항 등록 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항이 등록에 성공했을 때 : OK
        3. 공지사항이 등록에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
    @PostMapping("/admin/notice/write")
    public ResponseEntity<APIResponse<Long>> saveAdminNotice(
            @RequestBody NoticeSaveRequestDto noticeSaveRequestDto) {

        Long userId = securityUtils.getUserIdInSecurityContext();

        Long noticeId = noticeService.registerNotice(
                userId,
                noticeSaveRequestDto.getNoticeTitle(),
                noticeSaveRequestDto.getNoticeContent(),
                noticeSaveRequestDto.getIsAttatchment(),
                noticeSaveRequestDto.getIsOpen());

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(noticeId));
    }


    /*
        관리자 공지사항 조회 / 검색 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 데이터 조회에 성공했을 때 : OK
        3. 페이지 내에 한 개의 공지사항도 존재하지 않을 때 : NOT_FOUND
     */
    @Override
    @GetMapping("/admin/notice")
    public ResponseEntity<APIResponse<PageResponse<NoticeAdminListOneShowResponseDto>>> showAdminNotice(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "searchText", required = false) String searchText) {

        Page<Notice> noticeList = noticeService.findNoticeAdmin(searchType, searchText, page-1);

        List<NoticeAdminListOneShowResponseDto> noticeAdminDtoList = noticeList.stream()
                .map(NoticeContoller::noticeToNoticeAdminListOneShowResponseDto).collect(Collectors.toList());

        PageResponse<NoticeAdminListOneShowResponseDto> adminNoticePage = PageResponse.<NoticeAdminListOneShowResponseDto>builder()
                .currentPage(page)
                .pageSize(pageSize)
                .totalElement(noticeList.getTotalElements())
                .totalPages(noticeList.getTotalPages())
                .content(noticeAdminDtoList)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(adminNoticePage));
    }

    public static NoticeAdminListOneShowResponseDto noticeToNoticeAdminListOneShowResponseDto(Notice notice) {

        return NoticeAdminListOneShowResponseDto.builder()
                .noticeId(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .writerNickname(notice.getWriter().getNickname())
                .writeDate(notice.getWriteDate())
                .hits(notice.getHits())
                .isAttatchment(notice.getIsAttatchment())
                .isOpen(notice.getIsOpen())
                .build();
    }


    /*
        관리자 공지사항 목록 공개여부 수정 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공개여부 수정에 성공했을 때 : OK
        3. 공개여부 수정에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 해당 공지사항을 찾지 못했을 때 : NOT_FOUND
     */
    @Override
    @PutMapping("/admin/notice/{noticeId}/closed")
    public ResponseEntity<APIResponse<Long>> modifyNoticeClosed(
            @PathVariable Long noticeId) {

        noticeService.modifyNoticeClosed(noticeId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success());
    }


    /*
        관리자 공지사항 상세 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항의 상세 데이터 조회에 성공했을 때 : OK
        3. 공지사항의 상세 데이터 조회에 실패했을 때 : NOT_FOUND
     */
    @Override
    @GetMapping("/admin/notice/{noticeId}/view")
    public ResponseEntity<APIResponse<NoticeDetailAdminShowResponseDto>> showAdminNoticeDetail(
            @PathVariable Long noticeId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "searchText", required = false) String searchText) {

        noticeService.upHits(noticeId);

        Notice notice = noticeService.findNoticeById(noticeId);
        Notice previousNotice = noticeService.findNoticeById(noticeId-1);
        Notice nextNotice = noticeService.findNoticeById(noticeId+1);

        NoticeDetailAdminShowResponseDto noticeDetailAdminShowResponseDto = NoticeDetailAdminShowResponseDto.builder()
                .page(page)
                .searchType(searchType)
                .searchText(searchText)
                .noticeId(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .noticeContent(notice.getNoticeContent())
                .writeDate(notice.getWriteDate())
                .correctDate(notice.getCorrectDate())
                .writerNickname(notice.getWriter().getNickname())
                .hits(notice.getHits())
                .isAttatchment(notice.getIsAttatchment())
                .isOpen(notice.getIsOpen())
                .previousTitle(previousNotice.getNoticeTitle())
                .previousWriteDate(previousNotice.getWriteDate())
                .nextTitle(nextNotice.getNoticeTitle())
                .nextWriteDate(nextNotice.getWriteDate())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(noticeDetailAdminShowResponseDto));
    }


    /*
        관리자 공지사항 수정 화면 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 수정 화면 조회에 성공했을 때 : OK
        3. 공지사항 수정 화면 조회에 실패했을 때 : NOT_FOUND
     */
    @Override
    @GetMapping("/admin/notice/{noticeId}/modify")
    public ResponseEntity<APIResponse<NoticeShowModifyPageResponseDto>> showModifyAdminNotice(
            @PathVariable Long noticeId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "searchText", required = false) String searchText) {

        Notice notice = noticeService.findNoticeById(noticeId);

        NoticeShowModifyPageResponseDto noticeShowModifyPageResponseDto = NoticeShowModifyPageResponseDto.builder()
                .page(page)
                .searchType(searchType)
                .searchText(searchText)
                .noticeId(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .noticeContent(notice.getNoticeContent())
                .isAttatchment(notice.getIsAttatchment())
                .isOpen(notice.getIsOpen())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(noticeShowModifyPageResponseDto));
    }


    /*
        관리자 공지사항 수정 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 수정에 성공했을 때 : OK
        3. 공지사항 수정에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 해당 공지사항을 찾지 못했을 때 : NOT_FOUND
        5. 데이터의 형식이 올바르지 않음 : BAD_REQUEST
            +) 공지사항 수정 화면에 들어온 시간이 해당 공지사항 최종수정일시보다 작음
     */
    @Override
    @PutMapping("/admin/notice/{noticeId}/modify")
    public ResponseEntity<APIResponse<Long>> modifyAdminNotice(
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeModifyRequestDto noticeModifyRequestDto) {

        Long userId = securityUtils.getUserIdInSecurityContext();

        Notice notice = noticeService.findNoticeById(noticeId);

        if (noticeModifyRequestDto.getModifyInModifyPageTime().isAfter(notice.getCorrectDate())) {
            noticeService.modifyNotice(
                    noticeId,
                    noticeModifyRequestDto.getNoticeTitle(),
                    noticeModifyRequestDto.getNoticeContent(),
                    userId,
                    noticeModifyRequestDto.getIsAttatchment(),
                    noticeModifyRequestDto.getIsOpen());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "공지사항 수정 화면에 들어온 시간이 해당 공지사항 최종수정일시보다 작습니다."));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success());
    }


    /*
        관리자 공지사항 삭제 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 삭제에 성공했을 때 : OK
        3. 공지사항 삭제에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 해당 공지사항을 찾지 못했을 때 : NOT_FOUND
     */
    @Override
    @DeleteMapping("/admin/notice/{noticeId}/delete")
    public ResponseEntity<APIResponse<Long>> deleteAdminNotice(
            @PathVariable Long noticeId) {

        noticeService.deleteAdminNotice(noticeId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success());
    }


    /*
        관리자 공지사항 목록 삭제 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 목록 삭제에 성공했을 때 : OK
        3. 공지사항 목록 삭제에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 해당 공지사항을 찾지 못했을 때 : NOT_FOUND
        5. 공지사항 중 일부만 삭제에 실패했을 때 : MULTI_STATUS
     */
    @Override
    @DeleteMapping("/admin/notice/delete")
    public ResponseEntity<APIResponse<Integer>> deleteAdminNoticeList(
            @RequestBody @Valid NoticeListDeleteRequestDto noticeListDeleteRequestDto) {

        List<Long> noticeIdList = noticeListDeleteRequestDto.getNoticeIds();

        Integer deleteCount = noticeService.deleteAdminNoticeList(noticeIdList);

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(deleteCount));
    }


    /*
        공지사항 조회 / 검색 API
        1. 공지사항 데이터 조회에 성공했을 때 : OK
        2. 페이지 내에 한 개의 공지사항도 존재하지 않을 때 : NOT_FOUND
     */
    @Override
    @GetMapping("/notice")
    public ResponseEntity<APIResponse<PageResponse<NoticeListOneShowResponseDto>>> showNotice(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "searchText", required = false) String searchText) {

        Page<Notice> noticeList = noticeService.findNotice(searchText, page-1);

        List<NoticeListOneShowResponseDto> noticeDtoList = noticeList.stream()
                .map(NoticeContoller::noticeToNoticeListOneShowResponseDto).collect(Collectors.toList());

        PageResponse<NoticeListOneShowResponseDto> adminNoticePage = PageResponse.<NoticeListOneShowResponseDto>builder()
                .currentPage(page)
                .pageSize(pageSize)
                .totalElement(noticeList.getTotalElements())
                .totalPages(noticeList.getTotalPages())
                .content(noticeDtoList)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(adminNoticePage));
    }

    public static NoticeListOneShowResponseDto noticeToNoticeListOneShowResponseDto(Notice notice) {

        return NoticeListOneShowResponseDto.builder()
                .noticeId(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .writeDate(notice.getWriteDate())
                .isAttatchment(notice.getIsAttatchment())
                .build();
    }


    /*
        공지사항 상세 조회 API
        1. 공지사항의 상세 데이터 조회에 성공했을 때 : OK
        2. 공지사항의 상세 데이터 조회에 실패했을 때 : NOT_FOUND
     */
    @Override
    @GetMapping("/notice/{noticeId}/view")
    public ResponseEntity<APIResponse<NoticeDetailShowResponseDto>> showNoticeDetail(
            @PathVariable Long noticeId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "searchText", required = false) String searchText) {

        Notice notice = noticeService.findNoticeById(noticeId);
        Notice previousNotice = noticeService.findNoticeById(noticeId-1);
        Notice nextNotice = noticeService.findNoticeById(noticeId+1);

        NoticeDetailShowResponseDto noticeDetailShowResponseDto = NoticeDetailShowResponseDto.builder()
                .page(page)
                .searchText(searchText)
                .noticeId(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .noticeContent(notice.getNoticeContent())
                .writeDate(notice.getWriteDate())
                .correctDate(notice.getCorrectDate())
                .writerNickname(notice.getWriter().getNickname())
                .hits(notice.getHits())
                .isAttatchment(notice.getIsAttatchment())
                .isOpen(notice.getIsOpen())
                .previousTitle(previousNotice.getNoticeTitle())
                .previousWriteDate(previousNotice.getWriteDate())
                .nextTitle(nextNotice.getNoticeTitle())
                .nextWriteDate(nextNotice.getWriteDate())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(noticeDetailShowResponseDto));
    }
}