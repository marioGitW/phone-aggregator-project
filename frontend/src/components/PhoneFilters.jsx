import './PhoneFilters.css';

export default function PhoneFilters({
  searchInput,
  setSearchInput,
  onSearch,
  onClearSearch,
}) {
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
    </div>
  );
}


