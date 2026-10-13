"use client";

import { useState } from "react";
import { api, type LeadCreatePayload } from "@/lib/api";

const SOURCES = new Set([
  "GOOGLE_ADS",
  "FACEBOOK_ADS",
  "INSTAGRAM",
  "WEBSITE",
  "WHATSAPP",
  "REFERRAL",
  "JUSTDIAL",
  "WALK_IN",
  "B2B",
  "EXISTING_CUSTOMER",
  "OTHER",
]);

type Props = { onClose: () => void; onDone: () => void };

function parseCsv(text: string): LeadCreatePayload[] {
  const lines = text
    .replace(/^\uFEFF/, "")
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean);
  if (lines.length < 2) return [];
  const headers = lines[0].split(",").map((h) => h.trim().toLowerCase());
  const idx = (names: string[]) => headers.findIndex((h) => names.includes(h));
  const iName = idx(["name", "customername", "customer_name", "full_name"]);
  const iMobile = idx(["mobile", "phone", "mobilenumber", "mobile_number"]);
  const iSource = idx(["source"]);
  const iEmail = idx(["email"]);
  const iDest = idx(["destination", "location", "city"]);
  if (iName < 0 || iMobile < 0) throw new Error("CSV needs name and mobile columns");

  const rows: LeadCreatePayload[] = [];
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(",").map((c) => c.trim().replace(/^"|"$/g, ""));
    const name = cols[iName];
    const mobile = cols[iMobile];
    if (!name || !mobile) continue;
    const rawSource = (iSource >= 0 ? cols[iSource] : "WEBSITE")?.toUpperCase().replace(/\s+/g, "_") || "WEBSITE";
    const source = (SOURCES.has(rawSource) ? rawSource : "OTHER") as LeadCreatePayload["source"];
    rows.push({
      customerName: name,
      mobileNumber: mobile,
      source,
      email: iEmail >= 0 ? cols[iEmail] || undefined : undefined,
      destination: iDest >= 0 ? cols[iDest] || undefined : undefined,
      consentGiven: true,
      consentScope: "import_csv",
      numPersons: 1,
    });
  }
  return rows;
}

export function LeadImportModal({ onClose, onDone }: Props) {
  const [error, setError] = useState<string | null>(null);
  const [progress, setProgress] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onFile(file: File) {
    setError(null);
    setBusy(true);
    try {
      const text = await file.text();
      const rows = parseCsv(text);
      if (rows.length === 0) throw new Error("No valid rows found");
      let ok = 0;
      let fail = 0;
      for (const row of rows.slice(0, 200)) {
        try {
          await api.createLead(row);
          ok++;
        } catch {
          fail++;
        }
        setProgress(`Imported ${ok} / ${rows.length}${fail ? ` (${fail} failed)` : ""}`);
      }
      onDone();
      if (fail === 0) onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Import failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
        <div className="mb-3 flex items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Import leads</h2>
            <p className="text-sm text-slate-500">
              CSV with columns: name, mobile, source, email, destination (max 200 rows)
            </p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700">
            ✕
          </button>
        </div>
        {error && <p className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        {progress && <p className="mb-3 text-sm text-slate-600">{progress}</p>}
        <label className="flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 px-6 py-10 text-sm text-slate-600 hover:border-sky-300 hover:bg-sky-50">
          <span className="mb-1 font-medium text-slate-800">{busy ? "Importing…" : "Choose CSV file"}</span>
          <span className="text-xs text-slate-400">consent marked for import rows</span>
          <input
            type="file"
            accept=".csv,text/csv"
            disabled={busy}
            className="hidden"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) void onFile(f);
            }}
          />
        </label>
      </div>
    </div>
  );
}
