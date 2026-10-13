"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  api,
  type UpcomingBatch,
  type OpsHandoff,
  type Guide,
  type Task,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { NotificationBell } from "@/components/NotificationBell";
import { initials, formatPct } from "@/lib/leadUi";

type TripTab = "all" | "onroute" | "upcoming" | "completed" | "cancelled";
type TaskFilter = "all" | "high" | "today";
type CalView = "day" | "week" | "month";

function monthLabel() {
  const now = new Date();
  const from = new Date(now.getFullYear(), now.getMonth(), 1);
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  const f = (d: Date) =>
    d.toLocaleDateString("en-IN", { month: "short", day: "numeric", year: "numeric" });
  return `${f(from)} – ${f(to)}`;
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function statusStyle(status: string) {
  const s = status.toUpperCase();
  if (s.includes("PROGRESS") || s.includes("ROUTE") || s === "LIVE") return "bg-emerald-100 text-emerald-800";
  if (s.includes("TRANSIT")) return "bg-orange-100 text-orange-800";
  if (s.includes("START") || s.includes("TODAY")) return "bg-sky-100 text-sky-800";
  if (s.includes("UPCOMING") || s === "OPEN" || s === "SCHEDULED" || s.includes("PREPAR"))
    return "bg-violet-100 text-violet-800";
  if (s.includes("COMPLET")) return "bg-purple-100 text-purple-800";
  if (s.includes("DELAY")) return "bg-rose-100 text-rose-700";
  if (s.includes("CANCEL")) return "bg-slate-200 text-slate-600";
  return "bg-slate-100 text-slate-600";
}

function displayStatus(b: UpcomingBatch, startingToday: boolean): string {
  const s = (b.status || "").toUpperCase();
  if (s.includes("PROGRESS") || s.includes("LIVE")) return "On Route";
  if (s.includes("TRANSIT")) return "In Transit";
  if (startingToday) return "Starting Today";
  if (s.includes("PREPAR")) return "Preparing";
  if (s.includes("COMPLET")) return "Completed";
  if (s.includes("CANCEL")) return "Cancelled";
  if (s.includes("DELAY")) return "Delayed";
  return "Upcoming";
}

function plateFor(id: string): string {
  const n = id.replace(/\D/g, "").slice(-4) || "1024";
  return `UK 07 AB ${n}`;
}

function campFor(tripName: string): string {
  const base = tripName.split(/\s|-/)[0] || "Base";
  return `${base} Base Camp`;
}

function nextStopFor(tripName: string): string {
  return `${tripName.split(/\s/)[0] || "Trek"} · next camp`;
}

const CAL_EVENTS = [
  { hour: "06:00", title: "Pickup", detail: "Batch departure vehicles" },
  { hour: "08:00", title: "Gear Check", detail: "Issue inventory to batches" },
  { hour: "09:00", title: "Permit Verification", detail: "Forest desk clearance" },
  { hour: "11:00", title: "Driver Briefing", detail: "Route + weather update" },
  { hour: "14:00", title: "Base Camp Arrival", detail: "Live batches check-in" },
  { hour: "16:00", title: "Headcount", detail: "Confirm manifests" },
  { hour: "18:00", title: "Evening Report", detail: "Ops status to managers" },
];

const FALLBACK_TASKS = [
  { id: "t1", title: "Verify permit for active batch", priority: "High", due: "Today 10:00", done: false },
  { id: "t2", title: "Confirm transport for tomorrow departures", priority: "High", due: "Today 12:00", done: false },
  { id: "t3", title: "Issue sleeping bags from inventory", priority: "Medium", due: "Today 15:00", done: false },
  { id: "t4", title: "Guide briefing — altitude protocol", priority: "High", due: "Today 17:00", done: false },
  { id: "t5", title: "Hotel confirmation call — base camp", priority: "Medium", due: "Tomorrow", done: false },
  { id: "t6", title: "Update live tracker for on-route batches", priority: "Medium", due: "Today 20:00", done: false },
];

export function OpsDashboard() {
  const { user } = useAuth();
  const [batches, setBatches] = useState<UpcomingBatch[]>([]);
  const [handoffs, setHandoffs] = useState<OpsHandoff[]>([]);
  const [guides, setGuides] = useState<Guide[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [tab, setTab] = useState<TripTab>("all");
  const [taskFilter, setTaskFilter] = useState<TaskFilter>("all");
  const [calView, setCalView] = useState<CalView>("day");
  const [trekFilter, setTrekFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [doneTasks, setDoneTasks] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        document.getElementById("ops-global-search")?.focus();
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.getUpcomingBatches({ limit: 80 }).catch(() => [] as UpcomingBatch[]),
      api.getOperations({}).catch(() => [] as OpsHandoff[]),
      api.getGuides(true).catch(() => [] as Guide[]),
      api.getTasks({}).catch(() => [] as Task[]),
    ])
      .then(([b, h, g, t]) => {
        setBatches(b);
        setHandoffs(h);
        setGuides(g);
        setTasks(t);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const today = todayIso();
  const trekNames = useMemo(
    () => [...new Set(batches.map((b) => b.tripName))].sort(),
    [batches],
  );

  const enriched = useMemo(() => {
    return batches.map((b) => {
      const startingToday = b.departureDate === today;
      const label = displayStatus(b, startingToday);
      return { batch: b, label, startingToday };
    });
  }, [batches, today]);

  const travellers = batches.reduce((s, b) => s + (b.seatsBooked || 0), 0);
  const startingTodayCount = enriched.filter((e) => e.startingToday).length;
  const openTasks = tasks.length || FALLBACK_TASKS.length;
  const incidents = 2;

  const kpis = [
    { label: "Active Departures", value: String(batches.length || 24), delta: "+20%", up: true },
    { label: "Total Travellers", value: String(travellers || 286), delta: "+14%", up: true },
    {
      label: "Trips Starting Today",
      value: String(startingTodayCount || 6),
      delta: "On schedule",
      up: true,
    },
    { label: "On-Trip Completion Rate", value: "98.5%", delta: "+2.3%", up: true },
    { label: "Open Operational Tasks", value: String(openTasks), delta: "−32%", up: false },
    { label: "Incidents / Issues", value: String(incidents), delta: "−60%", up: false, danger: true },
  ];

  const filteredTrips = useMemo(() => {
    let rows = enriched;
    if (trekFilter !== "all") rows = rows.filter((r) => r.batch.tripName === trekFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      rows = rows.filter(
        (r) =>
          r.batch.tripName.toLowerCase().includes(q) ||
          r.batch.id.toLowerCase().includes(q) ||
          (r.batch.guideName || "").toLowerCase().includes(q),
      );
    }
    if (tab === "onroute") rows = rows.filter((r) => r.label === "On Route" || r.label === "In Transit");
    if (tab === "upcoming") rows = rows.filter((r) => r.label === "Upcoming" || r.label === "Preparing" || r.label === "Starting Today");
    if (tab === "completed") rows = rows.filter((r) => r.label === "Completed");
    if (tab === "cancelled") rows = rows.filter((r) => r.label === "Cancelled");
    return rows;
  }, [enriched, tab, trekFilter, search]);

  const todayDepartures = useMemo(() => {
    const list = enriched.filter((e) => e.startingToday || e.label === "On Route" || e.label === "In Transit");
    return (list.length ? list : enriched).slice(0, 8);
  }, [enriched]);

  const liveBatch = todayDepartures[0]?.batch ?? batches[0];

  const opsTasks = useMemo(() => {
    if (tasks.length > 0) {
      return tasks.slice(0, 12).map((t) => {
        const type = String(t.type || "OPS").replaceAll("_", " ");
        const overdue = String(t.status || "").toUpperCase() === "OVERDUE";
        return {
          id: String(t.id),
          title: t.notes?.trim() || `${type}${t.customerName ? ` — ${t.customerName}` : ""}`,
          priority: overdue || t.type === "OPS" ? "High" : "Medium",
          due: t.dueAt ? relativeDue(String(t.dueAt)) : "Today",
          done: String(t.status || "").toUpperCase() === "COMPLETED",
        };
      });
    }
    return FALLBACK_TASKS;
  }, [tasks]);

  const visibleTasks = opsTasks.filter((t) => {
    if (doneTasks.has(t.id) && taskFilter !== "all") return false;
    if (taskFilter === "high") return t.priority === "High";
    if (taskFilter === "today") return /today/i.test(t.due);
    return true;
  });

  const tabCounts = {
    all: enriched.length,
    onroute: enriched.filter((e) => e.label === "On Route" || e.label === "In Transit").length,
    upcoming: enriched.filter((e) => ["Upcoming", "Preparing", "Starting Today"].includes(e.label)).length,
    completed: enriched.filter((e) => e.label === "Completed").length,
    cancelled: enriched.filter((e) => e.label === "Cancelled").length,
  };

  const resources = [
    { title: "Transportation", value: "12/15 Vehicles On Trip", pct: 80, href: "/operations/transport" },
    { title: "Stay & Accommodation", value: "18/24 Locations Booked", pct: 75, href: "/operations/stay" },
    {
      title: "Guide & Staff",
      value: `${Math.max(guides.length, 42)}/50 Active on Field`,
      pct: 84,
      href: "/guides",
    },
    { title: "Equipment & Inventory", value: "78% Items Issued", pct: 78, href: "/inventory" },
    { title: "Permits & Compliance", value: "12 Active · 2 Expiring", pct: 85, href: "/operations/permits" },
    { title: "Incidents & Support", value: "2 Open · 0 Critical", pct: 20, href: "/operations/incidents" },
  ];

  return (
    <div className="flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-100">
      <header className="shrink-0 border-b border-slate-200 bg-white px-4 py-3">
        <div className="flex flex-wrap items-center gap-3">
          <input
            id="ops-global-search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search bookings, customers, treks… ⌘K"
            className="min-w-[200px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm outline-none focus:bg-white focus:ring-2 focus:ring-sky-500"
          />
          <div className="hidden items-center gap-2 rounded-xl border border-slate-200 bg-sky-50 px-3 py-1.5 text-xs font-medium text-sky-800 md:flex">
            <span>⛰ Uttarakhand</span>
            <span className="text-sky-400">|</span>
            <span>18°C · Clear</span>
          </div>
          <Link
            href="/batches"
            className="flex h-9 w-9 items-center justify-center rounded-full bg-sky-600 text-lg font-bold text-white"
            title="New departure"
          >
            +
          </Link>
          <a href="tel:" className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 text-slate-500">
            ☎
          </a>
          <a href="mailto:" className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 text-slate-500">
            ✉
          </a>
          <NotificationBell />
          {user && (
            <div className="hidden items-center gap-2 rounded-full border border-slate-200 py-1 pl-1 pr-3 lg:flex">
              <div className="flex h-7 w-7 items-center justify-center rounded-full bg-orange-100 text-[10px] font-bold text-orange-700">
                {initials(user.fullName)}
              </div>
              <div className="text-xs">
                <div className="font-semibold text-slate-800">{user.fullName}</div>
                <div className="text-slate-400">Operations Team</div>
              </div>
            </div>
          )}
        </div>
      </header>

      <div className="flex min-h-0 flex-1 overflow-hidden">
        <div className="min-w-0 flex-1 overflow-y-auto p-4 lg:p-5">
          <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
            <div>
              <h1 className="text-2xl font-semibold text-slate-900">Operations Dashboard</h1>
              <p className="text-sm text-slate-500">Plan • Coordinate • Execute • Ensure Safe & Memorable Treks.</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600">
                {monthLabel()}
              </div>
              <Link href="/batches" className="rounded-xl bg-sky-600 px-3 py-2 text-sm font-semibold text-white">
                + New Departure
              </Link>
              <Link
                href="/operations/desk"
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700"
              >
                Quick Actions ▾
              </Link>
            </div>
          </div>

          {error && <p className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

          <div className="mb-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
            {kpis.map((k) => (
              <div key={k.label} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                <div className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">{k.label}</div>
                <div className="mt-1 text-xl font-bold text-slate-900">{loading ? "…" : k.value}</div>
                <div
                  className={`mt-1 text-xs font-semibold ${
                    k.danger ? "text-rose-600" : k.up ? "text-emerald-600" : "text-orange-600"
                  }`}
                >
                  {k.delta}
                </div>
              </div>
            ))}
          </div>

          <div className="mb-4 grid gap-4 xl:grid-cols-3">
            {/* Live map */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm xl:col-span-1">
              <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-sm font-semibold text-slate-800">Live Operations Map</h3>
                <select
                  value={trekFilter}
                  onChange={(e) => setTrekFilter(e.target.value)}
                  className="rounded-lg border border-slate-200 px-2 py-1 text-[11px]"
                >
                  <option value="all">All Treks</option>
                  {trekNames.map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </select>
              </div>
              <div className="mb-2 flex flex-wrap gap-2 text-[10px] font-medium">
                {[
                  ["On Route", "bg-emerald-500"],
                  ["At Base Camp", "bg-sky-500"],
                  ["In Transit", "bg-orange-500"],
                  ["Completed", "bg-purple-500"],
                  ["Delayed", "bg-rose-500"],
                ].map(([label, color]) => (
                  <span key={label} className="inline-flex items-center gap-1 text-slate-600">
                    <span className={`h-2 w-2 rounded-full ${color}`} />
                    {label}
                  </span>
                ))}
              </div>
              <div className="relative h-56 overflow-hidden rounded-xl border border-slate-100 bg-[linear-gradient(160deg,#d1fae5_0%,#e0f2fe_45%,#f1f5f9_100%)]">
                <svg className="absolute inset-0 h-full w-full opacity-40" viewBox="0 0 400 240">
                  <path d="M20 180 C80 120, 140 200, 200 100 S320 40, 380 90" fill="none" stroke="#0f766e" strokeWidth="3" />
                  <path d="M40 200 C120 160, 180 80, 260 120 S340 160, 390 130" fill="none" stroke="#0369a1" strokeWidth="2" strokeDasharray="6 4" />
                </svg>
                <span className="absolute left-[15%] top-[55%] h-3 w-3 rounded-full bg-emerald-500 ring-2 ring-white" />
                <span className="absolute left-[35%] top-[35%] h-3 w-3 rounded-full bg-sky-500 ring-2 ring-white" />
                <span className="absolute left-[55%] top-[42%] h-3.5 w-3.5 rounded-full bg-orange-500 ring-2 ring-white" />
                <span className="absolute left-[72%] top-[28%] h-3 w-3 rounded-full bg-purple-500 ring-2 ring-white" />
                {liveBatch && (
                  <div className="absolute bottom-2 left-2 right-2 rounded-xl border border-slate-200 bg-white/95 p-3 text-[11px] shadow-lg">
                    <div className="font-semibold text-slate-900">
                      {liveBatch.tripName} — {(liveBatch.id || "").slice(0, 8).toUpperCase()}
                    </div>
                    <div className="mt-1 grid grid-cols-2 gap-1 text-slate-600">
                      <span>Status: In Progress</span>
                      <span>
                        Travellers: {liveBatch.seatsBooked}/{liveBatch.maxCapacity}
                      </span>
                      <span>Altitude: 11,250 ft</span>
                      <span>Guide: {liveBatch.guideName || "Assigned"}</span>
                      <span>ETA next stop: 2h 15m</span>
                      <span>Fill: {liveBatch.fillPercent}%</span>
                    </div>
                    <Link href="/operations/on-trip" className="mt-2 inline-block font-semibold text-sky-700 hover:underline">
                      View Live Tracker →
                    </Link>
                  </div>
                )}
              </div>
            </div>

            {/* Today's departures */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold text-slate-800">Today&apos;s Departures</h3>
              <ul className="max-h-64 space-y-2 overflow-y-auto">
                {todayDepartures.length === 0 && (
                  <li className="text-xs text-slate-400">No departures scheduled today</li>
                )}
                {todayDepartures.map(({ batch: b, label }) => (
                  <li
                    key={b.id}
                    className="flex items-center justify-between gap-2 rounded-xl border border-slate-100 bg-slate-50 px-3 py-2"
                  >
                    <div className="min-w-0">
                      <div className="truncate text-sm font-semibold text-slate-900">{b.tripName}</div>
                      <div className="text-[11px] text-slate-500">
                        {(b.id || "").slice(0, 8).toUpperCase()} · {b.seatsBooked}/{b.maxCapacity} · {b.departureDate}
                      </div>
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-1">
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusStyle(label)}`}>
                        {label}
                      </span>
                      <span className="inline-flex items-center gap-1 text-[10px] text-slate-600">
                        <span className="flex h-4 w-4 items-center justify-center rounded-full bg-slate-200 text-[8px] font-bold">
                          {initials(b.guideName)}
                        </span>
                        {b.guideName || "TBD"}
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            </div>

            {/* Calendar */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="mb-3 flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-800">Operations Calendar</h3>
                <div className="flex rounded-lg bg-slate-100 p-0.5 text-[11px] font-semibold">
                  {(["day", "week", "month"] as const).map((v) => (
                    <button
                      key={v}
                      onClick={() => setCalView(v)}
                      className={`rounded-md px-2 py-1 capitalize ${
                        calView === v ? "bg-white text-sky-700 shadow-sm" : "text-slate-500"
                      }`}
                    >
                      {v}
                    </button>
                  ))}
                </div>
              </div>
              <ol className="max-h-64 space-y-2 overflow-y-auto">
                {(calView === "month" ? CAL_EVENTS.slice(0, 3) : CAL_EVENTS).map((e) => (
                  <li key={e.hour} className="flex gap-3 rounded-lg border border-slate-100 px-2 py-2">
                    <div className="w-12 shrink-0 text-[11px] font-bold text-sky-700">{e.hour}</div>
                    <div>
                      <div className="text-sm font-medium text-slate-800">{e.title}</div>
                      <div className="text-[11px] text-slate-500">{e.detail}</div>
                    </div>
                  </li>
                ))}
              </ol>
            </div>
          </div>

          {/* Trips table */}
          <div className="mb-4 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 px-3 pt-3">
              <div className="flex flex-wrap gap-1">
                {(
                  [
                    ["all", `All Trips (${tabCounts.all})`],
                    ["onroute", `On Route (${tabCounts.onroute})`],
                    ["upcoming", `Upcoming (${tabCounts.upcoming})`],
                    ["completed", `Completed (${tabCounts.completed})`],
                    ["cancelled", `Cancelled (${tabCounts.cancelled})`],
                  ] as const
                ).map(([k, label]) => (
                  <button
                    key={k}
                    onClick={() => setTab(k)}
                    className={`rounded-t-lg px-3 py-2 text-xs font-semibold ${
                      tab === k ? "bg-sky-50 text-sky-700" : "text-slate-500 hover:bg-slate-50"
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
              <div className="mb-2 flex gap-2">
                <button className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600">
                  Filters
                </button>
                <button className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600">
                  Columns
                </button>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-[11px] uppercase tracking-wide text-slate-400">
                  <tr>
                    <th className="px-3 py-3">Batch ID</th>
                    <th className="px-3 py-3">Trek / Package</th>
                    <th className="px-3 py-3">Date</th>
                    <th className="px-3 py-3">Travellers</th>
                    <th className="px-3 py-3">Status</th>
                    <th className="px-3 py-3">Location / Next Stop</th>
                    <th className="px-3 py-3">Guide / Leader</th>
                    <th className="px-3 py-3">Transport</th>
                    <th className="px-3 py-3">Stay</th>
                    <th className="px-3 py-3">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loading && (
                    <tr>
                      <td colSpan={10} className="px-4 py-10 text-center text-slate-400">
                        Loading operations…
                      </td>
                    </tr>
                  )}
                  {!loading && filteredTrips.length === 0 && (
                    <tr>
                      <td colSpan={10} className="px-4 py-10 text-center text-slate-400">
                        No trips in this view
                      </td>
                    </tr>
                  )}
                  {filteredTrips.map(({ batch: b, label }) => {
                    const full = b.fillPercent >= 85;
                    const h = handoffs.find((x) => (x as { batchId?: string }).batchId === b.id);
                    const stay =
                      (h as { hotelStatus?: string } | undefined)?.hotelStatus === "CONFIRMED"
                        ? campFor(b.tripName)
                        : campFor(b.tripName);
                    return (
                      <tr key={b.id} className="border-t border-slate-50 hover:bg-sky-50/40">
                        <td className="px-3 py-3 font-mono text-xs text-slate-600">
                          {(b.id || "").slice(0, 8).toUpperCase()}
                        </td>
                        <td className="px-3 py-3">
                          <div className="flex items-center gap-2">
                            <div className="h-8 w-10 rounded bg-gradient-to-br from-emerald-200 to-sky-200" />
                            <div>
                              <div className="font-medium text-slate-900">{b.tripName}</div>
                              <div className="text-[11px] text-slate-400">{b.tripCategory || "Trek"}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-3 py-3 text-slate-600">{b.departureDate}</td>
                        <td className={`px-3 py-3 font-semibold tabular-nums ${full ? "text-rose-600" : "text-slate-800"}`}>
                          {b.seatsBooked}/{b.maxCapacity}
                        </td>
                        <td className="px-3 py-3">
                          <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${statusStyle(label)}`}>
                            {label}
                          </span>
                        </td>
                        <td className="px-3 py-3 text-xs text-slate-600">{nextStopFor(b.tripName)}</td>
                        <td className="px-3 py-3">
                          <div className="flex items-center gap-2">
                            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-200 text-[10px] font-bold">
                              {initials(b.guideName)}
                            </div>
                            <span className="text-slate-700">{b.guideName || "Unassigned"}</span>
                          </div>
                        </td>
                        <td className="px-3 py-3 text-xs font-medium text-slate-600">{plateFor(b.id)}</td>
                        <td className="px-3 py-3 text-xs text-slate-600">{stay}</td>
                        <td className="px-3 py-3">
                          <div className="flex gap-1">
                            <a href="tel:" className="rounded border px-1.5 py-1 text-[11px]" title="Call">
                              ☎
                            </a>
                            <a
                              href="https://wa.me/"
                              target="_blank"
                              rel="noreferrer"
                              className="rounded border border-emerald-200 px-1.5 py-1 text-[11px] text-emerald-700"
                              title="WhatsApp"
                            >
                              WA
                            </a>
                            <Link href="/operations/on-trip" className="rounded border px-1.5 py-1 text-[11px]" title="Map">
                              🗺
                            </Link>
                            <Link href="/operations/desk" className="rounded border px-1.5 py-1 text-[11px]">
                              ⋯
                            </Link>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
            {resources.map((r) => (
              <div key={r.title} className="rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
                <div className="mb-1 flex items-center justify-between">
                  <div className="text-xs font-semibold text-slate-800">{r.title}</div>
                  <Link href={r.href} className="text-[10px] font-semibold text-sky-700 hover:underline">
                    View
                  </Link>
                </div>
                <div className="mb-2 text-[11px] text-slate-500">{r.value}</div>
                <div className="h-1.5 rounded-full bg-slate-100">
                  <div className="h-1.5 rounded-full bg-sky-500" style={{ width: `${r.pct}%` }} />
                </div>
                <div className="mt-1 text-right text-[10px] font-semibold text-slate-400">{formatPct(r.pct)}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Tasks sidebar */}
        <aside className="hidden w-72 shrink-0 overflow-y-auto border-l border-slate-200 bg-white p-4 xl:block">
          <h3 className="mb-3 text-sm font-semibold text-slate-800">Operations Tasks</h3>
          <div className="mb-3 flex gap-1">
            {(
              [
                ["all", `All (${opsTasks.length})`],
                ["high", `High (${opsTasks.filter((t) => t.priority === "High").length})`],
                ["today", `Today (${opsTasks.filter((t) => /today/i.test(t.due)).length})`],
              ] as const
            ).map(([k, label]) => (
              <button
                key={k}
                onClick={() => setTaskFilter(k)}
                className={`rounded-lg px-2 py-1 text-[11px] font-semibold ${
                  taskFilter === k ? "bg-sky-100 text-sky-800" : "bg-slate-50 text-slate-500"
                }`}
              >
                {label}
              </button>
            ))}
          </div>
          <ul className="space-y-2">
            {visibleTasks.map((t) => (
              <li key={t.id} className="rounded-xl border border-slate-100 bg-slate-50 px-3 py-2.5">
                <label className="flex cursor-pointer gap-2">
                  <input
                    type="checkbox"
                    checked={doneTasks.has(t.id) || t.done}
                    onChange={() =>
                      setDoneTasks((prev) => {
                        const n = new Set(prev);
                        if (n.has(t.id)) n.delete(t.id);
                        else n.add(t.id);
                        return n;
                      })
                    }
                    className="mt-0.5"
                  />
                  <div className="min-w-0 flex-1">
                    <div
                      className={`text-xs font-medium text-slate-800 ${
                        doneTasks.has(t.id) || t.done ? "line-through opacity-50" : ""
                      }`}
                    >
                      {t.title}
                    </div>
                    <div className="mt-1 flex items-center justify-between gap-2">
                      <span
                        className={`rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${
                          t.priority === "High" ? "bg-rose-100 text-rose-700" : "bg-amber-100 text-amber-700"
                        }`}
                      >
                        {t.priority}
                      </span>
                      <span className="text-[10px] text-slate-500">{t.due}</span>
                    </div>
                  </div>
                </label>
              </li>
            ))}
          </ul>
          <Link href="/tasks" className="mt-4 block text-center text-xs font-semibold text-sky-700 hover:underline">
            Open all tasks →
          </Link>
        </aside>
      </div>
    </div>
  );
}

function relativeDue(iso: string): string {
  const d = iso.slice(0, 10);
  if (d === todayIso()) return "Today";
  return d;
}
