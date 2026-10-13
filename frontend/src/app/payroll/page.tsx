"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api, type TeamUser } from "@/lib/api";
import { PAYROLL_ROLES, canEditPayroll } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { AdminPage } from "@/components/AdminPage";

export default function PayrollPage() {
  const { user } = useAuth();
  const canEdit = canEditPayroll(user?.role);
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [staff, setStaff] = useState<TeamUser[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    userId: "",
    basicPay: 30000,
    allowances: 5000,
    deductions: 0,
    status: "DRAFT",
  });

  const reload = useCallback(() => {
    api.getBasecampPayroll(month).then(setRows).catch((e: Error) => setError(e.message));
  }, [month]);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    if (!canEdit) return;
    api.getTeamUsers({ active: true }).then(setStaff).catch(() => undefined);
  }, [canEdit]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await api.upsertBasecampPayroll({ ...form, periodMonth: month });
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed");
    }
  }

  return (
    <AdminPage
      title="Payroll"
      subtitle="Restricted — ADMIN/CEO edit; managers & finance view"
      allowedRoles={PAYROLL_ROLES}
    >
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} className="mb-4 rounded-lg border px-3 py-2 text-sm" />
      {canEdit ? (
        <form onSubmit={onSubmit} className="mb-4 grid gap-2 rounded-xl border bg-white p-4 md:grid-cols-5">
          <select required value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })} className="rounded-lg border px-3 py-2 text-sm md:col-span-2">
            <option value="">Select staff</option>
            {staff.map((s) => (
              <option key={s.id} value={s.id}>{s.fullName} · {s.role}</option>
            ))}
          </select>
          <input type="number" min={0} value={form.basicPay} onChange={(e) => setForm({ ...form, basicPay: Number(e.target.value) })} className="rounded-lg border px-3 py-2 text-sm" />
          <input type="number" min={0} value={form.allowances} onChange={(e) => setForm({ ...form, allowances: Number(e.target.value) })} className="rounded-lg border px-3 py-2 text-sm" />
          <button type="submit" className="rounded-lg bg-slate-900 px-3 py-2 text-sm text-white">Save payslip</button>
        </form>
      ) : (
        <p className="mb-3 text-xs text-slate-500">View-only. Payroll edits require ADMIN/CEO.</p>
      )}
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-[11px] uppercase text-slate-400">
            <tr>
              <th className="px-4 py-3 text-left">Staff</th>
              <th className="px-4 py-3 text-left">Month</th>
              <th className="px-4 py-3 text-left">Basic</th>
              <th className="px-4 py-3 text-left">Net</th>
              <th className="px-4 py-3 text-left">Status</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={String(r.id)} className="border-t">
                <td className="px-4 py-3 font-medium">{String(r.userName)}</td>
                <td className="px-4 py-3">{String(r.periodMonth)}</td>
                <td className="px-4 py-3 tabular-nums">₹{Number(r.basicPay).toLocaleString("en-IN")}</td>
                <td className="px-4 py-3 font-semibold tabular-nums">₹{Number(r.netPay).toLocaleString("en-IN")}</td>
                <td className="px-4 py-3 text-xs font-semibold">{String(r.status)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminPage>
  );
}
