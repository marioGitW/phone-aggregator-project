import { capitalize } from '../utils/formatters';

export default function OfferCard({ offer, isLowest, price }) {
  return (
    <div
      className={`flex items-center justify-between gap-4 rounded-2xl border px-4 py-4 shadow-sm transition sm:px-6 ${
        isLowest
          ? 'border-2 border-green-200 bg-green-50'
          : 'border-neutral-200 bg-white hover:border-neutral-300 hover:bg-neutral-50'
      }`}
    >
      <div className="flex items-center gap-3">
        <div className="flex flex-col gap-1">
          <span
            className={`inline-flex w-fit rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide ${
              isLowest
                ? 'border border-green-200 bg-white text-green-700'
                : 'border border-neutral-200 bg-neutral-100 text-neutral-700'
            }`}
          >
            {capitalize(offer.source)}
          </span>
          {isLowest && (
            <span className="inline-flex w-fit rounded-full bg-green-600 px-2.5 py-1 text-xs font-bold text-white">
              Best price
            </span>
          )}
        </div>
      </div>

      <div className="flex-1 text-right">
        <span className="text-lg font-bold tracking-tight text-neutral-950 tabular-nums">
          {price}
        </span>
      </div>

      <a
        href={offer.siteLink}
        target="_blank"
        rel="noopener noreferrer"
        className={`inline-flex shrink-0 items-center rounded-full px-4 py-2.5 text-sm font-semibold transition ${
          isLowest
            ? 'border-0 bg-green-600 text-white hover:bg-green-700'
            : 'border border-neutral-200 bg-white text-neutral-700 hover:border-neutral-300 hover:bg-neutral-50'
        }`}
      >
        Visit
        <span className="ml-1 inline sm:hidden">→</span>
        <span className="ml-1 hidden sm:inline">Store →</span>
      </a>
    </div>
  );
}

