import { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check, X } from 'lucide-react';

// 2GB devices are effectively unusable today and clutter the filter for little benefit -
// still fully supported by the API/matching, just not offered as a UI choice.
const HIDDEN_RAM_OPTIONS = [2];

const SORT_OPTIONS = [
  { value: '', label: 'Default' },
  { value: 'price,asc', label: 'Price: Low → High' },
  { value: 'price,desc', label: 'Price: High → Low' },
];

// Canonical color name -> swatch hex. Mirrors the base color vocabulary
// ColorCanonicalizationService can produce on the backend. Unmapped names fall back to a
// neutral swatch rather than guessing.
const COLOR_SWATCHES = {
  black: '#171717',
  white: '#f5f5f5',
  gray: '#9ca3af',
  blue: '#3b82f6',
  silver: '#c0c0c0',
  green: '#22c55e',
  pink: '#ec4899',
  purple: '#a855f7',
  violet: '#8b5cf6',
  orange: '#f97316',
  cream: '#fdf6e3',
  graphite: '#4b5563',
  mint: '#6ee7b7',
  navy: '#1e3a5f',
  teal: '#14b8a6',
  sage: '#9caf88',
  gold: '#d4af37',
  red: '#ef4444',
  coral: '#ff7f50',
  cyan: '#06b6d4',
  titanium: '#878681',
  ultramarine: '#3f00ff',
  lavender: '#b57edc',
  yellow: '#eab308',
  bronze: '#cd7f32',
  olive: '#808000',
  peach: '#ffcba4',
  beige: '#e8d9c5',
  turquoise: '#40e0d0',
};
const FALLBACK_SWATCH = '#d4d4d8';

// 1024/2048 GB read far better as 1/2 TB than as raw gigabyte counts.
const formatStorageLabel = (gb) => (gb % 1024 === 0 ? `${gb / 1024} TB` : `${gb} GB`);

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

