"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api, type CustomerListItem } from "@/lib/api";
import { OPS_DESK } from "@/lib/rbac";
import { AdminPage } from "@/components/AdminPage";

export default function DocumentsPage() {
  const [customers, setCustomers] = useState<CustomerListItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.getCustomers().then(setCustomers).catch((e: Error) => setError(e.message));
  }, []);

  return (
    <AdminPage
      title="Documents"
      subtitle="Traveller compliance documents — presigned S3 uploads (OPS+ only)"
      allowedRoles={OPS_DESK}
    >
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      <div className="mb-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
        File bytes never pass through the API. Open a traveller to request upload URLs / confirm docs.
      </div>
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-[11px] uppercase text-slate-400">
            <tr>
              <th className="px-4 py-3 text-left">Traveller</th>
              <th className="px-4 py-3 text-left">Mobile</th>
              <th className="px-4 py-3 text-left">Trips</th>
              <th className="px-4 py-3 text-left">Actions</th>
            </tr>
          </thead>
          <tbody>
            {customers.slice(0, 50).map((c) => (
              <tr key={c.id as string} className="border-t">
                <td className="px-4 py-3 font-medium">{c.fullName}</td>
                <td className="px-4 py-3 text-slate-500">{c.mobileNumber ?? "—"}</td>
                <td className="px-4 py-3 tabular-nums">{c.totalTrips ?? 0}</td>
                <td className="px-4 py-3">
                  <Link href="/customers" className="text-xs font-medium text-emerald-700 hover:underline">
                    Open travelers
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminPage>
  );
}
