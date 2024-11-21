package com.be3c.sysmetic.domain.member.dto.inquiry;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SaveInquiryDetailRequestDto {

    private Long inquiryId;
    private String answerContent;
}