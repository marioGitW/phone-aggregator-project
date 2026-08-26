/**
 * PhoneAnalyticsPage
 * Top-level analytics dashboard page for the phone price-comparison app.
 * Apple.com-inspired: full-width stacked "panels", generous white space,
 * near-black text on off-white, single sky-blue accent used sparingly.
 *
 * Composes the five chart/card components (already built):
 *   - AvgPriceByBrandChart
 *   - ListingsPerSourceChart
 *   - SourceComparisonChart
 *   - PriceDistributionChart
 *   - CheapestPhoneCard
 *
 * NOTE ON API CLIENT: this file uses a small local `fetch` wrapper against
 * BASE_URL below. If your project already has a shared API client (e.g.
 * src/api/client.js or an axios instance), swap `apiGet` for that instead —
 * the shape of each `apiGet(...)` call below tells you exactly what each
 * endpoint needs to return.
 */

import { useEffect, useState } from 'react';

import AvgPriceByBrandChart from '../components/AvgPriceByBrandChart';
import ListingsPerSourceChart from '../components/ListingsPerSourceChart';
import SourceComparisonChart from '../components/SourceComparisonChart';
import PriceDistributionChart from '../components/PriceDistributionChart';
import CheapestPhoneCard from '../components/CheapestPhoneCard';

import {
  fetchAveragePriceByBrand,
  fetchListingsPerSource,
  fetchPriceComparison,
  fetchCheapestPerBrand,
  fetchPriceDistribution,
  searchPhones,
} from '../api/analyticsService';
import {Link} from "react-router-dom";

function SectionHeader({ eyebrow, title, description }) {
  return (
    <div className="mx-auto mb-10 max-w-2xl text-center">
      {eyebrow && (
        <p className="mb-3 text-xs font-semibold uppercase tracking-[0.28em] text-sky-600">
          {eyebrow}
        </p>
      )}
      <h2 className="text-3xl font-semibold tracking-tight text-neutral-950 sm:text-4xl">
        {title}
      </h2>
      {description && (
        <p className="mt-3 text-base leading-relaxed text-neutral-600">
          {description}
        </p>
      )}
    </div>
  );
}

function Panel({ children, className = '' }) {
  return (
    <section className={`mx-auto w-full max-w-6xl px-6 py-16 sm:px-8 lg:py-20 ${className}`}>
      {children}
    </section>
  );
}

function ChartSkeleton({ height = 300 }) {
  return (
    <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
      <div className="mb-8 space-y-3">
        <div className="h-6 w-56 animate-pulse rounded-full bg-neutral-100" />
        <div className="h-4 w-72 animate-pulse rounded-full bg-neutral-100" />
      </div>
      <div
        className="w-full animate-pulse rounded-2xl bg-neutral-100"
        style={{ height }}
      />
    </div>
  );
}

function CardSkeleton() {
  return (
    <div className="overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm">
      <div className="h-48 w-full animate-pulse bg-neutral-100" />
      <div className="space-y-3 px-5 pb-5 pt-4">
        <div className="h-3 w-16 animate-pulse rounded-full bg-neutral-100" />
        <div className="h-4 w-full animate-pulse rounded-full bg-neutral-100" />
        <div className="h-6 w-24 animate-pulse rounded-full bg-neutral-100 mx-auto" />
      </div>
    </div>
  );
}

