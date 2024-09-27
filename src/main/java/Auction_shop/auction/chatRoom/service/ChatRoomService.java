package Auction_shop.auction.chatRoom.service;

import Auction_shop.auction.chat.dto.ChatDto;
import Auction_shop.auction.chatRoom.domain.ChatRoom;
import Auction_shop.auction.chatRoom.dto.ChatRoomListResponseDto;

import java.util.List;
import java.util.Optional;

public interface ChatRoomService {
    List<ChatRoomListResponseDto> findChatRoomsByUserId(Long userId);
    Optional<ChatRoom> findChatRoomInfo(Long userId, Long yourId, Long postId);
    Long createNewChatRoom(Long userId, Long yourId, Long postId);
    List<ChatDto> fetchChatLog(Long roomId);
}
