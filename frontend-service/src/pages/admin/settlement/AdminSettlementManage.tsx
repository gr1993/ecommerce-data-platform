import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Table, DatePicker, Button, Space, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { DownloadOutlined, DollarOutlined, ShoppingOutlined, FileTextOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import './AdminSettlementManage.css'

const { RangePicker } = DatePicker

interface SettlementData {
  settlement_id: string
  period: string
  total_order_amount: number
  discount_amount: number
  coupon_amount: number
  refund_amount: number
  return_amount: number
  net_revenue: number
  order_count: number
  created_at: string
}

function AdminSettlementManage() {
  const [settlementData, setSettlementData] = useState<SettlementData[]>([])
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs()
  ])
  const [summary, setSummary] = useState({
    totalOrderAmount: 0,
    totalDiscountAmount: 0,
    totalCouponAmount: 0,
    totalRefundAmount: 0,
    totalReturnAmount: 0,
    totalNetRevenue: 0,
    totalOrderCount: 0
  })

  // 정산 데이터 로드 (샘플 데이터)
  useEffect(() => {
    // TODO: API 호출로 정산 데이터 로드
    const sampleData: SettlementData[] = [
      {
        settlement_id: '1',
        period: '2024-01-01 ~ 2024-01-07',
        total_order_amount: 50000000,
        discount_amount: 2500000,
        coupon_amount: 1500000,
        refund_amount: 500000,
        return_amount: 300000,
        net_revenue: 45200000,
        order_count: 250,
        created_at: '2024-01-08 10:00:00'
      },
      {
        settlement_id: '2',
        period: '2024-01-08 ~ 2024-01-14',
        total_order_amount: 55000000,
        discount_amount: 2800000,
        coupon_amount: 1800000,
        refund_amount: 600000,
        return_amount: 400000,
        net_revenue: 49400000,
        order_count: 275,
        created_at: '2024-01-15 10:00:00'
      },
      {
        settlement_id: '3',
        period: '2024-01-15 ~ 2024-01-21',
        total_order_amount: 60000000,
        discount_amount: 3000000,
        coupon_amount: 2000000,
        refund_amount: 700000,
        return_amount: 500000,
        net_revenue: 53800000,
        order_count: 300,
        created_at: '2024-01-22 10:00:00'
      },
      {
        settlement_id: '4',
        period: '2024-01-22 ~ 2024-01-28',
        total_order_amount: 58000000,
        discount_amount: 2900000,
        coupon_amount: 1900000,
        refund_amount: 650000,
        return_amount: 450000,
        net_revenue: 52050000,
        order_count: 290,
        created_at: '2024-01-29 10:00:00'
      }
    ]
    setSettlementData(sampleData)

    // 요약 통계 계산
    const total = sampleData.reduce((acc, item) => ({
      totalOrderAmount: acc.totalOrderAmount + item.total_order_amount,
      totalDiscountAmount: acc.totalDiscountAmount + item.discount_amount,
      totalCouponAmount: acc.totalCouponAmount + item.coupon_amount,
      totalRefundAmount: acc.totalRefundAmount + item.refund_amount,
      totalReturnAmount: acc.totalReturnAmount + item.return_amount,
      totalNetRevenue: acc.totalNetRevenue + item.net_revenue,
      totalOrderCount: acc.totalOrderCount + item.order_count
    }), {
      totalOrderAmount: 0,
      totalDiscountAmount: 0,
      totalCouponAmount: 0,
      totalRefundAmount: 0,
      totalReturnAmount: 0,
      totalNetRevenue: 0,
      totalOrderCount: 0
    })
    setSummary(total)
  }, [dateRange])

  const handleDateRangeChange = (dates: any) => {
    if (dates) {
      setDateRange([dates[0], dates[1]])
    }
  }

  const handleDownloadReport = () => {
    // TODO: API 호출로 보고서 다운로드
    message.success('보고서 다운로드가 시작되었습니다.')
    // 실제로는 CSV 또는 Excel 파일 다운로드
  }

  const columns: ColumnsType<SettlementData> = [
    {
      title: '정산 기간',
      dataIndex: 'period',
      key: 'period',
      width: 200,
    },
    {
      title: '주문 총액',
      dataIndex: 'total_order_amount',
      key: 'total_order_amount',
      render: (amount: number) => `${amount.toLocaleString()}원`,
      align: 'right',
      width: 150,
    },
    {
      title: '할인 금액',
      dataIndex: 'discount_amount',
      key: 'discount_amount',
      render: (amount: number) => `${amount.toLocaleString()}원`,
      align: 'right',
      width: 130,
    },
    {
      title: '쿠폰 적용 금액',
      dataIndex: 'coupon_amount',
      key: 'coupon_amount',
      render: (amount: number) => `${amount.toLocaleString()}원`,
      align: 'right',
      width: 150,
    },
    {
      title: '환불 금액',
      dataIndex: 'refund_amount',
      key: 'refund_amount',
      render: (amount: number) => (
        <span style={{ color: '#dc3545' }}>-{amount.toLocaleString()}원</span>
      ),
      align: 'right',
      width: 130,
    },
    {
      title: '반품 금액',
      dataIndex: 'return_amount',
      key: 'return_amount',
      render: (amount: number) => (
        <span style={{ color: '#dc3545' }}>-{amount.toLocaleString()}원</span>
      ),
      align: 'right',
      width: 130,
    },
    {
      title: '순매출',
      dataIndex: 'net_revenue',
      key: 'net_revenue',
      render: (amount: number) => (
        <strong style={{ color: '#28a745', fontSize: '16px' }}>
          {amount.toLocaleString()}원
        </strong>
      ),
      align: 'right',
      width: 150,
    },
    {
      title: '주문 건수',
      dataIndex: 'order_count',
      key: 'order_count',
      align: 'right',
      width: 100,
    },
    {
      title: '생성 일시',
      dataIndex: 'created_at',
      key: 'created_at',
      render: (date: string) => {
        const dateObj = new Date(date)
        return dateObj.toLocaleString('ko-KR', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      },
      width: 160,
    },
  ]

  return (
    <div className="admin-settlement-manage">
      <div className="settlement-manage-container">
        <div className="settlement-header">
          <h2>정산 관리</h2>
          <Space>
            <RangePicker
              value={dateRange}
              onChange={handleDateRangeChange}
              format="YYYY-MM-DD"
            />
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={handleDownloadReport}
              style={{ backgroundColor: '#FFC107', borderColor: '#FFC107', color: '#343A40', fontWeight: 600 }}
            >
              보고서 다운로드
            </Button>
          </Space>
        </div>

        {/* 요약 통계 카드 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '1.5rem' }}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="주문 총액"
                value={summary.totalOrderAmount}
                prefix={<ShoppingOutlined />}
                suffix="원"
                valueStyle={{ color: '#007BFF' }}
                formatter={(value) => `${Number(value).toLocaleString()}`}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="할인/쿠폰 적용 금액"
                value={summary.totalDiscountAmount + summary.totalCouponAmount}
                prefix={<DollarOutlined />}
                suffix="원"
                valueStyle={{ color: '#6c757d' }}
                formatter={(value) => `-${Number(value).toLocaleString()}`}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="환불/반품 금액"
                value={summary.totalRefundAmount + summary.totalReturnAmount}
                prefix={<DollarOutlined />}
                suffix="원"
                valueStyle={{ color: '#dc3545' }}
                formatter={(value) => `-${Number(value).toLocaleString()}`}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="순매출"
                value={summary.totalNetRevenue}
                prefix={<FileTextOutlined />}
                suffix="원"
                valueStyle={{ color: '#28a745', fontSize: '20px' }}
                formatter={(value) => `${Number(value).toLocaleString()}`}
              />
            </Card>
          </Col>
        </Row>

        {/* 정산 내역 테이블 */}
        <Card title="정산 내역">
          <Table
            columns={columns}
            dataSource={settlementData}
            rowKey="settlement_id"
            scroll={{ x: 'max-content' }}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `총 ${total}건`,
            }}
          />
        </Card>
      </div>
    </div>
  )
}

export default AdminSettlementManage

