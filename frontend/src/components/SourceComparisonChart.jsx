/**
 * SourceComparisonChart
 * Horizontal bar chart showing price comparison across sources for a selected phone
 * Helps identify the cheapest retailer
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
import { formatPrice, capitalize } from '../utils/formatters';

export default function SourceComparisonChart({ data = [] }) {
  const ACCENT_COLOR = '#0071e3';
  const NEUTRAL_GRAY = '#d4d4d8';

  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      const { source, price } = payload[0].payload;
      return (
        <div className="rounded-2xl border border-neutral-200 bg-white px-4 py-3 shadow-lg">
          <p className="text-sm font-semibold text-neutral-950">
            {capitalize(source)}
          </p>
          <p className="text-sm font-medium text-sky-600">
            {formatPrice(price)}
          </p>
        </div>
      );
    }
    return null;
  };

  // Sort by price, cheapest first
  const sortedData = [...data].sort((a, b) => a.price - b.price);

  const displayData = sortedData.map((item) => ({
    ...item,
    displaySource: capitalize(item.source),
  }));

  // Find cheapest for highlighting
  const minPrice = displayData.length > 0 ? displayData[0].price : 0;

  return (
    <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
      <div className="mb-8">
        <h2 className="text-2xl font-semibold tracking-tight text-neutral-950">
          Price Comparison Across Sources
        </h2>
        <p className="mt-2 text-sm text-neutral-600">
          Find the best price for your phone
        </p>
      </div>

      {data.length === 0 ? (
        <div className="flex h-64 items-center justify-center text-neutral-500">
          <p>Select a phone to see price comparison</p>
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={300}>
          <BarChart
            data={displayData}
            layout="vertical"
            margin={{ top: 20, right: 30, left: 120, bottom: 20 }}
          >
            <CartesianGrid
              strokeDasharray="0"
              stroke={NEUTRAL_GRAY}
              vertical={true}
            />
            <XAxis
              type="number"
              tick={{ fill: '#71717a', fontSize: 12 }}
              axisLine={{ stroke: NEUTRAL_GRAY }}
              tickLine={{ stroke: NEUTRAL_GRAY }}
            />
            <YAxis
              dataKey="displaySource"
              type="category"
              tick={{ fill: '#71717a', fontSize: 12, fontWeight: 500 }}
              axisLine={{ stroke: NEUTRAL_GRAY }}
              tickLine={{ stroke: NEUTRAL_GRAY }}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(0, 113, 227, 0.08)' }} />
            <Bar dataKey="price" fill={ACCENT_COLOR} radius={[0, 12, 12, 0]}>
              {displayData.map((entry, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={entry.price === minPrice ? '#059669' : ACCENT_COLOR}
                  opacity={0.9}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

