export default function BrandFilter({ brands, selectedBrands, onChange }) {
  return (
    <section className="rounded-3xl border border-neutral-200 bg-white p-5 shadow-sm">
      <div className="mb-4">
        <h3 className="text-sm font-semibold text-neutral-900">Brand</h3>
        <p className="mt-1 text-sm text-neutral-500">Filter by manufacturer.</p>
      </div>

      {brands.length === 0 ? (
        <p className="text-sm text-neutral-500">No brands available.</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {brands.map((brand) => (
            <label
              key={brand}
              className={`inline-flex cursor-pointer items-center gap-2 rounded-full border px-3 py-2 text-sm transition ${
                selectedBrands.includes(brand)
                  ? 'border-sky-200 bg-sky-50 text-sky-700'
                  : 'border-neutral-200 bg-white text-neutral-700 hover:border-neutral-300 hover:bg-neutral-50'
              }`}
            >
              <input
                type="checkbox"
                checked={selectedBrands.includes(brand)}
                onChange={() => onChange(brand)}
                className="h-4 w-4 rounded border-neutral-300 text-sky-600 focus:ring-sky-500"
              />
              <span className="capitalize">{brand}</span>
            </label>
          ))}
        </div>
      )}
    </section>
  );
}

