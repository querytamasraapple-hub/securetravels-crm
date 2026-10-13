"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api, type FinancePnL } from "@/lib/api";
import { FINANCE_PORTAL } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { FinanceShell, HBar, KpiCard, Panel, inr } from "@/components/finance/FinanceShell";

function monthStart() {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10);
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function PnLView() {
  const [from, setFrom] = useState(monthStart());
  const [to, setTo] = useState(today());
  const [data, setData] = useState<FinancePnL | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    api
      .getFinancePnL({ from, to })
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [from, to]);

  useEffect(() => {
    reload();
  }, [reload]);

  const maxExp = Math.max(1, ...(data?.expenseLines.map((r) => Number(r.amount)) ?? [1]));
  const maxRev = Math.max(1, ...(data?.revenueByTrip.map((r) => Number(r.amount)) ?? [1]));

  return (
    <FinanceShell
      title="Profit & loss"
      subtitle="Trek company P&L — collected revenue vs operating costs"
      actions={
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href="/finance/invoices?new=1"
            className="rounded-lg bg-emerald-700 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-800"
          >
            Create invoice
          </Link>
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
          />
          <span className="text-slate-400">→</span>
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
          />
        </div>
      }
    >
      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {loading && !data && <p className="text-sm text-slate-400">Calculating P&L…</p>}
      {data && (
        <div className="space-y-6">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <KpiCard tone="accent" label="Revenue" value={inr(data.revenue)} hint={`${data.from} → ${data.to}`} />
            <KpiCard tone="warn" label="Expenses" value={inr(data.expenses)} />
            <KpiCard
              tone={data.grossProfit >= 0 ? "good" : "bad"}
              label="Gross profit"
              value={inr(data.grossProfit)}
            />
            <KpiCard
              tone={data.marginPct >= 0 ? "good" : "bad"}
              label="Margin"
              value={`${Number(data.marginPct).toFixed(1)}%`}
            />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Panel title="Cost structure">
              <HBar
                rows={data.expenseLines.map((r) => ({
                  name: r.name,
                  amount: Number(r.amount),
                  max: maxExp,
                }))}
              />
            </Panel>
            <Panel title="Revenue by trek">
              <HBar
                rows={data.revenueByTrip.map((r) => ({
                  name: r.name,
                  amount: Number(r.amount),
                  max: maxRev,
                }))}
              />
            </Panel>
          </div>

          <Panel title="Statement">
            <div className="space-y-2 text-sm">
              <Row label="Collected trek revenue" value={inr(data.revenue)} />
              <Row label="Less: operating expenses" value={`(${inr(data.expenses)})`} muted />
              <div className="border-t border-slate-200 pt-2">
                <Row label="Gross profit / (loss)" value={inr(data.grossProfit)} bold />
              </div>
              <Row label="Operating margin" value={`${Number(data.marginPct).toFixed(1)}%`} muted />
            </div>
          </Panel>
        </div>
      )}
    </FinanceShell>
  );
}

function Row({
  label,
  value,
  muted,
  bold,
}: {
  label: string;
  value: string;
  muted?: boolean;
  bold?: boolean;
}) {
  return (
    <div className="flex justify-between gap-4">
      <span className={muted ? "text-slate-500" : "text-slate-700"}>{label}</span>
      <span className={`tabular-nums ${bold ? "font-semibold text-slate-900" : "text-slate-800"}`}>
        {value}
      </span>
    </div>
  );
}

export default function FinancePnLPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <PnLView />
    </Protected>
  );
}
