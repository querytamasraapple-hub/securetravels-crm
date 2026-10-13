"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  api,
  type FinanceExpense,
  type FinanceExpenseCreatePayload,
  type Trip,
} from "@/lib/api";
import { FINANCE_PORTAL, canWriteFinance } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { Protected } from "@/components/Protected";
import { FinanceShell, inr, KpiCard } from "@/components/finance/FinanceShell";

const CATEGORIES = [
  "GUIDE_FEES",
  "TRANSPORT",
  "HOTEL",
  "PERMITS",
  "FOOD",
  "GEAR",
  "MARKETING",
  "SALARIES",
  "INSURANCE",
  "OTHER",
];

const METHODS = ["CASH", "UPI", "BANK_TRANSFER", "CARD", "CHEQUE"];

function ExpensesView() {
  const { user } = useAuth();
  const canWrite = canWriteFinance(user?.role);
  const [rows, setRows] = useState<FinanceExpense[]>([]);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<FinanceExpenseCreatePayload>({
    category: "GUIDE_FEES",
    title: "",
    amount: 0,
    expenseDate: new Date().toISOString().slice(0, 10),
    paymentMethod: "BANK_TRANSFER",
    status: "POSTED",
  });

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    Promise.all([api.getFinanceExpenses(), api.getTrips(true).catch(() => [] as Trip[])])
      .then(([expenses, tripList]) => {
        setRows(expenses);
        setTrips(tripList);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const posted = rows.filter((r) => r.status === "POSTED");
  const total = posted.reduce((s, r) => s + Number(r.amount), 0);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.createFinanceExpense({
        ...form,
        amount: Number(form.amount),
        tripId: form.tripId || undefined,
        vendorName: form.vendorName || undefined,
        notes: form.notes || undefined,
      });
      setShowForm(false);
      setForm({
        category: "GUIDE_FEES",
        title: "",
        amount: 0,
        expenseDate: new Date().toISOString().slice(0, 10),
        paymentMethod: "BANK_TRANSFER",
        status: "POSTED",
      });
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save expense");
    }
  }

  async function voidRow(id: string) {
    try {
      await api.voidFinanceExpense(id);
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not void");
    }
  }

  return (
    <FinanceShell
      title="Operating expenses"
      subtitle="Guide fees, permits, transport, hotels — full company cost book"
      actions={
        canWrite ? (
          <button
            onClick={() => setShowForm((v) => !v)}
            className="rounded-lg bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
          >
            {showForm ? "Close form" : "Add expense"}
          </button>
        ) : null
      }
    >
      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="mb-6 grid gap-4 md:grid-cols-2">
        <KpiCard tone="warn" label="Posted expenses" value={inr(total)} hint={`${posted.length} lines`} />
        <KpiCard label="All lines" value={String(rows.length)} hint="Including draft / void" />
      </div>

      {showForm && canWrite && (
        <form
          onSubmit={onSubmit}
          className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-3"
        >
          <label className="text-sm">
            <span className="font-medium text-slate-700">Category</span>
            <select
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            >
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c.replaceAll("_", " ")}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm md:col-span-2">
            <span className="font-medium text-slate-700">Title</span>
            <input
              required
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="EBC porter wages — April batch"
            />
          </label>
          <label className="text-sm">
            <span className="font-medium text-slate-700">Amount (₹)</span>
            <input
              type="number"
              min={1}
              required
              value={form.amount || ""}
              onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            />
          </label>
          <label className="text-sm">
            <span className="font-medium text-slate-700">Date</span>
            <input
              type="date"
              required
              value={form.expenseDate}
              onChange={(e) => setForm({ ...form, expenseDate: e.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            />
          </label>
          <label className="text-sm">
            <span className="font-medium text-slate-700">Payment method</span>
            <select
              value={form.paymentMethod}
              onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            >
              {METHODS.map((m) => (
                <option key={m} value={m}>
                  {m.replaceAll("_", " ")}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            <span className="font-medium text-slate-700">Trek / package</span>
            <select
              value={form.tripId ?? ""}
              onChange={(e) => setForm({ ...form, tripId: e.target.value || undefined })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            >
              <option value="">Company overhead</option>
              {trips.map((t) => (
                <option key={t.id} value={t.id as string}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            <span className="font-medium text-slate-700">Vendor</span>
            <input
              value={form.vendorName ?? ""}
              onChange={(e) => setForm({ ...form, vendorName: e.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="Guide / hotel / transporter"
            />
          </label>
          <label className="text-sm md:col-span-3">
            <span className="font-medium text-slate-700">Notes</span>
            <textarea
              value={form.notes ?? ""}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              rows={2}
            />
          </label>
          <div className="md:col-span-3">
            <button
              type="submit"
              className="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800"
            >
              Post expense
            </button>
          </div>
        </form>
      )}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-100 bg-slate-50 text-[11px] uppercase tracking-wide text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">Date</th>
              <th className="px-4 py-3 font-medium">Category</th>
              <th className="px-4 py-3 font-medium">Title</th>
              <th className="px-4 py-3 font-medium">Trek</th>
              <th className="px-4 py-3 font-medium">Amount</th>
              <th className="px-4 py-3 font-medium">Status</th>
              {canWrite && <th className="px-4 py-3 font-medium" />}
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-slate-400">
                  Loading…
                </td>
              </tr>
            )}
            {!loading && rows.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-slate-400">
                  No expenses posted yet — add guide fees, permits, hotels…
                </td>
              </tr>
            )}
            {rows.map((r) => (
              <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                <td className="px-4 py-3 text-slate-600">{r.expenseDate}</td>
                <td className="px-4 py-3 text-xs font-medium uppercase text-slate-500">
                  {r.category.replaceAll("_", " ")}
                </td>
                <td className="px-4 py-3">
                  <div className="font-medium text-slate-800">{r.title}</div>
                  {r.vendorName && <div className="text-xs text-slate-400">{r.vendorName}</div>}
                </td>
                <td className="px-4 py-3 text-slate-500">{r.tripName ?? "—"}</td>
                <td className="px-4 py-3 font-semibold tabular-nums">{inr(r.amount)}</td>
                <td className="px-4 py-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                      r.status === "POSTED"
                        ? "bg-emerald-50 text-emerald-700"
                        : r.status === "VOID"
                          ? "bg-slate-100 text-slate-500"
                          : "bg-amber-50 text-amber-700"
                    }`}
                  >
                    {r.status}
                  </span>
                </td>
                {canWrite && (
                  <td className="px-4 py-3">
                    {r.status === "POSTED" && (
                      <button
                        onClick={() => voidRow(r.id)}
                        className="text-xs font-medium text-red-600 hover:underline"
                      >
                        Void
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </FinanceShell>
  );
}

export default function FinanceExpensesPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <ExpensesView />
    </Protected>
  );
}
