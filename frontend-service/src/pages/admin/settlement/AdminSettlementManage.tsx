import { useState, useEffect, useCallback } from 'react'
import { Card, Row, Col, Statistic, Table, DatePicker, Button, Space, message, Tag, Typography, Modal } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { 
  DownloadOutlined, 
  DollarOutlined, 
  ShoppingOutlined, 
  FileTextOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import './AdminSettlementManage.css'
import { settlementApi } from '../../../api/settlementApi'
import { type ReconciliationError, type DailySettlementResponse } from '../../../types/settlement'

const { RangePicker } = DatePicker
const { Title, Text } = Typography

function AdminSettlementManage() {
  const [dailySettlements, setDailySettlements] = useState<DailySettlementResponse[]>([])
  const [reconciliationErrors, setReconciliationErrors] = useState<ReconciliationError[]>([])
  const [isErrorModalOpen, setIsErrorModalOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs('2026-03-01'),
    dayjs('2026-03-31')
  ])

  const [summary, setSummary] = useState({
    totalOrderAmount: 0,
    totalRefundAmount: 0,
    totalNetRevenue: 0,
    totalOrderCount: 0
  })

  // 오류 탐지 데이터 로드
  const fetchErrors = useCallback(async () => {
    try {
      const data = await settlementApi.getReconciliationErrors()
      setReconciliationErrors(data)
    } catch (error) {
      console.error('오류 데이터 로드 실패:', error)
      message.error('오류 탐지 데이터를 불러오는데 실패했습니다.')
      
      // API 실패 시 샘플 데이터 유지 (개발 확인용)
      setReconciliationErrors([
        {
          orderNumber: 'ORD-20240324-001',
          category: 'SALES',
          status: 'AMOUNT_MISMATCH',
          amount: 55000,
          eventAt: '2024-03-24 14:20:00',
          errorMessage: '주문-결제 금액 불일치 (주문: 55,000 / 결제: 50,000)'
        }
      ])
    }
  }, [])

  // 정산 데이터 로드
  const loadSettlementData = useCallback(async () => {
    setLoading(true)
    try {
      const start = dateRange[0].format('YYYY-MM-DD')
      const end = dateRange[1].format('YYYY-MM-DD')
      const data = await settlementApi.getDailySettlements(start, end)
      setDailySettlements(data)

      // 요약 통계 계산
      const total = data.reduce((acc, item) => ({
        totalOrderAmount: acc.totalOrderAmount + item.totalOrderAmount,
        totalRefundAmount: acc.totalRefundAmount + item.refundAmount,
        totalNetRevenue: acc.totalNetRevenue + item.netRevenue,
        totalOrderCount: acc.totalOrderCount + item.orderCount
      }), {
        totalOrderAmount: 0,
        totalRefundAmount: 0,
        totalNetRevenue: 0,
        totalOrderCount: 0
      })
      setSummary(total)
    } catch (error) {
      console.error('정산 데이터 로드 실패:', error)
      message.error('정산 데이터를 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }, [dateRange])

  useEffect(() => {
    loadSettlementData()
    fetchErrors()
  }, [loadSettlementData, fetchErrors])

  const handleDateRangeChange = (dates: any) => {
    if (dates) {
      setDateRange([dates[0], dates[1]])
    }
  }

  const handleDownloadReport = () => {
    message.success('보고서 다운로드가 시작되었습니다.')
  }

  const columns: ColumnsType<DailySettlementResponse> = [
    {
      title: '정산 일자',
      dataIndex: 'settlementDate',
      key: 'settlementDate',
      width: 150,
      sorter: (a, b) => dayjs(a.settlementDate).unix() - dayjs(b.settlementDate).unix(),
    },
    {
      title: '주문 총액',
      dataIndex: 'totalOrderAmount',
      key: 'totalOrderAmount',
      render: (amount: number) => `${(amount ?? 0).toLocaleString()}원`,
      align: 'right',
      width: 150,
    },
    {
      title: '주문 건수',
      dataIndex: 'orderCount',
      key: 'orderCount',
      render: (count: number) => `${(count ?? 0).toLocaleString()}건`,
      align: 'right',
      width: 100,
    },
    {
      title: '환불/반품 금액',
      dataIndex: 'refundAmount',
      key: 'refundAmount',
      render: (amount: number) => (
        <span style={{ color: '#dc3545' }}>
          {Math.abs(amount ?? 0).toLocaleString()}원
        </span>
      ),
      align: 'right',
      width: 130,
    },
    {
      title: '환불 건수',
      dataIndex: 'refundCount',
      key: 'refundCount',
      render: (count: number) => `${(count ?? 0).toLocaleString()}건`,
      align: 'right',
      width: 100,
    },
    {
      title: '순매출',
      dataIndex: 'netRevenue',
      key: 'netRevenue',
      render: (amount: number) => (
        <strong style={{ color: '#28a745', fontSize: '16px' }}>
          {(amount ?? 0).toLocaleString()}원
        </strong>
      ),
      align: 'right',
      width: 150,
    },
  ]

  const errorColumns: ColumnsType<ReconciliationError> = [
    {
      title: '주문 번호',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 180,
    },
    {
      title: '구분',
      dataIndex: 'category',
      key: 'category',
      render: (category) => (
        <Tag color={category === 'SALES' ? 'blue' : 'orange'}>
          {category === 'SALES' ? '매출' : '취소'}
        </Tag>
      ),
      width: 80,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const statusMap: Record<string, { color: string, text: string }> = {
          'AMOUNT_MISMATCH': { color: 'error', text: '금액 불일치' },
          'PAYMENT_NOT_FOUND': { color: 'warning', text: '결제데이터 누락' },
          'ORDER_NOT_FOUND': { color: 'warning', text: '주문데이터 누락' },
          'ORDER_CANCEL_NOT_FOUND': { color: 'warning', text: '취소데이터 누락' },
          'PAYMENT_CANCEL_NOT_FOUND': { color: 'warning', text: '결제취소 누락' },
        }
        const s = statusMap[status] || { color: 'default', text: status }
        return <Tag color={s.color}>{s.text}</Tag>
      },
      width: 120,
    },
    {
      title: '발생 금액',
      dataIndex: 'amount',
      key: 'amount',
      render: (amount: number) => `${(amount ?? 0).toLocaleString()}원`,
      align: 'right',
      width: 120,
    },
    {
      title: '오류 상세 정보',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      render: (text) => <Text type="danger">{text}</Text>
    },
    {
      title: '발생 일시',
      dataIndex: 'eventAt',
      key: 'eventAt',
      width: 180,
    }
  ]

  return (
    <div className="admin-settlement-manage">
      <div className="settlement-manage-container">
        <div className="settlement-header">
          <Title level={2}>정산 관리</Title>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => { loadSettlementData(); fetchErrors(); }} loading={loading}>새로고침</Button>
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
                title="총 주문 건수"
                value={summary.totalOrderCount}
                prefix={<ShoppingOutlined />}
                suffix="건"
                valueStyle={{ color: '#6c757d' }}
                formatter={(value) => `${Number(value).toLocaleString()}`}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="환불/반품 금액"
                value={summary.totalRefundAmount}
                prefix={<DollarOutlined />}
                suffix="원"
                valueStyle={{ color: '#dc3545' }}
                formatter={(value) => `${Math.abs(Number(value)).toLocaleString()}`}
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

        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          {/* 데이터 불일치 및 오류 탐지 섹션 */}
          <Card 
            title={
              <Space>
                <WarningOutlined style={{ color: '#faad14' }} />
                <span>데이터 불일치 및 오류 탐지 (최신 3건)</span>
                {reconciliationErrors.length > 0 && (
                  <Tag color="error">{reconciliationErrors.length}건 발생</Tag>
                )}
              </Space>
            }
            extra={
              <Button type="link" onClick={() => setIsErrorModalOpen(true)}>
                전체 보기
              </Button>
            }
            className="error-detection-card"
          >
            {reconciliationErrors.length > 0 ? (
              <Table
                columns={errorColumns}
                dataSource={reconciliationErrors.slice(0, 3)}
                rowKey="orderNumber"
                size="small"
                pagination={false}
              />
            ) : (
              <div style={{ textAlign: 'center', padding: '20px' }}>
                <CheckCircleOutlined style={{ fontSize: '24px', color: '#52c41a', marginBottom: '8px' }} />
                <p>탐지된 데이터 불일치가 없습니다.</p>
              </div>
            )}
          </Card>

          {/* 정산 내역 테이블 */}
          <Card title="일별 정산 내역 (배치 결과)">
            <Table
              columns={columns}
              dataSource={dailySettlements}
              rowKey="settlementDate"
              scroll={{ x: 'max-content' }}
              loading={loading}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total) => `총 ${total}건`,
              }}
            />
          </Card>
        </Space>

        {/* 오류 전체 보기 모달 */}
        <Modal
          title={
            <Space>
              <WarningOutlined style={{ color: '#faad14' }} />
              <span>데이터 불일치 및 오류 탐지 전체 내역</span>
            </Space>
          }
          open={isErrorModalOpen}
          onOk={() => setIsErrorModalOpen(false)}
          onCancel={() => setIsErrorModalOpen(false)}
          width={1000}
          footer={[
            <Button key="close" onClick={() => setIsErrorModalOpen(false)}>닫기</Button>
          ]}
        >
          <Table
            columns={errorColumns}
            dataSource={reconciliationErrors}
            rowKey="orderNumber"
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
            }}
          />
        </Modal>
      </div>
    </div>
  )
}

export default AdminSettlementManage
