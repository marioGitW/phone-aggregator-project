import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import OfferCard from '../components/OfferCard';
import { fetchProductOffers } from '../api/phoneService';
import { formatPrice,capitalize } from '../utils/formatters';

const PLACEHOLDER_IMAGE =
  "data:image/svg+xml;charset=UTF-8," +
  encodeURIComponent(`
    <svg width="640" height="480" viewBox="0 0 640 480" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="640" height="480" rx="36" fill="#F5F5F5"/>
      <rect x="198" y="108" width="244" height="264" rx="36" fill="#FFFFFF" stroke="#D4D4D8" stroke-width="10"/>
      <rect x="242" y="156" width="156" height="12" rx="6" fill="#D4D4D8"/>
      <rect x="242" y="188" width="120" height="12" rx="6" fill="#E4E4E7"/>
      <rect x="242" y="236" width="156" height="96" rx="18" fill="#E4E4E7"/>
      <circle cx="320" cy="350" r="14" fill="#D4D4D8"/>
    </svg>
  `);

const getImageSrc = (imageUrl) =>
  typeof imageUrl === 'string' && imageUrl.trim() ? imageUrl : PLACEHOLDER_IMAGE;

function SkeletonRow() {
  return (
    <div className="flex items-center justify-between gap-4 rounded-2xl border border-neutral-200 bg-white px-4 py-4 sm:px-6">
      <div className="h-4 w-20 animate-pulse rounded-full bg-neutral-100" />
      <div className="h-6 w-28 animate-pulse rounded-full bg-neutral-100" />
      <div className="h-10 w-24 animate-pulse rounded-full bg-neutral-100" />
    </div>
  );
}

export default function ProductPage() {
  const { id } = useParams();

  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryToken, setRetryToken] = useState(0);

  useEffect(() => {
    const loadOffers = async () => {
      setLoading(true);
      setError(null);

      try {
        const data = await fetchProductOffers(id);
        setOffers(data || []);
      } catch {
        setError('Failed to load product offers.');
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      loadOffers();
    }
  }, [id, retryToken]);

  // Parse price string: remove non-digits and convert to number for sorting
  const parsePrice = (priceStr) => {
    const cleaned = String(priceStr).replace(/\D/g, '');
    return parseInt(cleaned, 10) || 0;
  };

  // Sort offers by price ascending
  const sortedOffers = [...offers].sort((a, b) => parsePrice(a.price) - parsePrice(b.price));

  // Get lowest price for badge
  const lowestPrice = sortedOffers.length > 0 ? formatPrice(sortedOffers[0].price) : null;

  // Get first non-null image for hero
  const heroImage = getImageSrc(offers.find((offer) => offer.imageUrl)?.imageUrl);

  const productTitle = offers[0]?.title || 'Product offers';

  const handleRetry = () => {
    setRetryToken((current) => current + 1);
  };

  if (error) {
    return (
      <div className="min-h-screen bg-neutral-50 px-4 py-6 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-4xl">
          <Link
            to="/"
            className="inline-flex items-center gap-2 rounded-full border border-neutral-200 px-4 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50"
          >
            <span aria-hidden="true">←</span>
            Back to phones
          </Link>

          <div className="mt-12 flex min-h-[50vh] items-center justify-center rounded-4xl border border-neutral-200 bg-white shadow-sm">
            <div className="w-full max-w-md px-6 py-12 text-center">
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
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-neutral-50 text-neutral-900">
      <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
        <Link
          to="/"
          className="inline-flex items-center gap-2 rounded-full border border-neutral-200 px-4 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50"
        >
          <span aria-hidden="true">←</span>
          Back to phones
        </Link>

        {loading ? (
          <div className="mt-8 space-y-6">
            <div className="grid gap-6 sm:grid-cols-3">
              <div className="aspect-square animate-pulse rounded-4xl bg-neutral-200" />
              <div className="sm:col-span-2 space-y-4">
                <div className="h-8 w-3/4 animate-pulse rounded-full bg-neutral-200" />
                <div className="h-6 w-1/2 animate-pulse rounded-full bg-neutral-200" />
                <div className="mt-6 h-10 w-40 animate-pulse rounded-full bg-neutral-200" />
              </div>
            </div>

            <div className="mt-12 space-y-4">
              <div className="h-6 w-40 animate-pulse rounded-full bg-neutral-200" />
              {Array.from({ length: 4 }).map((_, index) => (
                <SkeletonRow key={index} />
              ))}
            </div>
          </div>
        ) : offers.length === 0 ? (
          <div className="mt-12 flex min-h-[50vh] items-center justify-center rounded-4xl border border-dashed border-neutral-200 bg-white">
            <div className="w-full max-w-md px-6 py-12 text-center">
              <h2 className="text-2xl font-semibold tracking-tight text-neutral-950">
                No offers available
              </h2>
              <p className="mx-auto mt-3 text-sm leading-6 text-neutral-600">
                Unfortunately, no stores currently have this phone in stock.
              </p>
              <Link
                to="/"
                className="mt-6 inline-flex items-center justify-center rounded-full bg-sky-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-sky-700"
              >
                Browse all phones
              </Link>
            </div>
          </div>
        ) : (
          <>
            <section className="mt-8 grid gap-6 rounded-4xl border border-neutral-200 bg-white p-6 shadow-sm sm:grid-cols-3">
              <div className="aspect-square overflow-hidden rounded-3xl bg-neutral-100">
                <img
                  src={heroImage}
                  alt={productTitle}
                  className="h-full w-full object-cover"
                  onError={(event) => {
                    event.currentTarget.onerror = null;
                    event.currentTarget.src = PLACEHOLDER_IMAGE;
                  }}
                />
              </div>

              <div className="sm:col-span-2 flex flex-col justify-between">
                <div>
                  <h1 className="text-3xl font-semibold tracking-tight text-neutral-950 sm:text-4xl">
                    {capitalize(productTitle)}
                  </h1>
                  <p className="mt-4 text-sm text-neutral-600">
                    Found {offers.length} offer{offers.length === 1 ? '' : 's'} from across our partner stores.
                  </p>
                </div>

                {lowestPrice && (
                  <div className="mt-6 inline-flex w-fit items-center gap-3 rounded-full border-2 border-sky-200 bg-sky-50 px-5 py-3">
                    <span className="text-sm font-medium text-sky-700">Starting from</span>
                    <span className="text-2xl font-bold text-sky-900">{lowestPrice}</span>
                  </div>
                )}
              </div>
            </section>

            <section className="mt-12">
              <div className="mb-6">
                <h2 className="text-2xl font-semibold tracking-tight text-neutral-950">
                  Price comparison
                </h2>
                <p className="mt-2 text-sm text-neutral-600">
                  Sorted by price — lowest first.
                </p>
              </div>

              <div className="space-y-3">
                {sortedOffers.map((offer, index) => (
                  <OfferCard
                    key={offer.id}
                    offer={offer}
                    isLowest={index === 0}
                    price={formatPrice(offer.price)}
                  />
                ))}
              </div>
            </section>
          </>
        )}
      </div>
    </div>
  );
}

