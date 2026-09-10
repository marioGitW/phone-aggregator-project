/**
 * Live-backend data client.
 * Talks directly to the Spring Boot backend over HTTP. This is the exact
 * implementation phoneService.js/analyticsService.js used before they were
 * split behind ./dataClient.js - same endpoints, same error handling.
 */

import { API_BASE_URL, API_ENDPOINTS } from '../config';

export const fetchPhones = async (page = 0, size = 20, filters = {}) => {
  const url = new URL(`${API_BASE_URL}${API_ENDPOINTS.PHONES}`);

  url.searchParams.append('page', page);
  url.searchParams.append('size', size);

  if (filters.search) {
    url.searchParams.append('search', filters.search);
  }
  if (filters.brands && filters.brands.length > 0) {
    filters.brands.forEach((brand) => url.searchParams.append('brand', brand));
  }
  if (filters.sources && filters.sources.length > 0) {
    filters.sources.forEach((source) => url.searchParams.append('source', source));
  }
  if (filters.colors && filters.colors.length > 0) {
    filters.colors.forEach((color) => url.searchParams.append('color', color));
  }
  if (filters.storage && filters.storage.length > 0) {
    filters.storage.forEach((storage) => url.searchParams.append('storage', storage));
  }
  if (filters.ram && filters.ram.length > 0) {
    filters.ram.forEach((ram) => url.searchParams.append('ram', ram));
  }
  if (filters.minPrice) {
    url.searchParams.append('minPrice', filters.minPrice);
  }
  if (filters.maxPrice) {
    url.searchParams.append('maxPrice', filters.maxPrice);
  }
  if (filters.sort) {
    url.searchParams.append('sort', filters.sort);
  }

  const response = await fetch(url.toString());
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchBrands = async () => {
  const response = await fetch(`${API_BASE_URL}/api/phones/brands`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchSources = async () => {
  const response = await fetch(`${API_BASE_URL}/api/phones/sources`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchColors = async () => {
  const response = await fetch(`${API_BASE_URL}/api/phones/colors`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchStorageOptions = async () => {
  const response = await fetch(`${API_BASE_URL}/api/phones/storage`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchRamOptions = async () => {
  const response = await fetch(`${API_BASE_URL}/api/phones/ram`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchProductOffers = async (id) => {
  const response = await fetch(`${API_BASE_URL}/api/products/${id}/offers`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchPriceHistory = async (phoneModelId) => {
  const response = await fetch(`${API_BASE_URL}/api/models/${phoneModelId}/price-history`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchAveragePriceByBrand = async () => {
  const response = await fetch(`${API_BASE_URL}/api/analytics/average-price-by-brand`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchListingsPerSource = async () => {
  const response = await fetch(`${API_BASE_URL}/api/analytics/listings-per-source`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchPriceComparison = async (normalizedTitle) => {
  const url = new URL(`${API_BASE_URL}/api/analytics/price-comparison`);
  url.searchParams.append('normalizedTitle', normalizedTitle);

  const response = await fetch(url.toString());
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchCheapestPerBrand = async () => {
  const response = await fetch(`${API_BASE_URL}/api/analytics/cheapest-per-brand`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

export const fetchPriceDistribution = async () => {
  const response = await fetch(`${API_BASE_URL}/api/analytics/price-distribution`);
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};

/**
 * NOTE: there is no /api/phones/search endpoint on the backend - this was already
 * true before the demo-mode split (analyticsService.js called this same URL). Kept
 * as-is for parity; PhoneAnalyticsPage's search box will fail against live mode
 * until that endpoint exists.
 */
export const searchPhones = async (query) => {
  const url = new URL(`${API_BASE_URL}/api/phones/search`);
  url.searchParams.append('q', query);

  const response = await fetch(url.toString());
  if (!response.ok) {
    throw new Error(`Backend returned status ${response.status}`);
  }
  return response.json();
};
