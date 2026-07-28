/**
 * HomePage
 * Fetches phones from the backend and manages page/filter state
 */

import { useState, useEffect } from 'react';
import { fetchPhones, fetchBrands } from '../api/phoneService';
import PhoneCard from '../components/PhoneCard';
import BrandFilter from '../components/BrandFilter';
import PhoneFilters from '../components/PhoneFilters';
import './HomePage.css';

export default function HomePage() {
  const [phones, setPhones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [availableBrands, setAvailableBrands] = useState([]);
  const [searchInput, setSearchInput] = useState('');

  // Filter state
  const [filters, setFilters] = useState({
    search: '',
    brands: [],
    sources: [],
    minPrice: '',
    maxPrice: '',
    sort: ''
  });

  useEffect(() => {
    const loadPhones = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetchPhones(page, pageSize, filters);

        console.log('Full response from backend:', response);
        console.log('Phones received:', response.content);

        setPhones(response.content || []);
        setTotalPages(response.totalPages || 0);
      } catch (err) {
        console.error('Failed to load phones:', err);
        setError(err.message || 'Failed to load phones. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadPhones();
  }, [page, pageSize, filters]);

  /**
   * Load available brands on component mount
   * This is independent from phone pagination/filtering
   * so the brand list remains consistent regardless of current filters
   */
  useEffect(() => {
    const loadBrands = async () => {
      try {
        const brands = await fetchBrands();
        setAvailableBrands(brands);
      } catch (err) {
        console.error('Failed to load brands:', err);
        // Don't show error to user for brands; they can still browse without filters
      }
    };

    loadBrands();
  }, []);

  /**
   * Update filters and reset pagination to first page
   * Ensures pagination is valid with new filter results
   */
  const updateFilters = (newFilters) => {
    setFilters(newFilters);
    setPage(0);  // Reset to first page
  };

  const handleSearch = () => {
    updateFilters({
      ...filters,
      search: searchInput,
    });
  };

  const handleClearSearch = () => {
    setSearchInput('');
    updateFilters({
      ...filters,
      search: '',
    });
  };

  const handleBrandChange = (brand) => {
    const updatedBrands = filters.brands.includes(brand)
      ? filters.brands.filter((currentBrand) => currentBrand !== brand)
      : [...filters.brands, brand];

    setFilters({
      ...filters,
      brands: updatedBrands,
    });

    setPage(0);
  };

  const nextPage = () => {
    if (page < totalPages - 1) {
      setPage(page + 1);
    }
  };

  const previousPage = () => {
    if (page > 0) {
      setPage(page - 1);
    }
  };

  if (loading) {
    return <div className="home-page"><p>Loading phones...</p></div>;
  }

  if (error) {
    return (
      <div className="home-page">
        <p className="error">Error: {error}</p>
        <button onClick={() => window.location.reload()}>Retry</button>
      </div>
    );
  }

  return (
    <div className="home-page">
      <h1>Available Phones</h1>
      <BrandFilter
        brands={availableBrands}
        selectedBrands={filters.brands}
        onChange={handleBrandChange}
      />
      <PhoneFilters
        filters={filters}
        searchInput={searchInput}
        setSearchInput={setSearchInput}
        onSearch={handleSearch}
        onClearSearch={handleClearSearch}
      />
      <p className="info">Found {phones.length} phones (Page {page + 1} of {totalPages})</p>

      {/* Debug info to verify filters are being sent */}
      <details style={{ marginBottom: '1rem', padding: '0.5rem', backgroundColor: '#f5f5f5' }}>
        <summary style={{ cursor: 'pointer', fontWeight: 'bold' }}>
          Debug: Current Filters
        </summary>
        <pre style={{ fontSize: '0.85rem', overflow: 'auto' }}>
          {JSON.stringify(filters, null, 2)}
        </pre>
      </details>

      {phones.length === 0 ? (
        <p>No phones found.</p>
      ) : (
        <>
          <div className="phones-list">
            {phones.map((phone) => (
              <PhoneCard key={phone.id} phone={phone} />
            ))}
          </div>

          <div className="pagination">
            <button
              onClick={previousPage}
              disabled={page === 0}
              className="pagination-btn"
            >
              ← Previous
            </button>

            <span className="pagination-info">
              Page {page + 1} of {totalPages}
            </span>

            <button
              onClick={nextPage}
              disabled={page === totalPages - 1}
              className="pagination-btn"
            >
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  );
}

