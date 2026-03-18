import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Badge, Select, Space } from 'antd'
import { ShoppingOutlined, DollarOutlined, UserAddOutlined, WarningOutlined, EyeOutlined } from '@ant-design/icons'
import { Column, Line } from '@ant-design/charts'
import './AdminDashboard.css'

const { Option } = Select

interface DashboardStats {
  totalOrders: number
  dailyRevenue: number
  weeklyRevenue: number
  monthlyRevenue: number
  newMembers: number
  lowStockCount: number
  todayVisitors: number
  weekVisitors: number
}

interface PopularProduct {
  product_name: string
  sales_count: number
}

interface RevenueTrend {
  date: string
  revenue: number
}

function AdminDashboard() {
  const [stats, setStats] = useState<DashboardStats>({
    totalOrders: 0,
    dailyRevenue: 0,
    weeklyRevenue: 0,
    monthlyRevenue: 0,
    newMembers: 0,
    lowStockCount: 0,
    todayVisitors: 0,
    weekVisitors: 0
  })
  const [popularProducts, setPopularProducts] = useState<PopularProduct[]>([])
  const [revenueTrend, setRevenueTrend] = useState<RevenueTrend[]>([])
  const [topN, setTopN] = useState<number>(5)

  // 대시보드 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 대시보드 데이터 로드
    setStats({
      totalOrders: 1250,
      dailyRevenue: 2500000,
      weeklyRevenue: 15000000,
      monthlyRevenue: 65000000,
      newMembers: 45,
      lowStockCount: 8,
      todayVisitors: 320,
      weekVisitors: 2150
    })

    setPopularProducts([
      { product_name: '노트북', sales_count: 125 },
      { product_name: '스마트폰', sales_count: 98 },
      { product_name: '태블릿', sales_count: 76 },
      { product_name: '이어폰', sales_count: 65 },
      { product_name: '마우스', sales_count: 54 },
      { product_name: '키보드', sales_count: 43 },
      { product_name: '모니터', sales_count: 32 },
      { product_name: '웹캠', sales_count: 28 }
    ])

    setRevenueTrend([
      { date: '2024-01-01', revenue: 1200000 },
      { date: '2024-01-02', revenue: 1500000 },
      { date: '2024-01-03', revenue: 1800000 },
      { date: '2024-01-04', revenue: 2200000 },
      { date: '2024-01-05', revenue: 2500000 },
      { date: '2024-01-06', revenue: 2800000 },
      { date: '2024-01-07', revenue: 3000000 },
      { date: '2024-01-08', revenue: 3200000 },
      { date: '2024-01-09', revenue: 2900000 },
      { date: '2024-01-10', revenue: 3100000 },
      { date: '2024-01-11', revenue: 3300000 },
      { date: '2024-01-12', revenue: 3500000 },
      { date: '2024-01-13', revenue: 3400000 },
      { date: '2024-01-14', revenue: 3600000 }
    ])
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

        {/* 통계 카드 */}
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="총 주문 수"
                value={stats.totalOrders}
                prefix={<ShoppingOutlined />}
                valueStyle={{ color: '#007BFF' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="일 매출"
                value={stats.dailyRevenue}
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
                value={stats.weeklyRevenue}
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
                value={stats.monthlyRevenue}
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
                value={stats.newMembers}
                prefix={<UserAddOutlined />}
                valueStyle={{ color: '#17a2b8' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Badge count={stats.lowStockCount} overflowCount={99}>
                <Statistic
                  title="상품 재고 알림"
                  value={stats.lowStockCount}
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
                value={stats.todayVisitors}
                prefix={<EyeOutlined />}
                valueStyle={{ color: '#6c757d' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="이번 주 방문자 수"
                value={stats.weekVisitors}
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
      </div>
    </div>
  )
}

export default AdminDashboard
