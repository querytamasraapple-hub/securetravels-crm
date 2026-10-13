"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { api, type UpcomingBatch } from "@/lib/api";
import { Protected } from "@/components/Protected";
import { Sidebar } from "@/components/Sidebar";

function monthStart(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

function ymd(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function CalendarView() {
  const [cursor, setCursor] = useState(() => monthStart(new Date()));
  const [batches, setBatches] = useState<UpcomingBatch[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  const from = useMemo(() => ymd(new Date(cursor.getFullYear(), cursor.getMonth(), 1)), [cursor]);
  const to = useMemo(
    () => ymd(new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0)),
    [cursor],
  );

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setBatches(await api.getUpcomingBatches({ from, to, limit: 200 }));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load calendar");
      setBatches([]);
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  useEffect(() => {
    load();
  }, [load]);

  const byDate = useMemo(() => {
    const map = new Map<string, UpcomingBatch[]>();
    for (const b of batches) {
      const key = b.departureDate;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(b);
    }
    return map;
  }, [batches]);

  const days = useMemo(() => {
    const first = monthStart(cursor);
    const startPad = first.getDay(); // 0 Sun
    const daysInMonth = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0).getDate();
    const cells: { date: Date | null; key: string }[] = [];
    for (let i = 0; i < startPad; i++) cells.push({ date: null, key: `pad-${i}` });
    for (let d = 1; d <= daysInMonth; d++) {
      const date = new Date(cursor.getFullYear(), cursor.getMonth(), d);
      cells.push({ date, key: ymd(date) });
    }
    return cells;
  }, [cursor]);

  const selectedBatches = selected ? byDate.get(selected) ?? [] : [];
  const monthLabel = cursor.toLocaleDateString("en-IN", { month: "long", year: "numeric" });
  const today = ymd(new Date());

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <main className="flex-1 p-6">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Departure calendar</h1>
            <p className="text-sm text-slate-500">
              Batch departures for treks and tours — seats, guides, and fill at a glance.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCursor(new Date(cursor.getFullYear(), cursor.getMonth() - 1, 1))}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50"
            >
              ←
            </button>
            <div className="min-w-[140px] text-center text-sm font-semibold text-slate-800">
              {monthLabel}
            </div>
            <button
              onClick={() => setCursor(new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1))}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50"
            >
              →
            </button>
            <button
              onClick={() => setCursor(monthStart(new Date()))}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50"
            >
              Today
            </button>
          </div>
        </div>

        {error && (
          <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
        )}

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 overflow-hidden rounded-xl border border-slate-200 bg-white">
            <div className="grid grid-cols-7 border-b border-slate-200 bg-slate-50 text-center text-[11px] font-semibold uppercase tracking-wide text-slate-400">
              {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((d) => (
                <div key={d} className="px-1 py-2">{d}</div>
              ))}
            </div>
            {loading ? (
              <p className="py-16 text-center text-sm text-slate-400">Loading departures…</p>
            ) : (
              <div className="grid grid-cols-7">
                {days.map((cell) => {
                  if (!cell.date) {
                    return <div key={cell.key} className="min-h-[72px] border-b border-r border-slate-100 bg-slate-50/50" />;
                  }
                  const key = cell.key;
                  const events = byDate.get(key) ?? [];
                  const isToday = key === today;
                  const isSelected = key === selected;
                  return (
                    <button
                      key={key}
                      type="button"
                      onClick={() => setSelected(key)}
                      className={`min-h-[72px] border-b border-r border-slate-100 p-1.5 text-left hover:bg-slate-50 ${
                        isSelected ? "bg-slate-100" : "bg-white"
                      }`}
                    >
                      <div
                        className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-medium ${
                          isToday ? "bg-slate-900 text-white" : "text-slate-700"
                        }`}
                      >
                        {cell.date.getDate()}
                      </div>
                      {events.length > 0 && (
                        <div className="mt-1 space-y-0.5">
                          {events.slice(0, 2).map((e) => (
                            <div
                              key={e.id}
                              className="truncate rounded bg-sky-50 px-1 text-[10px] font-medium text-sky-800"
                            >
                              {e.tripName}
                            </div>
                          ))}
                          {events.length > 2 && (
                            <div className="text-[10px] text-slate-400">+{events.length - 2} more</div>
                          )}
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          <aside className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="text-sm font-semibold text-slate-900">
              {selected
                ? new Date(selected + "T00:00:00").toLocaleDateString("en-IN", {
                    weekday: "long",
                    day: "numeric",
                    month: "long",
                  })
                : "Select a day"}
            </h2>
            <p className="mt-1 text-xs text-slate-400">
              {batches.length} departure{batches.length === 1 ? "" : "s"} this month
            </p>

            {!selected ? (
              <p className="mt-8 text-center text-sm text-slate-400">Click a date with a blue tag.</p>
            ) : selectedBatches.length === 0 ? (
              <p className="mt-8 text-center text-sm text-slate-400">No departures on this day.</p>
            ) : (
              <ul className="mt-4 space-y-3">
                {selectedBatches.map((b) => (
                  <li key={b.id} className="rounded-lg border border-slate-100 p-3">
                    <div className="text-sm font-semibold text-slate-900">{b.tripName}</div>
                    <div className="mt-1 text-xs text-slate-500">
                      {b.seatsBooked}/{b.maxCapacity} seats · {b.fillPercent}% fill
                      {b.guideName ? ` · ${b.guideName}` : " · No guide"}
                    </div>
                    <div className="mt-2 flex gap-2">
                      <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600">
                        {b.status}
                      </span>
                      <Link href="/trips" className="text-[11px] font-medium text-slate-700 underline">
                        Open packages
                      </Link>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </aside>
        </div>
      </main>
    </div>
  );
}

export default function CalendarPage() {
  return (
    <Protected>
      <CalendarView />
    </Protected>
  );
}
