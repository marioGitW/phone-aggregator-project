import { useEffect, useState } from 'react';
import { fetchPhones, fetchBrands, fetchSources } from '../api/phoneService';
import PhoneCard from '../components/PhoneCard';
import BrandFilter from '../components/BrandFilter';
import PhoneFilters from '../components/PhoneFilters';

const createFilters = () => ({
  search: '',
  brands: [],
  sources: [],
  minPrice: '',
  maxPrice: '',
  sort: '',
});

const cloneFilters = (filters) => ({
  ...filters,
  brands: [...filters.brands],
  sources: [...filters.sources],
});

function SkeletonCard() {
  return (
    <div className="overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm">
      <div className="aspect-4/3 animate-pulse bg-neutral-100" />
      <div className="space-y-4 p-5">
        <div className="h-3 w-24 animate-pulse rounded-full bg-neutral-100" />
        <div className="space-y-2">
          <div className="h-4 w-full animate-pulse rounded-full bg-neutral-100" />
          <div className="h-4 w-4/5 animate-pulse rounded-full bg-neutral-100" />
        </div>
        <div className="flex items-center justify-between gap-3">
          <div className="h-7 w-28 animate-pulse rounded-full bg-neutral-100" />
          <div className="h-8 w-20 animate-pulse rounded-full bg-neutral-100" />
        </div>
      </div>
    </div>
  );
}

