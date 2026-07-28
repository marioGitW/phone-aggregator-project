import './PhoneFilters.css';

export default function PhoneFilters({
  filters,
  setFilters,
  availableSources = [],
  searchInput,
  setSearchInput,
  onSearch,
  onClearSearch,
}) {
  const handleSourceChange = (source) => {
    const updatedSources = filters.sources && filters.sources.includes(source)
      ? filters.sources.filter((s) => s !== source)
      : [ ...(filters.sources || []), source ];

    setFilters({
      ...filters,
      sources: updatedSources,
    });
  };

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
    </div>
  );
}