function ErrorNotice({ message, onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 rounded-3xl border border-neutral-200 bg-white p-10 text-center shadow-sm">
      <p className="text-sm text-neutral-500">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="rounded-full bg-neutral-950 px-5 py-2 text-sm font-medium text-white transition duration-150 ease-out hover:bg-neutral-800"
        >
          Try again
        </button>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function PhoneAnalyticsPage() {
  // Average price by brand
  const [avgPriceData, setAvgPriceData] = useState([]);
  const [avgPriceStatus, setAvgPriceStatus] = useState('loading'); // loading | ready | error

  // Listings per source
  const [listingsData, setListingsData] = useState([]);
  const [listingsStatus, setListingsStatus] = useState('loading');

  // Price distribution
  const [distributionData, setDistributionData] = useState([]);
  const [distributionStatus, setDistributionStatus] = useState('loading');

  // Cheapest phone per brand — service returns { [brand]: phone }
  const [cheapestData, setCheapestData] = useState({});
  const [cheapestStatus, setCheapestStatus] = useState('loading');

  // Phone search + source comparison
  const [searchTerm, setSearchTerm] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchOpen, setSearchOpen] = useState(false);
  const [selectedPhone, setSelectedPhone] = useState(null); // { title, normalizedTitle }
  const [comparisonData, setComparisonData] = useState([]);
  const [comparisonStatus, setComparisonStatus] = useState('idle'); // idle | loading | ready | error

  const loadAvgPrice = () => {
    setAvgPriceStatus('loading');
    fetchAveragePriceByBrand()
      .then((data) => {
        setAvgPriceData(data);
        setAvgPriceStatus('ready');
      })
      .catch(() => setAvgPriceStatus('error'));
  };

  const loadListings = () => {
    setListingsStatus('loading');
    fetchListingsPerSource()
      .then((data) => {
        setListingsData(data);
        setListingsStatus('ready');
      })
      .catch(() => setListingsStatus('error'));
  };

  const loadDistribution = () => {
    setDistributionStatus('loading');
    fetchPriceDistribution()
      .then((data) => {
        setDistributionData(data);
        setDistributionStatus('ready');
      })
      .catch(() => setDistributionStatus('error'));
  };

  const loadCheapest = () => {
    setCheapestStatus('loading');
    fetchCheapestPerBrand()
      .then((data) => {
        setCheapestData(data);
        setCheapestStatus('ready');
      })
      .catch(() => setCheapestStatus('error'));
  };

  useEffect(() => {
    loadAvgPrice();
    loadListings();
    loadDistribution();
    loadCheapest();
  }, []);

  // Debounced phone search for the source-comparison panel
  useEffect(() => {
    const term = searchTerm.trim();
    if (term.length < 2) {
      setSearchResults([]);
      return undefined;
    }

    const timeout = setTimeout(() => {
      searchPhones(term)
        .then((data) => setSearchResults(Array.isArray(data) ? data.slice(0, 8) : []))
        .catch(() => setSearchResults([]));
    }, 250);

    return () => clearTimeout(timeout);
  }, [searchTerm]);

  const handleSelectPhone = (phone) => {
    setSelectedPhone(phone);
    setSearchTerm(phone.title);
    setSearchOpen(false);
    setComparisonStatus('loading');

    fetchPriceComparison(phone.normalizedTitle)
      .then((data) => {
        setComparisonData(data);
        setComparisonStatus('ready');
      })
      .catch(() => setComparisonStatus('error'));
  };

  const cheapestByBrand = cheapestData; // already { [brand]: phone }

  return (
    <div className="min-h-screen bg-[#fafafa]">
      {/* Hero */}
      <header className="mx-auto max-w-5xl px-6 pb-14 pt-10 text-center sm:px-8 sm:pt-10">
              <div className="flex items-center start-auto gap-4">
                  <Link
                      to="/"
                      className="inline-flex items-center gap-2 rounded-full border border-neutral-200 px-4 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50"
                  >
                      <span aria-hidden="true">←</span>
                      Back to phones
                  </Link>
              </div>
        <p className="mb-4 text-xs font-semibold uppercase tracking-[0.28em] text-sky-600">
          Market overview
        </p>
        <h1 className="text-4xl font-semibold tracking-tight text-neutral-950 sm:text-5xl">
          Phone Price Analytics
        </h1>
        <p className="mx-auto mt-4 max-w-xl text-lg leading-relaxed text-neutral-600">
          A live look at how prices move across brands and retailers, drawn
          straight from today's listings.
        </p>
      </header>

      {/* Average price by brand */}
      <Panel>
        <SectionHeader
          eyebrow="By brand"
          title="Average price by brand"
          description="How Samsung, Apple, Xiaomi, and Honor compare on average across every listing."
        />
        {avgPriceStatus === 'loading' && <ChartSkeleton />}
        {avgPriceStatus === 'error' && (
          <ErrorNotice message="Couldn't load average prices." onRetry={loadAvgPrice} />
        )}
        {avgPriceStatus === 'ready' && <AvgPriceByBrandChart data={avgPriceData} />}
      </Panel>

      {/* Cheapest per brand */}
      <Panel className="bg-white/60">
        <SectionHeader
          eyebrow="Best value"
          title="Cheapest phone per brand"
          description="The single lowest price found for each brand, right now."
        />
        {cheapestStatus === 'loading' && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <CardSkeleton key={i} />
            ))}
          </div>
        )}
        {cheapestStatus === 'error' && (
          <ErrorNotice message="Couldn't load cheapest phones." onRetry={loadCheapest} />
        )}
        {cheapestStatus === 'ready' && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {['samsung', 'apple', 'xiaomi', 'honor'].map((brand) => (
              <CheapestPhoneCard key={brand} brand={brand} phone={cheapestByBrand[brand]} />
            ))}
          </div>
        )}
      </Panel>

      {/*/!* Source comparison for a selected phone *!/*/}
      {/*<Panel>*/}
      {/*  <SectionHeader*/}
      {/*    eyebrow="Shop smart"*/}
      {/*    title="Compare a phone across sources"*/}
      {/*    description="Search for a phone to see how its price stacks up across every retailer that lists it."*/}
      {/*  />*/}

      {/*  <div className="relative mx-auto mb-8 max-w-md">*/}
      {/*    <input*/}
      {/*      type="text"*/}
      {/*      value={searchTerm}*/}
      {/*      onChange={(e) => {*/}
      {/*        setSearchTerm(e.target.value);*/}
      {/*        setSearchOpen(true);*/}
      {/*        if (!e.target.value) {*/}
      {/*          setSelectedPhone(null);*/}
      {/*          setComparisonStatus('idle');*/}
      {/*          setComparisonData([]);*/}
      {/*        }*/}
      {/*      }}*/}
      {/*      onFocus={() => setSearchOpen(true)}*/}
      {/*      placeholder="Search for a phone, e.g. iPhone 15"*/}
      {/*      className="w-full rounded-full border border-neutral-200 bg-white px-5 py-3 text-sm text-neutral-900 shadow-sm outline-none transition duration-150 ease-out placeholder:text-neutral-400 focus:border-sky-300 focus:ring-2 focus:ring-sky-100"*/}
      {/*    />*/}

      {/*    {searchOpen && searchResults.length > 0 && (*/}
      {/*      <ul className="absolute z-10 mt-2 w-full overflow-hidden rounded-2xl border border-neutral-200 bg-white shadow-lg">*/}
      {/*        {searchResults.map((phone, i) => (*/}
      {/*          <li key={phone.normalizedTitle ?? phone.siteLink ?? i}>*/}
      {/*            <button*/}
      {/*              onClick={() => handleSelectPhone(phone)}*/}
      {/*              className="block w-full px-5 py-3 text-left text-sm text-neutral-800 transition duration-150 ease-out hover:bg-sky-50 hover:text-sky-700"*/}
      {/*            >*/}
      {/*              {phone.title}*/}
      {/*            </button>*/}
      {/*          </li>*/}
      {/*        ))}*/}
      {/*      </ul>*/}
      {/*    )}*/}
      {/*  </div>*/}

      {/*  {comparisonStatus === 'idle' && (*/}
      {/*    <SourceComparisonChart data={[]} />*/}
      {/*  )}*/}
      {/*  {comparisonStatus === 'loading' && <ChartSkeleton />}*/}
      {/*  {comparisonStatus === 'error' && (*/}
      {/*    <ErrorNotice*/}
      {/*      message="Couldn't load prices for that phone."*/}
      {/*      onRetry={() => selectedPhone && handleSelectPhone(selectedPhone)}*/}
      {/*    />*/}
      {/*  )}*/}
      {/*  {comparisonStatus === 'ready' && <SourceComparisonChart data={comparisonData} />}*/}
      {/*</Panel>*/}

      {/* Listings per source + price distribution */}
      <Panel className="bg-white/60">
        <SectionHeader
          eyebrow="Data coverage"
          title="Where the data comes from"
          description="How listings break down across retailers, and how the overall market is priced."
        />
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
          <div>
            {listingsStatus === 'loading' && <ChartSkeleton />}
            {listingsStatus === 'error' && (
              <ErrorNotice message="Couldn't load listings per source." onRetry={loadListings} />
            )}
            {listingsStatus === 'ready' && <ListingsPerSourceChart data={listingsData} />}
          </div>
          <div>
            {distributionStatus === 'loading' && <ChartSkeleton />}
            {distributionStatus === 'error' && (
              <ErrorNotice
                message="Couldn't load price distribution."
                onRetry={loadDistribution}
              />
            )}
            {distributionStatus === 'ready' && (
              <PriceDistributionChart data={distributionData} />
            )}
          </div>
        </div>
      </Panel>

      <footer className="mx-auto max-w-6xl px-6 pb-16 pt-4 text-center text-xs text-neutral-400 sm:px-8">
        Prices update as new listings are scraped. All figures in MKD.
      </footer>
    </div>
  );
}
