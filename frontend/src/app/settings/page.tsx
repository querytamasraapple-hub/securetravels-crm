"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { CRM_STAFF, canEditSettings } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { AdminPage } from "@/components/AdminPage";

const LABELS: Record<string, string> = {
  "company.legal_name": "Legal name",
  "company.trade_name": "Trade name",
  "company.gstin": "GSTIN",
  "company.support_email": "Support email",
  "company.support_phone": "Support phone",
  "company.timezone": "Timezone",
  "company.address": "Address",
  "company.website": "Website",
};

export default function SettingsPage() {
  const { user } = useAuth();
  const canEdit = canEditSettings(user?.role);
  const [settings, setSettings] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const reload = useCallback(() => {
    api.getBasecampSettings().then(setSettings).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  async function save(key: string, value: string) {
    setSaved(null);
    try {
      const next = await api.updateBasecampSetting(key, value);
      setSettings(next);
      setSaved(key);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed");
    }
  }

  return (
    <AdminPage
      title="Settings"
      subtitle="Safe company profile only — secrets / JWT / API keys cannot be stored here"
      allowedRoles={CRM_STAFF}
    >
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      {!canEdit && <p className="mb-3 text-xs text-slate-500">View-only. ADMIN/CEO can edit.</p>}
      <div className="space-y-3 rounded-xl border bg-white p-4">
        {Object.keys(LABELS).map((key) => (
          <label key={key} className="block text-sm">
            <span className="font-medium text-slate-700">{LABELS[key]}</span>
            <div className="mt-1 flex gap-2">
              <input
                disabled={!canEdit}
                value={settings[key] ?? ""}
                onChange={(e) => setSettings({ ...settings, [key]: e.target.value })}
                className="w-full rounded-lg border px-3 py-2 disabled:bg-slate-50"
              />
              {canEdit && (
                <button onClick={() => save(key, settings[key] ?? "")} className="rounded-lg bg-slate-900 px-3 py-2 text-xs text-white">
                  {saved === key ? "Saved" : "Save"}
                </button>
              )}
            </div>
          </label>
        ))}
      </div>
    </AdminPage>
  );
}
