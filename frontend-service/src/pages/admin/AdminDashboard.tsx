import { useState, useEffect, useRef, useCallback } from 'react'
import { Card, Row, Col, Statistic, Badge, Select, Spin, Alert, Tooltip } from 'antd'
import { ShoppingOutlined, DollarOutlined, UserAddOutlined, WarningOutlined, EyeOutlined, SyncOutlined } from '@ant-design/icons'
import { Column, Line } from '@ant-design/charts'
import { analyticsApi, type DashboardSummary, type RevenueTrend } from '../../api/analyticsApi'
import './AdminDashboard.css'

const { Option } = Select

/** 요약 카드 폴링 간격 (ms) */
const SUMMARY_POLL_MS = 1_000   // 1초
/** 차트 데이터 폴링 간격 (ms) */
const CHART_POLL_MS  = 10_000   // 10초

// ---- 날짜 helpers ----
const fmt = (d: Date) => d.toISOString().slice(0, 10)
const today = fmt(new Date())
const monthStart = fmt(new Date(new Date().getFullYear(), new Date().getMonth(), 1))
const currentMonth = new Date().getMonth() + 1  // 1-indexed (예: 3)

// ---- 차트용 로컬 타입 ----
interface PopularProduct {
  product_name: string
  sales_count: number
}

