package com.example.analyticsservice.repository;

import com.example.analyticsservice.dto.CategoryRevenueDto;
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
}
