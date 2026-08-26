/**
 * ListingsPerSourceChart
 * Bar or Donut chart displaying count of phones per source
 * Apple-inspired minimal design
 */

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import { capitalize } from '../utils/formatters';

export default function ListingsPerSourceChart({ data = [] }) {
  const ACCENT_COLOR = '#0071e3';
  const NEUTRAL_GRAY = '#d4d4d8';

  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      const { source, count } = payload[0].payload;
      return (
        <div className="rounded-2xl border border-neutral-200 bg-white px-4 py-3 shadow-lg">
          <p className="text-sm font-semibold text-neutral-950">
            {capitalize(source)}
          </p>
          <p className="text-sm font-medium text-sky-600">
            {count} {count === 1 ? 'listing' : 'listings'}
          </p>
        </div>
      );
    }
    return null;
  };

  const displayData = data.map((item) => ({
    ...item,
    displaySource: capitalize(item.source),
  }));

  return (
    <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
      <div className="mb-8">
        <h2 className="text-2xl font-semibold tracking-tight text-neutral-950">
          Listings per Source
        </h2>
        <p className="mt-2 text-sm text-neutral-600">
          Data coverage across different retailers
        </p>
      </div>

      {data.length === 0 ? (
        <div className="flex h-64 items-center justify-center text-neutral-500">
          <p>No data available</p>
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={300}>
          <BarChart
            data={displayData}
            margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
          >
            <CartesianGrid
              strokeDasharray="0"
              stroke={NEUTRAL_GRAY}
              vertical={false}
            />
            <XAxis
              dataKey="displaySource"
              tick={{ fill: '#71717a', fontSize: 12, fontWeight: 500 }}
              axisLine={{ stroke: NEUTRAL_GRAY }}
              tickLine={{ stroke: NEUTRAL_GRAY }}
              angle={-45}
              textAnchor="end"
              height={80}
            />
            <YAxis
              tick={{ fill: '#71717a', fontSize: 12 }}
              axisLine={{ stroke: NEUTRAL_GRAY }}
              tickLine={{ stroke: NEUTRAL_GRAY }}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(0, 113, 227, 0.08)' }} />
            <Bar dataKey="count" fill={ACCENT_COLOR} radius={[12, 12, 0, 0]}>
              {displayData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={ACCENT_COLOR} opacity={0.9} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

