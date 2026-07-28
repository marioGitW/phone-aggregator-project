import './BrandFilter.css';

export default function BrandFilter({ brands, selectedBrands, onChange }) {
  return (
    <div className="brand-filter">
      <h3 className="brand-filter__title">Brand</h3>

      {brands.length === 0 ? (
        <p className="brand-filter__empty">No brands available.</p>
      ) : (
        <div className="brand-filter__list">
          {brands.map((brand) => (
            <label key={brand} className="brand-filter__item">
              <input
                type="checkbox"
                checked={selectedBrands.includes(brand)}
                onChange={() => onChange(brand)}
              />
              <span>{brand}</span>
            </label>
          ))}
        </div>
      )}
    </div>
  );
}

