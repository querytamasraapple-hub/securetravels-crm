"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { api, type Payment } from "@/lib/api";
import { FINANCE_PORTAL, canWritePayments } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { Protected } from "@/components/Protected";
import { FinanceShell, inr, KpiCard } from "@/components/finance/FinanceShell";

const STATUS_STYLE: Record<string, string> = {
  PENDING: "bg-amber-50 text-amber-700",
  PARTIAL: "bg-sky-50 text-sky-700",
  COMPLETED: "bg-emerald-50 text-emerald-700",
  OVERDUE: "bg-red-50 text-red-700",
  CANCELLED: "bg-slate-100 text-slate-500",
  REFUNDED: "bg-purple-50 text-purple-700",
};

function ReceivablesView() {
  const { user } = useAuth();
  const canWrite = canWritePayments(user?.role);
  const [rows, setRows] = useState<Payment[]>([]);
  const [status, setStatus] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    api
      .getPayments({ status: status || undefined })
      .then(setRows)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [status]);

  useEffect(() => {
    reload();
  }, [reload]);

  const totals = useMemo(() => {
    let collected = 0;
    let open = 0;
    let overdue = 0;
    for (const p of rows) {
      const amt = Number(p.amount ?? 0);
      if (p.status === "COMPLETED" || p.status === "PARTIAL") collected += amt;
      if (p.status === "PENDING" || p.status === "OVERDUE") open += amt;
      if (p.status === "OVERDUE") overdue += amt;
    }
    return { collected, open, overdue };
  }, [rows]);

  async function mark(id: string, next: "COMPLETED" | "REFUNDED") {
    try {
      await api.updatePaymentStatus(id, {
        status: next,
        paidAt: next === "COMPLETED" ? new Date().toISOString() : undefined,
      });
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Update failed");
    }
  }

  return (
    <FinanceShell
      title="Receivables ledger"
      subtitle="All booking advances, balances and overdue trek payments"
      actions={
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
        >
          <option value="">All statuses</option>
          {Object.keys(STATUS_STYLE).map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      }
    >
      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <KpiCard tone="good" label="In view · collected" value={inr(totals.collected)} />
        <KpiCard tone="warn" label="In view · open" value={inr(totals.open)} />
        <KpiCard tone="bad" label="In view · overdue" value={inr(totals.overdue)} />
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-100 bg-slate-50 text-[11px] uppercase tracking-wide text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">Booking</th>
              <th className="px-4 py-3 font-medium">Type</th>
              <th className="px-4 py-3 font-medium">Amount</th>
              <th className="px-4 py-3 font-medium">Due</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Ref</th>
              {canWrite && <th className="px-4 py-3 font-medium">Actions</th>}
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
                  No payment lines
                </td>
              </tr>
            )}
            {rows.map((p) => (
              <tr key={p.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                <td className="px-4 py-3 font-mono text-xs text-slate-600">
                  {(p.bookingId as string)?.slice(0, 8)}…
                </td>
                <td className="px-4 py-3 text-slate-700">{p.amountType}</td>
                <td className="px-4 py-3 font-semibold tabular-nums">{inr(Number(p.amount))}</td>
                <td className="px-4 py-3 text-slate-500">{p.dueDate ?? "—"}</td>
                <td className="px-4 py-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${STATUS_STYLE[p.status as string] ?? "bg-slate-100"}`}
                  >
                    {p.status}
                  </span>
                </td>
                <td className="px-4 py-3 text-xs text-slate-400">{p.gatewayRef ?? "—"}</td>
                {canWrite && (
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      {(p.status === "PENDING" || p.status === "OVERDUE" || p.status === "PARTIAL") && (
                        <button
                          onClick={() => mark(p.id as string, "COMPLETED")}
                          className="text-xs font-medium text-emerald-700 hover:underline"
                        >
                          Mark paid
                        </button>
                      )}
                      {p.status === "COMPLETED" && (
                        <button
                          onClick={() => mark(p.id as string, "REFUNDED")}
                          className="text-xs font-medium text-purple-700 hover:underline"
                        >
                          Refund
                        </button>
                      )}
                    </div>
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

export default function FinancePaymentsPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <ReceivablesView />
    </Protected>
  );
}
