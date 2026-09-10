import { formatPrice, capitalize } from '../utils/formatters';

/**
 * One row per store: a "from" price (or a range when colors are priced differently),
 * expandable to the individual color variants that store carries. Stores with a single
 * color skip the expand affordance entirely and link straight out.
 */
export default function StoreOfferGroup({ group, isLowest }) {
  const { source, minPrice, maxPrice, availableColors = [], colors = [] } = group;
  const hasPriceRange = maxPrice > minPrice;
  const hasMultipleColors = colors.length > 1;

  const badgeClasses = isLowest
    ? 'border border-green-200 bg-white text-green-700'
    : 'border border-neutral-200 bg-neutral-100 text-neutral-700';

  const containerClasses = isLowest
    ? 'border-2 border-green-200 bg-green-50'
    : 'border-neutral-200 bg-white hover:border-neutral-300 hover:bg-neutral-50';

  const visitLinkClasses = isLowest
    ? 'border-0 bg-green-600 text-white hover:bg-green-700'
    : 'border border-neutral-200 bg-white text-neutral-700 hover:border-neutral-300 hover:bg-neutral-50';

  const summaryContent = (
    <>
      <div className="flex items-center gap-3">
        <div className="flex flex-col gap-1">
          <span className={`inline-flex w-fit rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide ${badgeClasses}`}>
            {capitalize(source)}
          </span>
          <div className="flex flex-wrap items-center gap-1">
            {isLowest && (
              <span className="inline-flex w-fit rounded-full bg-green-600 px-2.5 py-1 text-xs font-bold text-white">
                Best price
              </span>
            )}
            {availableColors.length > 0 && (
              <span className="text-xs text-neutral-500">
                {availableColors.length} color{availableColors.length === 1 ? '' : 's'}: {availableColors.map(capitalize).join(', ')}
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="flex flex-1 items-center justify-end gap-4">
        <span className="text-lg font-bold tracking-tight text-neutral-950 tabular-nums">
          {hasPriceRange ? 'from ' : ''}{formatPrice(minPrice)}
        </span>

        {hasMultipleColors ? (
          <span className="inline-flex shrink-0 items-center gap-1 rounded-full border border-neutral-200 bg-white px-4 py-2.5 text-sm font-semibold text-neutral-700 transition group-open:hidden">
            Show colors
            <span aria-hidden="true">▾</span>
          </span>
        ) : (
          <a
            href={colors[0]?.siteLink}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(event) => event.stopPropagation()}
            className={`inline-flex shrink-0 items-center rounded-full px-4 py-2.5 text-sm font-semibold transition ${visitLinkClasses}`}
          >
            Visit
            <span className="ml-1 inline sm:hidden">→</span>
            <span className="ml-1 hidden sm:inline">Store →</span>
          </a>
        )}
      </div>
    </>
  );

  if (!hasMultipleColors) {
    return (
      <div className={`group flex items-center justify-between gap-4 rounded-2xl border px-4 py-4 shadow-sm transition sm:px-6 ${containerClasses}`}>
        {summaryContent}
      </div>
    );
  }

  return (
    <details className={`group rounded-2xl border px-4 py-4 shadow-sm transition sm:px-6 ${containerClasses}`}>
      <summary className="flex cursor-pointer list-none items-center justify-between gap-4 [&::-webkit-details-marker]:hidden">
        {summaryContent}
      </summary>

      <div className="mt-4 space-y-2 border-t border-neutral-200 pt-4">
        {colors.map((color) => (
          <div
            key={color.offerId}
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-neutral-50 px-3 py-2.5 sm:flex-nowrap"
          >
            <span className="text-sm font-medium text-neutral-700">
              {capitalize(color.colorRaw || color.colorCanonical || 'Color')}
            </span>
            <div className="flex items-center gap-4">
              <span className="text-sm font-semibold tabular-nums text-neutral-950">
                {formatPrice(color.price)}
              </span>
              <a
                href={color.siteLink}
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm font-semibold text-sky-600 hover:text-sky-700 hover:underline"
              >
                Visit →
              </a>
            </div>
          </div>
        ))}
      </div>
    </details>
  );
}
