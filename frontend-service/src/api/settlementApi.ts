import { API_BASE_URL } from '../config/env'
import { type ReconciliationError, type SettlementData, type DailySettlementResponse } from '../types/settlement'

export const settlementApi = {
  /**
   * 대조 오류 탐지 데이터 가져오기
   */
  getReconciliationErrors: async (): Promise<ReconciliationError[]> => {
    const response = await fetch(`${API_BASE_URL}/api/settlement/dashboard/errors`)
    if (!response.ok) {
      throw new Error('대조 오류 데이터를 불러오는데 실패했습니다.')
    }
    return response.json()
  },

  /**
   * 일별 정산 데이터 리스트 가져오기
   */
  getDailySettlements: async (start: string, end: string): Promise<DailySettlementResponse[]> => {
    const response = await fetch(`${API_BASE_URL}/api/settlement/dashboard/daily?start=${start}&end=${end}`)
    if (!response.ok) {
      throw new Error('일별 정산 데이터를 불러오는데 실패했습니다.')
    }
    return response.json()
  },

  /**
   * 정산 내역 리스트 가져오기 (샘플 데이터 포함 - 실제로는 getDailySettlements 사용 권장)
   */
  getSettlementList: async (): Promise<SettlementData[]> => {
    // 이전 샘플 데이터 유지 (필요 시)
    return [
      {
        settlement_id: '1',
        period: '2024-03-01 ~ 2024-03-07',
        total_order_amount: 50000000,
        discount_amount: 0,
        coupon_amount: 0,
        refund_amount: 500000,
        return_amount: 300000,
        net_revenue: 49200000,
        order_count: 250,
        created_at: '2024-03-08 10:00:00'
      }
    ]
  }
}
