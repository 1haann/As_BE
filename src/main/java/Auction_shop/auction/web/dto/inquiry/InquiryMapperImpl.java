package Auction_shop.auction.web.dto.inquiry;

import Auction_shop.auction.domain.inquriy.Inquiry;
import Auction_shop.auction.domain.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryMapperImpl implements InquiryMapper{

    @Override
    public InquiryResponseDto toResponseDto(Inquiry inquiry) {
        InquiryResponseDto responseDto = new InquiryResponseDto().builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.isStatus())
                .imageUrls(inquiry.getImageUrls())
                .build();
        return responseDto;
    }

    @Override
    public InquiryListResponseDto toListResponseDto(Inquiry inquiry){

        String imageUrl = null;
        if(!inquiry.getImageList().isEmpty()){
            imageUrl = inquiry.getImageUrls().get(0);
        }

        InquiryListResponseDto responseDto = InquiryListResponseDto.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .imageUrl(imageUrl)
                .status(inquiry.isStatus())
                .build();
        return responseDto;
    }

    @Override
    public Inquiry toEntity(InquiryCreateDto inquiryDto, Member member) {
        Inquiry inquiry = Inquiry.builder()
                .member(member)
                .title(inquiryDto.getTitle())
                .content(inquiryDto.getContent())
                .status(false)
                .build();
        return inquiry;
    }
}