/**
 * Environment Configuration
 *
 * Centralized environment variables using Vite
 */

/**
 * API Gateway Base URL
 * @default http://localhost:8080
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
