/**
 * Analytics API Service
 * Handles all analytics-related API calls to the Spring Boot backend
 */

import { API_BASE_URL } from './config';

/**
 * Fetch average price by brand
 * @returns {Promise<Array>} Array of {brand, averagePrice}
 * @throws {Error} If the request fails
 */
export const fetchAveragePriceByBrand = async () => {
  try {
    const url = `${API_BASE_URL}/api/analytics/average-price-by-brand`;
    console.log(`[analyticsService] Fetching average price by brand from: ${url}`);

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const data = await response.json();
    console.log(`[analyticsService] Received data for ${data.length} brands`);
    return data;
  } catch (error) {
    console.error('[analyticsService] Error fetching average price by brand:', error);
    throw error;
  }
};

/**
 * Fetch listings count per source
 * @returns {Promise<Array>} Array of {source, count}
 * @throws {Error} If the request fails
 */
export const fetchListingsPerSource = async () => {
  try {
    const url = `${API_BASE_URL}/api/analytics/listings-per-source`;
    console.log(`[analyticsService] Fetching listings per source from: ${url}`);

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const data = await response.json();
    console.log(`[analyticsService] Received data for ${data.length} sources`);
    return data;
  } catch (error) {
    console.error('[analyticsService] Error fetching listings per source:', error);
    throw error;
  }
};

/**
 * Fetch price comparison across sources for a specific phone
 * @param {string} normalizedTitle The normalized title of the phone
 * @returns {Promise<Array>} Array of {source, price}
 * @throws {Error} If the request fails
 */
export const fetchPriceComparison = async (normalizedTitle) => {
  try {
    const url = new URL(`${API_BASE_URL}/api/analytics/price-comparison`);
    url.searchParams.append('normalizedTitle', normalizedTitle);

    console.log(`[analyticsService] Fetching price comparison from: ${url.toString()}`);

    const response = await fetch(url.toString());

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const data = await response.json();
    console.log(`[analyticsService] Received price data for ${data.length} sources`);
    return data;
  } catch (error) {
    console.error('[analyticsService] Error fetching price comparison:', error);
    throw error;
  }
};

/**
 * Fetch the cheapest phone per brand
 * @returns {Promise<Object>} Object with brands as keys and cheapest phone data as values
 * @throws {Error} If the request fails
 */
export const fetchCheapestPerBrand = async () => {
  try {
    const url = `${API_BASE_URL}/api/analytics/cheapest-per-brand`;
    console.log(`[analyticsService] Fetching cheapest phones per brand from: ${url}`);

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const data = await response.json();
    console.log(`[analyticsService] Received cheapest phone data for ${Object.keys(data).length} brands`);
    return data;
  } catch (error) {
    console.error('[analyticsService] Error fetching cheapest per brand:', error);
    throw error;
  }
};

/**
 * Additions to analyticsService.js
 * Drop these two functions in alongside the existing four — same style,
 * same error handling pattern, same base URL usage.
 */

/**
 * Fetch phone counts bucketed into price ranges (e.g. 0-10k, 10-20k, ...)
 * @returns {Promise<Array>} Array of {range, count}
 * @throws {Error} If the request fails
 */
export const fetchPriceDistribution = async () => {
  try {
    const url = `${API_BASE_URL}/api/analytics/price-distribution`;
    console.log(`[analyticsService] Fetching price distribution from: ${url}`);

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const data = await response.json();
    console.log(`[analyticsService] Received ${data.length} price buckets`);
    return data;
  } catch (error) {
    console.error('[analyticsService] Error fetching price distribution:', error);
    throw error;
  }
};

/**
 * Search phones by title, for the source-comparison picker
 * @param {string} query Free-text search term
 * @returns {Promise<Array>} Array of {title, normalizedTitle}
 * @throws {Error} If the request fails
 */
export const searchPhones = async (query) => {
  try {
    const url = new URL(`${API_BASE_URL}/api/phones/search`);
    url.searchParams.append('q', query);

    console.log(`[analyticsService] Searching phones from: ${url.toString()}`);

    const response = await fetch(url.toString());

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const data = await response.json();
    console.log(`[analyticsService] Received ${data.length} search results`);
    return data;
  } catch (error) {
    console.error('[analyticsService] Error searching phones:', error);
    throw error;
  }
};

