"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  api,
  type Booking,
  type Guide,
  type Trip,
  type TripDetail,
  type UpcomingBatch,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { canManageCatalogue } from "@/lib/rbac";
import { TripForm } from "@/components/trip/TripForm";
import { BatchesPanel } from "@/components/trip/BatchesPanel";
import { TreksShell } from "@/components/treks/TreksShell";
import { TrekEditPanel } from "@/components/treks/TrekEditPanel";
import {
  coverGradient,
  difficultyClass,
  difficultyLabel,
  inferRegion,
  inr,
  loadFeaturedIds,
  pseudoRating,
  saveFeaturedIds,
  seasonHint,
  terrainTags,
  trekBadge,
} from "@/components/treks/trekUi";

type TableTab = "all" | "active" | "draft" | "featured" | "seasonal" | "custom";

export function TreksManagement() {
  const { user } = useAuth();
  const canManage = canManageCatalogue(user?.role);

  const [trips, setTrips] = useState<Trip[]>([]);
  const [batches, setBatches] = useState<UpcomingBatch[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [guides, setGuides] = useState<Guide[]>([]);
  const [selected, setSelected] = useState<TripDetail | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formTrip, setFormTrip] = useState<TripDetail | null>(null);
  const [showBatches, setShowBatches] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [featuredIds, setFeaturedIds] = useState<Set<string>>(new Set());
  const [view, setView] = useState<"grid" | "list">("grid");
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("all");
  const [region, setRegion] = useState("all");
  const [difficulty, setDifficulty] = useState("all");
  const [season, setSeason] = useState("all");
  const [status, setStatus] = useState("all");
  const [featuredOnly, setFeaturedOnly] = useState("all");
  const [sort, setSort] = useState("updated");
  const [tableTab, setTableTab] = useState<TableTab>("all");
  const [selectedRows, setSelectedRows] = useState<Set<string>>(new Set());

  useEffect(() => {
    setFeaturedIds(loadFeaturedIds());
  }, []);

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    Promise.all([
      api.getTrips(canManage ? false : true),
      api.getUpcomingBatches({ limit: 200 }),
      api.getBookings().catch(() => [] as Booking[]),
      api.getGuides(canManage ? false : true).catch(() => [] as Guide[]),
    ])
      .then(([tripList, batchList, bookingList, guideList]) => {
        setTrips(tripList);
        setBatches(batchList);
        setBookings(bookingList);
        setGuides(guideList);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load treks"))
      .finally(() => setLoading(false));
  }, [canManage]);

  useEffect(() => {
    reload();
  }, [reload]);

  const batchCountByTrip = useMemo(() => {
    const m = new Map<string, number>();
    for (const b of batches) {
      m.set(b.tripId, (m.get(b.tripId) ?? 0) + 1);
    }
    return m;
  }, [batches]);

  const bookingRevenue = useMemo(
    () => bookings.reduce((s, b) => s + Number(b.netAmount ?? b.totalAmount ?? 0), 0),
    [bookings],
  );

  const regions = useMemo(
    () => [...new Set(trips.map((t) => inferRegion(t.name)))].sort(),
    [trips],
  );

  function toggleFeatured(id: string) {
    setFeaturedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      saveFeaturedIds(next);
      return next;
    });
  }

  async function openTrip(t: Trip) {
    try {
      const detail = await api.getTrip(t.id as string);
      setSelected(detail);
      setShowBatches(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load trek");
    }
  }

  async function toggleActive(t: Trip) {
    if (!canManage || !t.id) return;
    try {
      const updated = await api.updateTrip(t.id, { active: !t.active });
      setTrips((list) => list.map((x) => (x.id === t.id ? { ...x, active: updated.active } : x)));
      if (selected?.id === t.id) setSelected(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not update status");
    }
  }

  const filtered = useMemo(() => {
    let list = [...trips];
    const qq = q.trim().toLowerCase();
    if (qq) {
      list = list.filter(
        (t) =>
          t.name?.toLowerCase().includes(qq) ||
          t.slug?.toLowerCase().includes(qq) ||
          t.category?.toLowerCase().includes(qq),
      );
    }
    if (category !== "all") list = list.filter((t) => t.category === category);
    if (region !== "all") list = list.filter((t) => inferRegion(t.name) === region);
    if (difficulty !== "all") list = list.filter((t) => (t.difficulty ?? "MODERATE") === difficulty);
    if (season !== "all")
      list = list.filter((t) => seasonHint(t).toLowerCase().includes(season.toLowerCase()));
    if (status === "active") list = list.filter((t) => t.active);
    if (status === "draft") list = list.filter((t) => !t.active);
    if (featuredOnly === "yes") list = list.filter((t) => t.id && featuredIds.has(t.id));
    if (featuredOnly === "no") list = list.filter((t) => t.id && !featuredIds.has(t.id));

    if (tableTab === "active") list = list.filter((t) => t.active);
    if (tableTab === "draft") list = list.filter((t) => !t.active);
    if (tableTab === "featured") list = list.filter((t) => t.id && featuredIds.has(t.id));
    if (tableTab === "seasonal") list = list.filter((t) => Number(t.durationDays) >= 6);
    if (tableTab === "custom") list = list.filter((t) => t.category === "CUSTOM" || t.bookingType === "CUSTOM_FIT");

    list.sort((a, b) => {
      if (sort === "price-asc") return Number(a.baseCost) - Number(b.baseCost);
      if (sort === "price-desc") return Number(b.baseCost) - Number(a.baseCost);
      if (sort === "name") return (a.name ?? "").localeCompare(b.name ?? "");
      if (sort === "batches")
        return (batchCountByTrip.get(b.id ?? "") ?? 0) - (batchCountByTrip.get(a.id ?? "") ?? 0);
      return (b.name ?? "").localeCompare(a.name ?? "");
    });
    return list;
  }, [
    trips,
    q,
    category,
    region,
    difficulty,
    season,
    status,
    featuredOnly,
    tableTab,
    sort,
    featuredIds,
    batchCountByTrip,
  ]);

  const activeCount = trips.filter((t) => t.active).length;
  const draftCount = trips.length - activeCount;
  const packageCount =
    trips.filter((t) => t.category === "CUSTOM" || t.bookingType === "CUSTOM_FIT").length +
    trips.length;
  const featuredCount = trips.filter((t) => t.id && featuredIds.has(t.id)).length;
  const avgRating =
    trips.length === 0
      ? 0
      : Math.round((trips.reduce((s, t) => s + pseudoRating(t.id), 0) / trips.length) * 10) / 10;

  function resetFilters() {
    setQ("");
    setCategory("all");
    setRegion("all");
    setDifficulty("all");
    setSeason("all");
    setStatus("all");
    setFeaturedOnly("all");
    setSort("updated");
    setTableTab("all");
  }

  const cardTrips = filtered.slice(0, 8);

  return (
    <TreksShell
      title="Treks & Packages"
      subtitle="Manage trek catalogue, packages, batches, pricing, media, SEO and reviews in one place"
      breadcrumb="Manage Treks"
      actions={
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Import Treks ▾
          </button>
          {canManage && (
            <button
              type="button"
              onClick={() => {
                setFormTrip(null);
                setShowForm(true);
              }}
              className="rounded-lg bg-sky-600 px-3 py-2 text-sm font-semibold text-white hover:bg-sky-500"
            >
              + Add New Trek
            </button>
          )}
          <button
            type="button"
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            More Actions ▾
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
          {
            label: "Total Treks",
            value: String(trips.length),
            delta: "+12%",
            hint: `Active: ${activeCount} | Draft: ${draftCount}`,
          },
          {
            label: "Total Packages",
            value: String(packageCount),
            delta: "+28%",
            hint: "Group | Custom | Seasonal",
          },
          {
            label: "Upcoming Batches",
            value: String(batches.length),
            delta: "+32%",
            hint: "Next 30 Days",
          },
          {
            label: "Total Bookings",
            value: bookings.length.toLocaleString("en-IN"),
            delta: "+26%",
            hint: `${inr(bookingRevenue)} Revenue`,
          },
          {
            label: "Avg. Rating",
            value: avgRating ? `${avgRating} ★` : "—",
            delta: "",
            hint: `${Math.max(trips.length * 8, 0)} reviews (est.)`,
          },
          {
            label: "Featured Treks",
            value: String(featuredCount),
            delta: "",
            hint: "On Homepage",
          },
        ].map((k) => (
          <div key={k.label} className="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
            <div className="text-[11px] font-medium uppercase tracking-wide text-slate-400">
              {k.label}
            </div>
            <div className="mt-1 flex items-baseline gap-2">
              <span className="text-2xl font-semibold tabular-nums text-slate-900">{k.value}</span>
              {k.delta && <span className="text-xs font-semibold text-emerald-600">{k.delta}</span>}
            </div>
            <div className="mt-0.5 text-xs text-slate-500">{k.hint}</div>
          </div>
        ))}
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-2 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search treks, packages, slugs…"
          className="min-w-[180px] flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-sky-400"
        />
        {[
          [
            category,
            setCategory,
            [
              ["all", "Category"],
              ["TREK", "Trek"],
              ["PILGRIMAGE", "Pilgrimage"],
              ["LEISURE", "Leisure"],
              ["CUSTOM", "Custom"],
            ],
          ],
          [
            region,
            setRegion,
            [["all", "Region"], ...regions.map((r) => [r, r] as [string, string])],
          ],
          [
            difficulty,
            setDifficulty,
            [
              ["all", "Difficulty"],
              ["EASY", "Easy"],
              ["MODERATE", "Easy-Moderate"],
              ["DIFFICULT", "Moderate"],
              ["VERY_DIFFICULT", "Challenging"],
            ],
          ],
          [
            season,
            setSeason,
            [
              ["all", "Season"],
              ["Weekend", "Weekend"],
              ["Spring", "Spring / Autumn"],
              ["Summer", "Summer / Monsoon"],
            ],
          ],
          [
            status,
            setStatus,
            [
              ["all", "Status"],
              ["active", "Active"],
              ["draft", "Draft"],
            ],
          ],
          [
            featuredOnly,
            setFeaturedOnly,
            [
              ["all", "Featured"],
              ["yes", "Featured only"],
              ["no", "Not featured"],
            ],
          ],
        ].map(([value, setter, options], i) => (
          <select
            key={i}
            value={value as string}
            onChange={(e) => (setter as (v: string) => void)(e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 text-xs font-medium text-slate-700"
          >
            {(options as [string, string][]).map(([v, label]) => (
              <option key={v} value={v}>
                {label}
              </option>
            ))}
          </select>
        ))}
        <button
          type="button"
          onClick={resetFilters}
          className="text-xs font-semibold text-sky-700 hover:underline"
        >
          Reset
        </button>
        <div className="ml-auto flex items-center gap-2">
          <div className="flex rounded-lg border border-slate-200 p-0.5">
            <button
              type="button"
              onClick={() => setView("grid")}
              className={`rounded-md px-2.5 py-1.5 text-xs font-semibold ${
                view === "grid" ? "bg-sky-600 text-white" : "text-slate-500"
              }`}
            >
              Grid
            </button>
            <button
              type="button"
              onClick={() => setView("list")}
              className={`rounded-md px-2.5 py-1.5 text-xs font-semibold ${
                view === "list" ? "bg-sky-600 text-white" : "text-slate-500"
              }`}
            >
              List
            </button>
          </div>
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            className="rounded-lg border border-slate-200 px-2.5 py-2 text-xs font-medium text-slate-700"
          >
            <option value="updated">Latest Updated</option>
            <option value="name">Name A–Z</option>
            <option value="price-asc">Price ↑</option>
            <option value="price-desc">Price ↓</option>
            <option value="batches">Most batches</option>
          </select>
        </div>
      </div>

      <div className={`flex flex-col gap-4 ${selected ? "xl:flex-row" : ""}`}>
        <div className="min-w-0 flex-1 space-y-4">
          {loading && <p className="text-sm text-slate-400">Loading treks…</p>}

          {!loading && view === "grid" && (
            <div className="flex gap-3 overflow-x-auto pb-1">
              {cardTrips.map((t, index) => {
                const featured = !!(t.id && featuredIds.has(t.id));
                const badge = trekBadge(t, featured, index);
                const rating = pseudoRating(t.id);
                return (
                  <button
                    key={t.id}
                    type="button"
                    onClick={() => openTrip(t)}
                    className={`w-56 shrink-0 overflow-hidden rounded-2xl border bg-white text-left shadow-sm transition hover:shadow-md ${
                      selected?.id === t.id ? "border-sky-400 ring-2 ring-sky-100" : "border-slate-200"
                    }`}
                  >
                    <div className="relative h-32" style={{ background: coverGradient(t) }}>
                      {badge && (
                        <span
                          className={`absolute left-2 top-2 rounded-md px-2 py-0.5 text-[10px] font-bold ${badge.className}`}
                        >
                          {badge.label}
                        </span>
                      )}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          if (t.id) toggleFeatured(t.id);
                        }}
                        className="absolute right-2 top-2 rounded-full bg-white/90 px-1.5 py-0.5 text-xs"
                        title="Favorite / Featured"
                      >
                        {featured ? "★" : "♡"}
                      </button>
                    </div>
                    <div className="p-3">
                      <div className="truncate text-sm font-semibold text-slate-900">{t.name}</div>
                      <div className="mt-0.5 text-[11px] text-slate-500">
                        {inferRegion(t.name)} · {t.durationDays}D · {difficultyLabel(t.difficulty)}
                      </div>
                      <div className="mt-1.5 flex flex-wrap gap-1">
                        {terrainTags(t).map((tag) => (
                          <span
                            key={tag}
                            className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-600"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                      <div className="mt-2 flex items-center justify-between text-xs">
                        <span className="font-semibold text-slate-900">
                          {inr(t.baseCost)}
                          <span className="font-normal text-slate-400">/person</span>
                        </span>
                        <span className="text-amber-600">{rating} ★</span>
                      </div>
                      <div className="mt-1 text-[11px] text-slate-400">
                        {batchCountByTrip.get(t.id ?? "") ?? 0} upcoming batches
                      </div>
                      <div className="mt-2 rounded-md bg-sky-50 py-1.5 text-center text-[11px] font-semibold text-sky-700">
                        View Details
                      </div>
                    </div>
                  </button>
                );
              })}
              {cardTrips.length === 0 && (
                <p className="w-full py-8 text-center text-sm text-slate-400">No treks match filters</p>
              )}
            </div>
          )}

          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-wrap gap-1 border-b border-slate-100 px-3 pt-3">
              {(
                [
                  ["all", `All Treks (${trips.length})`],
                  ["active", `Active (${activeCount})`],
                  ["draft", `Draft (${draftCount})`],
                  ["featured", `Featured (${featuredCount})`],
                  ["seasonal", "Seasonal"],
                  ["custom", "Custom Packages"],
                ] as [TableTab, string][]
              ).map(([id, label]) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setTableTab(id)}
                  className={`rounded-t-md px-3 py-2 text-xs font-semibold ${
                    tableTab === id ? "bg-sky-50 text-sky-700" : "text-slate-500 hover:text-slate-800"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[960px] text-left text-xs">
                <thead className="bg-slate-50/80 text-[10px] uppercase tracking-wide text-slate-400">
                  <tr>
                    <th className="px-3 py-2.5">
                      <input
                        type="checkbox"
                        checked={
                          filtered.length > 0 &&
                          filtered.every((t) => t.id && selectedRows.has(t.id))
                        }
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedRows(new Set(filtered.map((t) => t.id!).filter(Boolean)));
                          } else setSelectedRows(new Set());
                        }}
                      />
                    </th>
                    <th className="px-2 py-2.5 font-semibold">#</th>
                    <th className="px-2 py-2.5 font-semibold">Trek / Package</th>
                    <th className="px-2 py-2.5 font-semibold">Region</th>
                    <th className="px-2 py-2.5 font-semibold">Duration</th>
                    <th className="px-2 py-2.5 font-semibold">Difficulty</th>
                    <th className="px-2 py-2.5 font-semibold">Base Price</th>
                    <th className="px-2 py-2.5 font-semibold">Batches</th>
                    <th className="px-2 py-2.5 font-semibold">Status</th>
                    <th className="px-2 py-2.5 font-semibold">Featured</th>
                    <th className="px-2 py-2.5 font-semibold">Updated</th>
                    <th className="px-2 py-2.5 font-semibold">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filtered.map((t, i) => {
                    const featured = !!(t.id && featuredIds.has(t.id));
                    return (
                      <tr
                        key={t.id}
                        className={`cursor-pointer hover:bg-sky-50/40 ${
                          selected?.id === t.id ? "bg-sky-50/70" : ""
                        }`}
                        onClick={() => openTrip(t)}
                      >
                        <td className="px-3 py-2.5" onClick={(e) => e.stopPropagation()}>
                          <input
                            type="checkbox"
                            checked={!!(t.id && selectedRows.has(t.id))}
                            onChange={(e) => {
                              setSelectedRows((prev) => {
                                const next = new Set(prev);
                                if (e.target.checked) next.add(t.id!);
                                else next.delete(t.id!);
                                return next;
                              });
                            }}
                          />
                        </td>
                        <td className="px-2 py-2.5 tabular-nums text-slate-400">{i + 1}</td>
                        <td className="px-2 py-2.5">
                          <div className="flex items-center gap-2">
                            <div
                              className="h-9 w-12 shrink-0 rounded-md"
                              style={{ background: coverGradient(t) }}
                            />
                            <div className="min-w-0">
                              <div className="truncate font-semibold text-slate-800">{t.name}</div>
                              <div className="truncate text-[10px] text-slate-400">{t.slug}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-2 py-2.5 text-slate-600">{inferRegion(t.name)}</td>
                        <td className="px-2 py-2.5 tabular-nums">{t.durationDays}D</td>
                        <td className="px-2 py-2.5">
                          <span
                            className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${difficultyClass(t.difficulty)}`}
                          >
                            {difficultyLabel(t.difficulty)}
                          </span>
                        </td>
                        <td className="px-2 py-2.5 font-medium tabular-nums">{inr(t.baseCost)}</td>
                        <td className="px-2 py-2.5 tabular-nums">
                          {batchCountByTrip.get(t.id ?? "") ?? 0}
                        </td>
                        <td className="px-2 py-2.5" onClick={(e) => e.stopPropagation()}>
                          <button
                            type="button"
                            disabled={!canManage}
                            onClick={() => toggleActive(t)}
                            className={`relative h-5 w-9 rounded-full transition ${
                              t.active ? "bg-emerald-500" : "bg-slate-300"
                            }`}
                            title={t.active ? "Active" : "Draft"}
                          >
                            <span
                              className={`absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition ${
                                t.active ? "left-4" : "left-0.5"
                              }`}
                            />
                          </button>
                        </td>
                        <td className="px-2 py-2.5" onClick={(e) => e.stopPropagation()}>
                          <button
                            type="button"
                            onClick={() => t.id && toggleFeatured(t.id)}
                            className={`text-base ${featured ? "text-amber-500" : "text-slate-300"}`}
                          >
                            ★
                          </button>
                        </td>
                        <td className="px-2 py-2.5 text-slate-400">—</td>
                        <td className="px-2 py-2.5" onClick={(e) => e.stopPropagation()}>
                          <div className="flex items-center gap-1">
                            {canManage && (
                              <button
                                type="button"
                                onClick={async () => {
                                  const detail = await api.getTrip(t.id as string);
                                  setFormTrip(detail);
                                  setShowForm(true);
                                }}
                                className="rounded px-1.5 py-1 text-slate-500 hover:bg-slate-100 hover:text-sky-700"
                                title="Edit"
                              >
                                ✎
                              </button>
                            )}
                            <button
                              type="button"
                              onClick={() => openTrip(t)}
                              className="rounded px-1.5 py-1 text-slate-500 hover:bg-slate-100"
                              title="More"
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
                      <td colSpan={12} className="px-4 py-10 text-center text-slate-400">
                        No treks match the current filters
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {selected && showBatches && (
            <BatchesPanel
              trip={selected}
              guides={guides}
              canManage={canManage}
              onChanged={async () => {
                if (selected.id) setSelected(await api.getTrip(selected.id));
                reload();
              }}
            />
          )}
        </div>

        {selected && (
          <TrekEditPanel
            trip={selected}
            featured={!!(selected.id && featuredIds.has(selected.id))}
            canManage={canManage}
            onClose={() => setSelected(null)}
            onSaved={(detail) => {
              setSelected(detail);
              setTrips((list) =>
                list.map((x) =>
                  x.id === detail.id
                    ? {
                        ...x,
                        name: detail.name,
                        category: detail.category,
                        difficulty: detail.difficulty,
                        durationDays: detail.durationDays,
                        active: detail.active,
                        baseCost: detail.baseCost,
                      }
                    : x,
                ),
              );
            }}
            onToggleFeatured={() => selected.id && toggleFeatured(selected.id)}
            onOpenFullEdit={() => {
              setFormTrip(selected);
              setShowForm(true);
            }}
          />
        )}
      </div>

      {selected && (
        <div className="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setShowBatches((v) => !v)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
          >
            {showBatches ? "Hide batches panel" : "Show batches for selected trek"}
          </button>
        </div>
      )}

      {showForm && (
        <TripForm
          trip={formTrip}
          onSaved={async () => {
            await reload();
            if (formTrip?.id) {
              setSelected(await api.getTrip(formTrip.id));
            }
          }}
          onClose={() => {
            setShowForm(false);
            setFormTrip(null);
          }}
        />
      )}
    </TreksShell>
  );
}
