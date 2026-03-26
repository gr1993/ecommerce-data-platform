import { ANALYTICS_API_BASE_URL } from '../config/env'

export interface CategoryRevenue {
  category_id: number
  category_name: string
  revenue: number
}

export const analyticsApi = {
  /**
   * 카테고리별 매출 통계 가져오기
   */
  getCategoryStats: async (startDate: string, endDate: string): Promise<CategoryRevenue[]> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/stats/categories?startDate=${startDate}&endDate=${endDate}`)
    if (!response.ok) {
      throw new Error('카테고리별 통계를 불러오는데 실패했습니다.')
    }
    return response.json()
  }
}
