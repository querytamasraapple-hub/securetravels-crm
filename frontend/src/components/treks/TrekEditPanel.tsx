"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { api, type TripDetail, ApiClientError } from "@/lib/api";
import {
  coverGradient,
  difficultyLabel,
  inferRegion,
  inr,
} from "@/components/treks/trekUi";

const DIFFICULTIES = ["EASY", "MODERATE", "DIFFICULT", "VERY_DIFFICULT"] as const;
const CATEGORIES = ["TREK", "PILGRIMAGE", "LEISURE", "CUSTOM"] as const;

export function TrekEditPanel({
  trip,
  featured,
  canManage,
  onClose,
  onSaved,
  onToggleFeatured,
  onOpenFullEdit,
}: {
  trip: TripDetail;
  featured: boolean;
  canManage: boolean;
  onClose: () => void;
  onSaved: (detail: TripDetail) => void;
  onToggleFeatured: () => void;
  onOpenFullEdit: () => void;
}) {
  const [tab, setTab] = useState<"details" | "itinerary" | "media" | "pricing">("details");
  const [name, setName] = useState(trip.name ?? "");
  const [description, setDescription] = useState(
    (trip.itinerary ?? "").split("\n")[0]?.slice(0, 180) || `${trip.name} — Himalayan adventure`,
  );
  const [region, setRegion] = useState(inferRegion(trip.name));
  const [category, setCategory] = useState(trip.category ?? "TREK");
  const [difficulty, setDifficulty] = useState(trip.difficulty ?? "MODERATE");
  const [durationDays, setDurationDays] = useState(String(trip.durationDays ?? 5));
  const [active, setActive] = useState(!!trip.active);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setName(trip.name ?? "");
    setDescription(
      (trip.itinerary ?? "").split("\n")[0]?.slice(0, 180) || `${trip.name} — Himalayan adventure`,
    );
    setRegion(inferRegion(trip.name));
    setCategory(trip.category ?? "TREK");
    setDifficulty(trip.difficulty ?? "MODERATE");
    setDurationDays(String(trip.durationDays ?? 5));
    setActive(!!trip.active);
    setTab("details");
    setError(null);
  }, [trip]);

  async function save(e: FormEvent) {
    e.preventDefault();
    if (!canManage || !trip.id) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await api.updateTrip(trip.id, {
        name,
        category: category as (typeof CATEGORIES)[number],
        difficulty: difficulty as (typeof DIFFICULTIES)[number],
        durationDays: Number(durationDays),
        active,
        itinerary: trip.itinerary
          ? `${description}\n${trip.itinerary.split("\n").slice(1).join("\n")}`.trim()
          : description,
      });
      onSaved(updated);
    } catch (err) {
      if (err instanceof ApiClientError && err.body.fieldErrors?.length) {
        setError(err.body.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(" · "));
      } else {
        setError(err instanceof Error ? err.message : "Could not save");
      }
    } finally {
      setSaving(false);
    }
  }

  const tabs = [
    ["details", "Trek Details"],
    ["itinerary", "Itinerary"],
    ["media", "Media"],
    ["pricing", "Pricing"],
  ] as const;

  return (
    <aside className="flex w-full max-w-md shrink-0 flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-lg xl:w-[380px]">
      <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
        <div className="min-w-0">
          <div className="truncate text-sm font-semibold text-slate-900">{trip.name}</div>
          <div className="text-[11px] text-slate-400">Quick edit panel</div>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="rounded-md px-2 py-1 text-slate-400 hover:bg-slate-50 hover:text-slate-700"
        >
          ✕
        </button>
      </div>

      <div className="flex gap-1 overflow-x-auto border-b border-slate-100 px-2 pt-2">
        {tabs.map(([id, label]) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`shrink-0 rounded-t-md px-3 py-2 text-xs font-semibold ${
              tab === id ? "bg-sky-50 text-sky-700" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {error && (
          <div className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
            {error}
          </div>
        )}

        {tab === "media" && (
          <div>
            <div
              className="relative mb-3 h-36 overflow-hidden rounded-xl"
              style={{ background: coverGradient(trip) }}
            >
              <div className="absolute inset-0 flex items-end bg-gradient-to-t from-black/50 to-transparent p-3">
                <button
                  type="button"
                  className="rounded-md bg-white/90 px-2.5 py-1 text-[11px] font-semibold text-slate-800"
                >
                  Change Cover Image
                </button>
              </div>
            </div>
            <p className="text-xs text-slate-500">Gallery · 1 cover · Manage full media library</p>
            <Link
              href="/trips/media"
              className="mt-2 inline-block text-xs font-semibold text-sky-700 hover:underline"
            >
              Open Images &amp; Media →
            </Link>
          </div>
        )}

        {tab === "itinerary" && (
          <div className="space-y-2">
            <p className="whitespace-pre-wrap text-sm text-slate-700">
              {trip.itinerary || "No itinerary yet. Add day-wise plan from Itineraries & Days."}
            </p>
            <Link
              href="/trips/itineraries"
              className="inline-block text-xs font-semibold text-sky-700 hover:underline"
            >
              Manage Itinerary →
            </Link>
          </div>
        )}

        {tab === "pricing" && (
          <div className="space-y-3 text-sm">
            <div className="rounded-xl border border-slate-100 bg-slate-50 px-3 py-3">
              <div className="text-[11px] uppercase tracking-wide text-slate-400">Base price</div>
              <div className="text-lg font-semibold tabular-nums">{inr(trip.baseCost)} / person</div>
            </div>
            <div className="text-xs text-slate-500">
              Booking type: {trip.bookingType === "FIXED_BATCH" ? "Fixed batch" : "Custom fit"} ·{" "}
              {difficultyLabel(trip.difficulty)}
            </div>
            <Link
              href="/trips/pricing"
              className="inline-block text-xs font-semibold text-sky-700 hover:underline"
            >
              Update Pricing →
            </Link>
            <Link
              href="/batches"
              className="ml-3 inline-block text-xs font-semibold text-sky-700 hover:underline"
            >
              Manage Batches →
            </Link>
          </div>
        )}

        {tab === "details" && (
          <form onSubmit={save} className="space-y-3">
            <div
              className="relative h-28 overflow-hidden rounded-xl"
              style={{ background: coverGradient(trip) }}
            >
              <div className="absolute inset-0 flex items-end bg-gradient-to-t from-black/45 to-transparent p-3">
                <span className="text-[11px] font-semibold text-white">Cover preview</span>
              </div>
            </div>

            <label className="block text-xs font-medium text-slate-600">
              Trek Name
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={!canManage}
                className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-sky-400"
              />
            </label>

            <label className="block text-xs font-medium text-slate-600">
              Short Description
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={!canManage}
                rows={3}
                className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-sky-400"
              />
            </label>

            <div className="grid grid-cols-2 gap-2">
              <label className="block text-xs font-medium text-slate-600">
                Region
                <select
                  value={region}
                  onChange={(e) => setRegion(e.target.value)}
                  disabled={!canManage}
                  className="mt-1 w-full rounded-lg border border-slate-200 px-2 py-2 text-sm"
                >
                  {["Uttarakhand", "Himachal Pradesh", "Ladakh", "Sikkim", "Nepal", "Kashmir"].map(
                    (r) => (
                      <option key={r}>{r}</option>
                    ),
                  )}
                </select>
              </label>
              <label className="block text-xs font-medium text-slate-600">
                Trek Category
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value as typeof category)}
                  disabled={!canManage}
                  className="mt-1 w-full rounded-lg border border-slate-200 px-2 py-2 text-sm"
                >
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-xs font-medium text-slate-600">
                Difficulty Level
                <select
                  value={difficulty}
                  onChange={(e) => setDifficulty(e.target.value as typeof difficulty)}
                  disabled={!canManage}
                  className="mt-1 w-full rounded-lg border border-slate-200 px-2 py-2 text-sm"
                >
                  {DIFFICULTIES.map((d) => (
                    <option key={d} value={d}>
                      {difficultyLabel(d)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-xs font-medium text-slate-600">
                Duration (days)
                <input
                  type="number"
                  min={1}
                  value={durationDays}
                  onChange={(e) => setDurationDays(e.target.value)}
                  disabled={!canManage}
                  className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
                />
              </label>
            </div>

            <div className="space-y-2 rounded-xl border border-slate-100 bg-slate-50 p-3">
              <label className="flex items-center justify-between text-xs font-medium text-slate-700">
                <span>Featured Trek (Show on homepage)</span>
                <button
                  type="button"
                  onClick={onToggleFeatured}
                  disabled={!canManage}
                  className={`relative h-5 w-9 rounded-full transition ${
                    featured ? "bg-sky-500" : "bg-slate-300"
                  }`}
                >
                  <span
                    className={`absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition ${
                      featured ? "left-4" : "left-0.5"
                    }`}
                  />
                </button>
              </label>
              <label className="flex items-center justify-between text-xs font-medium text-slate-700">
                <span>Active (Visible on website)</span>
                <button
                  type="button"
                  onClick={() => setActive((a) => !a)}
                  disabled={!canManage}
                  className={`relative h-5 w-9 rounded-full transition ${
                    active ? "bg-emerald-500" : "bg-slate-300"
                  }`}
                >
                  <span
                    className={`absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition ${
                      active ? "left-4" : "left-0.5"
                    }`}
                  />
                </button>
              </label>
            </div>

            {canManage && (
              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 rounded-lg bg-sky-600 px-3 py-2 text-sm font-semibold text-white hover:bg-sky-500 disabled:opacity-60"
                >
                  {saving ? "Saving…" : "Save Changes"}
                </button>
                <button
                  type="button"
                  onClick={onOpenFullEdit}
                  className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  Preview
                </button>
              </div>
            )}
          </form>
        )}

        <div className="mt-5 border-t border-slate-100 pt-4">
          <div className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-slate-400">
            Quick Actions
          </div>
          <div className="space-y-1">
            {[
              ["/trips/bulk", "Clone Trek"],
              ["/trips/packages", "Create Similar Package"],
              ["/batches", "Manage Batches"],
              ["/trips/pricing", "Update Pricing"],
              ["/trips/itineraries", "Manage Itinerary"],
              ["/trips/highlights", "Add Highlights"],
              ["/trips/faqs", "Manage FAQs"],
              ["/trips/seo", "SEO Settings"],
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
        </div>
      </div>
    </aside>
  );
}
