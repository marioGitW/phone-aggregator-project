/**
 * LoadingSpinner
 * Small, dependency-free CSS spinner - no external libraries, styles are injected via the
 * <style> tag below so this drops in anywhere with a single import.
 *
 * The spinner actually shown on first page load lives as static markup in index.html
 * itself (see the #initial-loader element there): in a client-rendered app there's no
 * React tree yet during that gap, so it can't be this component - React replaces that
 * static markup once main.jsx mounts the app, which is what makes it disappear once the
 * app is ready. This component is the same visual design (same ring size/speed/colors),
 * packaged for reuse inside the React tree itself (e.g. a future Suspense fallback, or a
 * page's own async loading state).
 */
export default function LoadingSpinner({ label = 'Loading' }) {
  return (
    <div className="lpa-spinner-overlay" role="status" aria-label={label}>
      <span className="lpa-spinner" aria-hidden="true" />
      {label && <span className="lpa-spinner-label">{label}</span>}
      <style>{`
        .lpa-spinner-overlay {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 14px;
          min-height: 160px;
          width: 100%;
        }
        .lpa-spinner {
          width: 26px;
          height: 26px;
          border-radius: 50%;
          border: 2.5px solid rgba(2, 132, 199, 0.14);
          border-top-color: #0284c7;
          animation: lpa-spin 0.85s linear infinite;
        }
        .lpa-spinner-label {
          font-size: 13px;
          color: #71717a;
        }
        @keyframes lpa-spin {
          to {
            transform: rotate(360deg);
          }
        }
        @media (prefers-reduced-motion: reduce) {
          .lpa-spinner {
            animation-duration: 2.2s;
          }
        }
      `}</style>
    </div>
  );
}
