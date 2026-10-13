"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { HR_ROLES, MANAGERS, hasRole } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { AdminPage } from "@/components/AdminPage";

export default function LeavePage() {
  const { user } = useAuth();
  const isManager = hasRole(user?.role, MANAGERS);
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    leaveType: "CASUAL",
    fromDate: new Date().toISOString().slice(0, 10),
    toDate: new Date().toISOString().slice(0, 10),
    reason: "",
  });

  const reload = useCallback(() => {
    api.getBasecampLeave().then(setRows).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await api.createBasecampLeave(form);
      setForm({ ...form, reason: "" });
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed");
    }
  }

  return (
    <AdminPage title="Leave" subtitle="Request leave; managers approve / reject" allowedRoles={HR_ROLES}>
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      <form onSubmit={onSubmit} className="mb-4 grid gap-2 rounded-xl border bg-white p-4 md:grid-cols-5">
        <select value={form.leaveType} onChange={(e) => setForm({ ...form, leaveType: e.target.value })} className="rounded-lg border px-3 py-2 text-sm">
          {["CASUAL", "SICK", "EARNED", "UNPAID", "COMP_OFF"].map((t) => <option key={t}>{t}</option>)}
        </select>
        <input type="date" value={form.fromDate} onChange={(e) => setForm({ ...form, fromDate: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
        <input type="date" value={form.toDate} onChange={(e) => setForm({ ...form, toDate: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
        <input placeholder="Reason" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
        <button type="submit" className="rounded-lg bg-slate-900 px-3 py-2 text-sm text-white">Request</button>
      </form>
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-[11px] uppercase text-slate-400">
            <tr>
              <th className="px-4 py-3 text-left">Staff</th>
              <th className="px-4 py-3 text-left">Type</th>
              <th className="px-4 py-3 text-left">Dates</th>
              <th className="px-4 py-3 text-left">Status</th>
              {isManager && <th className="px-4 py-3" />}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={String(r.id)} className="border-t">
                <td className="px-4 py-3 font-medium">{String(r.userName)}</td>
                <td className="px-4 py-3">{String(r.leaveType)}</td>
                <td className="px-4 py-3 text-slate-500">{String(r.fromDate)} → {String(r.toDate)}</td>
                <td className="px-4 py-3 text-xs font-semibold">{String(r.status)}</td>
                {isManager && (
                  <td className="px-4 py-3 space-x-2">
                    {r.status === "PENDING" && (
                      <>
                        <button onClick={() => api.reviewBasecampLeave(String(r.id), "APPROVED").then(reload)} className="text-xs text-emerald-700 hover:underline">Approve</button>
                        <button onClick={() => api.reviewBasecampLeave(String(r.id), "REJECTED").then(reload)} className="text-xs text-rose-600 hover:underline">Reject</button>
                      </>
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
