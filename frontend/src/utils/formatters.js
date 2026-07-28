/**
 * Utility functions for formatting data
 */

/**
 * Format price with thousand separators
 * @param {number} price - Price in integer (e.g., 8890)
 * @returns {string} Formatted price (e.g., "8,890 MKD")
 */
export const formatPrice = (price) => {
  if (price === null || price === undefined) {
    return 'N/A';
  }
  return new Intl.NumberFormat('en-US').format(price) + ' MKD';
};

/**
 * Capitalize first letter of a string
 * @param {string} str - Input string
 * @returns {string} Capitalized string
 */
export const capitalize = (str) => {
  if (!str) return '';
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
};

