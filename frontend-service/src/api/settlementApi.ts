import { API_BASE_URL } from '../config/env'
import { type ReconciliationError, type SettlementData } from '../types/settlement'

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
   * 정산 내역 리스트 가져오기 (샘플 데이터 포함)
   */
  getSettlementList: async (): Promise<SettlementData[]> => {
    // TODO: 실제 API 연동 시 아래 fetch 활성화
    // const response = await fetch(`${API_BASE_URL}/api/settlement/history`)
    // if (!response.ok) throw new Error('정산 내역 로드 실패')
    // return response.json()

    // 현재는 기존에 정의된 샘플 데이터 반환
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
