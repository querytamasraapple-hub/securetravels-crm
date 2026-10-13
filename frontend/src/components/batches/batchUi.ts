import type { Trip, UpcomingBatch } from "@/lib/api";
import { coverGradient, inferRegion } from "@/components/treks/trekUi";

export type UiBatchStatus =
  | "Upcoming"
  | "Preparing"
  | "Confirmed"
  | "Starting"
  | "On Trek"
  | "Completed"
  | "Cancelled";

export type BatchTab = "all" | "upcoming" | "ongoing" | "completed" | "cancelled";

export function parseDate(iso?: string | null) {
  if (!iso) return null;
  const d = new Date(iso.includes("T") ? iso : `${iso}T00:00:00`);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function formatShort(iso?: string | null) {
  const d = parseDate(iso);
  if (!d) return "—";
  return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

export function addDays(iso: string, days: number) {
  const d = parseDate(iso);
  if (!d) return iso;
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}

export function daysUntil(iso: string, today = new Date()) {
  const d = parseDate(iso);
  if (!d) return 999;
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  return Math.round((d.getTime() - start.getTime()) / 86400000);
}

export function batchCode(b: Pick<UpcomingBatch, "id" | "tripName" | "departureDate">) {
  const name = (b.tripName || "XX").replace(/[^A-Za-z]/g, "");
  const prefix = (name.slice(0, 1) + (name.slice(1, 2) || "X")).toUpperCase();
  const year = (b.departureDate || "2026").slice(2, 4);
  const seed = [...(b.id || "0")].reduce((a, c) => a + c.charCodeAt(0), 0);
  const n = String((seed % 90) + 1).padStart(2, "0");
  return `${prefix}${year}-${n}`;
}

export function uiStatus(b: UpcomingBatch, durationDays = 5): UiBatchStatus {
  const api = (b.status || "").toUpperCase();
  if (api === "CANCELLED") return "Cancelled";
  if (api === "CLOSED") return "Completed";

  const until = daysUntil(b.departureDate);
  const end = until + Math.max(1, durationDays) - 1;

  if (until <= 0 && end >= 0) return "On Trek";
  if (api === "READY_FOR_DEPARTURE" || until === 0) return "Starting";
  if (until === 1) return "Preparing";
  if (until <= 7 && Number(b.fillPercent) >= 70) return "Confirmed";
  if (until < 0) return "Completed";
  return "Upcoming";
}

export function statusClass(s: UiBatchStatus) {
  switch (s) {
    case "On Trek":
      return "bg-emerald-50 text-emerald-700";
    case "Confirmed":
      return "bg-green-50 text-green-700";
    case "Starting":
      return "bg-sky-50 text-sky-700";
    case "Preparing":
      return "bg-amber-50 text-amber-700";
    case "Upcoming":
      return "bg-blue-50 text-blue-700";
    case "Completed":
      return "bg-slate-100 text-slate-600";
    case "Cancelled":
      return "bg-rose-50 text-rose-700";
  }
}

export function tabForStatus(s: UiBatchStatus): BatchTab {
  if (s === "Cancelled") return "cancelled";
  if (s === "Completed") return "completed";
  if (s === "On Trek" || s === "Starting") return "ongoing";
  return "upcoming";
}

export function permitStatus(b: UpcomingBatch, ui: UiBatchStatus) {
  if (ui === "Cancelled") return { label: "Pending", className: "bg-rose-50 text-rose-700" };
  if (ui === "On Trek" || ui === "Completed" || ui === "Confirmed")
    return { label: "Approved", className: "bg-emerald-50 text-emerald-700" };
  if (ui === "Starting" || ui === "Preparing")
    return { label: "In Process", className: "bg-amber-50 text-amber-700" };
  const seed = [...(b.id || "")].reduce((a, c) => a + c.charCodeAt(0), 0);
  if (seed % 5 === 0) return { label: "Pending", className: "bg-rose-50 text-rose-700" };
  if (seed % 3 === 0) return { label: "In Process", className: "bg-amber-50 text-amber-700" };
  return { label: "Approved", className: "bg-emerald-50 text-emerald-700" };
}

export function transportLabel(plan?: string | null, tripName?: string) {
  if (plan && plan.trim()) {
    const p = plan.toLowerCase();
    if (p.includes("innova")) return "Innova";
    if (p.includes("tempo")) return "Tempo Traveller";
    if (p.includes("bus")) return "Bus";
    return plan.length > 22 ? `${plan.slice(0, 20)}…` : plan;
  }
  const seed = [...(tripName || "x")].reduce((a, c) => a + c.charCodeAt(0), 0);
  return seed % 2 === 0 ? "Tempo Traveller" : "Innova";
}

export function baseCampFor(tripName?: string) {
  const n = (tripName || "").toLowerCase();
  if (/kedar|sankri/.test(n)) return "Sankri";
  if (/chopta|tungnath/.test(n)) return "Chopta";
  if (/har ki|sankri|osla/.test(n)) return "Sankri";
  if (/hampta|manali/.test(n)) return "Jobra";
  if (/triund/.test(n)) return "McLeod Ganj";
  return inferRegion(tripName).split(" ")[0] || "Basecamp";
}

export function seasonType(category?: string, tripName?: string) {
  const n = (tripName || "").toLowerCase();
  if (/snow|winter|kedar/.test(n)) return "Winter Trek";
  if (category === "PILGRIMAGE") return "Pilgrimage";
  if (category === "LEISURE") return "Leisure";
  if (category === "CUSTOM") return "Custom Package";
  return "Group Trek";
}

export function initials(name?: string | null) {
  if (!name) return "?";
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

export function tripCover(trip?: Pick<Trip, "id" | "name" | "category"> | null, tripName?: string) {
  return coverGradient(trip ?? { id: tripName, name: tripName, category: "TREK" });
}

export function liveTrackingSteps(ui: UiBatchStatus, tripName?: string) {
  const camp = baseCampFor(tripName);
  if (ui !== "On Trek" && ui !== "Starting") {
    return [
      { time: "T-1 day", text: "Pre-departure briefing scheduled", done: ui === "Confirmed" || ui === "Preparing" },
      { time: "Departure morning", text: `Assemble at ${camp}`, done: false },
      { time: "Day 1", text: "Trek begins — check-in & gear issue", done: false },
    ];
  }
  return [
    { time: "06:00 AM", text: `Batch started (${camp})`, done: true },
    { time: "09:30 AM", text: "Trekking en route — all travellers accounted", done: true },
    { time: "01:00 PM", text: "Lunch break", done: true },
    { time: "05:30 PM", text: "Reached campsite", done: ui === "On Trek" },
    { time: "Tomorrow", text: "Summit attempt (start ~3:00 AM)", done: false },
  ];
}
