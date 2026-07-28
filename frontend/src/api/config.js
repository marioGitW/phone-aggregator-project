/**
 * API Configuration
 * Uses Vite environment variables for backend URL
 */

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8083';

export const API_ENDPOINTS = {
  PHONES: '/api/phones',
};