function FiltersPanel({
  filters,
  setFilters,
  availableBrands,
  availableSources,
  onBrandToggle,
  onApply,
  onReset,
}) {
  return (
    <div className="space-y-6">
      <BrandFilter
        brands={availableBrands}
        selectedBrands={filters.brands}
        onChange={onBrandToggle}
      />

      <PhoneFilters
        filters={filters}
        setFilters={setFilters}
        availableSources={availableSources}
      />

      <div className="rounded-3xl border border-neutral-200 bg-white p-5 shadow-sm">
        <div className="space-y-3">
          <p className="text-sm text-neutral-500">
            Changes in this panel are applied when you press Apply.
          </p>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              type="button"
              onClick={onReset}
              className="inline-flex items-center justify-center rounded-full border border-neutral-200 px-4 py-2.5 text-sm font-medium text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50"
            >
              Clear filters
            </button>
            <button
              type="button"
              onClick={onApply}
              className="inline-flex items-center justify-center rounded-full bg-sky-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-sky-700"
            >
              Apply filters
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function HomePage() {
  const [phones, setPhones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [availableBrands, setAvailableBrands] = useState([]);
  const [availableSources, setAvailableSources] = useState([]);
  const [searchInput, setSearchInput] = useState('');
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
  const [retryToken, setRetryToken] = useState(0);

  const [filters, setFilters] = useState(createFilters);
  const [draftFilters, setDraftFilters] = useState(createFilters);

  useEffect(() => {
    const loadPhones = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetchPhones(page, pageSize, filters);
        setPhones(response.content || []);
        setTotalPages(response.totalPages || 0);
      } catch {
        setError('Failed to load phones. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadPhones();
  }, [page, pageSize, filters, retryToken]);

  useEffect(() => {
    const loadMetadata = async () => {
      try {
        const [brands, sources] = await Promise.all([fetchBrands(), fetchSources()]);
        setAvailableBrands(brands || []);
        setAvailableSources(sources || []);
      } catch {
        // Keep browsing functional even if metadata endpoints fail.
        setAvailableBrands([]);
        setAvailableSources([]);
      }
    };

    loadMetadata();
  }, []);

  useEffect(() => {
    if (!mobileFiltersOpen) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setMobileFiltersOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mobileFiltersOpen]);

  const openMobileFilters = () => {
    setDraftFilters(cloneFilters(filters));
    setMobileFiltersOpen(true);
  };

  const updateBrandDraft = (brand) => {
    setDraftFilters((current) => {
      const updatedBrands = current.brands.includes(brand)
        ? current.brands.filter((currentBrand) => currentBrand !== brand)
        : [...current.brands, brand];

      return {
        ...current,
        brands: updatedBrands,
      };
    });
  };

  const applyDraftFilters = () => {
    const nextFilters = cloneFilters(draftFilters);
    setFilters(nextFilters);
    setPage(0);
    setMobileFiltersOpen(false);
  };

  const resetAllFilters = () => {
    const reset = createFilters();
    setFilters(reset);
    setDraftFilters(reset);
    setSearchInput('');
    setPage(0);
    setMobileFiltersOpen(false);
  };

  const handleSearch = () => {
    const nextFilters = {
      ...filters,
      search: searchInput.trim(),
    };

    setFilters(nextFilters);
    setDraftFilters((current) => ({
      ...current,
      search: searchInput.trim(),
    }));
    setPage(0);
  };

  const handleClearSearch = () => {
    const nextFilters = {
      ...filters,
      search: '',
    };

    setSearchInput('');
    setFilters(nextFilters);
    setDraftFilters((current) => ({
      ...current,
      search: '',
    }));
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

  const handleRetry = () => {
    setRetryToken((current) => current + 1);
  };

  if (error && !loading) {
    return (
      <div className="min-h-screen bg-neutral-50 px-4 py-12 sm:px-6 lg:px-8">
        <div className="mx-auto flex min-h-[60vh] max-w-2xl items-center justify-center">
          <div className="w-full rounded-4xl border border-neutral-200 bg-white p-8 text-center shadow-sm">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-rose-50 text-2xl text-rose-600">
              !
            </div>
            <h1 className="text-3xl font-semibold tracking-tight text-neutral-950">
              Something went wrong
            </h1>
            <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-neutral-600">
              {error}
            </p>
            <button
              type="button"
              onClick={handleRetry}
              className="mt-6 inline-flex items-center justify-center rounded-full bg-sky-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-sky-700"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  const totalPagesLabel = totalPages > 0 ? totalPages : 1;

  return (
    <div className="min-h-screen bg-neutral-50 text-neutral-900">
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        <header className="rounded-4xl border border-neutral-200 bg-white px-5 py-6 shadow-sm sm:px-8 sm:py-8">
          <div className="flex flex-col gap-5">
            <div className="space-y-3">
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-sky-600">
                Phone comparison
              </p>
              <h1 className="max-w-3xl text-3xl font-semibold tracking-tight text-neutral-950 sm:text-5xl">
                Find the best phone price with less noise.
              </h1>
              <p className="max-w-2xl text-sm leading-6 text-neutral-600 sm:text-base">
                Compare offers across stores with calm, minimal browsing. Search directly,
                then refine by brand, store, price, and sort order.
              </p>
            </div>

            <form
              className="flex flex-col gap-3 lg:flex-row lg:items-center"
              onSubmit={(event) => {
                event.preventDefault();
                handleSearch();
              }}
            >
              <div className="flex-1 rounded-2xl border border-neutral-200 bg-neutral-50 px-4 py-3 shadow-sm transition focus-within:ring-2 focus-within:ring-sky-100">
                <label htmlFor="phone-search" className="sr-only">
                  Search phones
                </label>
                <input
                  id="phone-search"
                  type="search"
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  placeholder="Search phones, brands, or models"
                  className="w-full border-0 bg-transparent text-sm text-neutral-900 focus:outline-none focus:ring-0 sm:text-base"
                />
              </div>

              <div className="flex items-center gap-3">
                <button
                  type="submit"
                  className="inline-flex items-center justify-center rounded-full bg-sky-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-sky-700"
                >
                  Search
                </button>
                <button
                  type="button"
                  onClick={handleClearSearch}
                  className="inline-flex items-center justify-center rounded-full border border-neutral-200 px-5 py-3 text-sm font-medium text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50"
                >
                  Clear
                </button>
                <button
                  type="button"
                  onClick={openMobileFilters}
                  className="inline-flex items-center justify-center rounded-full border border-neutral-200 px-5 py-3 text-sm font-medium text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50 lg:hidden"
                >
                  Filters
                </button>
              </div>
            </form>
          </div>
        </header>

        <div className="mt-8 grid gap-8 lg:grid-cols-[320px_minmax(0,1fr)]">
          <aside className="sticky top-8 hidden h-fit space-y-6 lg:block">
            <FiltersPanel
              filters={draftFilters}
              setFilters={setDraftFilters}
              availableBrands={availableBrands}
              availableSources={availableSources}
              onBrandToggle={updateBrandDraft}
              onApply={applyDraftFilters}
              onReset={resetAllFilters}
            />
          </aside>

          <section className="space-y-6">
            <div className="flex flex-col gap-3 rounded-4xl border border-neutral-200 bg-white px-5 py-4 shadow-sm sm:flex-row sm:items-center sm:justify-between sm:px-6">
              <div>
                <p className="text-sm font-medium text-neutral-900">
                  {phones.length > 0
                    ? `Showing ${phones.length} result${phones.length === 1 ? '' : 's'}`
                    : 'No phones found'}
                </p>
                <p className="mt-1 text-sm text-neutral-500">
                  Page {page + 1} of {totalPagesLabel}
                </p>
              </div>

              <div className="text-sm text-neutral-500">
                {filters.search ? (
                  <span>
                    Search: <span className="font-medium text-neutral-900">{filters.search}</span>
                  </span>
                ) : (
                  <span>Browse all available offers</span>
                )}
              </div>
            </div>

            {loading ? (
              <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
                {Array.from({ length: 8 }).map((_, index) => (
                  <SkeletonCard key={index} />
                ))}
              </div>
            ) : phones.length === 0 ? (
              <div className="flex min-h-[36vh] items-center justify-center rounded-4xl border border-dashed border-neutral-200 bg-white px-6 py-12 text-center shadow-sm">
                <div>
                  <h2 className="text-2xl font-semibold tracking-tight text-neutral-950">
                    No phones match your filters
                  </h2>
                  <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-neutral-600">
                    Try adjusting the search, brand, source, price, or sort controls.
                  </p>
                  <button
                    type="button"
                    onClick={resetAllFilters}
                    className="mt-6 inline-flex items-center justify-center rounded-full border border-neutral-200 px-5 py-2.5 text-sm font-medium text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50"
                  >
                    Clear filters
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
                  {phones.map((phone) => (
                    <PhoneCard key={phone.id} phone={phone} />
                  ))}
                </div>

                <div className="flex items-center justify-between rounded-4xl border border-neutral-200 bg-white px-4 py-4 shadow-sm sm:px-6">
                  <button
                    type="button"
                    onClick={previousPage}
                    disabled={page === 0}
                    className="inline-flex items-center gap-2 rounded-full px-3 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <span aria-hidden="true">←</span>
                    Previous
                  </button>

                  <span className="text-sm text-neutral-500">
                    Page {page + 1} of {totalPagesLabel}
                  </span>

                  <button
                    type="button"
                    onClick={nextPage}
                    disabled={page >= totalPages - 1}
                    className="inline-flex items-center gap-2 rounded-full px-3 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Next
                    <span aria-hidden="true">→</span>
                  </button>
                </div>
              </>
            )}
          </section>
        </div>
      </div>

      {mobileFiltersOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            onClick={() => setMobileFiltersOpen(false)}
            className="absolute inset-0 bg-neutral-950/40 backdrop-blur-[2px]"
            aria-label="Close filters"
          />

          <div className="absolute right-0 top-0 h-full w-full max-w-md overflow-y-auto border-l border-neutral-200 bg-neutral-50 shadow-2xl">
            <div className="sticky top-0 z-10 flex items-center justify-between border-b border-neutral-200 bg-neutral-50 px-5 py-4 backdrop-blur">
              <div>
                <p className="text-sm font-semibold text-neutral-900">Filters</p>
                <p className="text-xs text-neutral-500">Apply changes when ready.</p>
              </div>
              <button
                type="button"
                onClick={() => setMobileFiltersOpen(false)}
                className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-neutral-200 bg-white text-neutral-600 transition hover:bg-neutral-100"
                aria-label="Close filter drawer"
              >
                ×
              </button>
            </div>

            <div className="space-y-6 p-5">
              <FiltersPanel
                filters={draftFilters}
                setFilters={setDraftFilters}
                availableBrands={availableBrands}
                availableSources={availableSources}
                onBrandToggle={updateBrandDraft}
                onApply={applyDraftFilters}
                onReset={resetAllFilters}
              />
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

