"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type FinanceAging } from "@/lib/api";
import { FINANCE_PORTAL } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { FinanceShell, KpiCard, inr } from "@/components/finance/FinanceShell";

function AgingView() {
  const [data, setData] = useState<FinanceAging | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => {
    setError(null);
    api.getFinanceAging().then(setData).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return (
    <FinanceShell
      title="AR aging"
      subtitle="Outstanding trek receivables by days past due"
    >
      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}
      {data && (
        <div className="space-y-6">
          <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-6">
            <KpiCard label="Current" value={inr(data.current)} />
            <KpiCard label="1–30 days" value={inr(data.days1to30)} tone="warn" />
            <KpiCard label="31–60 days" value={inr(data.days31to60)} tone="warn" />
            <KpiCard label="61–90 days" value={inr(data.days61to90)} tone="bad" />
            <KpiCard label="90+ days" value={inr(data.days90plus)} tone="bad" />
            <KpiCard label="Total open" value={inr(data.totalOutstanding)} tone="accent" />
          </div>
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-slate-100 bg-slate-50 text-[11px] uppercase tracking-wide text-slate-400">
                <tr>
                  <th className="px-4 py-3">Booking</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Due</th>
                  <th className="px-4 py-3">Days</th>
                  <th className="px-4 py-3">Bucket</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody>
                {data.lines.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-slate-400">No open receivables</td>
                  </tr>
                )}
                {data.lines.map((l, i) => (
                  <tr key={`${l.bookingRef}-${i}`} className="border-b border-slate-50">
                    <td className="px-4 py-3 font-mono text-xs">{l.bookingRef}</td>
                    <td className="px-4 py-3">{l.amountType}</td>
                    <td className="px-4 py-3 font-semibold tabular-nums">{inr(l.amount)}</td>
                    <td className="px-4 py-3 text-slate-500">{l.dueDate ?? "—"}</td>
                    <td className="px-4 py-3 tabular-nums">{l.daysPastDue}</td>
                    <td className="px-4 py-3">
                      <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold">{l.bucket}</span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-500">{l.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </FinanceShell>
  );
}

export default function AgingPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <AgingView />
    </Protected>
  );
}
