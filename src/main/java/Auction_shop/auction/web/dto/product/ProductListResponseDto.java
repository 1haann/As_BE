package Auction_shop.auction.web.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponseDto {

    private Long product_id;
    private String title;
    private boolean isSold;
    private Set<String> tradeTypes;
    private String tradeLocation;
    private int initial_price;
    private String imageUrl;
    private int likeCount;
}
