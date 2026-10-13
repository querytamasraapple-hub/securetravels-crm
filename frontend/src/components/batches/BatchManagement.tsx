"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  api,
  type Batch,
  type Booking,
  type Guide,
  type Trip,
  type UpcomingBatch,
  ApiClientError,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { canManageCatalogue } from "@/lib/rbac";
import { TreksShell } from "@/components/treks/TreksShell";
import { BatchDetailPanel } from "@/components/batches/BatchDetailPanel";
import {
  batchCode,
  daysUntil,
  formatShort,
  initials,
  permitStatus,
  seasonType,
  statusClass,
  tabForStatus,
  transportLabel,
  tripCover,
  uiStatus,
  type BatchTab,
  type UiBatchStatus,
} from "@/components/batches/batchUi";

function monthStart(offset = 0) {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth() + offset, 1).toISOString().slice(0, 10);
}

function monthEnd(offset = 2) {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth() + offset + 1, 0).toISOString().slice(0, 10);
}

export function BatchManagement() {
  const { user } = useAuth();
  const canManage = canManageCatalogue(user?.role);

  const [from, setFrom] = useState(monthStart(0));
  const [to, setTo] = useState(monthEnd(2));
  const [rows, setRows] = useState<UpcomingBatch[]>([]);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [guides, setGuides] = useState<Guide[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [selected, setSelected] = useState<UpcomingBatch | null>(null);
  const [selectedDetail, setSelectedDetail] = useState<Batch | null>(null);
  const [selectedBookings, setSelectedBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [tab, setTab] = useState<BatchTab>("all");
  const [selectedRows, setSelectedRows] = useState<Set<string>>(new Set());
  const [showCreate, setShowCreate] = useState(false);
  const [editBatch, setEditBatch] = useState<UpcomingBatch | null>(null);
  const [editDetail, setEditDetail] = useState<Batch | null>(null);
  const [calMonth, setCalMonth] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1);
  });

  const tripById = useMemo(() => {
    const m = new Map<string, Trip>();
    for (const t of trips) if (t.id) m.set(t.id, t);
    return m;
  }, [trips]);

  const durationFor = useCallback(
    (tripId: string) => Number(tripById.get(tripId)?.durationDays ?? 5),
    [tripById],
  );

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    Promise.all([
      api.getUpcomingBatches({ from, to, limit: 200 }),
      api.getTrips(false).catch(() => api.getTrips(true)),
      api.getGuides(true).catch(() => [] as Guide[]),
      api.getBookings().catch(() => [] as Booking[]),
    ])
      .then(([batchList, tripList, guideList, bookingList]) => {
        setRows(batchList);
        setTrips(tripList);
        setGuides(guideList);
        setBookings(bookingList);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [from, to]);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    if (!selected) {
      setSelectedDetail(null);
      setSelectedBookings([]);
      return;
    }
    api
      .listBatches(selected.tripId)
      .then((list) => setSelectedDetail(list.find((b) => b.id === selected.id) ?? null))
      .catch(() => setSelectedDetail(null));
    setSelectedBookings(bookings.filter((b) => b.batchId === selected.id));
  }, [selected, bookings]);

  useEffect(() => {
    if (!editBatch) {
      setEditDetail(null);
      return;
    }
    if (selectedDetail?.id === editBatch.id) {
      setEditDetail(selectedDetail);
      return;
    }
    api
      .listBatches(editBatch.tripId)
      .then((list) => setEditDetail(list.find((b) => b.id === editBatch.id) ?? null))
      .catch(() => setEditDetail(null));
  }, [editBatch, selectedDetail]);

  const enriched = useMemo(() => {
    return rows.map((b) => {
      const duration = durationFor(b.tripId);
      const ui = uiStatus(b, duration);
      return { b, duration, ui, tab: tabForStatus(ui), code: batchCode(b) };
    });
  }, [rows, durationFor]);

  const counts = useMemo(() => {
    const c = { all: enriched.length, upcoming: 0, ongoing: 0, completed: 0, cancelled: 0 };
    for (const e of enriched) c[e.tab] += 1;
    return c;
  }, [enriched]);

  const filtered = useMemo(() => {
    let list = enriched;
    if (tab !== "all") list = list.filter((e) => e.tab === tab);
    const qq = q.trim().toLowerCase();
    if (qq) {
      list = list.filter(
        (e) =>
          e.code.toLowerCase().includes(qq) ||
          e.b.tripName.toLowerCase().includes(qq) ||
          (e.b.guideName || "").toLowerCase().includes(qq) ||
          e.b.departureDate.includes(qq),
      );
    }
    return list.sort((a, b) => a.b.departureDate.localeCompare(b.b.departureDate));
  }, [enriched, tab, q]);

  const totalTravellers = rows.reduce((s, b) => s + Number(b.seatsBooked || 0), 0);
  const next15 = enriched
    .filter((e) => {
      const d = daysUntil(e.b.departureDate);
      return d >= 0 && d <= 15 && e.tab === "upcoming";
    })
    .slice(0, 5);

  const statusDonut = useMemo(() => {
    const map = new Map<UiBatchStatus, number>();
    for (const e of enriched) map.set(e.ui, (map.get(e.ui) ?? 0) + 1);
    const total = enriched.length || 1;
    const colors: Record<string, string> = {
      Upcoming: "#3b82f6",
      "On Trek": "#10b981",
      Confirmed: "#22c55e",
      Starting: "#0ea5e9",
      Preparing: "#f59e0b",
      Completed: "#94a3b8",
      Cancelled: "#f43f5e",
    };
    return [...map.entries()].map(([name, count]) => ({
      name,
      count,
      pct: ((count / total) * 100).toFixed(1),
      color: colors[name] ?? "#64748b",
    }));
  }, [enriched]);

  const liveBatch = enriched.find((e) => e.ui === "On Trek") ?? enriched.find((e) => e.ui === "Starting");

  function exportCsv() {
    const lines = [
      "BatchID,Trek,Departure,Status,Travellers,Capacity,Guide",
      ...filtered.map(
        (e) =>
          `${e.code},"${e.b.tripName}",${e.b.departureDate},${e.ui},${e.b.seatsBooked},${e.b.maxCapacity},"${e.b.guideName || ""}"`,
      ),
    ];
    const blob = new Blob([lines.join("\n")], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `batches-${from}-${to}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  const calDays = useMemo(() => buildCalendar(calMonth, enriched), [calMonth, enriched]);

  return (
    <TreksShell
      title="Batch Management"
      breadcrumb="Batch Management"
      subtitle="Create, schedule and manage trek batches, track capacity, assignments, permits, and real-time status."
      actions={
        <div className="flex flex-wrap items-center gap-2">
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 text-sm"
          />
          <span className="text-slate-400">–</span>
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 text-sm"
          />
          {canManage && (
            <button
              type="button"
              onClick={() => setShowCreate(true)}
              className="rounded-lg bg-sky-600 px-3 py-2 text-sm font-semibold text-white hover:bg-sky-500"
            >
              + Create Batch
            </button>
          )}
          <button
            type="button"
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Import Batches
          </button>
          <button
            type="button"
            onClick={exportCsv}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            title="More"
          >
            ⋯
          </button>
        </div>
      }
    >
      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
        {[
          { label: "Total Batches", value: counts.all, delta: "+28%", hint: "Upcoming + Ongoing" },
          { label: "Upcoming Batches", value: counts.upcoming, delta: "+14%", hint: "Next window" },
          { label: "Ongoing Batches", value: counts.ongoing, delta: "", hint: "On Trek / Starting" },
          { label: "Completed Batches", value: counts.completed, delta: "+36%", hint: "This season" },
          {
            label: "Cancelled / Postponed",
            value: counts.cancelled,
            delta: "↓ 25%",
            hint: "This season",
            bad: true,
          },
          { label: "Total Travellers", value: totalTravellers, delta: "", hint: "Across all batches" },
        ].map((k) => (
          <div key={k.label} className="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
            <div className="text-[11px] font-medium uppercase tracking-wide text-slate-400">{k.label}</div>
            <div className="mt-1 flex items-baseline gap-2">
              <span className="text-2xl font-semibold tabular-nums text-slate-900">{k.value}</span>
              {k.delta && (
                <span className={`text-xs font-semibold ${k.bad ? "text-rose-600" : "text-emerald-600"}`}>
                  {k.delta}
                </span>
              )}
            </div>
            <div className="mt-0.5 text-xs text-slate-500">{k.hint}</div>
          </div>
        ))}
      </div>

      <div className={`flex flex-col gap-4 ${selected ? "xl:flex-row" : ""}`}>
        <div className="min-w-0 flex-1 space-y-4">
          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-3 pt-3">
              {(
                [
                  ["all", `All Batches (${counts.all})`],
                  ["upcoming", `Upcoming (${counts.upcoming})`],
                  ["ongoing", `Ongoing (${counts.ongoing})`],
                  ["completed", `Completed (${counts.completed})`],
                  ["cancelled", `Cancelled (${counts.cancelled})`],
                ] as [BatchTab, string][]
              ).map(([id, label]) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setTab(id)}
                  className={`rounded-t-md px-3 py-2 text-xs font-semibold ${
                    tab === id ? "bg-sky-50 text-sky-700" : "text-slate-500 hover:text-slate-800"
                  }`}
                >
                  {label}
                </button>
              ))}
              <div className="ml-auto flex flex-wrap items-center gap-2 pb-2">
                <input
                  value={q}
                  onChange={(e) => setQ(e.target.value)}
                  placeholder="Search batch ID, trek, guide…"
                  className="w-48 rounded-lg border border-slate-200 px-3 py-1.5 text-xs outline-none focus:border-sky-400"
                />
                <button
                  type="button"
                  className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs font-medium text-slate-600"
                >
                  Filters
                </button>
                <button
                  type="button"
                  className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs font-medium text-slate-600"
                >
                  Columns
                </button>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[1100px] text-left text-xs">
                <thead className="bg-slate-50/80 text-[10px] uppercase tracking-wide text-slate-400">
                  <tr>
                    <th className="px-3 py-2.5">
                      <input
                        type="checkbox"
                        checked={
                          filtered.length > 0 && filtered.every((e) => selectedRows.has(e.b.id))
                        }
                        onChange={(e) => {
                          if (e.target.checked) setSelectedRows(new Set(filtered.map((x) => x.b.id)));
                          else setSelectedRows(new Set());
                        }}
                      />
                    </th>
                    <th className="px-2 py-2.5 font-semibold">Batch ID</th>
                    <th className="px-2 py-2.5 font-semibold">Trek / Package</th>
                    <th className="px-2 py-2.5 font-semibold">Departure / Return</th>
                    <th className="px-2 py-2.5 font-semibold">Duration</th>
                    <th className="px-2 py-2.5 font-semibold">Travellers</th>
                    <th className="px-2 py-2.5 font-semibold">Status</th>
                    <th className="px-2 py-2.5 font-semibold">Guide / Leader</th>
                    <th className="px-2 py-2.5 font-semibold">Transport</th>
                    <th className="px-2 py-2.5 font-semibold">Permits</th>
                    <th className="px-2 py-2.5 font-semibold">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {loading && (
                    <tr>
                      <td colSpan={11} className="px-4 py-10 text-center text-slate-400">
                        Loading batches…
                      </td>
                    </tr>
                  )}
                  {!loading &&
                    filtered.map((e) => {
                      const trip = tripById.get(e.b.tripId);
                      const nights = Math.max(0, e.duration - 1);
                      const ret = new Date(`${e.b.departureDate}T00:00:00`);
                      ret.setDate(ret.getDate() + nights);
                      const permit = permitStatus(e.b, e.ui);
                      const fillHot = Number(e.b.fillPercent) >= 85;
                      return (
                        <tr
                          key={e.b.id}
                          onClick={() => setSelected(e.b)}
                          className={`cursor-pointer hover:bg-sky-50/50 ${
                            selected?.id === e.b.id ? "bg-sky-50/70" : ""
                          }`}
                        >
                          <td className="px-3 py-2.5" onClick={(ev) => ev.stopPropagation()}>
                            <input
                              type="checkbox"
                              checked={selectedRows.has(e.b.id)}
                              onChange={(ev) => {
                                setSelectedRows((prev) => {
                                  const next = new Set(prev);
                                  if (ev.target.checked) next.add(e.b.id);
                                  else next.delete(e.b.id);
                                  return next;
                                });
                              }}
                            />
                          </td>
                          <td className="px-2 py-2.5 font-semibold tabular-nums text-slate-800">
                            {e.code}
                          </td>
                          <td className="px-2 py-2.5">
                            <div className="flex items-center gap-2">
                              <div
                                className="h-9 w-12 shrink-0 rounded-md"
                                style={{ background: tripCover(trip, e.b.tripName) }}
                              />
                              <div className="min-w-0">
                                <div className="truncate font-semibold text-slate-800">{e.b.tripName}</div>
                                <div className="text-[10px] text-slate-400">
                                  {seasonType(trip?.category ?? e.b.tripCategory, e.b.tripName)}
                                </div>
                              </div>
                            </div>
                          </td>
                          <td className="px-2 py-2.5 tabular-nums text-slate-600">
                            {formatShort(e.b.departureDate)}
                            <div className="text-[10px] text-slate-400">
                              → {formatShort(ret.toISOString().slice(0, 10))}
                            </div>
                          </td>
                          <td className="px-2 py-2.5 tabular-nums">
                            {e.duration}D/{nights}N
                          </td>
                          <td className="px-2 py-2.5">
                            <span className={`font-semibold tabular-nums ${fillHot ? "text-rose-600" : "text-slate-800"}`}>
                              {e.b.seatsBooked}
                            </span>
                            <span className="text-slate-400">/{e.b.maxCapacity}</span>
                          </td>
                          <td className="px-2 py-2.5">
                            <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusClass(e.ui)}`}>
                              {e.ui}
                            </span>
                          </td>
                          <td className="px-2 py-2.5">
                            <div className="flex items-center gap-1.5">
                              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-200 text-[10px] font-bold text-slate-600">
                                {initials(e.b.guideName)}
                              </span>
                              <span className="truncate text-slate-700">{e.b.guideName || "—"}</span>
                            </div>
                          </td>
                          <td className="px-2 py-2.5 text-slate-600">
                            {transportLabel(selectedDetail?.id === e.b.id ? selectedDetail.transportPlan : null, e.b.tripName)}
                          </td>
                          <td className="px-2 py-2.5">
                            <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${permit.className}`}>
                              {permit.label}
                            </span>
                          </td>
                          <td className="px-2 py-2.5" onClick={(ev) => ev.stopPropagation()}>
                            <div className="flex gap-1">
                              {canManage && (
                                <button
                                  type="button"
                                  onClick={() => setEditBatch(e.b)}
                                  className="rounded px-1.5 py-1 text-slate-500 hover:bg-slate-100 hover:text-sky-700"
                                  title="Edit"
                                >
                                  ✎
                                </button>
                              )}
                              <button
                                type="button"
                                onClick={() => setSelected(e.b)}
                                className="rounded px-1.5 py-1 text-slate-500 hover:bg-slate-100"
                              >
                                ⋯
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  {!loading && filtered.length === 0 && (
                    <tr>
                      <td colSpan={11} className="px-4 py-10 text-center text-slate-400">
                        No batches in this range. Create a batch to get started.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-4">
            <Panel title="Batch Calendar">
              <div className="mb-2 flex items-center justify-between">
                <button
                  type="button"
                  className="text-xs text-slate-500"
                  onClick={() =>
                    setCalMonth(new Date(calMonth.getFullYear(), calMonth.getMonth() - 1, 1))
                  }
                >
                  ‹
                </button>
                <span className="text-xs font-semibold text-slate-700">
                  {calMonth.toLocaleDateString("en-IN", { month: "long", year: "numeric" })}
                </span>
                <button
                  type="button"
                  className="text-xs text-slate-500"
                  onClick={() =>
                    setCalMonth(new Date(calMonth.getFullYear(), calMonth.getMonth() + 1, 1))
                  }
                >
                  ›
                </button>
              </div>
              <div className="grid grid-cols-7 gap-1 text-center text-[10px] text-slate-400">
                {["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"].map((d) => (
                  <div key={d}>{d}</div>
                ))}
                {calDays.map((cell) => (
                  <div
                    key={cell.key}
                    className={`min-h-8 rounded-md p-0.5 ${cell.inMonth ? "bg-slate-50" : "opacity-30"}`}
                  >
                    <div className="text-[10px] text-slate-600">{cell.day}</div>
                    <div className="mt-0.5 flex flex-wrap justify-center gap-0.5">
                      {cell.dots.slice(0, 3).map((c, i) => (
                        <span key={i} className="h-1.5 w-1.5 rounded-full" style={{ background: c }} />
                      ))}
                    </div>
                  </div>
                ))}
              </div>
              <div className="mt-2 flex flex-wrap gap-2 text-[10px] text-slate-500">
                <span className="inline-flex items-center gap-1">
                  <span className="h-1.5 w-1.5 rounded-full bg-blue-500" /> Upcoming
                </span>
                <span className="inline-flex items-center gap-1">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" /> Ongoing
                </span>
                <span className="inline-flex items-center gap-1">
                  <span className="h-1.5 w-1.5 rounded-full bg-slate-400" /> Completed
                </span>
              </div>
            </Panel>

            <Panel title="Batch Status Overview">
              <Donut slices={statusDonut} />
            </Panel>

            <Panel title="Upcoming Batches (Next 15 Days)">
              <div className="space-y-2">
                {next15.length === 0 && <p className="text-xs text-slate-400">No departures in next 15 days</p>}
                {next15.map((e) => (
                  <button
                    key={e.b.id}
                    type="button"
                    onClick={() => setSelected(e.b)}
                    className="flex w-full items-center gap-2 rounded-lg border border-slate-100 px-2 py-2 text-left hover:bg-slate-50"
                  >
                    <div
                      className="h-8 w-10 shrink-0 rounded-md"
                      style={{ background: tripCover(tripById.get(e.b.tripId), e.b.tripName) }}
                    />
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-xs font-semibold text-slate-800">{e.b.tripName}</div>
                      <div className="text-[10px] text-slate-400">{formatShort(e.b.departureDate)}</div>
                    </div>
                    <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusClass(e.ui)}`}>
                      {e.ui}
                    </span>
                  </button>
                ))}
              </div>
            </Panel>

            <Panel title="Live Batch Tracking">
              {liveBatch ? (
                <div>
                  <div className="mb-2 text-xs font-semibold text-slate-800">
                    {liveBatch.b.tripName} · {liveBatch.code}
                  </div>
                  <ol className="space-y-2">
                    {[
                      { t: "06:00 AM", x: "Batch started", done: true },
                      { t: "09:30 AM", x: `Trekking · ${liveBatch.b.seatsBooked}/${liveBatch.b.maxCapacity} travellers`, done: true },
                      { t: "01:00 PM", x: "Lunch break", done: true },
                      { t: "05:30 PM", x: "Reached campsite", done: liveBatch.ui === "On Trek" },
                      { t: "Tomorrow", x: "Summit attempt (~3:00 AM)", done: false },
                    ].map((s) => (
                      <li key={s.t} className="flex gap-2 text-xs">
                        <span
                          className={`mt-1 h-2 w-2 shrink-0 rounded-full ${s.done ? "bg-emerald-500" : "bg-slate-300"}`}
                        />
                        <div>
                          <div className="font-semibold text-slate-700">{s.t}</div>
                          <div className="text-slate-500">{s.x}</div>
                        </div>
                      </li>
                    ))}
                  </ol>
                </div>
              ) : (
                <p className="text-xs text-slate-400">No batches currently on trek</p>
              )}
            </Panel>
          </div>

          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-8">
            {[
              { label: "Create New Batch", action: () => canManage && setShowCreate(true), href: null },
              { label: "Clone Existing Batch", action: null, href: "/trips/bulk" },
              { label: "Bulk Operations", action: null, href: "/trips/bulk" },
              { label: "Assign Guide / Staff", action: null, href: "/guides" },
              { label: "Generate Itinerary", action: null, href: "/trips/itineraries" },
              { label: "Send Notifications", action: null, href: "/messages" },
              { label: "Manage Permits", action: null, href: "/operations/permits" },
              { label: "Export Reports", action: exportCsv, href: null },
            ].map((a) =>
              a.href ? (
                <Link
                  key={a.label}
                  href={a.href}
                  className="rounded-xl border border-slate-200 bg-white px-3 py-3 text-center text-[11px] font-semibold text-slate-700 shadow-sm hover:border-sky-300 hover:text-sky-800"
                >
                  {a.label}
                </Link>
              ) : (
                <button
                  key={a.label}
                  type="button"
                  onClick={() => a.action?.()}
                  className="rounded-xl border border-slate-200 bg-white px-3 py-3 text-center text-[11px] font-semibold text-slate-700 shadow-sm hover:border-sky-300 hover:text-sky-800"
                >
                  {a.label}
                </button>
              ),
            )}
          </div>
        </div>

        {selected && (
          <BatchDetailPanel
            batch={selected}
            detail={selectedDetail}
            durationDays={durationFor(selected.tripId)}
            tripCategory={tripById.get(selected.tripId)?.category ?? selected.tripCategory}
            bookings={selectedBookings}
            canManage={canManage}
            onClose={() => setSelected(null)}
            onEdit={() => setEditBatch(selected)}
          />
        )}
      </div>

      {(showCreate || editBatch) && (
        <BatchFormModal
          mode={editBatch ? "edit" : "create"}
          batch={editBatch}
          detail={editDetail}
          trips={trips.filter((t) => t.bookingType === "FIXED_BATCH" && t.active !== false)}
          guides={guides}
          onClose={() => {
            setShowCreate(false);
            setEditBatch(null);
          }}
          onSaved={() => {
            setShowCreate(false);
            setEditBatch(null);
            reload();
          }}
        />
      )}
    </TreksShell>
  );
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="mb-3 text-sm font-semibold text-slate-800">{title}</h3>
      {children}
    </section>
  );
}