function AdminDashboard() {
  const [stats, setStats] = useState<DashboardSummary>({
    total_orders: 0,
    daily_revenue: 0,
    weekly_revenue: 0,
    monthly_revenue: 0,
    new_members: 0,
    low_stock_count: 0,
    critical_product_name: '-',
    critical_product_stock: 0,
    today_visitors: 0,
    week_visitors: 0,
  })
  const [popularProducts, setPopularProducts] = useState<PopularProduct[]>([])
  const [revenueTrend, setRevenueTrend] = useState<RevenueTrend[]>([])
  const [topN, setTopN] = useState<number>(5)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)
  const [syncing, setSyncing] = useState(false)

  // ---- 요약 카드 fetch (30초 폴링) ----
  const fetchSummary = useCallback(async (isInitial = false) => {
    if (!isInitial) setSyncing(true)
    try {
      const summary = await analyticsApi.getDashboardSummary()
      setStats(summary)
      setLastUpdated(new Date())
    } catch (err) {
      if (isInitial) {
        setError(err instanceof Error ? err.message : '요약 데이터를 불러오는데 실패했습니다.')
      } else {
        console.warn('[Dashboard] 요약 폴링 실패 (이전 데이터 유지):', err)
      }
    } finally {
      if (!isInitial) setSyncing(false)
    }
  }, [])

  // ---- 차트 데이터 fetch (10초 폴링) ----
  const fetchCharts = useCallback(async (isInitial = false) => {
    try {
      const [trend, products] = await Promise.all([
        analyticsApi.getRevenueTrend(monthStart, today, 'daily'),
        analyticsApi.getDashboardPopularProducts(),
      ])
      setRevenueTrend(trend.map((r) => ({ date: r.date, revenue: Number(r.revenue) })))
      setPopularProducts(
        products.map((p) => ({
          product_name: p.product_name,
          sales_count: p.quantity,
        }))
      )
    } catch (err) {
      if (isInitial) {
        setError(err instanceof Error ? err.message : '차트 데이터를 불러오는데 실패했습니다.')
      } else {
        console.warn('[Dashboard] 차트 폴링 실패 (이전 데이터 유지):', err)
      }
    }
  }, [])

  // ---- 최초 로딩 ----
  useEffect(() => {
    const initialLoad = async () => {
      setLoading(true)
      setError(null)
      await Promise.all([fetchSummary(true), fetchCharts(true)])
      setLastUpdated(new Date())
      setLoading(false)
    }
    initialLoad()
  }, [fetchSummary, fetchCharts])

  // ---- 요약 폴링 (30초) ----
  const summaryTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  useEffect(() => {
    summaryTimerRef.current = setInterval(() => fetchSummary(false), SUMMARY_POLL_MS)
    return () => {
      if (summaryTimerRef.current) clearInterval(summaryTimerRef.current)
    }
  }, [fetchSummary])

  // ---- 차트 폴링 (2분) ----
  const chartTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  useEffect(() => {
    chartTimerRef.current = setInterval(() => fetchCharts(false), CHART_POLL_MS)
    return () => {
      if (chartTimerRef.current) clearInterval(chartTimerRef.current)
    }
  }, [fetchCharts])



  const popularProductsConfig = {
    data: popularProducts.slice(0, topN),
    xField: 'product_name',
    yField: 'sales_count',
    seriesField: 'product_name',
    legend: false,
    padding: [20, 20, 60, 40],
    meta: {
      product_name: {
        alias: '상품명',
        range: [0.1, 0.9],  // 막대 및 눈금 중앙 정렬
      },
      sales_count: {
        alias: '판매 수량',
        nice: true,
      },
    },
    xAxis: {
      label: {
        autoHide: false,
        autoRotate: true,
        style: { fontSize: 11 },
      },
    },
    color: '#007BFF',
    columnStyle: {
      radius: [4, 4, 0, 0],  // 위줹 둥근 모서리
    },
  }

  const revenueTrendConfig = {
    data: revenueTrend,
    xField: 'date',
    yField: 'revenue',
    point: {
      size: 5,
      shape: 'diamond',
    },
    label: {
      style: {
        fill: '#aaa',
      },
    },
    meta: {
      date: {
        alias: '날짜',
      },
      revenue: {
        alias: '매출 (원)',
        formatter: (value: number) => `${(value / 1000000).toFixed(1)}M`,
      },
    },
    color: '#FFC107',
    smooth: true,
  }

  return (
    <div className="admin-dashboard">
      <div className="dashboard-container">
        <div className="dashboard-header">
          <h2>대시보드</h2>
          {lastUpdated && (
            <Tooltip title={`마지막 갱신: ${lastUpdated.toLocaleTimeString('ko-KR')}`}>
              <span className="dashboard-last-updated">
                <SyncOutlined spin={syncing} style={{ marginRight: 4 }} />
                {lastUpdated.toLocaleTimeString('ko-KR')}
              </span>
            </Tooltip>
          )}
        </div>

        {/* 에러 배너 */}
        {error && (
          <Alert
            type="error"
            message={error}
            showIcon
            style={{ marginBottom: '1rem' }}
          />
        )}

        {/* 로딩 오버레이 */}
        <Spin spinning={loading} size="large">


        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="총 주문 수"
                value={stats.total_orders}
                prefix={<ShoppingOutlined />}
                valueStyle={{ color: '#007BFF' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="일 매출"
                value={stats.daily_revenue}
                prefix={<DollarOutlined />}
                suffix="원"
                valueStyle={{ color: '#28a745' }}
                formatter={(value) => `${Number(value).toLocaleString()}`}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="주 매출"
                value={stats.weekly_revenue}
                prefix={<DollarOutlined />}
                suffix="원"
                valueStyle={{ color: '#28a745' }}
                formatter={(value) => `${Number(value).toLocaleString()}`}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="월 매출"
                value={stats.monthly_revenue}
                prefix={<DollarOutlined />}
                suffix="원"
                valueStyle={{ color: '#28a745' }}
                formatter={(value) => `${Number(value).toLocaleString()}`}
              />
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: '1rem' }}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="신규 회원 수"
                value={stats.new_members}
                prefix={<UserAddOutlined />}
                valueStyle={{ color: '#17a2b8' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Badge count={stats.low_stock_count} overflowCount={99}>
                <Statistic
                  title={`${stats.critical_product_name} 재고 알림`}
                  value={stats.critical_product_stock < 0 ? '-' : stats.critical_product_stock}
                  prefix={<WarningOutlined />}
                  valueStyle={{ color: '#dc3545' }}
                  suffix={stats.critical_product_stock >= 0 ? '개' : undefined}
                />
              </Badge>
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="오늘 방문자 수"
                value={stats.today_visitors}
                prefix={<EyeOutlined />}
                valueStyle={{ color: '#6c757d' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="이번 주 방문자 수"
                value={stats.week_visitors}
                prefix={<EyeOutlined />}
                valueStyle={{ color: '#6c757d' }}
              />
            </Card>
          </Col>
        </Row>

        {/* 차트 영역 */}
        <Row gutter={[16, 16]} style={{ marginTop: '1.5rem' }}>
          <Col xs={24} lg={12}>
            <Card
              title={`${currentMonth}월 인기 상품 Top N`}
              extra={
                <Select
                  value={topN}
                  onChange={setTopN}
                  style={{ width: 80 }}
                  size="small"
                >
                  <Option value={5}>Top 5</Option>
                  <Option value={10}>Top 10</Option>
                  <Option value={15}>Top 15</Option>
                </Select>
              }
            >
              <Column {...popularProductsConfig} height={300} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title={`${currentMonth}월 매출 추이 그래프`}>
              <Line {...revenueTrendConfig} height={300} />
            </Card>
          </Col>
        </Row>
        </Spin>
      </div>
    </div>
  )
}

export default AdminDashboard
