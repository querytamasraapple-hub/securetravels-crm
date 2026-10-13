"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  api,
  type Lead,
  type LeadActivity,
  type LeadStatusPayload,
  type SalesUser,
  type Trip,
} from "@/lib/api";
import {
  STAGE_LABEL,
  STAGE_STYLE,
  SOURCE_LABEL,
  initials,
  relativeTime,
  leadScore,
  scoreStyle,
  buyingIntent,
  budgetRangeLabel,
  inr,
} from "@/lib/leadUi";

const STATUSES = ["NEW", "INTERESTED", "QUOTATION_SENT", "BOOKING_CONFIRMED", "LOST"] as const;

type Props = {
  leadId: string;
  trips: Trip[];
  salesUsers: SalesUser[];
  canWrite: boolean;
  onClose: () => void;
  onChanged: () => void;
  onStatusChange: (id: string, payload: LeadStatusPayload) => Promise<void>;
};

export function LeadDetailDrawer({
  leadId,
  trips,
  salesUsers,
  canWrite,
  onClose,
  onChanged,
  onStatusChange,
}: Props) {
  const [tab, setTab] = useState<"details" | "activity" | "automation" | "notes" | "files">("details");
  const [lead, setLead] = useState<Lead | null>(null);
  const [activity, setActivity] = useState<LeadActivity[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState("NEW");
  const [followUp, setFollowUp] = useState("");
  const [remarks, setRemarks] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setError(null);
    api
      .getLead(leadId)
      .then((l) => {
        setLead(l);
        setStatus(l.status ?? "NEW");
        setFollowUp(l.followUpDate ?? "");
        setRemarks(l.remarks ?? "");
      })
      .catch((e: Error) => setError(e.message));
    api.getLeadActivity(leadId).then(setActivity).catch(() => setActivity([]));
  }, [leadId]);

  const score = useMemo(() => (lead ? leadScore(lead) : 0), [lead]);
  const intent = buyingIntent(score);
  const trip = lead?.tripId ? trips.find((t) => t.id === lead.tripId) : undefined;
  const relatedTrips = useMemo(() => {
    const primary = trip ? [trip] : [];
    const extras = trips.filter((t) => t.id !== trip?.id).slice(0, 2);
    return [...primary, ...extras].slice(0, 3);
  }, [trip, trips]);
  const phone = lead?.whatsappNumber || lead?.mobileNumber;
  const wa = phone ? `https://wa.me/${String(phone).replace(/\D/g, "")}` : undefined;

  async function save(e?: FormEvent) {
    e?.preventDefault();
    if (!lead?.id || !canWrite) return;
    setSaving(true);
    setError(null);
    try {
      if (status !== lead.status) {
        await onStatusChange(lead.id, { status: status as LeadStatusPayload["status"] });
      }
      await api.updateLead(lead.id, {
        followUpDate: followUp || undefined,
        remarks: remarks || undefined,
      });
      const fresh = await api.getLead(lead.id);
      setLead(fresh);
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save");
    } finally {
      setSaving(false);
    }
  }

  return (
    <aside className="flex w-full max-w-md shrink-0 flex-col border-l border-slate-200 bg-white shadow-xl">
      <div className="flex items-start justify-between gap-3 border-b border-slate-100 px-5 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-sky-100 text-sm font-bold text-sky-700">
            {initials(lead?.customerName)}
          </div>
          <div>
            <h2 className="text-base font-semibold text-slate-900">{lead?.customerName ?? "Loading…"}</h2>
            <div className="mt-1 flex flex-wrap items-center gap-2">
              <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${STAGE_STYLE[status] ?? ""}`}>
                {STAGE_LABEL[status] ?? status}
              </span>
              <span className="text-xs text-slate-400">
                {SOURCE_LABEL[lead?.source ?? ""] ?? lead?.source} · {relativeTime(lead?.createdAt)}
              </span>
            </div>
          </div>
        </div>
        <button onClick={onClose} className="rounded-md p-1 text-slate-400 hover:bg-slate-100">
          ✕
        </button>
      </div>

      <div className="flex flex-wrap gap-1 border-b border-slate-100 px-2 pt-2">
        {(
          [
            ["details", "Lead Details"],
            ["activity", "Activities"],
            ["automation", "Automation"],
            ["notes", "Notes"],
            ["files", "Files"],
          ] as const
        ).map(([k, label]) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={`rounded-t-md px-2.5 py-2 text-[11px] font-semibold ${
              tab === k ? "bg-sky-50 text-sky-700" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-4">
        {error && <p className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

        {tab === "details" && lead && (
          <form onSubmit={save} className="space-y-4">
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500">Phone</dt>
                <dd className="font-medium text-slate-800">{lead.mobileNumber}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500">Email</dt>
                <dd className="font-medium text-slate-800">{lead.email || "—"}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500">Location</dt>
                <dd className="font-medium text-slate-800">{lead.destination || "—"}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-500">Summary</dt>
                <dd className="text-right font-medium text-slate-800">
                  {lead.numPersons} Adults
                  {lead.heat ? ` · ${lead.heat}` : ""}
                </dd>
              </div>
            </dl>

            <div className="grid grid-cols-2 gap-2">
              <Link
                href={`/bookings?leadId=${lead.id}`}
                className="rounded-xl bg-sky-600 px-3 py-2.5 text-center text-xs font-semibold text-white hover:bg-sky-700"
              >
                Convert to Booking
              </Link>
              <a
                href={lead.email ? `mailto:${lead.email}?subject=Trek itinerary` : "#"}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-center text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Send Itinerary
              </a>
            </div>

            <section>
              <div className="mb-2 flex items-center justify-between">
                <h3 className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
                  Interested treks
                </h3>
                <Link href="/trips" className="text-[11px] font-medium text-sky-700 hover:underline">
                  + Add more
                </Link>
              </div>
              <div className="grid grid-cols-3 gap-2">
                {relatedTrips.length === 0 && (
                  <div className="col-span-3 rounded-lg border border-dashed border-slate-200 px-3 py-4 text-center text-xs text-slate-400">
                    {lead.destination || "No trek linked"}
                  </div>
                )}
                {relatedTrips.map((t) => (
                  <div
                    key={t.id as string}
                    className="overflow-hidden rounded-lg border border-slate-100 bg-gradient-to-br from-sky-50 to-emerald-50 p-2"
                  >
                    <div className="mb-2 h-10 rounded bg-sky-200/60" />
                    <div className="truncate text-[11px] font-semibold text-slate-800">{t.name}</div>
                  </div>
                ))}
              </div>
            </section>

            <section className="space-y-3 rounded-xl border border-slate-100 bg-slate-50 p-3 text-sm">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Lead score</span>
                <span className={`rounded-full px-2 py-0.5 text-xs font-bold ${scoreStyle(score)}`}>{score}</span>
              </div>
              <div>
                <div className="mb-1 flex justify-between text-xs">
                  <span className="text-slate-500">Buying intent</span>
                  <span className="font-semibold text-slate-800">{intent.label}</span>
                </div>
                <div className="h-2 rounded-full bg-slate-200">
                  <div className={`h-2 rounded-full ${intent.bar}`} style={{ width: `${intent.pct}%` }} />
                </div>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Budget range</span>
                <span className="font-medium text-slate-800">{budgetRangeLabel(lead.budget != null ? Number(lead.budget) : null)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Travel date</span>
                <span className="font-medium text-slate-800">{lead.travelDate || "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Group size</span>
                <span className="font-medium text-slate-800">{lead.numPersons} Adults</span>
              </div>
              {lead.budget != null && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Budget</span>
                  <span className="font-medium text-slate-800">{inr(Number(lead.budget))}</span>
                </div>
              )}
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Lead owner</span>
                <span className="inline-flex items-center gap-1.5 font-medium text-slate-800">
                  <span className="flex h-5 w-5 items-center justify-center rounded-full bg-slate-200 text-[9px] font-bold">
                    {initials(lead.ownerName)}
                  </span>
                  {lead.ownerName || "Unassigned"}
                </span>
              </div>
            </section>

            <section className="space-y-3">
              <label className="block text-sm">
                <span className="text-xs font-semibold text-slate-500">Current stage</span>
                <select
                  disabled={!canWrite}
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
                >
                  {STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {STAGE_LABEL[s]}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-sm">
                <span className="text-xs font-semibold text-slate-500">Assigned to</span>
                <select
                  disabled
                  value={lead.ownerId ?? ""}
                  className="mt-1 w-full rounded-lg border border-slate-200 bg-slate-100 px-3 py-2 text-sm"
                >
                  <option value="">{lead.ownerName || "Unassigned"}</option>
                  {salesUsers.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.fullName}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-sm">
                <span className="text-xs font-semibold text-slate-500">Next follow up</span>
                <input
                  type="date"
                  disabled={!canWrite}
                  value={followUp}
                  onChange={(e) => setFollowUp(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
                />
              </label>
              {canWrite && (
                <button
                  type="submit"
                  disabled={saving}
                  className="w-full rounded-lg bg-sky-600 px-3 py-2 text-sm font-semibold text-white hover:bg-sky-700 disabled:opacity-50"
                >
                  {saving ? "Saving…" : "Save changes"}
                </button>
              )}
            </section>

            <div className="flex gap-2">
              <a href={`tel:${lead.mobileNumber}`} className="flex-1 rounded-lg bg-slate-900 py-2 text-center text-xs font-semibold text-white">
                Call
              </a>
              {wa && (
                <a href={wa} target="_blank" rel="noreferrer" className="flex-1 rounded-lg bg-emerald-600 py-2 text-center text-xs font-semibold text-white">
                  WhatsApp
                </a>
              )}
            </div>

            <Link href={`/leads/${lead.id}`} className="block text-center text-xs font-medium text-sky-700 hover:underline">
              Open full lead page →
            </Link>
          </form>
        )}

        {tab === "activity" && (
          <ul className="space-y-3">
            {activity.length === 0 && <li className="text-sm text-slate-400">No activity yet</li>}
            {activity.map((a) => (
              <li key={String(a.id)} className="rounded-lg border border-slate-100 px-3 py-2 text-sm">
                <div className="font-medium text-slate-800">{a.action}</div>
                <div className="text-xs text-slate-500">
                  {a.field ? `${a.field}: ${a.oldValue ?? "—"} → ${a.newValue ?? "—"}` : null}
                </div>
                <div className="mt-1 text-[11px] text-slate-400">
                  {a.actorName} · {relativeTime(a.createdAt)}
                </div>
              </li>
            ))}
          </ul>
        )}

        {tab === "automation" && (
          <div className="space-y-3">
            <h3 className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
              Upcoming automations
            </h3>
            {[
              "Follow up with new Instagram leads (WhatsApp)",
              "Send trek brochure if no reply in 24h",
              "Create call task on next follow-up date",
              "Notify owner when heat becomes HOT",
            ].map((item) => (
              <div key={item} className="rounded-xl border border-slate-100 bg-slate-50 px-3 py-3 text-sm text-slate-700">
                <div className="flex items-center justify-between gap-2">
                  <span>{item}</span>
                  <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-700">
                    Active
                  </span>
                </div>
              </div>
            ))}
            <Link href="/automations" className="block text-center text-xs font-medium text-sky-700 hover:underline">
              Configure automations →
            </Link>
          </div>
        )}

        {tab === "notes" && (
          <div>
            <textarea
              disabled={!canWrite}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              rows={8}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm"
              placeholder="Notes / remarks"
            />
            {canWrite && (
              <button onClick={() => void save()} className="mt-3 rounded-lg bg-slate-900 px-3 py-2 text-sm font-semibold text-white">
                Save notes
              </button>
            )}
          </div>
        )}

        {tab === "files" && (
          <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
            Compliance documents live under Documents. Open the customer profile to request uploads.
          </div>
        )}
      </div>
    </aside>
  );
}
