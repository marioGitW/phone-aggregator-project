/**
 * DemoModeNotice
 * Dismissible toast shown on first load in static demo mode, explaining that the data is
 * a sample fixture rather than live listings. Stacked with DemoBadge by the fixed
 * bottom-right wrapper in App.jsx. Dismissal is persisted per tab via sessionStorage, so it
 * won't nag again on the same tab but does reappear in a fresh one.
 */
import { useState } from 'react';

const STORAGE_KEY = 'demoModeNoticeDismissed';

const readDismissed = () => {
  try {
    return sessionStorage.getItem(STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
};

export default function DemoModeNotice() {
  const [dismissed, setDismissed] = useState(readDismissed);

  const handleDismiss = () => {
    setDismissed(true);
    try {
      sessionStorage.setItem(STORAGE_KEY, 'true');
    } catch {
      // sessionStorage unavailable (e.g. private browsing) - dismissal just won't persist.
    }
  };

  if (dismissed) return null;

  return (
    <div className="w-[calc(100vw-2rem)] max-w-sm rounded-2xl border border-amber-200 bg-amber-50 p-5 shadow-xl">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-amber-500" />
          <h2 className="text-sm font-semibold uppercase tracking-wide text-amber-800">
            Demo mode
          </h2>
        </div>
        <button
          type="button"
          onClick={handleDismiss}
          aria-label="Dismiss"
          className="shrink-0 text-lg leading-none text-amber-500 transition hover:text-amber-700"
        >
          &times;
        </button>
      </div>
      <p className="mt-3 text-sm leading-relaxed text-amber-800">
        You&rsquo;re viewing a static demo with sample data &mdash; no live backend is
        connected. Prices, listings and price history here are a fixed snapshot, not live.
      </p>
    </div>
  );
}
