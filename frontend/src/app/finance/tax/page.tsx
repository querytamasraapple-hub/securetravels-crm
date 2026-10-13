"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type FinanceTax } from "@/lib/api";
import { FINANCE_PORTAL } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { FinanceShell, HBar, KpiCard, Panel, inr } from "@/components/finance/FinanceShell";

function monthStart() {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10);
}

function TaxView() {
  const [from, setFrom] = useState(monthStart());
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10));
  const [data, setData] = useState<FinanceTax | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => {
    setError(null);
    api.getFinanceTax({ from, to }).then(setData).catch((e: Error) => setError(e.message));
  }, [from, to]);

  useEffect(() => {
    reload();
  }, [reload]);

  const max = Math.max(1, ...(data?.taxableByTrip.map((t) => Number(t.amount)) ?? [1]));

  return (
    <FinanceShell
      title="GST / Tax"
      subtitle="Estimated output GST on trek collections (default 5% tour package rate)"
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
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <KpiCard tone="accent" label="Taxable revenue" value={inr(data.taxableRevenue)} hint={`${data.gstRatePct}% GST model`} />
            <KpiCard label="Output GST" value={inr(data.outputGst)} />
            <KpiCard tone="good" label="Est. input credit" value={inr(data.estimatedInputCredit)} />
            <KpiCard tone={data.netGstPayable >= 0 ? "warn" : "good"} label="Net GST payable" value={inr(data.netGstPayable)} />
          </div>
          <Panel title="Taxable collections by trek">
            <HBar rows={data.taxableByTrip.map((t) => ({ name: t.name, amount: Number(t.amount), max }))} />
          </Panel>
          <p className="text-xs text-slate-400">
            Indicative for finance planning — confirm rates with your CA for pilgrimage / outbound / hotel-inclusive packages.
          </p>
        </div>
      )}
    </FinanceShell>
  );
}

export default function TaxPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <TaxView />
    </Protected>
  );
}
