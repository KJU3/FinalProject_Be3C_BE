package com.be3c.sysmetic.domain.member.controller;

import com.be3c.sysmetic.domain.member.dto.*;
import com.be3c.sysmetic.domain.member.entity.Notice;
import com.be3c.sysmetic.domain.member.service.NoticeService;
import com.be3c.sysmetic.global.common.response.APIResponse;
import com.be3c.sysmetic.global.common.response.ErrorCode;
import com.be3c.sysmetic.global.common.response.PageResponse;
import com.be3c.sysmetic.global.common.response.SuccessCode;
import com.be3c.sysmetic.global.util.SecurityUtils;
import com.be3c.sysmetic.global.util.file.dto.FileReferenceType;
import com.be3c.sysmetic.global.util.file.dto.FileRequest;
import com.be3c.sysmetic.global.util.file.dto.FileWithInfoResponse;
import com.be3c.sysmetic.global.util.file.service.FileService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
public class NoticeContoller implements NoticeControllerDocs {

    private final SecurityUtils securityUtils;

    private final NoticeService noticeService;
    private final FileService fileService;

    private final Integer pageSize = 10; // 한 페이지 크기

    /*
        관리자 공지사항 등록 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항이 등록에 성공했을 때 : OK
        3. 공지사항이 등록에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 데이터의 형식이 올바르지 않음 : BAD_REQUEST
        5. 등록하는 관리자 정보를 찾지 못했을 때 : NOT_FOUND
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/notice")
    public ResponseEntity<APIResponse<Long>> saveAdminNotice(
            @RequestPart(value = "NoticeSaveRequestDto") @Valid NoticeSaveRequestDto noticeSaveRequestDto,
            @RequestPart(value = "fileList", required = false) List<MultipartFile> fileList,
            @RequestPart(value = "imageList", required = false) List<MultipartFile> imageList) {

        if(fileList.size() > 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "등록하려는 파일이 3개 초과입니다."));
        }

        if(imageList.size() > 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "등록하려는 이미지가 5개 초과입니다."));
        }

        try {
            Long userId = securityUtils.getUserIdInSecurityContext();

            if (noticeService.registerNotice(
                    userId,
                    noticeSaveRequestDto.getNoticeTitle(),
                    noticeSaveRequestDto.getNoticeContent(),
                    noticeSaveRequestDto.getFileExists(),
                    noticeSaveRequestDto.getImageExists(),
                    noticeSaveRequestDto.getIsOpen(),
                    fileList,
                    imageList)) {

                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND, e.getMessage()));
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, e.getMessage()));
        }
    }


    /*
        관리자 공지사항 조회 / 검색 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 데이터 조회에 성공했을 때 : OK
        3. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/notice")
    public ResponseEntity<APIResponse<PageResponse<NoticeAdminListOneShowResponseDto>>> showAdminNotice(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "searchType", required = false, defaultValue = "title") String searchType,
            @RequestParam(value = "searchText", required = false) String searchText) {

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        if (!(searchType.equals("title") || searchType.equals("content") || searchType.equals("all") || searchType.equals("writer"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 searchType이 올바르지 않습니다."));
        }

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
                .writerNickname(notice.getWriterNickname())
                .writeDate(notice.getWriteDate())
                .hits(notice.getHits())
                .fileExist(notice.getFileExists())
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
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/admin/notice/{noticeId}/open-close")
    public ResponseEntity<APIResponse<Long>> modifyNoticeClosed(
            @PathVariable(name="noticeId") Long noticeId) {

        try {

            if (noticeService.modifyNoticeClosed(noticeId)) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND));
        }
    }


    /*
        관리자 공지사항 상세 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항의 상세 데이터 조회에 성공했을 때 : OK
        3. 해당 공지사항을 찾지 못했을 때 : NOT_FOUND
        4. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/notice/{noticeId}")
    public ResponseEntity<APIResponse<NoticeDetailAdminShowResponseDto>> showAdminNoticeDetail(
            @PathVariable(name="noticeId") Long noticeId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "searchType", required = false, defaultValue = "title") String searchType,
            @RequestParam(value = "searchText", required = false) String searchText) {

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        if (!(searchType.equals("title") || searchType.equals("content") || searchType.equals("all") || searchType.equals("writer"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 searchType이 올바르지 않습니다."));
        }

        try {

            Notice notice = noticeService.findNoticeById(noticeId);
            String previousNoticeTitle = noticeService.findPreviousNoticeTitle(noticeId);
            LocalDateTime previousNoticeWriteDate = noticeService.findPreviousNoticeWriteDate(noticeId);
            String nextNoticeTitle = noticeService.findNextNoticeTitle(noticeId);
            LocalDateTime nextNoticeWriteDate = noticeService.findNextNoticeWriteDate(noticeId);

            List<NoticeDetailFileShowResponseDto> fileDtoList = new ArrayList<>();
            if (notice.getFileExists()) {
                List<FileWithInfoResponse> fileList = fileService.getFileWithInfos(new FileRequest(FileReferenceType.NOTICE_BOARD_FILE, notice.getId()));

                for (FileWithInfoResponse f : fileList) {
                    NoticeDetailFileShowResponseDto noticeDetailFileShowResponseDto = NoticeDetailFileShowResponseDto.builder()
                            .fileId(f.id())
                            .fileSize(f.fileSize())
                            .originalName(f.originalName())
                            .path(f.url())
                            .build();
                    fileDtoList.add(noticeDetailFileShowResponseDto);
                }
            }

            List<NoticeDetailImageShowResponseDto> imageDtoList = new ArrayList<>();
            if (notice.getImageExists()) {
                List<FileWithInfoResponse> imageList = fileService.getFileWithInfos(new FileRequest(FileReferenceType.NOTICE_BOARD_IMAGE, notice.getId()));

                for (FileWithInfoResponse f : imageList) {
                    NoticeDetailImageShowResponseDto noticeDetailImageShowResponseDto = NoticeDetailImageShowResponseDto.builder()
                            .fileId(f.id())
                            .path(f.url())
                            .build();
                    imageDtoList.add(noticeDetailImageShowResponseDto);
                }
            }

            NoticeDetailAdminShowResponseDto noticeDetailAdminShowResponseDto = NoticeDetailAdminShowResponseDto.builder()
                    .page(page)
                    .searchType(searchType)
                    .searchText(searchText)
                    .noticeId(notice.getId())
                    .noticeTitle(notice.getNoticeTitle())
                    .noticeContent(notice.getNoticeContent())
                    .writeDate(notice.getWriteDate())
                    .correctDate(notice.getCorrectDate())
                    .writerNickname(notice.getWriterNickname())
                    .hits(notice.getHits())
                    .fileExist(notice.getFileExists())
                    .imageExist(notice.getImageExists())
                    .isOpen(notice.getIsOpen())
                    .fileDtoList(fileDtoList)
                    .imageDtoList(imageDtoList)
                    .previousTitle(previousNoticeTitle)
                    .previousWriteDate(previousNoticeWriteDate)
                    .nextTitle(nextNoticeTitle)
                    .nextWriteDate(nextNoticeWriteDate)
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(APIResponse.success(noticeDetailAdminShowResponseDto));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND));
        }
    }


    /*
        관리자 공지사항 수정 화면 조회 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 수정 화면 조회에 성공했을 때 : OK
        3. 공지사항 수정 화면 조회에 실패했을 때 : NOT_FOUND
        4. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/notice/{noticeId}/modify-page")
    public ResponseEntity<APIResponse<NoticeShowModifyPageResponseDto>> showModifyAdminNotice(
            @PathVariable(name="noticeId") Long noticeId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "searchType", required = false, defaultValue = "title") String searchType,
            @RequestParam(value = "searchText", required = false) String searchText) {

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        if (!(searchType.equals("title") || searchType.equals("content") || searchType.equals("all") || searchType.equals("writer"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "쿼리 파라미터 searchType이 올바르지 않습니다."));
        }

        try {
            Notice notice = noticeService.findNoticeById(noticeId);

            List<NoticeDetailFileShowResponseDto> fileDtoList = new ArrayList<>();
            if (notice.getFileExists()) {
                List<FileWithInfoResponse> fileList = fileService.getFileWithInfos(new FileRequest(FileReferenceType.NOTICE_BOARD_FILE, notice.getId()));

                for (FileWithInfoResponse f : fileList) {
                    NoticeDetailFileShowResponseDto noticeDetailFileShowResponseDto = NoticeDetailFileShowResponseDto.builder()
                            .fileId(f.id())
                            .fileSize(f.fileSize())
                            .originalName(f.originalName())
                            .path(f.url())
                            .build();
                    fileDtoList.add(noticeDetailFileShowResponseDto);
                }
            }

            List<NoticeDetailImageShowResponseDto> imageDtoList = new ArrayList<>();
            if (notice.getImageExists()) {
                List<FileWithInfoResponse> imageList = fileService.getFileWithInfos(new FileRequest(FileReferenceType.NOTICE_BOARD_IMAGE, notice.getId()));

                for (FileWithInfoResponse f : imageList) {
                    NoticeDetailImageShowResponseDto noticeDetailImageShowResponseDto = NoticeDetailImageShowResponseDto.builder()
                            .fileId(f.id())
                            .path(f.url())
                            .build();
                    imageDtoList.add(noticeDetailImageShowResponseDto);
                }
            }

            NoticeShowModifyPageResponseDto noticeShowModifyPageResponseDto = NoticeShowModifyPageResponseDto.builder()
                    .page(page)
                    .searchType(searchType)
                    .searchText(searchText)
                    .noticeId(notice.getId())
                    .noticeTitle(notice.getNoticeTitle())
                    .noticeContent(notice.getNoticeContent())
                    .fileExist(notice.getFileExists())
                    .imageExist(notice.getImageExists())
                    .isOpen(notice.getIsOpen())
                    .fileDtoList(fileDtoList)
                    .imageDtoList(imageDtoList)
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(APIResponse.success(noticeShowModifyPageResponseDto));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND));
        }
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
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/admin/notice/{noticeId}")
    public ResponseEntity<APIResponse<Long>> modifyAdminNotice(
            @PathVariable(name="noticeId") Long noticeId,
            @RequestPart(value = "NoticeModifyRequestDto") @Valid NoticeModifyRequestDto noticeModifyRequestDto,
            @RequestPart(value = "newFileList", required = false) List<MultipartFile> newFileList,
            @RequestPart(value = "newImageList", required = false) List<MultipartFile> newImageList) {

//        LocalDateTime modifyInModifyPageTime = noticeModifyRequestDto.getModifyInModifyPageTime();
//        if (modifyInModifyPageTime == null) {
//            String str = "3000-11-05 13:47:13.248";
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
//            modifyInModifyPageTime = LocalDateTime.parse(str, formatter);
//        }

        try {
            Long userId = securityUtils.getUserIdInSecurityContext();

            if (noticeService.modifyNotice(
                    noticeId,
                    noticeModifyRequestDto.getNoticeTitle(),
                    noticeModifyRequestDto.getNoticeContent(),
                    userId,
                    noticeModifyRequestDto.getFileExists(),
                    noticeModifyRequestDto.getImageExists(),
                    noticeModifyRequestDto.getIsOpen(),
                    noticeModifyRequestDto.getExistFileDtoList(),
                    noticeModifyRequestDto.getExistImageDtoList(),
                    newFileList,
                    newImageList)) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));

//            if (modifyInModifyPageTime.isBefore(notice.getCorrectDate())) {
//            } else {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "공지사항 수정 화면에 들어온 시간이 해당 공지사항 최종수정일시보다 작습니다."));
//            }
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND));
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST));
        }
    }


    /*
        관리자 공지사항 삭제 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 삭제에 성공했을 때 : OK
        3. 공지사항 삭제에 실패했을 때 : INTERNAL_SERVER_ERROR
        4. 해당 공지사항을 찾지 못했을 때 : NOT_FOUND
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/admin/notice/{noticeId}")
    public ResponseEntity<APIResponse<Long>> deleteAdminNotice(
            @PathVariable(name="noticeId") Long noticeId) {

        try {
            if (noticeService.deleteAdminNotice(noticeId)) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND));
        }
    }


    /*
        관리자 공지사항 목록 삭제 API
        1. 사용자 인증 정보가 없음 : FORBIDDEN
        2. 공지사항 목록 삭제에 성공했을 때 : OK
        3. 해당 공지사항을 찾지 못했을 때 : NOT_FOUND
        4. 공지사항 중 삭제에 실패했을 때 : MULTI_STATUS
     */
    @Override
//    @PreAuthorize("hasRole('ROLE_USER_MANAGER') or hasRole('ROLE_TRADER_MANAGER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/admin/notice")
    public ResponseEntity<APIResponse<Map<Long, String>>> deleteAdminNoticeList(
            @RequestBody @Valid NoticeListDeleteRequestDto noticeListDeleteRequestDto) {

        List<Long> noticeIdList = noticeListDeleteRequestDto.getNoticeIds();

        try {
            Map<Long, String> deleteResult = noticeService.deleteAdminNoticeList(noticeIdList);

            if (deleteResult.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(APIResponse.success());
            }
            return ResponseEntity.status(HttpStatus.MULTI_STATUS)
                    .body(APIResponse.success(SuccessCode.OK, deleteResult));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND));
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, e.getMessage()));
        }
    }


    /*
        공지사항 조회 / 검색 API
        1. 공지사항 데이터 조회에 성공했을 때 : OK
        2. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
    @GetMapping("/notice")
    public ResponseEntity<APIResponse<PageResponse<NoticeListOneShowResponseDto>>> showNotice(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "searchText", required = false) String searchText) {

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

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
                .fileExists(notice.getFileExists())
                .build();
    }


    /*
        공지사항 상세 조회 API
        1. 공지사항의 상세 데이터 조회에 성공했을 때 : OK
        2. 공지사항의 상세 데이터 조회에 실패했을 때 : NOT_FOUND
        3. 파라미터 데이터의 형식이 올바르지 않음 : BAD_REQUEST
     */
    @Override
    @GetMapping("/notice/{noticeId}")
    public ResponseEntity<APIResponse<NoticeDetailShowResponseDto>> showNoticeDetail(
            @PathVariable(name="noticeId") Long noticeId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "searchText", required = false) String searchText) {

        if (page <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.fail(ErrorCode.BAD_REQUEST, "페이지가 1 이하입니다."));
        }

        try {
            noticeService.upHits(noticeId);

            Notice notice = noticeService.findNoticeById(noticeId);
            String previousNoticeTitle = noticeService.findPreviousNoticeTitle(noticeId);
            LocalDateTime previousNoticeWriteDate = noticeService.findPreviousNoticeWriteDate(noticeId);
            String nextNoticeTitle = noticeService.findNextNoticeTitle(noticeId);
            LocalDateTime nextNoticeWriteDate = noticeService.findNextNoticeWriteDate(noticeId);

            List<NoticeDetailFileShowResponseDto> fileDtoList = new ArrayList<>();
            if (notice.getFileExists()) {
                List<FileWithInfoResponse> fileList = fileService.getFileWithInfos(new FileRequest(FileReferenceType.NOTICE_BOARD_FILE, notice.getId()));

                for (FileWithInfoResponse f : fileList) {
                    NoticeDetailFileShowResponseDto noticeDetailFileShowResponseDto = NoticeDetailFileShowResponseDto.builder()
                            .fileId(f.id())
                            .fileSize(f.fileSize())
                            .originalName(f.originalName())
                            .path(f.url())
                            .build();
                    fileDtoList.add(noticeDetailFileShowResponseDto);
                }
            }

            List<NoticeDetailImageShowResponseDto> imageDtoList = new ArrayList<>();
            if (notice.getImageExists()) {
                List<FileWithInfoResponse> imageList = fileService.getFileWithInfos(new FileRequest(FileReferenceType.NOTICE_BOARD_IMAGE, notice.getId()));

                for (FileWithInfoResponse f : imageList) {
                    NoticeDetailImageShowResponseDto noticeDetailImageShowResponseDto = NoticeDetailImageShowResponseDto.builder()
                            .fileId(f.id())
                            .path(f.url())
                            .build();
                    imageDtoList.add(noticeDetailImageShowResponseDto);
                }
            }

            NoticeDetailShowResponseDto noticeDetailShowResponseDto = NoticeDetailShowResponseDto.builder()
                    .page(page)
                    .searchText(searchText)
                    .noticeId(notice.getId())
                    .noticeTitle(notice.getNoticeTitle())
                    .noticeContent(notice.getNoticeContent())
                    .writeDate(notice.getWriteDate())
                    .fileDtoList(fileDtoList)
                    .imageDtoList(imageDtoList)
                    .previousTitle(previousNoticeTitle)
                    .previousWriteDate(previousNoticeWriteDate)
                    .nextTitle(nextNoticeTitle)
                    .nextWriteDate(nextNoticeWriteDate)
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(APIResponse.success(noticeDetailShowResponseDto));
        }
        catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.fail(ErrorCode.NOT_FOUND));
        }
    }
}