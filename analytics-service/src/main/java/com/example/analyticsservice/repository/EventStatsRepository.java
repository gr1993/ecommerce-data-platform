package com.example.analyticsservice.repository;

import com.example.analyticsservice.dto.LowStockDto;
import com.example.analyticsservice.dto.SignupStatsDto;
import com.example.analyticsservice.dto.VisitorStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 이벤트 기반 지표(page_viewed, user_registered, inventory_changed) 전담 Repository.
 * AggregatingMergeTree MV 조회 시 FINAL 키워드와 *Merge 함수를 반드시 사용해야 한다.
 */
@Repository
@RequiredArgsConstructor
public class EventStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    // -----------------------------------------------------------------------
    // 요약 지표 (Dashboard Summary용 단일 값 조회)
    // -----------------------------------------------------------------------

    /** 오늘 신규 가입자 수 */
    public long getTodaySignupCount() {
        String sql = """
                SELECT sum(signup_count)
                FROM default.daily_signup_stats_mv
                WHERE log_date = today()
                """;
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    /**
     * 재고 10개 미만 상품 수.
     * ReplacingMergeTree MV이므로 FINAL로 최신 상태만 읽는다.
     */
    public long getLowStockCount() {
        String sql = """
                SELECT count()
                FROM default.current_inventory_mv FINAL
                WHERE current_stock < 10
                """;
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    /**
     * 오늘 순 방문자 수 (Unique Visitors).
     * AggregatingMergeTree이므로 FINAL + uniqMerge() 사용.
     */
    public long getTodayVisitors() {
        String sql = """
                SELECT uniqMerge(unique_visitor_count)
                FROM default.daily_visitor_stats_mv FINAL
                WHERE log_date = today()
                """;
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    /** 이번 주(월~일) 순 방문자 수 */
    public long getWeekVisitors() {
        String sql = """
                SELECT uniqMerge(unique_visitor_count)
                FROM default.daily_visitor_stats_mv FINAL
                WHERE log_date >= toMonday(today())
                """;
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    // -----------------------------------------------------------------------
    // 추이 데이터 (차트용 기간 조회)
    // -----------------------------------------------------------------------

    /** 기간별 일별 신규 가입자 추이 */
    public List<SignupStatsDto> getDailySignupStats(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    formatDateTime(log_date, '%Y-%m-%d') AS date,
                    sum(signup_count)                    AS signup_count
                FROM default.daily_signup_stats_mv
                WHERE log_date BETWEEN ? AND ?
                GROUP BY log_date
                ORDER BY log_date
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SignupStatsDto(
                rs.getString("date"),
                rs.getLong("signup_count")
        ), startDate, endDate);
    }

    /** 기간별 일별 방문자 추이 (페이지뷰 + UV) */
    public List<VisitorStatsDto> getDailyVisitorStats(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    formatDateTime(log_date, '%Y-%m-%d') AS date,
                    countMerge(total_page_views)          AS total_page_views,
                    uniqMerge(unique_visitor_count)       AS unique_visitor_count
                FROM default.daily_visitor_stats_mv FINAL
                WHERE log_date BETWEEN ? AND ?
                GROUP BY log_date
                ORDER BY log_date
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new VisitorStatsDto(
                rs.getString("date"),
                rs.getLong("total_page_views"),
                rs.getLong("unique_visitor_count")
        ), startDate, endDate);
    }

    /** 재고 10개 미만 상품 목록 */
    public List<LowStockDto> getLowStockProducts() {
        String sql = """
                SELECT
                    product_id,
                    product_name,
                    current_stock
                FROM default.current_inventory_mv FINAL
                WHERE current_stock < 10
                ORDER BY current_stock ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LowStockDto(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getInt("current_stock")
        ));
    }
}