function ColorDropdown({ colors = [], selected = [], onToggle }) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const ref = useRef(null);

  useEffect(() => {
    const handleClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const filteredColors = colors.filter((color) =>
      color.toLowerCase().includes(search.trim().toLowerCase())
  );

  const triggerLabel =
      selected.length === 0
          ? 'All colors'
          : selected.length === 1
              ? <span className="capitalize">{selected[0]}</span>
              : `${selected.length} colors selected`;

  return (
      <div>
        <div className="relative" ref={ref}>
          <button
              type="button"
              onClick={() => setOpen((o) => !o)}
              className="flex w-full items-center justify-between rounded-2xl border border-neutral-200 bg-white px-4 py-3 text-sm text-neutral-900 shadow-sm outline-none transition focus:border-sky-300 focus:ring-2 focus:ring-sky-100"
          >
            <span>{triggerLabel}</span>
            <ChevronDown
                className={`h-4 w-4 text-neutral-400 transition-transform ${open ? 'rotate-180' : ''}`}
            />
          </button>

          {open && (
              <div className="absolute z-10 mt-2 w-full overflow-hidden rounded-2xl border border-neutral-200 bg-white shadow-lg">
                <div className="border-b border-neutral-100 p-2">
                  <input
                      type="text"
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      placeholder="Search colors..."
                      className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-900 outline-none transition focus:border-sky-300 focus:ring-2 focus:ring-sky-100"
                  />
                </div>

                <div className="max-h-64 overflow-y-auto p-1.5">
                  {filteredColors.length === 0 ? (
                      <p className="px-3 py-2.5 text-sm text-neutral-500">No colors match.</p>
                  ) : (
                      filteredColors.map((color) => {
                        const isSelected = selected.includes(color);
                        return (
                            <button
                                key={color}
                                type="button"
                                onClick={() => onToggle(color)}
                                className={`flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-left text-sm transition ${
                                    isSelected
                                        ? 'bg-sky-50 text-sky-900'
                                        : 'text-neutral-700 hover:bg-neutral-50'
                                }`}
                            >
                              <span
                                  aria-hidden="true"
                                  className="h-3 w-3 shrink-0 rounded-full border border-neutral-300"
                                  style={{ backgroundColor: COLOR_SWATCHES[color] || FALLBACK_SWATCH }}
                              />
                              <span className="flex-1 capitalize">{color}</span>
                              {isSelected && <Check className="h-4 w-4 text-sky-600" />}
                            </button>
                        );
                      })
                  )}
                </div>
              </div>
          )}
        </div>

        {selected.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              {selected.map((color) => (
                  <span
                      key={color}
                      className="inline-flex items-center gap-1.5 rounded-full border border-sky-200 bg-sky-50 py-1.5 pl-3 pr-2 text-xs font-medium text-sky-700"
                  >
                    <span
                        aria-hidden="true"
                        className="h-2.5 w-2.5 shrink-0 rounded-full border border-sky-300"
                        style={{ backgroundColor: COLOR_SWATCHES[color] || FALLBACK_SWATCH }}
                    />
                    <span className="capitalize">{color}</span>
                    <button
                        type="button"
                        onClick={() => onToggle(color)}
                        className="rounded-full p-0.5 text-sky-600 transition hover:bg-sky-100"
                        aria-label={`Remove ${color} filter`}
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </span>
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
                                       availableColors = [],
                                       availableStorage = [],
                                       availableRam = [],
                                     }) {
  const [priceError, setPriceError] = useState('');
  const visibleRamOptions = availableRam.filter((ram) => !HIDDEN_RAM_OPTIONS.includes(ram));

  const handleSourceChange = (source) => {
    const updatedSources = filters.sources && filters.sources.includes(source)
        ? filters.sources.filter((s) => s !== source)
        : [ ...(filters.sources || []), source ];

    setFilters({
      ...filters,
      sources: updatedSources,
    });
  };

  const handleColorChange = (color) => {
    const updatedColors = filters.colors && filters.colors.includes(color)
        ? filters.colors.filter((c) => c !== color)
        : [ ...(filters.colors || []), color ];

    setFilters({
      ...filters,
      colors: updatedColors,
    });
  };

  const handleStorageChange = (storage) => {
    const updatedStorage = filters.storage && filters.storage.includes(storage)
        ? filters.storage.filter((s) => s !== storage)
        : [ ...(filters.storage || []), storage ];

    setFilters({
      ...filters,
      storage: updatedStorage,
    });
  };

  const handleRamChange = (ram) => {
    const updatedRam = filters.ram && filters.ram.includes(ram)
        ? filters.ram.filter((r) => r !== ram)
        : [ ...(filters.ram || []), ram ];

    setFilters({
      ...filters,
      ram: updatedRam,
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
            <h3 className="text-sm font-semibold text-neutral-900">Color</h3>
            <p className="mt-1 text-sm text-neutral-500">Narrow by device color.</p>
          </div>
          {availableColors.length === 0 ? (
              <p className="text-sm text-neutral-500">No colors available.</p>
          ) : (
              <ColorDropdown
                  colors={availableColors}
                  selected={filters.colors || []}
                  onToggle={handleColorChange}
              />
          )}
        </div>

        <div>
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-neutral-900">Storage</h3>
            <p className="mt-1 text-sm text-neutral-500">Narrow by storage capacity.</p>
          </div>
          {availableStorage.length === 0 ? (
              <p className="text-sm text-neutral-500">No storage options available.</p>
          ) : (
              <div className="flex flex-wrap gap-2">
                {availableStorage.map((storage) => (
                    <label
                        key={storage}
                        className={`inline-flex cursor-pointer items-center gap-2 rounded-full border px-3 py-2 text-sm transition ${
                            filters.storage && filters.storage.includes(storage)
                                ? 'border-sky-200 bg-sky-50 text-sky-700'
                                : 'border-neutral-200 bg-white text-neutral-700 hover:border-neutral-300 hover:bg-neutral-50'
                        }`}
                    >
                      <input
                          type="checkbox"
                          checked={filters.storage && filters.storage.includes(storage)}
                          onChange={() => handleStorageChange(storage)}
                          className="h-4 w-4 rounded border-neutral-300 text-sky-600 focus:ring-sky-500"
                      />
                      <span>{formatStorageLabel(storage)}</span>
                    </label>
                ))}
              </div>
          )}
        </div>

        <div>
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-neutral-900">RAM</h3>
            <p className="mt-1 text-sm text-neutral-500">Narrow by memory size.</p>
          </div>
          {visibleRamOptions.length === 0 ? (
              <p className="text-sm text-neutral-500">No RAM options available.</p>
          ) : (
              <div className="flex flex-wrap gap-2">
                {visibleRamOptions.map((ram) => (
                    <label
                        key={ram}
                        className={`inline-flex cursor-pointer items-center gap-2 rounded-full border px-3 py-2 text-sm transition ${
                            filters.ram && filters.ram.includes(ram)
                                ? 'border-sky-200 bg-sky-50 text-sky-700'
                                : 'border-neutral-200 bg-white text-neutral-700 hover:border-neutral-300 hover:bg-neutral-50'
                        }`}
                    >
                      <input
                          type="checkbox"
                          checked={filters.ram && filters.ram.includes(ram)}
                          onChange={() => handleRamChange(ram)}
                          className="h-4 w-4 rounded border-neutral-300 text-sky-600 focus:ring-sky-500"
                      />
                      <span>{ram} GB</span>
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