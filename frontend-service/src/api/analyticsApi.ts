import { ANALYTICS_API_BASE_URL } from '../config/env'

export interface CategoryRevenue {
  category_id: number
  category_name: string
  revenue: number
}

export interface ProductRevenue {
  product_id: number
  product_name: string
  revenue: number
}

export interface RevenueTrend {
  date: string
  revenue: number
}

export interface ReturnExchangeStats {
  type: string
  count: number
  amount: number
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
  },

  /**
   * 상품별 매출 통계 가져오기
   */
  getProductStats: async (startDate: string, endDate: string, limit: number = 10): Promise<ProductRevenue[]> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/stats/products?startDate=${startDate}&endDate=${endDate}&limit=${limit}`)
    if (!response.ok) {
      throw new Error('상품별 통계를 불러오는데 실패했습니다.')
    }
    return response.json()
  },

  /**
   * 매출 추이 데이터 가져오기
   */
  getRevenueTrend: async (startDate: string, endDate: string, period: string = 'daily'): Promise<RevenueTrend[]> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/stats/revenue-trend?startDate=${startDate}&endDate=${endDate}&period=${period}`)
    if (!response.ok) {
      throw new Error('매출 추이 데이터를 불러오는데 실패했습니다.')
    }
    return response.json()
  },

  /**
   * CS 통계 가져오기
   */
  getClaimStats: async (startDate: string, endDate: string): Promise<ReturnExchangeStats[]> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/stats/claims?startDate=${startDate}&endDate=${endDate}`)
    if (!response.ok) {
      throw new Error('CS 통계를 불러오는데 실패했습니다.')
    }
    return response.json()
  }
}
