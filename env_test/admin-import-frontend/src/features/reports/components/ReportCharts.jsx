import {
  AreaChart, Area, BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend,
} from 'recharts';
import {  salesChartData, categoryChartData, ordersChartData  } from '@/features/dashboard/services/dashboard.service';
import { formatCurrency } from '@/utils/formatCurrency';

const PIE_COLORS = ['#6366f1', '#22d3ee', '#10b981', '#f59e0b', '#f43f5e', '#8b5cf6'];

export default function ReportCharts() {
  return (
    <div className="space-y-6">
      {/* Revenue trend */}
      <div className="card">
        <h3 className="mb-1 text-base font-semibold text-slate-900">Annual Revenue Trend</h3>
        <p className="mb-4 text-sm text-slate-500">Monthly revenue for 2026</p>
        <ResponsiveContainer width="100%" height={280}>
          <AreaChart data={salesChartData}>
            <defs>
              <linearGradient id="rg" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor="#6366f1" stopOpacity={0.15} />
                <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey="month" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false}
              tickFormatter={v => `$${(v/1000).toFixed(0)}k`} />
            <Tooltip formatter={val => formatCurrency(val)} />
            <Area type="monotone" dataKey="revenue" stroke="#6366f1" strokeWidth={2.5} fill="url(#rg)" dot={false} activeDot={{ r: 5 }} />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Orders per week */}
        <div className="card">
          <h3 className="mb-1 text-base font-semibold text-slate-900">Weekly Orders</h3>
          <p className="mb-4 text-sm text-slate-500">Orders per day this week</p>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={ordersChartData} barSize={28}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
              <XAxis dataKey="day" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <Tooltip />
              <Bar dataKey="orders" fill="#6366f1" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Category distribution */}
        <div className="card">
          <h3 className="mb-1 text-base font-semibold text-slate-900">Sales by Category</h3>
          <p className="mb-4 text-sm text-slate-500">Revenue share per category</p>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie
                data={categoryChartData}
                cx="50%"
                cy="50%"
                innerRadius={55}
                outerRadius={90}
                paddingAngle={3}
                dataKey="value"
              >
                {categoryChartData.map((_, i) => (
                  <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip formatter={val => `${val}%`} />
              <Legend iconType="circle" iconSize={8} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Sales vs Revenue line */}
        <div className="card lg:col-span-2">
          <h3 className="mb-1 text-base font-semibold text-slate-900">Sales vs Revenue Comparison</h3>
          <p className="mb-4 text-sm text-slate-500">12-month comparison</p>
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={salesChartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
              <XAxis dataKey="month" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false}
                tickFormatter={v => `$${(v/1000).toFixed(0)}k`} />
              <Tooltip formatter={val => formatCurrency(val)} />
              <Line type="monotone" dataKey="sales"   stroke="#6366f1" strokeWidth={2} dot={false} activeDot={{ r: 4 }} />
              <Line type="monotone" dataKey="revenue" stroke="#10b981" strokeWidth={2} dot={false} activeDot={{ r: 4 }} strokeDasharray="5 5" />
              <Legend iconType="circle" iconSize={8} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
