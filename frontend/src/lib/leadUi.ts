import type { Lead } from "@/lib/api";

/** Display helpers mapping CRM lead fields → Trekora-style Leads Management UI. */

export const STAGE_LABEL: Record<string, string> = {
  NEW: "New Lead",
  INTERESTED: "Interested",
  QUOTATION_SENT: "Proposal Sent",
  BOOKING_CONFIRMED: "Booking Confirmed",
  LOST: "Lost",
};

export const STAGE_STYLE: Record<string, string> = {
  NEW: "bg-sky-100 text-sky-800",
  INTERESTED: "bg-orange-100 text-orange-800",
  QUOTATION_SENT: "bg-violet-100 text-violet-800",
  BOOKING_CONFIRMED: "bg-emerald-100 text-emerald-800",
  LOST: "bg-rose-100 text-rose-700",
};

export const SOURCE_LABEL: Record<string, string> = {
  INSTAGRAM: "Instagram Ads",
  FACEBOOK_ADS: "Meta Ads",
  GOOGLE_ADS: "Google Ads",
  WEBSITE: "Website / Organic",
  REFERRAL: "Referral",
  WHATSAPP: "WhatsApp",
  JUSTDIAL: "JustDial",
  WALK_IN: "Walk-in",
  B2B: "B2B",
  EXISTING_CUSTOMER: "Existing customer",
  OTHER: "Others",
};

export const SOURCE_TABS: { key: string; label: string; source?: string }[] = [
  { key: "all", label: "All Leads" },
  { key: "INSTAGRAM", label: "Instagram", source: "INSTAGRAM" },
  { key: "GOOGLE_ADS", label: "Google Ads", source: "GOOGLE_ADS" },
  { key: "FACEBOOK_ADS", label: "Meta Ads", source: "FACEBOOK_ADS" },
  { key: "WEBSITE", label: "Website", source: "WEBSITE" },
  { key: "WHATSAPP", label: "WhatsApp", source: "WHATSAPP" },
  { key: "REFERRAL", label: "Referral", source: "REFERRAL" },
  { key: "OTHER", label: "More Sources", source: "OTHER" },
];

export const SOURCE_COLORS: Record<string, string> = {
  INSTAGRAM: "#e11d48",
  FACEBOOK_ADS: "#2563eb",
  GOOGLE_ADS: "#16a34a",
  WEBSITE: "#0ea5e9",
  REFERRAL: "#a855f7",
  WHATSAPP: "#22c55e",
  OTHER: "#94a3b8",
  JUSTDIAL: "#f59e0b",
  WALK_IN: "#64748b",
  B2B: "#0891b2",
  EXISTING_CUSTOMER: "#78716c",
};

export function initials(name?: string | null): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

export function relativeTime(iso?: string | null): string {
  if (!iso) return "—";
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return "—";
  const diff = Date.now() - t;
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Just now";
  if (mins < 60) return `${mins} min ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs} hour${hrs === 1 ? "" : "s"} ago`;
  const days = Math.floor(hrs / 24);
  if (days < 7) return `${days} day${days === 1 ? "" : "s"} ago`;
  return new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}

export function inr(n: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(n || 0);
}

export function formatPct(n: number): string {
  return `${(Number.isFinite(n) ? n : 0).toFixed(1)}%`;
}

/** Derive a 0–100 lead score from heat + enrichment fields (no separate score column in DB). */
export function leadScore(lead: Pick<Lead, "heat" | "budget" | "numPersons" | "status" | "followUpDate" | "email">): number {
  let score = lead.heat === "HOT" ? 82 : lead.heat === "WARM" ? 58 : 34;
  if (lead.budget != null && Number(lead.budget) >= 15000) score += 8;
  else if (lead.budget != null && Number(lead.budget) >= 8000) score += 4;
  if ((lead.numPersons ?? 0) >= 4) score += 5;
  else if ((lead.numPersons ?? 0) >= 2) score += 2;
  if (lead.email) score += 3;
  if (lead.followUpDate) score += 2;
  if (lead.status === "QUOTATION_SENT") score += 6;
  if (lead.status === "BOOKING_CONFIRMED") score = Math.max(score, 92);
  if (lead.status === "LOST") score = Math.min(score, 25);
  return Math.max(0, Math.min(99, Math.round(score)));
}

export function scoreStyle(score: number): string {
  if (score >= 75) return "bg-emerald-100 text-emerald-800";
  if (score >= 50) return "bg-amber-100 text-amber-800";
  return "bg-slate-100 text-slate-600";
}

export function buyingIntent(score: number): { label: string; pct: number; bar: string } {
  if (score >= 75) return { label: "High", pct: Math.min(100, score), bar: "bg-rose-500" };
  if (score >= 50) return { label: "Medium", pct: score, bar: "bg-amber-500" };
  return { label: "Low", pct: Math.max(20, score), bar: "bg-slate-400" };
}

export function budgetRangeLabel(budget?: number | null): string {
  if (budget == null) return "Not set";
  const b = Number(budget);
  if (b < 8000) return "Under ₹8k";
  if (b < 12000) return "₹8k – 12k";
  if (b < 18000) return "₹10k – 15k";
  if (b < 25000) return "₹15k – 25k";
  return "₹25k+";
}
