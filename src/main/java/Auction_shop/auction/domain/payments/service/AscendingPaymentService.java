package Auction_shop.auction.domain.payments.service;

import Auction_shop.auction.domain.bid.Bid;
import Auction_shop.auction.domain.bid.repository.BidJpaRepository;
import Auction_shop.auction.domain.bid.repository.BidRedisRepository;
import Auction_shop.auction.domain.member.Member;
import Auction_shop.auction.domain.member.service.MemberService;
import Auction_shop.auction.domain.payments.Payments;
import Auction_shop.auction.domain.payments.repository.PaymentsRepository;
import Auction_shop.auction.domain.product.Product;
import Auction_shop.auction.domain.product.ProductDocument;
import Auction_shop.auction.domain.product.elasticRepository.ProductElasticsearchRepository;
import Auction_shop.auction.domain.product.repository.ProductJpaRepository;
import Auction_shop.auction.web.dto.product.ProductMapper;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AscendingPaymentService {

    private IamportClient iamportClient;

    private final PaymentsRepository paymentsRepository;
    private final BidRedisRepository bidRedisRepository;
    private final BidJpaRepository bidJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductElasticsearchRepository productElasticsearchRepository;
    private final ProductMapper productMapper;
    private final MemberService memberService;

    @Value("${iamport.key}")
    private String apiKey;

    @Value("${iamport.secret}")
    private String secretKey;

    @PostConstruct
    public void AscendingPaymentService(){
        this.iamportClient = new IamportClient(apiKey, secretKey);
    }

    @Transactional
    public String PaymentsVerify(String impUid, Long productId, Long memberId) throws IamportResponseException, IOException {

        Product product = productJpaRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(productId + "에 해당하는 물건이 없습니다."));

        // Iamport로부터 결제 정보 확인
        IamportResponse<Payment> iamportResponse;
        try {
            iamportResponse = iamportClient.paymentByImpUid(impUid);
        } catch (Exception e) {
            throw new RuntimeException("결제 정보를 조회하는 데 실패: " + e.getMessage());
        }

        String merchantUid = iamportResponse.getResponse().getMerchantUid();
        int bidAmount = iamportResponse.getResponse().getAmount().intValue();
        // 상품의 현재 가격 확인
        int currentPrice = product.getCurrent_price();

        // 새로운 입찰 금액이 기존 가격보다 낮으면 결제 취소
        if (bidAmount <= currentPrice) {
            cancelPayment(impUid);
            return "지불한 금액 : " + bidAmount + "가 현재 가격 : " + currentPrice + "보다 높지 않음.";
        }

        // 이전 입찰자 결제 취소
        Payments existingPayment = paymentsRepository.findTopByProductIdOrderByCreatedAtDesc(productId).orElse(null);
        if (existingPayment != null) {
            cancelPayment(existingPayment.getImpUid());
            paymentsRepository.delete(existingPayment);
        }

        // 새로운 결제 저장
        Member member = memberService.getById(memberId);
        Payments newPayment = Payments.builder()
                .paymentMethod(iamportResponse.getResponse().getPayMethod())
                .amount(bidAmount)
                .merchantUid(merchantUid)
                .impUid(impUid)
                .member(member)
                .product(product)
                .build();
        paymentsRepository.save(newPayment);

        // 상품 가격 업데이트
        product.bidProduct(bidAmount);

        Bid bid = Bid.builder()
                .productId(productId)
                .memberId(memberId)
                .amount(bidAmount)
                .bidTime(LocalDateTime.now())
                .build();

        bidRedisRepository.save(bid);
        bidJpaRepository.save(bid);

        productJpaRepository.save(product);
        ProductDocument document = productMapper.toDocument(product);
        productElasticsearchRepository.save(document);

        return "결제가 완료되었습니다. 새로운 입찰 금액: " + bidAmount;
    }

    private void cancelPayment(String impUid) {
        try {
            CancelData cancelData = new CancelData(impUid, true);
            cancelData.setReason("상향식 경매에서 더 높은 입찰이 발생해 결제 취소");

            // Iamport API를 호출하여 결제를 취소
            IamportResponse<Payment> response = iamportClient.cancelPaymentByImpUid(cancelData);

            if (response.getCode() == 0) {
                System.out.println("결제가 취소되었습니다: " + impUid);
            } else {
                System.err.println("결제 취소 오류: " + response.getMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException("결제 취소 실패: " + e.getMessage());
        }
    }
}