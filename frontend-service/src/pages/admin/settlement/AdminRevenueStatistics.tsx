import { useState, useEffect } from 'react'
import { Card, Row, Col, DatePicker, Select, Button, Space } from 'antd'
import { Column, Line, Pie } from '@ant-design/charts'
import { SearchOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import './AdminRevenueStatistics.css'

const { RangePicker } = DatePicker
const { Option } = Select

interface CategoryRevenue {
  category_name: string
  revenue: number
}

interface ProductRevenue {
  product_name: string
  revenue: number
}

interface RevenueTrend {
  date: string
  revenue: number
}

interface ReturnExchangeStats {
  type: string
  count: number
  amount: number
}

function AdminRevenueStatistics() {
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs()
  ])
  const [periodType, setPeriodType] = useState<string>('daily')
  const [categoryRevenue, setCategoryRevenue] = useState<CategoryRevenue[]>([])
  const [productRevenue, setProductRevenue] = useState<ProductRevenue[]>([])
  const [revenueTrend, setRevenueTrend] = useState<RevenueTrend[]>([])
  const [returnExchangeStats, setReturnExchangeStats] = useState<ReturnExchangeStats[]>([])

  // 매출 통계 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 매출 통계 데이터 로드
    setCategoryRevenue([
      { category_name: '전자제품', revenue: 150000000 },
      { category_name: '의류', revenue: 80000000 },
      { category_name: '도서', revenue: 30000000 },
      { category_name: '식품', revenue: 50000000 },
      { category_name: '기타', revenue: 40000000 }
    ])

    setProductRevenue([
      { product_name: '노트북', revenue: 50000000 },
      { product_name: '스마트폰', revenue: 40000000 },
      { product_name: '태블릿', revenue: 30000000 },
      { product_name: '이어폰', revenue: 20000000 },
      { product_name: '마우스', revenue: 10000000 }
    ])

    setRevenueTrend([
      { date: '2024-01-01', revenue: 5000000 },
      { date: '2024-01-02', revenue: 5500000 },
      { date: '2024-01-03', revenue: 6000000 },
      { date: '2024-01-04', revenue: 6500000 },
      { date: '2024-01-05', revenue: 7000000 },
      { date: '2024-01-06', revenue: 7500000 },
      { date: '2024-01-07', revenue: 8000000 },
      { date: '2024-01-08', revenue: 7800000 },
      { date: '2024-01-09', revenue: 8200000 },
      { date: '2024-01-10', revenue: 8500000 },
      { date: '2024-01-11', revenue: 8800000 },
      { date: '2024-01-12', revenue: 9000000 },
      { date: '2024-01-13', revenue: 9200000 },
      { date: '2024-01-14', revenue: 9500000 }
    ])

    setReturnExchangeStats([
      { type: '반품', count: 25, amount: 5000000 },
      { type: '교환', count: 15, amount: 3000000 }
    ])
  }, [dateRange, periodType])

  const handleDateRangeChange = (dates: any) => {
    if (dates) {
      setDateRange([dates[0], dates[1]])
    }
  }

  const handleSearch = () => {
    // TODO: API 호출로 필터링된 데이터 조회
    console.log('검색:', dateRange, periodType)
  }

  const categoryRevenueConfig = {
    data: categoryRevenue,
    xField: 'category_name',
    yField: 'revenue',
    color: '#007BFF',
    meta: {
      category_name: {
        alias: '카테고리',
      },
      revenue: {
        alias: '매출 (원)',
        formatter: (value: number) => `${(value / 1000000).toFixed(1)}M`,
      },
    },
  }

  const productRevenueConfig = {
    data: productRevenue,
    xField: 'product_name',
    yField: 'revenue',
    color: '#FFC107',
    meta: {
      product_name: {
        alias: '상품명',
      },
      revenue: {
        alias: '매출 (원)',
        formatter: (value: number) => `${(value / 1000000).toFixed(1)}M`,
      },
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
    smooth: true,
    color: '#28a745',
    meta: {
      date: {
        alias: '날짜',
      },
      revenue: {
        alias: '매출 (원)',
        formatter: (value: number) => `${(value / 1000000).toFixed(1)}M`,
      },
    },
  }

  const returnExchangeConfig = {
    data: returnExchangeStats,
    angleField: 'amount',
    colorField: 'type',
    radius: 0.8,
    label: {
      type: 'outer',
      content: '{name}: {value}원',
      formatter: (datum: any) => {
        return `${datum.type}: ${(datum.amount / 1000000).toFixed(1)}M원`
      },
    },
    color: ['#dc3545', '#ffc107'],
  }

  return (
    <div className="admin-revenue-statistics">
      <div className="revenue-statistics-container">
        <div className="statistics-header">
          <h2>매출 통계</h2>
          <Space>
            <RangePicker
              value={dateRange}
              onChange={handleDateRangeChange}
              format="YYYY-MM-DD"
            />
            <Select
              value={periodType}
              onChange={setPeriodType}
              style={{ width: 120 }}
            >
              <Option value="daily">일별</Option>
              <Option value="weekly">주별</Option>
              <Option value="monthly">월별</Option>
            </Select>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={handleSearch}
              style={{ backgroundColor: '#007BFF', borderColor: '#007BFF' }}
            >
              조회
            </Button>
          </Space>
        </div>

        {/* 카테고리별 매출 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '1.5rem' }}>
          <Col xs={24} lg={12}>
            <Card title="카테고리별 매출">
              <Column {...categoryRevenueConfig} height={300} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="상품별 매출">
              <Column {...productRevenueConfig} height={300} />
            </Card>
          </Col>
        </Row>

        {/* 기간별 매출 추이 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '1.5rem' }}>
          <Col xs={24} lg={16}>
            <Card title="기간별 매출 추이">
              <Line {...revenueTrendConfig} height={300} />
            </Card>
          </Col>
          <Col xs={24} lg={8}>
            <Card title="반품/교환 현황">
              <Pie {...returnExchangeConfig} height={300} />
              <div style={{ marginTop: '1rem', padding: '1rem', background: '#f8f9fa', borderRadius: '4px' }}>
                <div style={{ marginBottom: '0.5rem' }}>
                  <strong>반품:</strong> {returnExchangeStats.find(s => s.type === '반품')?.count}건 / 
                  {returnExchangeStats.find(s => s.type === '반품')?.amount.toLocaleString()}원
                </div>
                <div>
                  <strong>교환:</strong> {returnExchangeStats.find(s => s.type === '교환')?.count}건 / 
                  {returnExchangeStats.find(s => s.type === '교환')?.amount.toLocaleString()}원
                </div>
              </div>
            </Card>
          </Col>
        </Row>
      </div>
    </div>
  )
}

export default AdminRevenueStatistics

