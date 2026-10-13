"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api, type FinanceBudget } from "@/lib/api";
import { FINANCE_PORTAL, canWriteFinance } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { Protected } from "@/components/Protected";
import { FinanceShell, KpiCard, Panel, inr } from "@/components/finance/FinanceShell";

const CATS = ["GUIDE_FEES", "TRANSPORT", "HOTEL", "PERMITS", "FOOD", "GEAR", "MARKETING", "SALARIES", "INSURANCE", "OTHER"];

function BudgetsView() {
  const { user } = useAuth();
  const canWrite = canWriteFinance(user?.role);
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const [data, setData] = useState<FinanceBudget | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [category, setCategory] = useState("GUIDE_FEES");
  const [amount, setAmount] = useState(100000);

  const reload = useCallback(() => {
    setError(null);
    api.getFinanceBudgets(month).then(setData).catch((e: Error) => setError(e.message));
  }, [month]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      const next = await api.upsertFinanceBudget({
        periodMonth: month,
        category,
        budgetAmount: Number(amount),
      });
      setData(next);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save budget");
    }
  }

  return (
    <FinanceShell
      title="Budgets vs actuals"
      subtitle="Set monthly cost budgets for trek operations and track spend"
      actions={
        <input
          type="month"
          value={month}
          onChange={(e) => setMonth(e.target.value)}
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
        />
      }
    >
      {error && <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
      {data && (
        <div className="space-y-6">
          <div className="grid gap-4 md:grid-cols-3">
            <KpiCard tone="accent" label="Budget" value={inr(data.totalBudget)} />
            <KpiCard tone="warn" label="Actual spend" value={inr(data.totalActual)} />
            <KpiCard tone={data.variance >= 0 ? "good" : "bad"} label="Variance" value={inr(data.variance)} hint="Budget − actual" />
          </div>

          {canWrite && (
            <form onSubmit={onSubmit} className="flex flex-wrap items-end gap-3 rounded-2xl border border-slate-200 bg-white p-4">
              <label className="text-sm">
                <span className="font-medium">Category</span>
                <select value={category} onChange={(e) => setCategory(e.target.value)} className="mt-1 block rounded-lg border px-3 py-2">
                  {CATS.map((c) => <option key={c} value={c}>{c.replaceAll("_", " ")}</option>)}
                </select>
              </label>
              <label className="text-sm">
                <span className="font-medium">Budget ₹</span>
                <input type="number" min={0} value={amount} onChange={(e) => setAmount(Number(e.target.value))} className="mt-1 block rounded-lg border px-3 py-2" />
              </label>
              <button type="submit" className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white">Save line</button>
            </form>
          )}

          <Panel title={`Budget lines · ${data.periodMonth}`}>
            <div className="space-y-3">
              {data.lines.length === 0 && <p className="text-sm text-slate-400">No budget lines — add categories above</p>}
              {data.lines.map((l) => {
                const max = Math.max(Number(l.budgetAmount), Number(l.actualAmount), 1);
                return (
                  <div key={l.category}>
                    <div className="mb-1 flex justify-between text-xs">
                      <span className="font-medium text-slate-700">{l.category.replaceAll("_", " ")}</span>
                      <span className="tabular-nums text-slate-500">
                        {inr(l.actualAmount)} / {inr(l.budgetAmount)} · {Number(l.usedPct).toFixed(0)}%
                      </span>
                    </div>
                    <div className="relative h-2 overflow-hidden rounded-full bg-slate-100">
                      <div className="absolute inset-y-0 left-0 rounded-full bg-slate-300" style={{ width: `${(Number(l.budgetAmount) / max) * 100}%` }} />
                      <div
                        className={`absolute inset-y-0 left-0 rounded-full ${Number(l.actualAmount) > Number(l.budgetAmount) && Number(l.budgetAmount) > 0 ? "bg-rose-500" : "bg-emerald-500"}`}
                        style={{ width: `${(Number(l.actualAmount) / max) * 100}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </Panel>
        </div>
      )}
    </FinanceShell>
  );
}

export default function BudgetsPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <BudgetsView />
    </Protected>
  );
}
