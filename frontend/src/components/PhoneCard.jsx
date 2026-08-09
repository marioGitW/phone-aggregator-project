/**
 * PhoneCard
 * Reusable component to display a single phone
 * Presentational component - receives data as props, no state or side effects
 */

import { formatPrice, capitalize } from '../utils/formatters';
import { useNavigate } from 'react-router-dom';

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

export default function PhoneCard({ phone }) {
  const navigate = useNavigate();

  const {
    id,
    brand,
    title,
    price,
    source,
    siteLink,
    imageUrl,
  } = phone;

  const openProductPage = () => {
    navigate(`/product/${id}`);
  };

  const handleCardKeyDown = (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      openProductPage();
    }
  };

  return (
    <article
      className="group flex h-full cursor-pointer flex-col overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-lg focus-within:ring-2 focus-within:ring-sky-500 focus-within:ring-offset-2"
      role="button"
      tabIndex={0}
      onClick={openProductPage}
      onKeyDown={handleCardKeyDown}
      aria-label={`Open details for ${title}`}
    >
      <div className="relative aspect-square overflow-hidden border-b border-neutral-100 bg-neutral-100">
        <img
          src={getImageSrc(imageUrl)}
          alt={title}
          className="h-full w-full object-contain p-6 transition duration-300 group-hover:scale-[1.03]"
          loading="lazy"
          onError={(event) => {
            event.currentTarget.onerror = null;
            event.currentTarget.src = PLACEHOLDER_IMAGE;
          }}
        />
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-16 bg-linear-to-t from-white/70 to-transparent" />
      </div>

      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-center justify-between gap-3">
          <span className="text-[11px] font-semibold uppercase tracking-[0.28em] text-neutral-500">
            {capitalize(brand)}
          </span>
        </div>

        <h3 className="mt-3 line-clamp-2 min-h-12 text-[15px] font-medium leading-6 text-neutral-900">
          {title}
        </h3>

        <div className="mt-4 flex items-end justify-between gap-3">
          <div className="text-2xl font-semibold tracking-tight text-neutral-950">
            {formatPrice(price)}
          </div>

          {siteLink ? (
            <a
              href={siteLink}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex shrink-0 items-center rounded-full border border-neutral-200 bg-neutral-50 px-3 py-1 text-sm font-medium text-neutral-700 transition duration-200 hover:border-sky-200 hover:bg-sky-50 hover:text-sky-700"
              onClick={(event) => event.stopPropagation()}
            >
              {capitalize(source)}
            </a>
          ) : (
            <span className="inline-flex shrink-0 items-center rounded-full border border-neutral-200 bg-neutral-50 px-3 py-1 text-sm font-medium text-neutral-500">
              {capitalize(source)}
            </span>
          )}
        </div>
      </div>
    </article>
  );
}

