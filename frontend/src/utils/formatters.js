/**
 * Utility functions for formatting data
 */

/**
 * Format price with thousand separators.
 *
 * @param {number} price - Price in integer (e.g. 19990)
 * @returns {string} Formatted price (e.g. "19,990 MKD")
 */
export const formatPrice = (price) => {
  if (price === null || price === undefined) {
    return 'N/A';
  }

  return new Intl.NumberFormat('en-US').format(price) + ' MKD';
};

/**
 * Convert string to uppercase.
 *
 * @param {string} str - Input string
 * @returns {string} Uppercase string
 */
export const capitalize = (str) => {
  if (!str) return '';

  return str.toUpperCase();
};