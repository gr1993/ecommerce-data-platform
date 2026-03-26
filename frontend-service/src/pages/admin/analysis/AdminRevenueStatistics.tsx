import { useState, useEffect } from 'react'
import { Card, Row, Col, DatePicker, Select, Button, Space, message } from 'antd'
import { Column, Line, Pie } from '@ant-design/charts'
import { SearchOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import './AdminRevenueStatistics.css'
import { analyticsApi } from '../../../api/analyticsApi'

const { RangePicker } = DatePicker
const { Option } = Select

interface CategoryRevenue {
  category_id: number
  category_name: string
  revenue: number
}

interface ProductRevenue {
  product_id: number
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
  const [loading, setLoading] = useState<boolean>(false)

  const fetchStats = async () => {
    setLoading(true)
    const startDate = dateRange[0].format('YYYY-MM-DD')
    const endDate = dateRange[1].format('YYYY-MM-DD')

    try {
      // 카테고리 통계만 실제 API 호출
      const catData = await analyticsApi.getCategoryStats(startDate, endDate)
      setCategoryRevenue(catData)

      // 나머지는 일단 샘플 데이터 유지
      setProductRevenue([
        { product_id: 1, product_name: '노트북', revenue: 50000000 },
        { product_id: 2, product_name: '스마트폰', revenue: 40000000 },
        { product_id: 3, product_name: '태블릿', revenue: 30000000 }
      ])
      setRevenueTrend([
        { date: '2024-01-01', revenue: 5000000 },
        { date: '2024-01-02', revenue: 5500000 }
      ])
      setReturnExchangeStats([
        { type: '반품', count: 25, amount: 5000000 },
        { type: '교환', count: 15, amount: 3000000 }
      ])
    } catch (error) {
      console.error('통계 데이터 로딩 실패:', error)
      message.error('통계 데이터를 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  // 매출 통계 데이터 로드
  useEffect(() => {
    fetchStats()
  }, [])

  const handleDateRangeChange = (dates: any) => {
    if (dates) {
      setDateRange([dates[0], dates[1]])
    }
  }

  const handleSearch = () => {
    fetchStats()
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
