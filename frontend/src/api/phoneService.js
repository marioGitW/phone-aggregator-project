/**
 * Phone API Service
 * Handles all phone-related API calls to the Spring Boot backend
 */

import { API_BASE_URL, API_ENDPOINTS } from './config';

/**
 * Fetch phones from the backend with pagination
 * @param {number} page - Page number (0-indexed)
 * @param {number} size - Number of items per page
 * @returns {Promise<Object>} Response containing phones and pagination metadata
 * @throws {Error} If the request fails
 */
export const fetchPhones = async (page = 0, size = 20) => {
  try {
    const url = new URL(`${API_BASE_URL}${API_ENDPOINTS.PHONES}`);
    url.searchParams.append('page', page);
    url.searchParams.append('size', size);

    console.log(`[phoneService] Fetching from: ${url.toString()}`);

    const response = await fetch(url.toString());

    if (!response.ok) {
      throw new Error(`Backend returned status ${response.status}`);
    }

    const data = await response.json();
    console.log(`[phoneService] Received ${data.content?.length || 0} phones`);
    return data;
  } catch (error) {
    console.error('[phoneService] Error fetching phones:', error);
    throw error;
  }
};

