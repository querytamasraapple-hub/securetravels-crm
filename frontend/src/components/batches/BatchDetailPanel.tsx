"use client";

import { useState } from "react";
import Link from "next/link";
import type { Batch, Booking, UpcomingBatch } from "@/lib/api";
import { inr, inferRegion } from "@/components/treks/trekUi";
import {
  batchCode,
  baseCampFor,
  formatShort,
  liveTrackingSteps,
  permitStatus,
  seasonType,
  statusClass,
  transportLabel,
  tripCover,
  uiStatus,
  type UiBatchStatus,
} from "@/components/batches/batchUi";

export function BatchDetailPanel({
  batch,
  detail,
  durationDays,
  tripCategory,
  bookings,
  canManage,
  onClose,
  onEdit,
}: {
  batch: UpcomingBatch;
  detail?: Batch | null;
  durationDays: number;
  tripCategory?: string;
  bookings: Booking[];
  canManage: boolean;
  onClose: () => void;
  onEdit: () => void;
}) {
  const [tab, setTab] = useState<
    "overview" | "travellers" | "itinerary" | "staff" | "transport" | "permits" | "more"
  >("overview");
  const ui = uiStatus(batch, durationDays);
  const permit = permitStatus(batch, ui);
  const code = batchCode(batch);
  const nights = Math.max(0, durationDays - 1);
  const returnDate = (() => {
    const d = new Date(`${batch.departureDate}T00:00:00`);
    d.setDate(d.getDate() + nights);
    return d.toISOString().slice(0, 10);
  })();
  const transport = transportLabel(detail?.transportPlan, batch.tripName);
  const amount = bookings.reduce((s, b) => s + Number(b.netAmount ?? b.totalAmount ?? 0), 0);
  const fill = Math.min(100, Number(batch.fillPercent ?? 0));

  const tabs = [
    ["overview", "Overview"],
    ["travellers", `Travellers (${batch.seatsBooked})`],
    ["itinerary", "Itinerary"],
    ["staff", "Staff"],
    ["transport", "Transport"],
    ["permits", "Permits"],
    ["more", "More"],
  ] as const;

  return (
    <aside className="flex w-full max-w-md shrink-0 flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-lg xl:w-[380px]">
      <div className="relative h-36" style={{ background: tripCover(null, batch.tripName) }}>
        <div className="absolute inset-0 bg-gradient-to-t from-black/55 to-transparent" />
        <button
          type="button"
          onClick={onClose}
          className="absolute right-2 top-2 rounded-md bg-white/90 px-2 py-1 text-xs text-slate-600"
        >
          ✕
        </button>
        <div className="absolute bottom-3 left-3 right-3 flex items-end justify-between gap-2">
          <span className={`rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${statusClass(ui)}`}>
            {ui}
          </span>
          {canManage && (
            <button
              type="button"
              onClick={onEdit}
              className="rounded-md bg-white/95 px-2.5 py-1 text-[11px] font-semibold text-slate-800"
            >
              Edit
            </button>
          )}
        </div>
      </div>

      <div className="border-b border-slate-100 px-4 py-3">
        <div className="text-sm font-semibold text-slate-900">
          {batch.tripName} — Batch {code}
        </div>
        <div className="mt-0.5 text-[11px] text-slate-500">
          {seasonType(tripCategory, batch.tripName)} · {inferRegion(batch.tripName)}
        </div>
      </div>

      <div className="flex gap-0.5 overflow-x-auto border-b border-slate-100 px-2 pt-2">
        {tabs.map(([id, label]) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`shrink-0 rounded-t-md px-2.5 py-2 text-[11px] font-semibold ${
              tab === id ? "bg-sky-50 text-sky-700" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-4 text-sm">
        {tab === "overview" && (
          <div className="grid grid-cols-2 gap-3">
            <Info label="Departure" value={formatShort(batch.departureDate)} />
            <Info label="Return" value={formatShort(returnDate)} />
            <Info label="Duration" value={`${durationDays} Days / ${nights} Nights`} />
            <Info label="Status" value={ui} />
            <div className="col-span-2 rounded-xl border border-slate-100 bg-slate-50 px-3 py-2.5">
              <div className="flex justify-between text-[11px] text-slate-500">
                <span>Travellers</span>
                <span className="font-semibold text-slate-800">
                  {batch.seatsBooked}/{batch.maxCapacity}
                </span>
              </div>
              <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-slate-200">
                <div
                  className={`h-full rounded-full ${fill >= 90 ? "bg-rose-500" : fill >= 70 ? "bg-amber-400" : "bg-emerald-500"}`}
                  style={{ width: `${fill}%` }}
                />
              </div>
            </div>
            <Info label="Guide / Leader" value={batch.guideName || "Unassigned"} />
            <Info label="Transport" value={transport} />
            <Info label="Base Camp" value={baseCampFor(batch.tripName)} />
            <Info
              label="Permits"
              value={
                <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${permit.className}`}>
                  {permit.label}
                </span>
              }
            />
            <div className="col-span-2 rounded-xl border border-sky-100 bg-sky-50/60 px-3 py-2.5">
              <div className="text-[11px] text-slate-500">Total Amount</div>
              <div className="text-lg font-semibold tabular-nums text-slate-900">
                {inr(amount || batch.seatsBooked * 6500)}
              </div>
            </div>
          </div>
        )}

        {tab === "travellers" && (
          <div className="space-y-2">
            {bookings.length === 0 && (
              <p className="text-xs text-slate-400">No linked bookings for this batch yet.</p>
            )}
            {bookings.slice(0, 12).map((b) => (
              <div
                key={b.id}
                className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2"
              >
                <div>
                  <div className="font-medium text-slate-800">{b.customerName || "Guest"}</div>
                  <div className="text-[11px] text-slate-400">
                    {b.bookingRef} · {b.numTravellers ?? 1} pax · {b.status}
                  </div>
                </div>
                <div className="text-xs font-semibold tabular-nums">{inr(b.netAmount ?? b.totalAmount)}</div>
              </div>
            ))}
            <Link href="/bookings" className="inline-block text-xs font-semibold text-sky-700 hover:underline">
              Manage Travellers →
            </Link>
          </div>
        )}

        {tab === "itinerary" && (
          <div className="space-y-2 text-xs text-slate-600">
            <p>Day-wise plan for {batch.tripName}.</p>
            {Array.from({ length: Math.min(durationDays, 6) }).map((_, i) => (
              <div key={i} className="rounded-lg border border-slate-100 px-3 py-2">
                <span className="font-semibold text-slate-800">Day {i + 1}</span> — Trek / camp activities
              </div>
            ))}
            <Link
              href="/trips/itineraries"
              className="inline-block text-xs font-semibold text-sky-700 hover:underline"
            >
              View Itinerary →
            </Link>
          </div>
        )}

        {tab === "staff" && (
          <div className="space-y-2">
            <div className="rounded-lg border border-slate-100 px-3 py-2">
              <div className="text-[11px] text-slate-400">Lead Guide</div>
              <div className="font-medium">{batch.guideName || "Unassigned"}</div>
            </div>
            <p className="text-xs text-slate-500">+2 support staff (estimate for full groups)</p>
            <Link href="/guides" className="inline-block text-xs font-semibold text-sky-700 hover:underline">
              Assign Guide / Staff →
            </Link>
          </div>
        )}

        {tab === "transport" && (
          <div className="space-y-2 text-xs">
            <div className="rounded-lg border border-slate-100 px-3 py-2">
              <div className="text-slate-400">Vehicle</div>
              <div className="font-medium text-slate-800">{transport}</div>
              {detail?.transportPlan && (
                <div className="mt-1 text-slate-500">{detail.transportPlan}</div>
              )}
            </div>
            <Link
              href="/operations/transport"
              className="inline-block font-semibold text-sky-700 hover:underline"
            >
              Transport desk →
            </Link>
          </div>
        )}

        {tab === "permits" && (
          <div className="space-y-2 text-xs">
            <span className={`inline-flex rounded-full px-2.5 py-1 font-semibold ${permit.className}`}>
              {permit.label}
            </span>
            <p className="text-slate-500">Forest / local permits for {inferRegion(batch.tripName)}.</p>
            <Link
              href="/operations/permits"
              className="inline-block font-semibold text-sky-700 hover:underline"
            >
              Manage Permits →
            </Link>
          </div>
        )}

        {tab === "more" && (
          <div className="space-y-1">
            {[
              ["/trips/map", "View on Map"],
              ["/messages", "Send Notifications"],
              ["/operations/on-trip", "On-trip Operations"],
              ["/trips", "Open Trek Catalogue"],
            ].map(([href, label]) => (
              <Link
                key={label}
                href={href}
                className="block rounded-md px-2 py-1.5 text-xs font-medium text-slate-600 hover:bg-sky-50 hover:text-sky-800"
              >
                {label}
              </Link>
            ))}
          </div>
        )}

        {(ui === "On Trek" || ui === "Starting") && tab === "overview" && (
          <div className="mt-4 border-t border-slate-100 pt-3">
            <div className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-slate-400">
              Live tracking
            </div>
            <LiveSteps ui={ui} tripName={batch.tripName} />
          </div>
        )}
      </div>

      <div className="flex flex-wrap gap-2 border-t border-slate-100 p-3">
        <Link
          href="/trips/itineraries"
          className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-[11px] font-semibold text-slate-700 hover:bg-slate-50"
        >
          View Itinerary
        </Link>
        <Link
          href="/bookings"
          className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-[11px] font-semibold text-slate-700 hover:bg-slate-50"
        >
          Manage Travellers
        </Link>
        <Link
          href="/trips/map"
          className="rounded-lg bg-sky-600 px-2.5 py-1.5 text-[11px] font-semibold text-white hover:bg-sky-500"
        >
          View on Map
        </Link>
      </div>
    </aside>
  );
}

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50/80 px-3 py-2">
      <div className="text-[10px] uppercase tracking-wide text-slate-400">{label}</div>
      <div className="mt-0.5 text-xs font-semibold text-slate-800">{value}</div>
    </div>
  );
}

function LiveSteps({ ui, tripName }: { ui: UiBatchStatus; tripName?: string }) {
  const steps = liveTrackingSteps(ui, tripName);
  return (
    <ol className="space-y-2">
      {steps.map((s) => (
        <li key={s.time + s.text} className="flex gap-2 text-xs">
          <span
            className={`mt-1 h-2 w-2 shrink-0 rounded-full ${s.done ? "bg-emerald-500" : "bg-slate-300"}`}
          />
          <div>
            <div className="font-semibold text-slate-700">{s.time}</div>
            <div className="text-slate-500">{s.text}</div>
          </div>
        </li>
      ))}
    </ol>
  );
}