function Donut({ slices }: { slices: { name: string; count: number; pct: string; color: string }[] }) {
  const total = slices.reduce((s, x) => s + x.count, 0) || 1;
  let cursor = 0;
  const stops = slices.map((sl) => {
    const share = (sl.count / total) * 100;
    const start = cursor;
    cursor += share;
    return `${sl.color} ${start}% ${cursor}%`;
  });
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div
        className="mx-auto h-28 w-28 shrink-0 rounded-full"
        style={{
          background: `conic-gradient(${stops.join(", ") || "#e2e8f0 0% 100%"})`,
          mask: "radial-gradient(circle at center, transparent 52%, black 53%)",
          WebkitMask: "radial-gradient(circle at center, transparent 52%, black 53%)",
        }}
      />
      <div className="min-w-0 flex-1 space-y-1">
        {slices.map((sl) => (
          <div key={sl.name} className="flex justify-between gap-2 text-[11px]">
            <span className="flex items-center gap-1.5 truncate">
              <span className="h-2 w-2 rounded-sm" style={{ background: sl.color }} />
              {sl.name}
            </span>
            <span className="tabular-nums text-slate-500">
              {sl.pct}% · {sl.count}
            </span>
          </div>
        ))}
        {slices.length === 0 && <p className="text-xs text-slate-400">No data</p>}
      </div>
    </div>
  );
}

