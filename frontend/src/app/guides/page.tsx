"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type Guide } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { canManageCatalogue } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { Sidebar } from "@/components/Sidebar";
import { GuidesPanel } from "@/components/trip/GuidesPanel";

function GuidesView() {
  const { user } = useAuth();
  const canManage = canManageCatalogue(user?.role);
  const [guides, setGuides] = useState<Guide[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setGuides(await api.getGuides(false));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load guides");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <main className="flex-1 p-6">
        <div className="mb-6">
          <h1 className="text-xl font-semibold text-slate-900">Guides</h1>
          <p className="text-sm text-slate-500">
            Trek leaders and tour guides — assign them to departure batches from Packages.
          </p>
        </div>
        {error && (
          <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
        )}
        {loading ? (
          <p className="py-12 text-center text-sm text-slate-400">Loading guides…</p>
        ) : (
          <div className="max-w-3xl">
            <GuidesPanel guides={guides} canManage={canManage} onChanged={load} />
          </div>
        )}
      </main>
    </div>
  );
}

export default function GuidesPage() {
  return (
    <Protected>
      <GuidesView />
    </Protected>
  );
}
