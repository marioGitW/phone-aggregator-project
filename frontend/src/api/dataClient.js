/**
 * Data-access layer entry point. Picks between the live backend and the static demo
 * fixtures based on VITE_DATA_MODE, so phoneService.js/analyticsService.js (and everything
 * importing DATA_MODE, like App.jsx's Demo badge/notice) don't need to know which one is
 * active - both clients implement the exact same function signatures.
 */

import * as apiDataClient from './clients/apiDataClient';
import * as staticDataClient from './clients/staticDataClient';

export const DATA_MODE = import.meta.env.VITE_DATA_MODE === 'static' ? 'static' : 'api';

const impl = DATA_MODE === 'static' ? staticDataClient : apiDataClient;

export const fetchPhones = impl.fetchPhones;
export const fetchBrands = impl.fetchBrands;
export const fetchSources = impl.fetchSources;
export const fetchColors = impl.fetchColors;
export const fetchStorageOptions = impl.fetchStorageOptions;
export const fetchRamOptions = impl.fetchRamOptions;
export const fetchProductOffers = impl.fetchProductOffers;
export const fetchPriceHistory = impl.fetchPriceHistory;
export const fetchAveragePriceByBrand = impl.fetchAveragePriceByBrand;
export const fetchListingsPerSource = impl.fetchListingsPerSource;
export const fetchPriceComparison = impl.fetchPriceComparison;
export const fetchCheapestPerBrand = impl.fetchCheapestPerBrand;
export const fetchPriceDistribution = impl.fetchPriceDistribution;
export const searchPhones = impl.searchPhones;
