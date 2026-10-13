"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type FinanceTripPnL } from "@/lib/api";
import { FINANCE_PORTAL } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { FinanceShell, KpiCard, inr } from "@/components/finance/FinanceShell";

function TripsView() {
  const [data, setData] = useState<FinanceTripPnL | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => {
    setError(null);
    api.getFinanceTripPnL().then(setData).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const trips = data?.trips ?? [];
  const totalProfit = trips.reduce((s, t) => s + Number(t.profit), 0);
  const totalRev = trips.reduce((s, t) => s + Number(t.revenue), 0);

  return (
    <FinanceShell title="Trek P&L" subtitle="Profitability by package — revenue, expenses, guide & vendor payouts">
      {error && <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
      <div className="mb-6 grid gap-4 md:grid-cols-2">
        <KpiCard tone="accent" label="Trek revenue" value={inr(totalRev)} />
        <KpiCard tone={totalProfit >= 0 ? "good" : "bad"} label="Combined trek profit" value={inr(totalProfit)} />
      </div>
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-100 bg-slate-50 text-[11px] uppercase tracking-wide text-slate-400">
            <tr>
              <th className="px-4 py-3">Trek / package</th>
              <th className="px-4 py-3">Bookings</th>
              <th className="px-4 py-3">Revenue</th>
              <th className="px-4 py-3">Expenses</th>
              <th className="px-4 py-3">Payouts</th>
              <th className="px-4 py-3">Profit</th>
              <th className="px-4 py-3">Margin</th>
            </tr>
          </thead>
          <tbody>
            {trips.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-8 text-slate-400">No trek activity yet</td></tr>
            )}
            {trips.map((t) => (
              <tr key={t.tripId} className="border-b border-slate-50 hover:bg-slate-50/60">
                <td className="px-4 py-3 font-medium text-slate-800">{t.tripName}</td>
                <td className="px-4 py-3 tabular-nums">{t.bookingCount}</td>
                <td className="px-4 py-3 tabular-nums">{inr(t.revenue)}</td>
                <td className="px-4 py-3 tabular-nums text-slate-500">{inr(t.expenses)}</td>
                <td className="px-4 py-3 tabular-nums text-slate-500">{inr(t.payouts)}</td>
                <td className={`px-4 py-3 font-semibold tabular-nums ${Number(t.profit) >= 0 ? "text-emerald-700" : "text-rose-600"}`}>
                  {inr(t.profit)}
                </td>
                <td className="px-4 py-3 tabular-nums">{Number(t.marginPct).toFixed(1)}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </FinanceShell>
  );
}

export default function TripsFinancePage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <TripsView />
    </Protected>
  );
}
