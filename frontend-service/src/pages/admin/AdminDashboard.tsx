import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Badge, Select, Spin, Alert } from 'antd'
import { ShoppingOutlined, DollarOutlined, UserAddOutlined, WarningOutlined, EyeOutlined } from '@ant-design/icons'
import { Column, Line } from '@ant-design/charts'
import { analyticsApi, type DashboardSummary, type ProductRevenue, type RevenueTrend } from '../../api/analyticsApi'
import './AdminDashboard.css'

const { Option } = Select

// ---- 날짜 helpers ----
const fmt = (d: Date) => d.toISOString().slice(0, 10)
const today = fmt(new Date())
const monthStart = fmt(new Date(new Date().getFullYear(), new Date().getMonth(), 1))
const twoWeeksAgo = fmt(new Date(Date.now() - 13 * 86_400_000))

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
    today_visitors: 0,
    week_visitors: 0,
  })
  const [popularProducts, setPopularProducts] = useState<PopularProduct[]>([])
  const [revenueTrend, setRevenueTrend] = useState<RevenueTrend[]>([])
  const [topN, setTopN] = useState<number>(5)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchAll = async () => {
      setLoading(true)
      setError(null)
      try {
        const [summary, trend, products] = await Promise.all([
          analyticsApi.getDashboardSummary(),
          analyticsApi.getRevenueTrend(twoWeeksAgo, today, 'daily'),
          analyticsApi.getProductStats(monthStart, today, 15),
        ])

        setStats(summary)

        setRevenueTrend(trend.map((r) => ({ date: r.date, revenue: Number(r.revenue) })))

        setPopularProducts(
          (products as ProductRevenue[]).map((p) => ({
            product_name: p.product_name,
            sales_count: p.quantity,
          }))
        )
      } catch (err) {
        setError(err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.')
      } finally {
        setLoading(false)
      }
    }

    fetchAll()
  }, [])



  const popularProductsConfig = {
    data: popularProducts.slice(0, topN),
    xField: 'sales_count',
    yField: 'product_name',
    seriesField: 'product_name',
    legend: false,
    meta: {
      sales_count: {
        alias: '판매 수량',
      },
      product_name: {
        alias: '상품명',
      },
    },
    color: '#007BFF',
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
                  title="상품 재고 알림"
                  value={stats.low_stock_count}
                  prefix={<WarningOutlined />}
                  valueStyle={{ color: '#dc3545' }}
                  suffix="개"
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
              title="인기 상품 Top N"
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
            <Card title="매출 추이 그래프">
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