function buildCalendar(
  month: Date,
  enriched: { b: UpcomingBatch; ui: UiBatchStatus }[],
) {
  const y = month.getFullYear();
  const m = month.getMonth();
  const first = new Date(y, m, 1);
  const startPad = first.getDay();
  const daysInMonth = new Date(y, m + 1, 0).getDate();
  const cells: { key: string; day: number; inMonth: boolean; dots: string[] }[] = [];

  const colorFor = (ui: UiBatchStatus) => {
    if (ui === "On Trek" || ui === "Starting") return "#10b981";
    if (ui === "Completed") return "#94a3b8";
    if (ui === "Cancelled") return "#f43f5e";
    return "#3b82f6";
  };

  for (let i = 0; i < startPad; i++) {
    cells.push({ key: `p-${i}`, day: 0, inMonth: false, dots: [] });
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const iso = `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
    const dots = enriched.filter((e) => e.b.departureDate === iso).map((e) => colorFor(e.ui));
    cells.push({ key: iso, day: d, inMonth: true, dots });
  }
  return cells;
}

function BatchFormModal({
  mode,
  batch,
  detail,
  trips,
  guides,
  onClose,
  onSaved,
}: {
  mode: "create" | "edit";
  batch: UpcomingBatch | null;
  detail: Batch | null;
  trips: Trip[];
  guides: Guide[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [tripId, setTripId] = useState(batch?.tripId ?? trips[0]?.id ?? "");
  const [departureDate, setDepartureDate] = useState(batch?.departureDate ?? "");
  const [maxCapacity, setMaxCapacity] = useState(String(batch?.maxCapacity ?? 24));
  const [guideId, setGuideId] = useState(detail?.guideId ?? "");
  const [transportPlan, setTransportPlan] = useState(detail?.transportPlan ?? "");
  const [status, setStatus] = useState(batch?.status ?? "OPEN");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (detail?.guideId) setGuideId(detail.guideId);
    if (detail?.transportPlan) setTransportPlan(detail.transportPlan ?? "");
  }, [detail]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (mode === "create") {
        if (!tripId) throw new Error("Select a trek");
        await api.createBatch(tripId, {
          departureDate,
          maxCapacity: Number(maxCapacity),
          guideId: guideId || undefined,
          transportPlan: transportPlan || undefined,
        });
      } else if (batch) {
        await api.updateBatch(batch.id, {
          departureDate,
          maxCapacity: Number(maxCapacity),
          guideId: guideId || undefined,
          unassignGuide: !guideId,
          transportPlan: transportPlan || undefined,
          status: status as Batch["status"],
        });
      }
      onSaved();
    } catch (err) {
      if (err instanceof ApiClientError && err.body.fieldErrors?.length) {
        setError(err.body.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(" · "));
      } else {
        setError(err instanceof Error ? err.message : "Could not save batch");
      }
    } finally {
      setSaving(false);
    }
  }

  const input =
    "mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-sky-400";

  return (
    <div className="fixed inset-0 z-30 flex items-start justify-center overflow-y-auto bg-slate-900/50 p-6 pt-16">
      <form onSubmit={submit} className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">
            {mode === "create" ? "Create Batch" : `Edit Batch · ${batch ? batchCode(batch) : ""}`}
          </h2>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-slate-700">
            ✕
          </button>
        </div>

        <div className="space-y-3">
          {mode === "create" && (
            <label className="block text-xs font-medium text-slate-600">
              Trek / Package
              <select
                required
                value={tripId}
                onChange={(e) => setTripId(e.target.value)}
                className={input}
              >
                <option value="">Select trek…</option>
                {trips.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </label>
          )}
          <div className="grid grid-cols-2 gap-3">
            <label className="block text-xs font-medium text-slate-600">
              Departure date
              <input
                type="date"
                required
                value={departureDate}
                onChange={(e) => setDepartureDate(e.target.value)}
                className={input}
              />
            </label>
            <label className="block text-xs font-medium text-slate-600">
              Capacity
              <input
                type="number"
                min={1}
                required
                value={maxCapacity}
                onChange={(e) => setMaxCapacity(e.target.value)}
                className={input}
              />
            </label>
          </div>
          <label className="block text-xs font-medium text-slate-600">
            Guide / Leader
            <select value={guideId} onChange={(e) => setGuideId(e.target.value)} className={input}>
              <option value="">Unassigned</option>
              {guides.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.name}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs font-medium text-slate-600">
            Transport plan
            <input
              value={transportPlan}
              onChange={(e) => setTransportPlan(e.target.value)}
              placeholder="Tempo Traveller UK07…"
              className={input}
            />
          </label>
          {mode === "edit" && (
            <label className="block text-xs font-medium text-slate-600">
              Status
              <select value={status} onChange={(e) => setStatus(e.target.value)} className={input}>
                {["OPEN", "READY_FOR_DEPARTURE", "CLOSED", "CANCELLED"].map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>

        {error && (
          <p className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-sky-600 px-4 py-2 text-sm font-semibold text-white hover:bg-sky-500 disabled:opacity-60"
          >
            {saving ? "Saving…" : mode === "create" ? "Create Batch" : "Save Changes"}
          </button>
        </div>
      </form>
    </div>
  );
}
