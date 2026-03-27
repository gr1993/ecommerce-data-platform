package com.example.eventbot.service;

import com.example.eventbot.domain.event.UserRegisteredEvent;
import com.example.eventbot.domain.event.InventoryChangedEvent;
import com.example.eventbot.domain.event.PageViewedEvent;
import com.example.eventbot.dto.response.EventGeneratorResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventGeneratorService {
    private final KafkaProducerService kafkaProducerService;
    private final Random random = new Random();
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 상품 마스터 데이터
    @Getter
    @AllArgsConstructor
    private static class ProductMaster {
        private final Long productId;
        private final String productName;
    }

    private static final List<ProductMaster> PRODUCT_MASTERS = Arrays.asList(
        new ProductMaster(1001L, "오버핏 맨투맨"),
        new ProductMaster(1002L, "슬림핏 슬랙스"),
        new ProductMaster(1003L, "데일리 캔버스화"),
        new ProductMaster(2001L, "노이즈캔슬링 헤드폰"),
        new ProductMaster(2002L, "4K 게이밍 모니터"),
        new ProductMaster(2003L, "기계식 키보드"),
        new ProductMaster(3001L, "유기농 샐러드 도시락"),
        new ProductMaster(3002L, "프리미엄 원두 커피"),
        new ProductMaster(3003L, "닭가슴살 패키지"),
        new ProductMaster(4001L, "수분 진정 크림"),
        new ProductMaster(4002L, "롱래스팅 립스틱"),
        new ProductMaster(4003L, "저자극 클렌징 폼"),
        new ProductMaster(5001L, "요가 매트 8mm"),
        new ProductMaster(5002L, "경량 러닝화"),
        new ProductMaster(5003L, "단백질 쉐이커")
    );

    private static final List<String> PAGE_URLS = Arrays.asList(
        "/products/category/fashion",
        "/products/category/electronics",
        "/products/category/food",
        "/products/category/beauty",
        "/products/category/sports",
        "/products/1001",
        "/products/1002",
        "/products/2001",
        "/products/2002",
        "/products/3001",
        "/products/4001",
        "/products/5002",
        "/cart",
        "/checkout",
        "/mypage/orders",
        "/search?q=헤드폰",
        "/search?q=운동화",
        "/",
        "/events/spring-sale",
        "/best-sellers"
    );
    
    // UI 한글 라벨 매핑을 위해 ConcurrentHashMap 사용
    private final Map<String, Long> eventCounts = new ConcurrentHashMap<>();

    private static final String REG_LABEL = "신규 회원 가입 (user_registered)";
    private static final String INV_LABEL = "재고 변동 알림 (inventory_changed)";
    private static final String PAGE_LABEL = "방문자 수 (page_viewed)";

    {
        eventCounts.put(REG_LABEL, 0L);
        eventCounts.put(INV_LABEL, 0L);
        eventCounts.put(PAGE_LABEL, 0L);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            new Thread(this::generateEvents).start();
            log.info("[이벤트] 랜덤 이벤트 발생기 시작");
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("[이벤트] 랜덤 이벤트 발생기 중단 요청됨");
        }
    }

    public EventGeneratorResponse getStatus() {
        // 순서 보장을 위해 LinkedHashMap으로 반환
        Map<String, Long> sortedCounts = new LinkedHashMap<>();
        sortedCounts.put(REG_LABEL, eventCounts.get(REG_LABEL));
        sortedCounts.put(INV_LABEL, eventCounts.get(INV_LABEL));
        sortedCounts.put(PAGE_LABEL, eventCounts.get(PAGE_LABEL));
        
        return EventGeneratorResponse.builder()
                .running(running.get())
                .counts(sortedCounts)
                .build();
    }

    private void generateEvents() {
        try {
            while (running.get()) {
                // 초당 1~5개의 이벤트를 랜덤하게 발생
                int count = random.nextInt(5) + 1;
                for (int i = 0; i < count; i++) {
                    if (!running.get()) break;
                    
                    int type = random.nextInt(3);
                    if (type == 0) { // 회원가입
                        UserRegisteredEvent event = UserRegisteredEvent.builder()
                                .userId(random.nextLong(10000) + 1)
                                .email("user" + random.nextInt(10000) + "@example.com")
                                .signupSource(random.nextBoolean() ? "WEB" : "MOBILE")
                                .registeredAt(LocalDateTime.now())
                                .build();
                        kafkaProducerService.publishUserRegistered(event);
                        eventCounts.merge(REG_LABEL, 1L, Long::sum);
                    } else if (type == 1) { // 재고변동
                        ProductMaster product = PRODUCT_MASTERS.get(random.nextInt(PRODUCT_MASTERS.size()));
                        InventoryChangedEvent event = InventoryChangedEvent.builder()
                                .productId(product.getProductId())
                                .productName(product.getProductName())
                                .changeAmount(random.nextInt(10) - 5) // -5 ~ +4
                                .currentStock(random.nextInt(100) + 10)
                                .changedAt(LocalDateTime.now())
                                .build();
                        kafkaProducerService.publishInventoryChanged(event);
                        eventCounts.merge(INV_LABEL, 1L, Long::sum);
                    } else { // 페이지뷰
                        PageViewedEvent event = PageViewedEvent.builder()
                                .userId(random.nextLong(10000) + 1)
                                .pageUrl(PAGE_URLS.get(random.nextInt(PAGE_URLS.size())))
                                .userAgent("Mozilla/5.0")
                                .viewedAt(LocalDateTime.now())
                                .build();
                        kafkaProducerService.publishPageViewed(event);
                        eventCounts.merge(PAGE_LABEL, 1L, Long::sum);
                    }
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            log.warn("[이벤트] 이벤트 발생기 쓰레드 중단됨.");
            Thread.currentThread().interrupt();
        } finally {
            running.set(false);
            log.info("[이벤트] 랜덤 이벤트 발생기 종료");
        }
    }
}
