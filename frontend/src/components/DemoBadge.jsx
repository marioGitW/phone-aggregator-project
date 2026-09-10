/**
 * DemoBadge
 * Small "Demo" badge, stacked with DemoModeNotice by the fixed bottom-right wrapper in
 * App.jsx, whenever the app is running against static fixtures instead of a live backend
 * (VITE_DATA_MODE=static). There's no write-triggering UI in this app to hide, so this is
 * purely informational and stays up even after the notice is dismissed.
 */
export default function DemoBadge() {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1.5 text-xs font-semibold uppercase tracking-wide text-amber-700 shadow-sm">
      <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
      Demo
    </span>
  );
}
