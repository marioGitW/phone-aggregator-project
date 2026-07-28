import './PhoneFilters.css';
import { useState, useEffect } from 'react';

export default function PhoneFilters({
  filters,
  setFilters,
  availableSources = [],
  searchInput,
  setSearchInput,
  onSearch,
  onClearSearch,
}) {
  const [priceError, setPriceError] = useState('');
  const [minPriceInput, setMinPriceInput] = useState(filters.minPrice || '');
  const [maxPriceInput, setMaxPriceInput] = useState(filters.maxPrice || '');

  const handleSourceChange = (source) => {
    const updatedSources = filters.sources && filters.sources.includes(source)
      ? filters.sources.filter((s) => s !== source)
      : [ ...(filters.sources || []), source ];

    setFilters({
      ...filters,
      sources: updatedSources,
    });
  };

  // Local input handlers: update local state only while typing
  const handleMinPriceChange = (e) => {
    const value = e.target.value;
    if (value === '') {
      setMinPriceInput('');
      setPriceError('');
      return;
    }
    const num = Number(value);
    if (Number.isNaN(num) || num < 0) return;
    setMinPriceInput(value);
    setPriceError('');
  };

  const handleMaxPriceChange = (e) => {
    const value = e.target.value;
    if (value === '') {
      setMaxPriceInput('');
      setPriceError('');
      return;
    }
    const num = Number(value);
    if (Number.isNaN(num) || num < 0) return;
    setMaxPriceInput(value);
    setPriceError('');
  };

  // Apply filters when user presses Enter
  const handleMinPriceEnter = (e) => {
    if (e.key !== 'Enter') return;
    if (minPriceInput === '') {
      setFilters({ ...filters, minPrice: '' });
      setPriceError('');
      return;
    }
    const num = Number(minPriceInput);
    if (Number.isNaN(num) || num < 0) {
      setPriceError('Minimum price must be a non-negative number');
      return;
    }
    if (maxPriceInput !== '' && Number(maxPriceInput) < num) {
      setPriceError('Minimum price cannot be greater than maximum price');
      return;
    }
    setPriceError('');
    setFilters({ ...filters, minPrice: minPriceInput });
  };

  const handleMaxPriceEnter = (e) => {
    if (e.key !== 'Enter') return;
    if (maxPriceInput === '') {
      setFilters({ ...filters, maxPrice: '' });
      setPriceError('');
      return;
    }
    const num = Number(maxPriceInput);
    if (Number.isNaN(num) || num < 0) {
      setPriceError('Maximum price must be a non-negative number');
      return;
    }
    if (minPriceInput !== '' && num < Number(minPriceInput)) {
      setPriceError('Maximum price must be greater than or equal to minimum price');
      return;
    }
    setPriceError('');
    setFilters({ ...filters, maxPrice: maxPriceInput });
  };

  // Sync local inputs when external filters change (e.g., Reset)
  useEffect(() => {
    setMinPriceInput(filters.minPrice || '');
    setMaxPriceInput(filters.maxPrice || '');
  }, [filters.minPrice, filters.maxPrice]);

  return (
    <div className="phone-filters">
      <label className="phone-filters__label" htmlFor="phone-search">
        Search phones
      </label>
      <div className="phone-filters__search-row">
        <input
          id="phone-search"
          type="text"
          placeholder="Search phones..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              onSearch();
            }
          }}
          className="phone-filters__input"
        />

        <button type="button" onClick={onSearch} className="phone-filters__button">
          Search
        </button>

        <button
          type="button"
          onClick={onClearSearch}
          className="phone-filters__button phone-filters__button--secondary"
        >
          Clear
        </button>
      </div>

      <div className="phone-filters__section">
        <h4 className="phone-filters__section-title">Source</h4>
        {availableSources.length === 0 ? (
          <p className="phone-filters__empty">No sources available.</p>
        ) : (
          <div className="phone-filters__checkboxes">
            {availableSources.map((source) => (
              <label key={source} className="phone-filters__checkbox-item">
                <input
                  type="checkbox"
                  checked={filters.sources && filters.sources.includes(source)}
                  onChange={() => handleSourceChange(source)}
                />
                <span>{source}</span>
              </label>
            ))}
          </div>
        )}
      </div>

      <div className="phone-filters__section phone-filters__price-section">
        <h4 className="phone-filters__section-title">Price Range</h4>
        <div className="phone-filters__price-row">
          <label className="phone-filters__price-label">
            Min Price
            <div className="phone-filters__price-input-row">
              <input
                type="number"
                min="0"
                placeholder="Min price"
                value={minPriceInput}
                onChange={handleMinPriceChange}
                onKeyDown={handleMinPriceEnter}
                className="phone-filters__input phone-filters__input--price"
              />
              <span className="phone-filters__price-currency">MKD</span>
            </div>
          </label>

          <label className="phone-filters__price-label">
            Max Price
            <div className="phone-filters__price-input-row">
              <input
                type="number"
                min="0"
                placeholder="Max price"
                value={maxPriceInput}
                onChange={handleMaxPriceChange}
                onKeyDown={handleMaxPriceEnter}
                className="phone-filters__input phone-filters__input--price"
              />
              <span className="phone-filters__price-currency">MKD</span>
            </div>
          </label>
        </div>
        {priceError && <p className="phone-filters__price-error">{priceError}</p>}
      </div>
    </div>
  );
}


