export interface SettlementData {
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

export interface ReconciliationError {
  orderNumber: string
  category: 'SALES' | 'CANCEL'
  status: string
  amount: number
  eventAt: string
  errorMessage: string
}
