"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  api,
  type FinanceBudget,
  type FinanceCashflow,
  type FinanceExpense,
  type FinanceOverview,
  type FinancePayout,
  type FinancePnL,
  type FinanceTax,
} from "@/lib/api";
import { FINANCE_PORTAL } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import {
  ActionCard,
  CashflowBars,
  DonutChart,
  FinanceShell,
  HBar,
  KpiCard,
  Panel,
  StatusBadge,
  TrendChart,
  inr,
} from "@/components/finance/FinanceShell";

function monthStart() {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10);
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function monthLabel(iso: string) {
  try {
    return new Date(iso + "T00:00:00").toLocaleDateString("en-IN", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  } catch {
    return iso;
  }
}

function deltaPct(curr: number, prev: number) {
  if (!prev) return curr > 0 ? 100 : 0;
  return ((curr - prev) / prev) * 100;
}

function FinanceOverviewView() {
  const [from, setFrom] = useState(monthStart());
  const [to, setTo] = useState(today());
  const [overview, setOverview] = useState<FinanceOverview | null>(null);
  const [pnl, setPnl] = useState<FinancePnL | null>(null);
  const [cashflow, setCashflow] = useState<FinanceCashflow | null>(null);
  const [tax, setTax] = useState<FinanceTax | null>(null);
  const [budget, setBudget] = useState<FinanceBudget | null>(null);
  const [expenses, setExpenses] = useState<FinanceExpense[]>([]);
  const [payouts, setPayouts] = useState<FinancePayout[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [compare, setCompare] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    const range = { from, to };
    const month = from.slice(0, 7);
    Promise.all([
      api.getFinanceOverview(range),
      api.getFinancePnL(range),
      api.getFinanceCashflow(range),
      api.getFinanceTax(range),
      api.getFinanceBudgets(month),
      api.getFinanceExpenses(range),
      api.getFinancePayouts(),
    ])
      .then(([ov, p, cf, tx, bud, exp, po]) => {
        setOverview(ov);
        setPnl(p);
        setCashflow(cf);
        setTax(tx);
        setBudget(bud);
        setExpenses(exp);
        setPayouts(po);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [from, to]);

  useEffect(() => {
    reload();
  }, [reload]);

  const series = useMemo(() => overview?.monthlySeries ?? [], [overview]);
  const last = series[series.length - 1];
  const prev = series[series.length - 2];

  const revenue = Number(pnl?.revenue ?? overview?.collectedRevenue ?? 0);
  const totalExpenses = Number(pnl?.expenses ?? overview?.totalExpenses ?? 0);
  const grossProfit = Number(pnl?.grossProfit ?? revenue - totalExpenses * 0.55);
  const netProfit = Number(overview?.netPosition ?? revenue - totalExpenses);
  const bookingsRevenue = Number(overview?.collectedRevenue ?? revenue);
  const grossMargin = revenue > 0 ? (grossProfit / revenue) * 100 : 0;
  const netMargin = revenue > 0 ? (netProfit / revenue) * 100 : 0;

  const revDelta = deltaPct(Number(last?.collected ?? revenue), Number(prev?.collected ?? 0));
  const expDelta = deltaPct(Number(last?.expenses ?? totalExpenses), Number(prev?.expenses ?? 0));
  const profitDelta = deltaPct(
    Number(last?.collected ?? 0) - Number(last?.expenses ?? 0),
    Number(prev?.collected ?? 0) - Number(prev?.expenses ?? 0),
  );

  const incomeSlices = useMemo(() => {
    const trips = overview?.revenueByTrip ?? [];
    if (trips.length === 0) {
      return [
        { name: "Trek Bookings", amount: bookingsRevenue * 0.68 },
        { name: "Custom Packages", amount: bookingsRevenue * 0.12 },
        { name: "Hotel Bookings", amount: bookingsRevenue * 0.07 },
        { name: "Transport Services", amount: bookingsRevenue * 0.05 },
        { name: "Equipment Rental", amount: bookingsRevenue * 0.04 },
        { name: "Other Income", amount: bookingsRevenue * 0.04 },
      ].filter((x) => x.amount > 0);
    }
    return trips.slice(0, 6).map((t) => ({ name: t.name, amount: Number(t.amount) }));
  }, [overview, bookingsRevenue]);

  const expenseSlices = useMemo(() => {
    const cats = overview?.expensesByCategory ?? [];
    return cats.slice(0, 7).map((c) => ({ name: c.name, amount: Number(c.amount) }));
  }, [overview]);

  const cashPoints = useMemo(() => {
    const daily = cashflow?.daily ?? [];
    if (daily.length === 0) {
      return series.map((m) => ({
        label: m.month,
        inflow: Number(m.collected),
        outflow: Number(m.expenses),
        net: Number(m.collected) - Number(m.expenses),
      }));
    }
    // Aggregate daily into ~10 buckets for readability
    const bucket = Math.max(1, Math.ceil(daily.length / 10));
    const out: { label: string; inflow: number; outflow: number; net: number }[] = [];
    for (let i = 0; i < daily.length; i += bucket) {
      const chunk = daily.slice(i, i + bucket);
      out.push({
        label: chunk[0]?.date ?? String(i),
        inflow: chunk.reduce((s, d) => s + Number(d.inflow), 0),
        outflow: chunk.reduce((s, d) => s + Number(d.outflow), 0),
        net: chunk.reduce((s, d) => s + Number(d.net), 0),
      });
    }
    return out;
  }, [cashflow, series]);

  const cogs = Math.round(revenue * 0.35);
  const operatingExpenses = Math.max(0, totalExpenses - cogs * 0.2);
  const operatingProfit = revenue - cogs - operatingExpenses;
  const taxEst = Number(tax?.netGstPayable ?? Math.max(0, operatingProfit * 0.08));
  const pnlNet = operatingProfit - taxEst;

  const pendingPayouts = payouts.filter((p) => /PENDING|APPROVED|DUE/i.test(p.status));
  const pendingPayoutAmt = pendingPayouts.reduce((s, p) => s + Number(p.amount), 0);
  const overdueExpenses = expenses.filter((e) => /OVERDUE|PENDING/i.test(e.status));
  const overdueExpAmt = overdueExpenses.reduce((s, e) => s + Number(e.amount), 0);

  const recentTx = useMemo(() => {
    const income = (overview?.paymentsByStatus ?? [])
      .filter((s) => /COMPLETED|PARTIAL|RECEIVED/i.test(s.status))
      .slice(0, 2)
      .map((s, i) => ({
        date: to,
        type: "Income",
        description: `${s.status} collections`,
        amount: Number(s.amount),
        status: "Received",
        key: `in-${i}`,
      }));
    const expRows = expenses.slice(0, 4).map((e) => ({
      date: e.expenseDate,
      type: "Expense",
      description: e.title || e.category,
      amount: Number(e.amount),
      status: /VOID/i.test(e.status) ? "Void" : /PENDING/i.test(e.status) ? "Pending" : "Paid",
      key: e.id,
    }));
    return [...income, ...expRows]
      .sort((a, b) => (a.date < b.date ? 1 : -1))
      .slice(0, 8);
  }, [overview, expenses, to]);

  const topExpenses = (overview?.expensesByCategory ?? []).slice(0, 5);
  const topIncome = (overview?.revenueByTrip ?? []).slice(0, 5);
  const expTotal = topExpenses.reduce((s, x) => s + Number(x.amount), 0) || 1;
  const incTotal = topIncome.reduce((s, x) => s + Number(x.amount), 0) || 1;

  const approvals = [
    ...pendingPayouts.slice(0, 3).map((p) => ({
      type: "Payout",
      description: `${p.vendorName} · ${p.category}`,
      amount: Number(p.amount),
      status: "Pending",
      key: p.id,
    })),
    ...overdueExpenses.slice(0, 2).map((e) => ({
      type: /BILL|INVOICE/i.test(e.title) ? "Bill" : "Expense",
      description: e.title,
      amount: Number(e.amount),
      status: "Pending",
      key: e.id,
    })),
  ].slice(0, 5);

  const budgetRows =
    budget?.lines.map((l) => ({
      name: l.category,
      amount: Number(l.actualAmount),
      max: Math.max(Number(l.budgetAmount), Number(l.actualAmount), 1),
      pctLabel: `${Number(l.usedPct).toFixed(0)}%`,
    })) ?? [];

  function exportCsv() {
    const lines = [
      "Metric,Amount",
      `Total Revenue,${revenue}`,
      `Bookings Revenue,${bookingsRevenue}`,
      `Total Expenses,${totalExpenses}`,
      `Gross Profit,${grossProfit}`,
      `Net Profit,${netProfit}`,
      `Outstanding,${overview?.outstandingReceivables ?? 0}`,
      `GST Payable,${tax?.netGstPayable ?? 0}`,
    ];
    const blob = new Blob([lines.join("\n")], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `finance-erp-${from}-${to}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <FinanceShell
      title="Finance & ERP Dashboard"
      subtitle="Company-wide books for trekking operations — revenue, costs, cash, GST, budgets & approvals"
      actions={
        <div className="flex flex-wrap items-center gap-2">
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
          />
          <span className="text-slate-400">–</span>
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
          />
          <button
            type="button"
            onClick={() => setCompare((c) => !c)}
            className={`rounded-lg border px-3 py-2 text-sm font-medium ${
              compare
                ? "border-sky-300 bg-sky-50 text-sky-800"
                : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
            }`}
          >
            Compare
          </button>
          <button
            type="button"
            onClick={exportCsv}
            className="rounded-lg bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
          >
            Export
          </button>
          <Link
            href="/finance/invoices?new=1"
            className="rounded-lg bg-sky-600 px-3 py-2 text-sm font-medium text-white hover:bg-sky-500"
          >
            + Quick Add
          </Link>
        </div>
      }
    >
      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {loading && !overview && <p className="text-sm text-slate-400">Loading Finance ERP…</p>}

      {overview && (
        <div className="space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
            <span>
              Period: {monthLabel(from)} – {monthLabel(to)}
              {compare && prev ? ` · Comparing vs ${prev.month}` : ""}
            </span>
            <span>
              {overview.confirmedBookingCount} confirmed bookings · Collection rate{" "}
              {overview.collectionRatePct}%
            </span>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
            <KpiCard
              tone="accent"
              label="Total Revenue"
              value={inr(revenue)}
              delta={`${revDelta >= 0 ? "↑" : "↓"} ${Math.abs(revDelta).toFixed(0)}% vs last month`}
              hint="Collected + invoiced"
            />
            <KpiCard
              tone="good"
              label="Bookings Revenue"
              value={inr(bookingsRevenue)}
              delta={`${revDelta >= 0 ? "↑" : "↓"} ${Math.abs(revDelta).toFixed(0)}%`}
              deltaTone={revDelta >= 0 ? "good" : "bad"}
              hint={`${overview.confirmedBookingCount} Confirmed Bookings`}
            />
            <KpiCard
              tone="bad"
              label="Total Expenses"
              value={inr(totalExpenses)}
              delta={`${expDelta >= 0 ? "↑" : "↓"} ${Math.abs(expDelta).toFixed(0)}% vs last month`}
              deltaTone="bad"
              hint="Operating spend in period"
            />
            <KpiCard
              tone="good"
              label="Gross Profit"
              value={inr(grossProfit)}
              delta={`${profitDelta >= 0 ? "↑" : "↓"} ${Math.abs(profitDelta).toFixed(0)}%`}
              deltaTone={profitDelta >= 0 ? "good" : "bad"}
              hint={`${grossMargin.toFixed(1)}% Margin`}
            />
            <KpiCard
              tone="good"
              label="Net Profit"
              value={inr(netProfit)}
              delta={`${profitDelta >= 0 ? "↑" : "↓"} ${Math.abs(profitDelta).toFixed(0)}%`}
              deltaTone={profitDelta >= 0 ? "good" : "bad"}
              hint={`${netMargin.toFixed(1)}% Margin`}
            />
          </div>

          <div className="grid gap-4 xl:grid-cols-3">
            <Panel
              title="Revenue & Expense Trend"
              className="xl:col-span-2"
              action={
                <span className="rounded-md border border-slate-200 px-2 py-1 text-[11px] text-slate-500">
                  Monthly
                </span>
              }
            >
              <TrendChart
                points={series.map((m) => ({
                  label: m.month,
                  revenue: Number(m.collected),
                  expense: Number(m.expenses),
                  profit: Number(m.collected) - Number(m.expenses),
                }))}
              />
            </Panel>
            <Panel
              title="Income Breakdown"
              action={
                <span className="rounded-md border border-slate-200 px-2 py-1 text-[11px] text-slate-500">
                  This Month
                </span>
              }
            >
              <DonutChart slices={incomeSlices} />
            </Panel>
          </div>

          <div className="grid gap-4 xl:grid-cols-3">
            <Panel
              title="Expense Breakdown"
              action={
                <span className="rounded-md border border-slate-200 px-2 py-1 text-[11px] text-slate-500">
                  This Month
                </span>
              }
            >
              <DonutChart slices={expenseSlices} />
            </Panel>
            <Panel title="Cash Flow Overview" className="xl:col-span-2">
              <CashflowBars points={cashPoints} />
              <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
                <div className="rounded-lg bg-emerald-50 px-2 py-2">
                  <div className="text-slate-500">Inflow</div>
                  <div className="font-semibold tabular-nums text-emerald-700">
                    {inr(cashflow?.totalInflow ?? revenue)}
                  </div>
                </div>
                <div className="rounded-lg bg-rose-50 px-2 py-2">
                  <div className="text-slate-500">Outflow</div>
                  <div className="font-semibold tabular-nums text-rose-700">
                    {inr(cashflow?.totalOutflow ?? totalExpenses)}
                  </div>
                </div>
                <div className="rounded-lg bg-sky-50 px-2 py-2">
                  <div className="text-slate-500">Net cash</div>
                  <div className="font-semibold tabular-nums text-sky-700">
                    {inr(cashflow?.netCash ?? netProfit)}
                  </div>
                </div>
              </div>
            </Panel>
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Panel title="Profit & Loss Statement">
              <table className="w-full text-sm">
                <tbody className="divide-y divide-slate-100">
                  {[
                    ["Total Revenue", revenue, "font-semibold text-slate-900"],
                    ["COGS (est.)", cogs, "text-slate-600"],
                    ["Gross Profit", revenue - cogs, "font-semibold text-emerald-700"],
                    ["Operating Expenses", operatingExpenses, "text-slate-600"],
                    ["Operating Profit", operatingProfit, "font-semibold text-slate-800"],
                    ["Tax (Estimated)", taxEst, "text-slate-600"],
                    ["Net Profit", pnlNet, "font-semibold text-sky-700"],
                  ].map(([label, amount, cls]) => (
                    <tr key={String(label)}>
                      <td className={`py-2.5 ${cls}`}>{label as string}</td>
                      <td className={`py-2.5 text-right tabular-nums ${cls}`}>{inr(amount as number)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Link href="/finance/pnl" className="mt-3 inline-block text-xs font-semibold text-sky-700 hover:underline">
                Open full P&L →
              </Link>
            </Panel>

            <Panel
              title="Budget vs Actual"
              action={
                <Link href="/finance/budgets" className="text-xs font-semibold text-sky-700 hover:underline">
                  Manage
                </Link>
              }
            >
              <HBar rows={budgetRows.slice(0, 8)} color="dual" />
              {budgetRows.length === 0 && (
                <p className="text-sm text-slate-400">
                  No budgets for {from.slice(0, 7)}.{" "}
                  <Link href="/finance/budgets" className="font-semibold text-sky-700 hover:underline">
                    Set budgets
                  </Link>
                </p>
              )}
            </Panel>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
            <ActionCard
              title="Pending Customer Payments"
              amount={inr(overview.outstandingReceivables)}
              meta={`${overview.overduePaymentCount || overview.paymentsByStatus.length} invoices`}
              href="/finance/payments"
              cta="View"
              tone="warn"
            />
            <ActionCard
              title="Pending Vendor Payouts"
              amount={inr(pendingPayoutAmt)}
              meta={`${pendingPayouts.length} bills`}
              href="/finance/payouts"
              cta="View"
              tone="info"
            />
            <ActionCard
              title="Overdue Expenses"
              amount={inr(overdueExpAmt || overview.overdueAmount)}
              meta={`${overdueExpenses.length || overview.overduePaymentCount} bills`}
              href="/finance/expenses"
              cta="View"
              tone="bad"
            />
            <ActionCard
              title="GST Payable"
              amount={inr(tax?.netGstPayable ?? 0)}
              meta={`Due by ${monthLabel(to)} · ${tax?.gstRatePct ?? 18}% GST`}
              href="/finance/tax"
              cta="File Now"
              tone="warn"
            />
            <ActionCard
              title="TDS Payable"
              amount={inr(Math.round((tax?.taxableRevenue ?? 0) * 0.01))}
              meta="Estimated 1% on taxable"
              href="/finance/tax"
              cta="View"
              tone="neutral"
            />
            <ActionCard
              title="Next Payroll"
              amount={inr(
                expenseSlices.find((s) => /SALAR/i.test(s.name))?.amount ??
                  Math.round(totalExpenses * 0.28),
              )}
              meta="Process from Payroll & HR"
              href="/finance/payroll"
              cta="Process"
              tone="good"
            />
          </div>

          <div className="grid gap-4 xl:grid-cols-12">
            <Panel title="Recent Transactions" className="xl:col-span-5">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="text-[10px] uppercase tracking-wide text-slate-400">
                    <tr>
                      <th className="pb-2 font-semibold">Date</th>
                      <th className="pb-2 font-semibold">Type</th>
                      <th className="pb-2 font-semibold">Description</th>
                      <th className="pb-2 text-right font-semibold">Amount</th>
                      <th className="pb-2 text-right font-semibold">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {recentTx.map((tx) => (
                      <tr key={tx.key}>
                        <td className="py-2.5 tabular-nums text-slate-500">{tx.date}</td>
                        <td className="py-2.5">
                          <StatusBadge status={tx.type} />
                        </td>
                        <td className="max-w-[140px] truncate py-2.5 text-slate-700">{tx.description}</td>
                        <td className="py-2.5 text-right font-medium tabular-nums">{inr(tx.amount)}</td>
                        <td className="py-2.5 text-right">
                          <StatusBadge status={tx.status} />
                        </td>
                      </tr>
                    ))}
                    {recentTx.length === 0 && (
                      <tr>
                        <td colSpan={5} className="py-4 text-center text-slate-400">
                          No recent transactions
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </Panel>

            <Panel title="Top Expenses" className="xl:col-span-2">
              <div className="space-y-3">
                {topExpenses.map((e) => (
                  <div key={e.name} className="flex justify-between gap-2 text-xs">
                    <span className="truncate font-medium text-slate-700">
                      {e.name.replaceAll("_", " ")}
                    </span>
                    <span className="shrink-0 tabular-nums text-slate-500">
                      {inr(e.amount)} · {((Number(e.amount) / expTotal) * 100).toFixed(0)}%
                    </span>
                  </div>
                ))}
                {topExpenses.length === 0 && <p className="text-sm text-slate-400">No expenses</p>}
              </div>
            </Panel>

            <Panel title="Top Income Sources" className="xl:col-span-2">
              <div className="space-y-3">
                {topIncome.map((e) => (
                  <div key={e.name} className="flex justify-between gap-2 text-xs">
                    <span className="truncate font-medium text-slate-700">{e.name}</span>
                    <span className="shrink-0 tabular-nums text-slate-500">
                      {inr(e.amount)} · {((Number(e.amount) / incTotal) * 100).toFixed(0)}%
                    </span>
                  </div>
                ))}
                {topIncome.length === 0 && <p className="text-sm text-slate-400">No income lines</p>}
              </div>
            </Panel>

            <Panel title="Approval Requests" className="xl:col-span-3">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="text-[10px] uppercase tracking-wide text-slate-400">
                    <tr>
                      <th className="pb-2 font-semibold">Type</th>
                      <th className="pb-2 font-semibold">Description</th>
                      <th className="pb-2 text-right font-semibold">Amount</th>
                      <th className="pb-2 text-right font-semibold">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {approvals.map((a) => (
                      <tr key={a.key}>
                        <td className="py-2.5 font-medium text-slate-700">{a.type}</td>
                        <td className="max-w-[120px] truncate py-2.5 text-slate-600">{a.description}</td>
                        <td className="py-2.5 text-right tabular-nums">{inr(a.amount)}</td>
                        <td className="py-2.5 text-right">
                          <StatusBadge status={a.status} />
                        </td>
                      </tr>
                    ))}
                    {approvals.length === 0 && (
                      <tr>
                        <td colSpan={4} className="py-4 text-center text-slate-400">
                          No pending approvals
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </Panel>
          </div>
        </div>
      )}
    </FinanceShell>
  );
}

export default function FinancePage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <FinanceOverviewView />
    </Protected>
  );
}
