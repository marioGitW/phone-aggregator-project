/**
 * Phone API Service
 * Thin re-export over the data-access layer (see ./dataClient.js), which picks between the
 * live backend and static demo fixtures based on VITE_DATA_MODE. Components import from here
 * exactly as before either way.
 */

export {
  fetchPhones,
  fetchBrands,
  fetchSources,
  fetchColors,
  fetchStorageOptions,
  fetchRamOptions,
  fetchProductOffers,
  fetchPriceHistory,
} from './dataClient';
