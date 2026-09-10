/**
 * PriceHistoryChart
 * Line chart of price over time, one line per source. Each source keeps its own
 * (timestamp, price) series rather than being pivoted into shared rows, since different
 * stores get scraped at slightly different times within the same run - Recharts plots
 * each Line against the shared numeric time axis independently.
 */
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { formatPrice, capitalize } from '../utils/formatters';

const NEUTRAL_GRAY = '#d4d4d8';
const SOURCE_COLORS = [
  '#0071e3', '#059669', '#f59e0b', '#dc2626', '#7c3aed', '#0891b2', '#db2777', '#65a30d',
];

const formatDate = (timestamp) =>
  new Date(timestamp).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload || payload.length === 0) {
    return null;
  }

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white px-4 py-3 shadow-lg">
      <p className="mb-1 text-xs font-medium text-neutral-500">{formatDate(label)}</p>
      <div className="space-y-1">
        {payload.map((entry) => (
          <p key={entry.dataKey + entry.name} className="text-sm font-medium" style={{ color: entry.color }}>
            {capitalize(entry.name)}: <span className="font-semibold">{formatPrice(entry.value)}</span>
          </p>
        ))}
      </div>
    </div>
  );
}

export default function PriceHistoryChart({ history = [] }) {
  const series = history
    .filter((entry) => entry.points && entry.points.length > 0)
    .map((entry, index) => ({
      source: entry.source,
      color: SOURCE_COLORS[index % SOURCE_COLORS.length],
      data: entry.points.map((point) => ({
        timestamp: new Date(point.scrapedAt).getTime(),
        price: point.price,
      })),
    }));

  const hasData = series.length > 0;
  const hasTrend = series.some((entry) => entry.data.length > 1);

  return (
    <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
      <div className="mb-8">
        <h2 className="text-2xl font-semibold tracking-tight text-neutral-950">
          Price history
        </h2>
        <p className="mt-2 text-sm text-neutral-600">
          {hasTrend
            ? "How each store's price has moved over time."
            : 'Only one price check so far — the trend fills in as new scrapes come in.'}
        </p>
      </div>

      {!hasData ? (
        <div className="flex h-64 items-center justify-center text-neutral-500">
          <p>No price history yet</p>
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={320}>
          <LineChart margin={{ top: 20, right: 30, left: 10, bottom: 10 }}>
            <CartesianGrid strokeDasharray="0" stroke={NEUTRAL_GRAY} vertical={false} />
            <XAxis
              dataKey="timestamp"
              type="number"
              domain={['dataMin', 'dataMax']}
              tickFormatter={formatDate}
              tick={{ fill: '#71717a', fontSize: 12 }}
              axisLine={{ stroke: NEUTRAL_GRAY }}
              tickLine={{ stroke: NEUTRAL_GRAY }}
            />
            <YAxis
              tickFormatter={(value) => formatPrice(value)}
              tick={{ fill: '#71717a', fontSize: 12 }}
              axisLine={{ stroke: NEUTRAL_GRAY }}
              tickLine={{ stroke: NEUTRAL_GRAY }}
              width={90}
            />
            <Tooltip content={<CustomTooltip />} />
            <Legend
              formatter={(value) => <span className="text-sm text-neutral-700">{capitalize(value)}</span>}
            />
            {series.map((entry) => (
              <Line
                key={entry.source}
                data={entry.data}
                dataKey="price"
                name={entry.source}
                stroke={entry.color}
                strokeWidth={2}
                dot={{ r: 4, fill: entry.color, strokeWidth: 0 }}
                activeDot={{ r: 6 }}
                isAnimationActive={false}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
