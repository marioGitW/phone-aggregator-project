/**
 * Phone API Service
 * Handles all phone-related API calls to the Spring Boot backend
 */

import { API_BASE_URL, API_ENDPOINTS } from './config';

/**
 * Fetch phones from the backend with pagination and optional filters
 * @param {number} page - Page number (0-indexed)
 * @param {number} size - Number of items per page
 * @param {Object} filters - Optional filter object
 * @param {string} filters.search - Search term (searches title and rawTitle)
 * @param {Array<string>} filters.brands - List of brand names to filter by
 * @param {Array<string>} filters.sources - List of source/store names to filter by
 * @param {number} filters.minPrice - Minimum price filter
 * @param {number} filters.maxPrice - Maximum price filter
 * @param {string} filters.sort - Sort parameter (e.g., "price,asc")
 * @returns {Promise<Object>} Response containing phones and pagination metadata
 * @throws {Error} If the request fails
 */
export const fetchPhones = async (page = 0, size = 20, filters = {}) => {
  try {
    const url = new URL(`${API_BASE_URL}${API_ENDPOINTS.PHONES}`);

    // Add pagination parameters
    url.searchParams.append('page', page);
    url.searchParams.append('size', size);

    // Add filter parameters only if they have values
    if (filters.search) {
      url.searchParams.append('search', filters.search);
    }

    if (filters.brands && filters.brands.length > 0) {
      filters.brands.forEach(brand => {
        url.searchParams.append('brand', brand);
      });
    }

    if (filters.sources && filters.sources.length > 0) {
      filters.sources.forEach(source => {
        url.searchParams.append('source', source);
      });
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

    console.log(`[phoneService] Fetching from: ${url.toString()}`);

    const response = await fetch(url.toString());

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const data = await response.json();
    console.log(`[phoneService] Received ${data.content?.length || 0} phones`);
    console.log(`[phoneService] Total pages: ${data.totalPages}`);
    return data;
  } catch (error) {
    console.error('[phoneService] Error fetching phones:', error);
    throw error;
  }
};

/**
 * Fetch distinct brands from the backend metadata endpoint
 * @returns {Promise<Array<string>>} List of available brands
 * @throws {Error} If the request fails
 */
export const fetchBrands = async () => {
  try {
    const url = `${API_BASE_URL}/api/phones/brands`;
    console.log(`[phoneService] Fetching brands from: ${url}`);

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const brands = await response.json();
    console.log(`[phoneService] Received ${brands.length} brands`);
    return brands;
  } catch (error) {
    console.error('[phoneService] Error fetching brands:', error);
    throw error;
  }
};

