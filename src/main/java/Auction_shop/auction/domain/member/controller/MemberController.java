package Auction_shop.auction.domain.member.controller;

import Auction_shop.auction.domain.member.Member;
import Auction_shop.auction.domain.member.service.MemberService;
import Auction_shop.auction.web.dto.MemberResponseDto;
import Auction_shop.auction.web.dto.MemberUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponseDto> getByMemberId(@PathVariable("memberId") Long memberId){
        Member member = memberService.getById(memberId);
        return ResponseEntity.ok(MemberResponseDto.create(member));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponseDto> updateMember(@PathVariable("memberId") Long memberId,
                                                          @RequestBody MemberUpdateDto memberUpdateDto){
        Member member = memberService.updateMember(memberId, memberUpdateDto);
        return ResponseEntity.ok(MemberResponseDto.create(member));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable("memberId") Long memberId){
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }
}