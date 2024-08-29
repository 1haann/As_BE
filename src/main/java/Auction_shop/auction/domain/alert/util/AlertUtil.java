package Auction_shop.auction.domain.alert.util;

import Auction_shop.auction.domain.alert.service.AlertService;
import Auction_shop.auction.sse.SSEConnection;
import Auction_shop.auction.web.dto.alert.AlertCreateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class AlertUtil {
    private final AlertService alertService;

    public void run(Long memberId, String content){
        AlertCreateDto createDto = AlertCreateDto.builder()
                .memberId(memberId)
                .content(content)
                .build();

        Long alertId = alertService.add(createDto);
        if(alertId == null) throw new RuntimeException("알림이 저장되지 않았습니다.");

        SSEConnection sseConnection = new SSEConnection();
        SseEmitter emitter = sseConnection.getEmitter(Long.toString(memberId));  // 상대방ID를 통해 상대방의 emitter를 가져옴
        sseConnection.sendEvent(emitter, content, null);  // SSE를 통해 상대방에게 알림
    }
}
