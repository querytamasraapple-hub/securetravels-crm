"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type FinanceCashflow } from "@/lib/api";
import { FINANCE_PORTAL } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { BarSeries, FinanceShell, KpiCard, Panel, inr } from "@/components/finance/FinanceShell";

function daysAgo(n: number) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

function CashflowView() {
  const [from, setFrom] = useState(daysAgo(29));
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10));
  const [data, setData] = useState<FinanceCashflow | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => {
    setError(null);
    api.getFinanceCashflow({ from, to }).then(setData).catch((e: Error) => setError(e.message));
  }, [from, to]);

  useEffect(() => {
    reload();
  }, [reload]);

  const chartPoints = (data?.daily ?? [])
    .filter((d) => Number(d.inflow) > 0 || Number(d.outflow) > 0)
    .slice(-14)
    .map((d) => ({
      label: d.date,
      a: Number(d.inflow),
      b: Number(d.outflow),
      aLabel: "Inflow",
      bLabel: "Outflow",
    }));

  return (
    <FinanceShell
      title="Cash flow"
      subtitle="Daily inflows (collections) vs outflows (expenses + paid payouts)"
      actions={
        <div className="flex items-center gap-2">
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm" />
          <span className="text-slate-400">→</span>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm" />
        </div>
      }
    >
      {error && <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
      {data && (
        <div className="space-y-6">
          <div className="grid gap-4 md:grid-cols-3">
            <KpiCard tone="good" label="Inflow" value={inr(data.totalInflow)} />
            <KpiCard tone="warn" label="Outflow" value={inr(data.totalOutflow)} />
            <KpiCard tone={data.netCash >= 0 ? "accent" : "bad"} label="Net cash" value={inr(data.netCash)} />
          </div>
          <Panel
            title="Last active days (inflow vs outflow)"
            action={
              <div className="flex gap-3 text-[11px] text-slate-500">
                <span><span className="mr-1 inline-block h-2 w-2 rounded-sm bg-slate-900" />In</span>
                <span><span className="mr-1 inline-block h-2 w-2 rounded-sm bg-emerald-500" />Out</span>
              </div>
            }
          >
            {chartPoints.length ? <BarSeries points={chartPoints} /> : <p className="text-sm text-slate-400">No cash movements in range</p>}
          </Panel>
        </div>
      )}
    </FinanceShell>
  );
}

export default function CashflowPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <CashflowView />
    </Protected>
  );
}
