import type { Trip } from "@/lib/api";

const FEATURED_KEY = "toh.featuredTrips";

export function inr(v: number | undefined | null) {
  return "₹" + Number(v ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export function loadFeaturedIds(): Set<string> {
  try {
    const raw = localStorage.getItem(FEATURED_KEY);
    if (!raw) return new Set();
    return new Set(JSON.parse(raw) as string[]);
  } catch {
    return new Set();
  }
}

export function saveFeaturedIds(ids: Set<string>) {
  try {
    localStorage.setItem(FEATURED_KEY, JSON.stringify([...ids]));
  } catch {
    /* ignore */
  }
}

export function inferRegion(name?: string | null): string {
  const n = (name ?? "").toLowerCase();
  if (/kedar|har ki|valley of flowers|roopkund|chopta|tungnath|nanda|uttarakhand|deoriatal|ali bedni/.test(n))
    return "Uttarakhand";
  if (/hampta|triund|bir|billling|spiti|manali|kullu|kasol|himachal|pin parvati/.test(n))
    return "Himachal Pradesh";
  if (/ladakh|pangong|nubra|markha|stok/.test(n)) return "Ladakh";
  if (/sikkim|goecha|kanchenjunga/.test(n)) return "Sikkim";
  if (/nepal|everest|annapurna|langtang/.test(n)) return "Nepal";
  if (/kashmir|gulmarg|sonamarg/.test(n)) return "Kashmir";
  return "Uttarakhand";
}

export function difficultyLabel(d?: string | null) {
  switch (d) {
    case "EASY":
      return "Easy";
    case "MODERATE":
      return "Easy-Moderate";
    case "DIFFICULT":
      return "Moderate";
    case "VERY_DIFFICULT":
      return "Challenging";
    default:
      return "Moderate";
  }
}

export function difficultyClass(d?: string | null) {
  switch (d) {
    case "EASY":
      return "bg-emerald-50 text-emerald-700";
    case "MODERATE":
      return "bg-lime-50 text-lime-700";
    case "DIFFICULT":
      return "bg-amber-50 text-amber-700";
    case "VERY_DIFFICULT":
      return "bg-rose-50 text-rose-700";
    default:
      return "bg-slate-100 text-slate-600";
  }
}

/** Deterministic cover from trip identity (no external dependency). */
export function coverGradient(trip: Pick<Trip, "id" | "name" | "category">) {
  const seed = [...(trip.id || trip.name || "trek")].reduce((a, c) => a + c.charCodeAt(0), 0);
  const hues = [
    ["#0f766e", "#164e63"],
    ["#1d4ed8", "#0e7490"],
    ["#b45309", "#9a3412"],
    ["#4c1d95", "#1e3a8a"],
    ["#065f46", "#14532d"],
    ["#9f1239", "#7c2d12"],
  ];
  const [a, b] = hues[seed % hues.length];
  return `linear-gradient(135deg, ${a}, ${b})`;
}

export function trekBadge(trip: Trip, featured: boolean, index: number): { label: string; className: string } | null {
  if (featured) return { label: "Featured", className: "bg-amber-400 text-amber-950" };
  if (trip.category === "CUSTOM") return { label: "Custom", className: "bg-violet-500 text-white" };
  if (!trip.active) return { label: "Draft", className: "bg-slate-500 text-white" };
  if (index === 0) return { label: "Bestseller", className: "bg-rose-500 text-white" };
  if (index === 1) return { label: "Popular", className: "bg-sky-500 text-white" };
  if (index === 2) return { label: "New", className: "bg-emerald-500 text-white" };
  if (index === 3) return { label: "Trending", className: "bg-violet-500 text-white" };
  return null;
}

export function pseudoRating(id?: string) {
  const seed = [...(id || "x")].reduce((a, c) => a + c.charCodeAt(0), 0);
  return Math.round((4.2 + (seed % 8) / 10) * 10) / 10;
}

export function terrainTags(trip: Trip): string[] {
  const tags: string[] = [];
  const n = (trip.name ?? "").toLowerCase();
  if (/snow|kedar|roopkund|hampta|winter/.test(n)) tags.push("Snow Trek");
  if (/forest|chopta|triund|deoria/.test(n)) tags.push("Forest");
  if (/summit|peak|stok|nanda/.test(n)) tags.push("Summit");
  if (trip.category === "PILGRIMAGE") tags.push("Pilgrimage");
  if (trip.category === "LEISURE") tags.push("Leisure");
  if (tags.length === 0) tags.push("Himalaya", "Scenic");
  return tags.slice(0, 3);
}

export function seasonHint(trip: Trip): string {
  const d = Number(trip.durationDays ?? 0);
  if (d <= 3) return "Weekend";
  if (d <= 6) return "Spring / Autumn";
  return "Summer / Monsoon";
}
