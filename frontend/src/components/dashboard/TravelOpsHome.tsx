"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  api,
  type Booking,
  type Trip,
  type TripPerformanceData,
  type UpcomingBatch,
} from "@/lib/api";

const CAT_STYLE: Record<string, string> = {
  TREK: "bg-emerald-50 text-emerald-700",
  PILGRIMAGE: "bg-amber-50 text-amber-700",
  LEISURE: "bg-sky-50 text-sky-700",
  CUSTOM: "bg-violet-50 text-violet-700",
};

function inr(v: number | undefined) {
  return "₹" + (v ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

function fmtDate(d?: string) {
  if (!d) return "—";
  return new Date(d + (d.length === 10 ? "T00:00:00" : "")).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
  });
}

/** Travel-ops widgets for the home dashboard (mockup-inspired, Trip On Himalaya data). */
export function TravelOpsHome() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [upcoming, setUpcoming] = useState<UpcomingBatch[]>([]);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [perf, setPerf] = useState<TripPerformanceData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    Promise.all([
      api.getBookings().catch(() => [] as Booking[]),
      api.getUpcomingBatches({ limit: 8 }).catch(() => [] as UpcomingBatch[]),
      api.getTrips(true).catch(() => [] as Trip[]),
      api.getTripPerformance({}).catch(() => null),
    ]).then(([b, u, t, p]) => {
      if (!alive) return;
      setBookings(b);
      setUpcoming(u);
      setTrips(t);
      setPerf(p);
    }).catch((e) => {
      if (alive) setError(e instanceof Error ? e.message : "Could not load travel ops");
    });
    return () => {
      alive = false;
    };
  }, []);

  const statusCounts = useMemo(() => {
    const c = { done: 0, booked: 0, canceled: 0, other: 0 };
    for (const b of bookings) {
      if (b.status === "COMPLETED") c.done++;
      else if (b.status === "CONFIRMED" || b.status === "QUOTATION") c.booked++;
      else if (b.status === "CANCELLED") c.canceled++;
      else c.other++;
    }
    return { ...c, total: bookings.length };
  }, [bookings]);

  const topTrips = useMemo(() => {
    const rows = [...(perf?.trips ?? [])];
    rows.sort((a, b) => (b.revenue ?? 0) - (a.revenue ?? 0));
    return rows.slice(0, 5);
  }, [perf]);

  const maxRev = Math.max(1, ...topTrips.map((t) => t.revenue ?? 0));

  return (
    <div className="space-y-6">
      {error && (
        <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
      )}

      {/* Booking status bar */}
      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-900">Trip bookings</h2>
          <Link href="/bookings" className="text-xs font-medium text-slate-500 hover:text-slate-800">
            View all →
          </Link>
        </div>
        <div className="mb-2 text-2xl font-semibold tabular-nums text-slate-900">
          {statusCounts.total.toLocaleString("en-IN")}
          <span className="ml-2 text-sm font-normal text-slate-400">total</span>
        </div>
        <div className="flex h-3 overflow-hidden rounded-full bg-slate-100">
          {statusCounts.total > 0 && (
            <>
              <div
                className="bg-emerald-500"
                style={{ width: `${(statusCounts.done / statusCounts.total) * 100}%` }}
                title="Completed"
              />
              <div
                className="bg-sky-500"
                style={{ width: `${(statusCounts.booked / statusCounts.total) * 100}%` }}
                title="Booked"
              />
              <div
                className="bg-rose-400"
                style={{ width: `${(statusCounts.canceled / statusCounts.total) * 100}%` }}
                title="Canceled"
              />
            </>
          )}
        </div>
        <div className="mt-3 flex flex-wrap gap-4 text-xs text-slate-600">
          <Legend color="bg-emerald-500" label="Done" value={statusCounts.done} />
          <Legend color="bg-sky-500" label="Booked" value={statusCounts.booked} />
          <Legend color="bg-rose-400" label="Canceled" value={statusCounts.canceled} />
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Top packages / destinations */}
        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900">Top packages</h2>
            <Link href="/reports" className="text-xs font-medium text-slate-500 hover:text-slate-800">
              Reports →
            </Link>
          </div>
          {topTrips.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">No trip revenue yet this period.</p>
          ) : (
            <ul className="space-y-3">
              {topTrips.map((t) => (
                <li key={t.tripId ?? t.name}>
                  <div className="mb-1 flex items-center justify-between text-sm">
                    <span className="truncate font-medium text-slate-800">{t.name}</span>
                    <span className="shrink-0 tabular-nums text-slate-600">{inr(t.revenue)}</span>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                    <div
                      className="h-full rounded-full bg-slate-800"
                      style={{ width: `${((t.revenue ?? 0) / maxRev) * 100}%` }}
                    />
                  </div>
                  <div className="mt-0.5 text-[11px] text-slate-400">
                    {t.bookings ?? 0} bookings · {t.fillRatePct ?? 0}% fill
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Upcoming departures */}
        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900">Upcoming departures</h2>
            <Link href="/calendar" className="text-xs font-medium text-slate-500 hover:text-slate-800">
              Calendar →
            </Link>
          </div>
          {upcoming.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">
              No upcoming batches. Add departures under Packages.
            </p>
          ) : (
            <ul className="space-y-2">
              {upcoming.map((b) => (
                <li
                  key={b.id}
                  className="flex items-start justify-between gap-3 rounded-lg border border-slate-100 px-3 py-2.5"
                >
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium text-slate-900">{b.tripName}</div>
                    <div className="mt-0.5 text-xs text-slate-400">
                      {fmtDate(b.departureDate)}
                      {b.guideName ? ` · ${b.guideName}` : ""}
                      {" · "}
                      {b.seatsBooked}/{b.maxCapacity} seats
                    </div>
                  </div>
                  <span
                    className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                      CAT_STYLE[b.tripCategory ?? ""] || "bg-slate-100 text-slate-600"
                    }`}
                  >
                    {b.tripCategory || b.status}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {/* Package cards */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-900">Travel packages</h2>
          <Link href="/trips" className="text-xs font-medium text-slate-500 hover:text-slate-800">
            Manage packages →
          </Link>
        </div>
        {trips.length === 0 ? (
          <p className="rounded-xl border border-dashed border-slate-200 bg-white py-10 text-center text-sm text-slate-400">
            No active packages yet.
          </p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {trips.slice(0, 8).map((t) => (
              <Link
                key={t.id}
                href="/trips"
                className="group overflow-hidden rounded-xl border border-slate-200 bg-white transition hover:border-slate-300 hover:shadow-sm"
              >
                <div
                  className={`h-24 ${packageTone(t.category)} flex items-end p-3`}
                >
                  <span
                    className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                      CAT_STYLE[t.category ?? ""] || "bg-white/80 text-slate-700"
                    }`}
                  >
                    {t.category || "TRIP"}
                  </span>
                </div>
                <div className="p-3">
                  <div className="truncate text-sm font-semibold text-slate-900 group-hover:underline">
                    {t.name}
                  </div>
                  <div className="mt-1 text-xs text-slate-500">
                    {t.durationDays != null ? `${t.durationDays} days` : "—"}
                    {t.baseCost != null ? ` · ${inr(Number(t.baseCost))}` : ""}
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function Legend({ color, label, value }: { color: string; label: string; value: number }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className={`h-2 w-2 rounded-full ${color}`} />
      {label}{" "}
      <span className="font-semibold tabular-nums text-slate-800">{value}</span>
    </span>
  );
}

function packageTone(category?: string) {
  switch (category) {
    case "TREK":
      return "bg-gradient-to-br from-emerald-700 to-slate-800";
    case "PILGRIMAGE":
      return "bg-gradient-to-br from-amber-600 to-orange-900";
    case "LEISURE":
      return "bg-gradient-to-br from-sky-600 to-indigo-900";
    default:
      return "bg-gradient-to-br from-slate-600 to-slate-900";
  }
}
