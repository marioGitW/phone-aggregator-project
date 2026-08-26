/**
 * CheapestPhoneCard
 * Minimal product card showing cheapest phone per brand
 * Apple-inspired design with product image and details
 */

import { formatPrice, capitalize } from '../utils/formatters';

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

export default function CheapestPhoneCard({ phone, brand }) {
  if (!phone) {
    return (
      <div className="rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm">
        <p className="text-center text-sm text-neutral-500">No data</p>
      </div>
    );
  }

  return (
    <article className="group flex flex-col overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm transition hover:shadow-lg">
      {/* Image container */}
      <div className="flex h-48 w-full items-center justify-center bg-neutral-50 p-4">
        <img
          src={getImageSrc(phone.imageUrl)}
          alt={phone.title}
          className="h-full w-full object-contain transition duration-300 group-hover:scale-105"
          loading="lazy"
          onError={(event) => {
            event.currentTarget.onerror = null;
            event.currentTarget.src = PLACEHOLDER_IMAGE;
          }}
        />
      </div>

      {/* Content */}
      <div className="flex flex-1 flex-col px-5 pb-5 pt-4">
        {/* Brand label */}
        <span className="text-[11px] font-semibold uppercase tracking-[0.28em] text-neutral-500">
          {capitalize(brand)}
        </span>

        {/* Title */}
        <h3 className="mt-2 line-clamp-2 min-h-12 text-[14px] font-medium leading-5 text-neutral-900">
          {capitalize(phone.title)}
        </h3>

        {/* Price and source */}
        <div className="mt-auto flex flex-col items-center pt-4">
          <div className="text-center text-lg font-bold tracking-tight text-neutral-950">
            {formatPrice(phone.price)}
          </div>

          {/* Source badge */}
          {phone.siteLink ? (
            <a
              href={phone.siteLink}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-2 inline-flex items-center rounded-full border border-neutral-200 bg-neutral-50 px-3 py-1 text-[11px] font-medium uppercase tracking-wide text-neutral-600 transition hover:border-sky-200 hover:bg-sky-50 hover:text-sky-700"
            >
              {capitalize(phone.source)}
            </a>
          ) : (
            <span className="mt-2 inline-flex items-center rounded-full border border-neutral-200 bg-neutral-50 px-3 py-1 text-[11px] font-medium uppercase tracking-wide text-neutral-500">
              {capitalize(phone.source)}
            </span>
          )}
        </div>
      </div>
    </article>
  );
}

