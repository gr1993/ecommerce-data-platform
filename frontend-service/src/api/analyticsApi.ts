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
  quantity: number
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

// ---- Dashboard Summary ----
export interface DashboardSummary {
  total_orders: number
  daily_revenue: number
  weekly_revenue: number
  monthly_revenue: number
  new_members: number
  low_stock_count: number
  critical_product_name: string   // 재고 가장 부족한 상품명
  critical_product_stock: number  // 해당 상품 현재 재고 수
  today_visitors: number
  week_visitors: number
}

// ---- Event Stats ----
export interface SignupStats {
  date: string
  signup_count: number
}

export interface VisitorStats {
  date: string
  total_page_views: number
  unique_visitor_count: number
}

export interface LowStock {
  product_id: number
  product_name: string
  current_stock: number
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
  },

  /**
   * 대시보드 요약 지표 가져오기 (카드 8개)
   * GET /api/v1/dashboard/summary
   */
  getDashboardSummary: async (): Promise<DashboardSummary> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/dashboard/summary`)
    if (!response.ok) {
      throw new Error('대시보드 데이터를 불러오는데 실패했습니다.')
    }
    return response.json()
  },

  /**
   * 이번 달 판매량 기준 인기 상품 Top 5 (대시보드 전용)
   * GET /api/v1/dashboard/popular-products
   */
  getDashboardPopularProducts: async (): Promise<ProductRevenue[]> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/dashboard/popular-products`)
    if (!response.ok) {
      throw new Error('인기 상품 데이터를 불러오는데 실패했습니다.')
    }
    return response.json()
  },

  /**
   * 일별 신규 가입자 추이
   * GET /api/v1/events/signups?startDate=&endDate=
   */
  getSignupStats: async (startDate: string, endDate: string): Promise<SignupStats[]> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/events/signups?startDate=${startDate}&endDate=${endDate}`)
    if (!response.ok) {
      throw new Error('신규 가입자 데이터를 불러오는데 실패했습니다.')
    }
    return response.json()
  },

  /**
   * 일별 방문자 추이 (총 PV + UV)
   * GET /api/v1/events/visitors?startDate=&endDate=
   */
  getVisitorStats: async (startDate: string, endDate: string): Promise<VisitorStats[]> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/events/visitors?startDate=${startDate}&endDate=${endDate}`)
    if (!response.ok) {
      throw new Error('방문자 데이터를 불러오는데 실패했습니다.')
    }
    return response.json()
  },

  /**
   * 재고 10개 미만 상품 목록
   * GET /api/v1/events/inventory/low-stock
   */
  getLowStockProducts: async (): Promise<LowStock[]> => {
    const response = await fetch(`${ANALYTICS_API_BASE_URL}/api/v1/events/inventory/low-stock`)
    if (!response.ok) {
      throw new Error('재고 부족 데이터를 불러오는데 실패했습니다.')
    }
    return response.json()
  },
}
