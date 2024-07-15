package Auction_shop.auction.security.auth.service;

import Auction_shop.auction.domain.member.Member;
import Auction_shop.auction.domain.member.service.MemberService;
import Auction_shop.auction.domain.refreshToken.RefreshToken;
import Auction_shop.auction.domain.refreshToken.repository.RefreshTokenRepository;
import Auction_shop.auction.security.jwt.JwtUtil;
import Auction_shop.auction.web.dto.AuthRequestDto;
import Auction_shop.auction.web.dto.AuthResponseDto;
import Auction_shop.auction.web.dto.MemberResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtUtil jwtUtil;
    private final MemberService memberService;
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${spring.jwt.accessToken_expiration_time}")
    private Long accessTokenExpiredMs;
    @Value("${spring.jwt.refreshToken_expiration_time}")
    private Long refreshTokenExpiredMs;
    public AuthResponseDto login(AuthRequestDto authRequestDto, HttpServletResponse response) {
        Member member = memberService.getByUuid(authRequestDto.getUuid());

        if(member == null){
            member = memberService.save(authRequestDto.getUuid());
        }

        MemberResponseDto memberResponseDto = MemberResponseDto.builder()
                .id(member.getId())
                .username(member.getUuid())
                .name(member.getName())
                .address(member.getAddress())
                .phone(member.getPhone())
                .point(member.getPoint())
                .role(member.getRole())
                .build();

        String accessToken = jwtUtil.createJwt("access", memberResponseDto, accessTokenExpiredMs);
        String refreshToken = jwtUtil.createJwt("refresh", memberResponseDto, refreshTokenExpiredMs);

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.addCookie(createCookie("refresh", refreshToken));

        RefreshToken token = RefreshToken.builder()
                .username(member.getUuid())
                .refreshToken(refreshToken)
                .build();

        refreshTokenRepository.save(token);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .available(member.isAvailable())
                .build();
    }

    public AuthResponseDto refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();

        if(cookies == null){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        for(Cookie cookie : cookies){
            if(cookie.getName().equals("refresh")){
                refreshToken = cookie.getValue();
                break;
            }
        }

        if(refreshToken == null){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        try {
            jwtUtil.isExpired(refreshToken);
        }
        catch (ExpiredJwtException e){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        String category = jwtUtil.getCategory(refreshToken);
        if(!category.equals("refresh")){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        if(!refreshTokenRepository.exist(refreshToken)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        refreshTokenRepository.delete(refreshToken);

        MemberResponseDto memberResponseDto = jwtUtil.getMemberResponseDto(refreshToken);

        String accessToken = jwtUtil.createJwt("access", memberResponseDto, accessTokenExpiredMs);
        refreshToken = jwtUtil.createJwt("refresh", memberResponseDto, refreshTokenExpiredMs);

        RefreshToken token = RefreshToken.builder()
                .username(memberResponseDto.getUsername())
                .refreshToken(refreshToken)
                .build();

        refreshTokenRepository.save(token);

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.addCookie(createCookie("refresh", refreshToken));

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .available(true)
                .build();
    }

    private Cookie createCookie(String key, String value){
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "None");
        cookie.setPath("/");
        cookie.setSecure(true);
        return cookie;
    }
}
