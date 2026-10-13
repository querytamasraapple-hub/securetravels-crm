"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { OPS_DESK, canWriteOperations } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { AdminPage } from "@/components/AdminPage";

export default function VisitorsPage() {
  const { user } = useAuth();
  const canWrite = canWriteOperations(user?.role);
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ visitorName: "", phone: "", purpose: "Office visit" });

  const reload = useCallback(() => {
    api.getBasecampVisitors().then(setRows).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await api.checkInBasecampVisitor(form);
      setForm({ visitorName: "", phone: "", purpose: "Office visit" });
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed");
    }
  }

  return (
    <AdminPage title="Visitors" subtitle="Office / basecamp visitor log" allowedRoles={OPS_DESK}>
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      {canWrite && (
        <form onSubmit={onSubmit} className="mb-4 grid gap-2 rounded-xl border bg-white p-4 md:grid-cols-4">
          <input required placeholder="Visitor name" value={form.visitorName} onChange={(e) => setForm({ ...form, visitorName: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
          <input placeholder="Phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
          <input required placeholder="Purpose" value={form.purpose} onChange={(e) => setForm({ ...form, purpose: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
          <button type="submit" className="rounded-lg bg-slate-900 px-3 py-2 text-sm text-white">Check in</button>
        </form>
      )}
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-[11px] uppercase text-slate-400">
            <tr>
              <th className="px-4 py-3 text-left">Visitor</th>
              <th className="px-4 py-3 text-left">Purpose</th>
              <th className="px-4 py-3 text-left">In</th>
              <th className="px-4 py-3 text-left">Out</th>
              {canWrite && <th className="px-4 py-3" />}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={String(r.id)} className="border-t">
                <td className="px-4 py-3">
                  <div className="font-medium">{String(r.visitorName)}</div>
                  <div className="text-xs text-slate-400">{String(r.phone ?? "")}</div>
                </td>
                <td className="px-4 py-3 text-slate-600">{String(r.purpose)}</td>
                <td className="px-4 py-3 text-xs text-slate-500">{r.checkedInAt ? new Date(String(r.checkedInAt)).toLocaleString() : "—"}</td>
                <td className="px-4 py-3 text-xs text-slate-500">{r.checkedOutAt ? new Date(String(r.checkedOutAt)).toLocaleString() : "—"}</td>
                {canWrite && (
                  <td className="px-4 py-3">
                    {!r.checkedOutAt && (
                      <button onClick={() => api.checkOutBasecampVisitor(String(r.id)).then(reload)} className="text-xs text-emerald-700 hover:underline">
                        Check out
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
