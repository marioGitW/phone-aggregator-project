import { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check } from 'lucide-react';

const SORT_OPTIONS = [
  { value: '', label: 'Default' },
  { value: 'price,asc', label: 'Price: Low → High' },
  { value: 'price,desc', label: 'Price: High → Low' },
];

function SortDropdown({ value, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const selected = SORT_OPTIONS.find((o) => o.value === value) || SORT_OPTIONS[0];

  useEffect(() => {
    const handleClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  return (
      <div className="relative" ref={ref}>
        <button
            type="button"
            onClick={() => setOpen((o) => !o)}
            className="flex w-full items-center justify-between rounded-2xl border border-neutral-200 bg-white px-4 py-3 text-sm text-neutral-900 shadow-sm outline-none transition focus:border-sky-300 focus:ring-2 focus:ring-sky-100"
        >
          <span>{selected.label}</span>
          <ChevronDown
              className={`h-4 w-4 text-neutral-400 transition-transform ${open ? 'rotate-180' : ''}`}
          />
        </button>

        {open && (
            <div className="absolute z-10 mt-2 w-full overflow-hidden rounded-2xl border border-neutral-200 bg-white p-1.5 shadow-lg">
              {SORT_OPTIONS.map((opt) => (
                  <button
                      key={opt.value}
                      type="button"
                      onClick={() => {
                        onChange(opt.value);
                        setOpen(false);
                      }}
                      className={`flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-sm transition ${
                          opt.value === value
                              ? 'bg-sky-50 text-sky-900'
                              : 'text-neutral-700 hover:bg-neutral-50'
                      }`}
                  >
                    {opt.label}
                    {opt.value === value && <Check className="h-4 w-4 text-sky-600" />}
                  </button>
              ))}
            </div>
        )}
      </div>
  );
}

export default function PhoneFilters({
                                       filters,
                                       setFilters,
                                       availableSources = [],
                                     }) {
  const [priceError, setPriceError] = useState('');

  const handleSourceChange = (source) => {
    const updatedSources = filters.sources && filters.sources.includes(source)
        ? filters.sources.filter((s) => s !== source)
        : [ ...(filters.sources || []), source ];

    setFilters({
      ...filters,
      sources: updatedSources,
    });
  };

  const handleMinPriceChange = (e) => {
    const value = e.target.value;
    if (value === '') {
      setFilters({ ...filters, minPrice: '' });
      setPriceError('');
      return;
    }
    const num = Number(value);
    if (Number.isNaN(num) || num < 0) return;

    const nextFilters = { ...filters, minPrice: value };
    if (nextFilters.maxPrice !== '' && Number(nextFilters.maxPrice) < num) {
      setPriceError('Minimum price cannot be greater than maximum price');
    } else {
      setPriceError('');
    }

    setFilters(nextFilters);
  };

  const handleMaxPriceChange = (e) => {
    const value = e.target.value;
    if (value === '') {
      setFilters({ ...filters, maxPrice: '' });
      setPriceError('');
      return;
    }
    const num = Number(value);
    if (Number.isNaN(num) || num < 0) return;

    const nextFilters = { ...filters, maxPrice: value };
    if (nextFilters.minPrice !== '' && Number(nextFilters.minPrice) > num) {
      setPriceError('Minimum price cannot be greater than maximum price');
    } else {
      setPriceError('');
    }

    setFilters(nextFilters);
  };

  return (
      <div className="space-y-6">
        <div>
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-neutral-900">Source</h3>
            <p className="mt-1 text-sm text-neutral-500">Narrow by store or marketplace.</p>
          </div>
          {availableSources.length === 0 ? (
              <p className="text-sm text-neutral-500">No sources available.</p>
          ) : (
              <div className="flex flex-wrap gap-2">
                {availableSources.map((source) => (
                    <label
                        key={source}
                        className={`inline-flex cursor-pointer items-center gap-2 rounded-full border px-3 py-2 text-sm transition ${
                            filters.sources && filters.sources.includes(source)
                                ? 'border-sky-200 bg-sky-50 text-sky-700'
                                : 'border-neutral-200 bg-white text-neutral-700 hover:border-neutral-300 hover:bg-neutral-50'
                        }`}
                    >
                      <input
                          type="checkbox"
                          checked={filters.sources && filters.sources.includes(source)}
                          onChange={() => handleSourceChange(source)}
                          className="h-4 w-4 rounded border-neutral-300 text-sky-600 focus:ring-sky-500"
                      />
                      <span className="capitalize">{source}</span>
                    </label>
                ))}
              </div>
          )}
        </div>

        <div>
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-neutral-900">Price range</h3>
            <p className="mt-1 text-sm text-neutral-500">Set a minimum and maximum price.</p>
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label className="space-y-2 text-sm font-medium text-neutral-700">
              <span>Min price</span>
              <div className="flex items-center gap-2 rounded-2xl border border-neutral-200 bg-white px-3 py-2 shadow-sm focus-within:ring-2 focus-within:ring-sky-100">
                <input
                    type="number"
                    min="0"
                    placeholder="0"
                    value={filters.minPrice || ''}
                    onChange={handleMinPriceChange}
                    className="w-full border-0 bg-transparent p-0 text-sm text-neutral-900 focus:outline-none focus:ring-0"
                />
                <span className="shrink-0 text-sm font-medium text-neutral-500">MKD</span>
              </div>
            </label>

            <label className="space-y-2 text-sm font-medium text-neutral-700">
              <span>Max price</span>
              <div className="flex items-center gap-2 rounded-2xl border border-neutral-200 bg-white px-3 py-2 shadow-sm focus-within:ring-2 focus-within:ring-sky-100">
                <input
                    type="number"
                    min="0"
                    placeholder="0"
                    value={filters.maxPrice || ''}
                    onChange={handleMaxPriceChange}
                    className="w-full border-0 bg-transparent p-0 text-sm text-neutral-900 focus:outline-none focus:ring-0"
                />
                <span className="shrink-0 text-sm font-medium text-neutral-500">MKD</span>
              </div>
            </label>
          </div>
          {priceError && <p className="mt-2 text-sm text-rose-600">{priceError}</p>}
        </div>

        <div>
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-neutral-900">Sort by</h3>
            <p className="mt-1 text-sm text-neutral-500">Choose how results should be ordered.</p>
          </div>
          <SortDropdown
              value={filters.sort || ''}
              onChange={(val) => setFilters({ ...filters, sort: val })}
          />
        </div>
      </div>
  );
}