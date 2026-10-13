"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api, type Guide } from "@/lib/api";
import { OPS_DESK, canWriteOperations } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { AdminPage } from "@/components/AdminPage";

const CATS = ["GUIDE", "HOTEL", "TRANSPORT", "DRIVER"];

export default function VendorsPage() {
  const { user } = useAuth();
  const canWrite = canWriteOperations(user?.role);
  const [rows, setRows] = useState<Guide[]>([]);
  const [category, setCategory] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [show, setShow] = useState(false);
  const [form, setForm] = useState({ category: "HOTEL", name: "", phone: "", city: "" });

  const reload = useCallback(() => {
    api.getVendors({ category: category || undefined })
      .then(setRows)
      .catch((e: Error) => setError(e.message));
  }, [category]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await api.createVendor({
        category: form.category,
        name: form.name,
        phone: form.phone || undefined,
        city: form.city || undefined,
      });
      setShow(false);
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create vendor");
    }
  }

  return (
    <AdminPage
      title="Vendors"
      subtitle="Hotels, transport, drivers and guides — ops write only"
      allowedRoles={OPS_DESK}
      actions={
        canWrite ? (
          <button onClick={() => setShow((v) => !v)} className="rounded-lg bg-slate-900 px-3 py-2 text-sm text-white">
            {show ? "Close" : "Add vendor"}
          </button>
        ) : null
      }
    >
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      <select value={category} onChange={(e) => setCategory(e.target.value)} className="mb-4 rounded-lg border px-3 py-2 text-sm">
        <option value="">All categories</option>
        {CATS.map((c) => <option key={c} value={c}>{c}</option>)}
      </select>
      {show && canWrite && (
        <form onSubmit={onSubmit} className="mb-4 grid gap-2 rounded-xl border bg-white p-4 md:grid-cols-4">
          <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} className="rounded-lg border px-3 py-2 text-sm">
            {CATS.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
          <input required placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
          <input placeholder="Phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
          <button type="submit" className="rounded-lg bg-emerald-700 px-3 py-2 text-sm text-white">Save</button>
        </form>
      )}
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-[11px] uppercase text-slate-400">
            <tr>
              <th className="px-4 py-3 text-left">Name</th>
              <th className="px-4 py-3 text-left">Category</th>
              <th className="px-4 py-3 text-left">Phone</th>
              <th className="px-4 py-3 text-left">City</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((v) => (
              <tr key={v.id as string} className="border-t">
                <td className="px-4 py-3 font-medium">{v.name}</td>
                <td className="px-4 py-3">{v.category}</td>
                <td className="px-4 py-3 text-slate-500">{v.phone ?? "—"}</td>
                <td className="px-4 py-3 text-slate-500">{(v as { city?: string }).city ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminPage>
  );
}
