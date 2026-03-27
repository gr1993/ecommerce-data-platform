package com.example.analyticsservice.repository;

import com.example.analyticsservice.dto.CategoryRevenueDto;
import com.example.analyticsservice.dto.ClaimStatsDto;
import com.example.analyticsservice.dto.ProductRevenueDto;
import com.example.analyticsservice.dto.RevenueTrendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StatsRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<CategoryRevenueDto> getCategoryRevenue(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                category_id, 
                category_name, 
                sum(daily_revenue) as revenue, 
                sum(daily_quantity) as quantity, 
                sum(order_count) as order_count
            FROM default.daily_category_revenue_mv
            WHERE sale_date BETWEEN ? AND ?
            GROUP BY category_id, category_name
            ORDER BY revenue DESC
            """;
            
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CategoryRevenueDto(
            rs.getLong("category_id"),
            rs.getString("category_name"),
            rs.getBigDecimal("revenue"),
            rs.getLong("quantity"),
            rs.getLong("order_count")
        ), startDate, endDate);
    }

    public List<ProductRevenueDto> getProductRevenue(LocalDate startDate, LocalDate endDate, int limit) {
        String sql = """
            SELECT 
                product_id, 
                product_name, 
                sum(daily_revenue) as revenue, 
                sum(daily_quantity) as quantity
            FROM default.daily_product_revenue_mv
            WHERE sale_date BETWEEN ? AND ?
            GROUP BY product_id, product_name
            ORDER BY revenue DESC
            LIMIT ?
            """;
            
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ProductRevenueDto(
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getBigDecimal("revenue"),
            rs.getLong("quantity")
        ), startDate, endDate, limit);
    }

    public List<RevenueTrendDto> getRevenueTrend(LocalDate startDate, LocalDate endDate, String period) {
        // ClickHouse의 날짜 변환 함수 활용
        String dateFunc = switch (period.toLowerCase()) {
            case "weekly" -> "toStartOfWeek(sale_date, 1)"; // 1: 월요일 시작
            case "monthly" -> "toStartOfMonth(sale_date)";
            default -> "sale_date";
        };

        String sql = String.format("""
            SELECT 
                formatDateTime(%s, '%%Y-%%m-%%d') as trend_date, 
                sum(daily_revenue) as revenue
            FROM default.daily_category_revenue_mv
            WHERE sale_date BETWEEN ? AND ?
            GROUP BY trend_date
            ORDER BY trend_date
            """, dateFunc);
            
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RevenueTrendDto(
            rs.getString("trend_date"),
            rs.getBigDecimal("revenue")
        ), startDate, endDate);
    }

    public List<ClaimStatsDto> getClaimStats(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                '반품' as type, 
                abs(sum(daily_claim_count)) as count, 
                abs(sum(daily_claim_amount)) as amount
            FROM default.daily_claim_stats_mv
            WHERE sale_date BETWEEN ? AND ?
            """;
            
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ClaimStatsDto(
            rs.getString("type"),
            rs.getLong("count"),
            rs.getBigDecimal("amount") == null ? java.math.BigDecimal.ZERO : rs.getBigDecimal("amount")
        ), startDate, endDate);
    }

    /**
     * 전체 누적 주문 수 (상태 무관, 중복 제거).
     * order_item_fact에서 날짜 제한 없이 distinct order_id를 카운트한다.
     */
    public long getTotalOrderCount() {
        String sql = """
            SELECT count(DISTINCT order_id)
            FROM default.order_item_fact
            """;
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }
}
