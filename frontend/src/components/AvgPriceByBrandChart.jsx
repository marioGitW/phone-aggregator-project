/**
 * AvgPriceByBrandChart
 * Bar chart displaying average price per brand
 * Apple-inspired minimal design with Recharts
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

export default function AvgPriceByBrandChart({ data = [] }) {
  const ACCENT_COLOR = '#0071e3';
  const NEUTRAL_GRAY = '#d4d4d8';

  // Custom tooltip to match Apple aesthetic
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      const { brand, averagePrice } = payload[0].payload;
      return (
        <div className="rounded-2xl border border-neutral-200 bg-white px-4 py-3 shadow-lg">
          <p className="text-sm font-semibold text-neutral-950">
            {capitalize(brand)}
          </p>
          <p className="text-sm font-medium text-sky-600">
            {formatPrice(averagePrice)}
          </p>
        </div>
      );
    }
    return null;
  };

  // Prepare data for display
  const displayData = data.map((item) => ({
    ...item,
    displayBrand: capitalize(item.brand),
  }));

  return (
    <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
      <div className="mb-8">
        <h2 className="text-2xl font-semibold tracking-tight text-neutral-950">
          Average Price by Brand
        </h2>
        <p className="mt-2 text-sm text-neutral-600">
          Compare average smartphone prices across brands
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
              dataKey="displayBrand"
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
            <Bar dataKey="averagePrice" fill={ACCENT_COLOR} radius={[12, 12, 0, 0]}>
              {displayData.map((entry, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={ACCENT_COLOR}
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

