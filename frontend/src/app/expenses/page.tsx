"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  api,
  type FinanceExpense,
  type FinanceExpenseCreatePayload,
  type Trip,
} from "@/lib/api";
import { MANAGERS, canWriteFinance } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { AdminPage } from "@/components/AdminPage";

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

function inr(n: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(n) || 0);
}

export default function ExpensesPage() {
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
    <AdminPage
      title="Expenses"
      subtitle="Ops cost book — managers can view; FINANCE/ADMIN post or void"
      allowedRoles={MANAGERS}
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

      <div className="mb-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-3">
          <div className="text-[11px] uppercase tracking-wide text-slate-400">Posted total</div>
          <div className="text-lg font-semibold text-slate-900">{inr(total)}</div>
          <div className="text-xs text-slate-500">{posted.length} lines</div>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-3">
          <div className="text-[11px] uppercase tracking-wide text-slate-400">All lines</div>
          <div className="text-lg font-semibold text-slate-900">{rows.length}</div>
          <div className="text-xs text-slate-500">Including draft / void</div>
        </div>
      </div>

      {showForm && canWrite && (
        <form
          onSubmit={onSubmit}
          className="mb-4 grid gap-2 rounded-xl border border-slate-200 bg-white p-4 md:grid-cols-3"
        >
          <select
            value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value })}
            className="rounded-lg border px-3 py-2 text-sm"
          >
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c.replaceAll("_", " ")}
              </option>
            ))}
          </select>
          <input
            required
            placeholder="Title"
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            className="rounded-lg border px-3 py-2 text-sm md:col-span-2"
          />
          <input
            type="number"
            min={1}
            required
            placeholder="Amount ₹"
            value={form.amount || ""}
            onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })}
            className="rounded-lg border px-3 py-2 text-sm"
          />
          <input
            type="date"
            required
            value={form.expenseDate}
            onChange={(e) => setForm({ ...form, expenseDate: e.target.value })}
            className="rounded-lg border px-3 py-2 text-sm"
          />
          <select
            value={form.paymentMethod}
            onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}
            className="rounded-lg border px-3 py-2 text-sm"
          >
            {METHODS.map((m) => (
              <option key={m} value={m}>
                {m.replaceAll("_", " ")}
              </option>
            ))}
          </select>
          <select
            value={form.tripId ?? ""}
            onChange={(e) => setForm({ ...form, tripId: e.target.value || undefined })}
            className="rounded-lg border px-3 py-2 text-sm"
          >
            <option value="">Company overhead</option>
            {trips.map((t) => (
              <option key={t.id} value={t.id as string}>
                {t.name}
              </option>
            ))}
          </select>
          <input
            placeholder="Vendor"
            value={form.vendorName ?? ""}
            onChange={(e) => setForm({ ...form, vendorName: e.target.value })}
            className="rounded-lg border px-3 py-2 text-sm"
          />
          <button type="submit" className="rounded-lg bg-emerald-700 px-3 py-2 text-sm text-white md:col-span-3">
            Post expense
          </button>
        </form>
      )}

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-100 bg-slate-50 text-[11px] uppercase tracking-wide text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">Date</th>
              <th className="px-4 py-3 font-medium">Category</th>
              <th className="px-4 py-3 font-medium">Title</th>
              <th className="px-4 py-3 font-medium">Amount</th>
              <th className="px-4 py-3 font-medium">Status</th>
              {canWrite && <th className="px-4 py-3 font-medium" />}
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-slate-400">
                  Loading…
                </td>
              </tr>
            )}
            {!loading && rows.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-slate-400">
                  No expenses yet
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
                <td className="px-4 py-3 font-semibold tabular-nums">{inr(r.amount)}</td>
                <td className="px-4 py-3 text-xs font-semibold uppercase text-slate-500">{r.status}</td>
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
    </AdminPage>
  );
}
