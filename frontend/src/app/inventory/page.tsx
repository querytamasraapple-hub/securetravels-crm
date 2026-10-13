"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { OPS_DESK, canWriteOperations } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { AdminPage } from "@/components/AdminPage";

export default function InventoryPage() {
  const { user } = useAuth();
  const canWrite = canWriteOperations(user?.role);
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    sku: "",
    name: "",
    category: "GEAR",
    quantity: 0,
    reorderLevel: 5,
    location: "Base store",
  });

  const reload = useCallback(() => {
    api.getBasecampInventory().then(setRows).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await api.createBasecampInventory(form);
      setForm({ ...form, sku: "", name: "" });
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed");
    }
  }

  return (
    <AdminPage title="Inventory" subtitle="Trek gear, safety kit and camp stock" allowedRoles={OPS_DESK}>
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      {canWrite && (
        <form onSubmit={onSubmit} className="mb-4 grid gap-2 rounded-xl border bg-white p-4 md:grid-cols-6">
          <input required placeholder="SKU" value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} className="rounded-lg border px-3 py-2 text-sm" />
          <input required placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="rounded-lg border px-3 py-2 text-sm md:col-span-2" />
          <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} className="rounded-lg border px-3 py-2 text-sm">
            {["GEAR", "SAFETY", "CAMPING", "MEDICAL", "OFFICE", "OTHER"].map((c) => <option key={c}>{c}</option>)}
          </select>
          <input type="number" min={0} value={form.quantity} onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })} className="rounded-lg border px-3 py-2 text-sm" />
          <button type="submit" className="rounded-lg bg-slate-900 px-3 py-2 text-sm text-white">Add item</button>
        </form>
      )}
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-[11px] uppercase text-slate-400">
            <tr>
              <th className="px-4 py-3 text-left">SKU</th>
              <th className="px-4 py-3 text-left">Item</th>
              <th className="px-4 py-3 text-left">Qty</th>
              <th className="px-4 py-3 text-left">Reorder</th>
              <th className="px-4 py-3 text-left">Location</th>
              {canWrite && <th className="px-4 py-3" />}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={String(r.id)} className={`border-t ${r.lowStock ? "bg-rose-50/50" : ""}`}>
                <td className="px-4 py-3 font-mono text-xs">{String(r.sku)}</td>
                <td className="px-4 py-3 font-medium">{String(r.name)} <span className="text-xs text-slate-400">{String(r.category)}</span></td>
                <td className="px-4 py-3 tabular-nums">{String(r.quantity)} {String(r.unit)}</td>
                <td className="px-4 py-3 tabular-nums text-slate-500">{String(r.reorderLevel)}</td>
                <td className="px-4 py-3 text-slate-500">{String(r.location ?? "—")}</td>
                {canWrite && (
                  <td className="px-4 py-3">
                    <button
                      onClick={() => {
                        const q = Number(prompt("New quantity", String(r.quantity)));
                        if (!Number.isNaN(q)) api.adjustBasecampInventory(String(r.id), q).then(reload);
                      }}
                      className="text-xs text-emerald-700 hover:underline"
                    >
                      Adjust
                    </button>
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
