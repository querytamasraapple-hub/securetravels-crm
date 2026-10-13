"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { FINANCE_ERP_TABS, financeNavForRole } from "@/lib/rbac";
import { NavIconMark } from "@/components/NavIcons";

export function FinanceShell({
  title,
  subtitle,
  actions,
  showTabs = true,
  children,
}: {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
  showTabs?: boolean;
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const nav = financeNavForRole(user?.role);
  const primary = nav.slice(0, 12);
  const more = nav.slice(12);

  return (
    <div className="flex min-h-screen bg-[#f3f5f8] text-slate-900">
      <aside className="flex w-64 shrink-0 flex-col overflow-y-auto border-r border-slate-800 bg-slate-950 text-slate-200">
        <div className="border-b border-slate-800 px-5 py-5">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-sky-500 text-sm font-bold text-white">
              ▲
            </div>
            <div>
              <div className="text-sm font-semibold tracking-tight text-white">Trip On Himalaya</div>
              <div className="text-[10px] uppercase tracking-[0.12em] text-slate-500">
                Explore · Live higher
              </div>
            </div>
          </div>
        </div>

        <nav className="flex-1 space-y-4 p-3">
          <div>
            <div className="mb-1 px-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-500">
              Finance & ERP
            </div>
            <div className="space-y-0.5">
              {primary.map((item) => {
                const active =
                  item.href === "/finance"
                    ? pathname === "/finance"
                    : pathname === item.href || pathname.startsWith(`${item.href}/`);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition ${
                      active
                        ? "bg-sky-600 text-white shadow-sm"
                        : "text-slate-300 hover:bg-slate-900 hover:text-white"
                    }`}
                  >
                    <NavIconMark name={item.icon} />
                    <span className="truncate">{item.label}</span>
                  </Link>
                );
              })}
            </div>
          </div>

          {more.length > 0 && (
            <div>
              <div className="mb-1 px-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                Ledgers & tools
              </div>
              <div className="space-y-0.5">
                {more.map((item) => {
                  const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={`flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition ${
                        active
                          ? "bg-sky-600 text-white"
                          : "text-slate-400 hover:bg-slate-900 hover:text-slate-200"
                      }`}
                    >
                      <NavIconMark name={item.icon} />
                      <span className="truncate">{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          )}
        </nav>

        <div className="mx-3 mb-3 overflow-hidden rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900 to-sky-950/60 p-3">
          <div className="mb-2 h-14 rounded-lg bg-[linear-gradient(135deg,#0ea5e966,#22c55e44)]" />
          <div className="text-xs font-semibold text-slate-100">
            Turn treks into sustainable business growth
          </div>
          <p className="mt-1 text-[10px] leading-relaxed text-slate-400">
            Collect · Control costs · Grow margin
          </p>
          <Link
            href="/finance"
            className="mt-2 inline-block rounded-md bg-sky-500 px-2.5 py-1 text-[10px] font-semibold text-white hover:bg-sky-400"
          >
            Manage Finances
          </Link>
        </div>

        <div className="border-t border-slate-800 p-4">
          {user && (
            <div className="mb-3 px-1">
              <div className="text-sm font-medium text-slate-100">{user.fullName || user.email}</div>
              <div className="text-[11px] uppercase tracking-wide text-sky-400">{user.role}</div>
            </div>
          )}
          <button
            onClick={logout}
            className="w-full rounded-lg px-3 py-2 text-left text-sm font-medium text-rose-400 hover:bg-slate-900"
          >
            Sign out
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 border-b border-slate-200/80 bg-white/95 px-6 py-4 backdrop-blur">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <h1 className="text-xl font-semibold tracking-tight text-slate-900">{title}</h1>
              {subtitle && <p className="mt-0.5 max-w-2xl text-sm text-slate-500">{subtitle}</p>}
            </div>
            {actions}
          </div>
          {showTabs && <FinanceErpTabs />}
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}

export function FinanceErpTabs() {
  const pathname = usePathname();
  return (
    <div className="mt-4 -mb-4 flex gap-1 overflow-x-auto border-t border-slate-100 pt-3">
      {FINANCE_ERP_TABS.map((tab) => {
        const active =
          tab.href === "/finance" ? pathname === "/finance" : pathname.startsWith(tab.href);
        return (
          <Link
            key={tab.href}
            href={tab.href}
            className={`shrink-0 rounded-t-lg px-3 py-2 text-xs font-semibold transition ${
              active
                ? "bg-sky-50 text-sky-700 ring-1 ring-sky-200"
                : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
            }`}
          >
            {tab.label}
          </Link>
        );
      })}
    </div>
  );
}

export function inr(v: number | undefined | null) {
  return "₹" + Number(v ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export function pct(v: number | undefined | null) {
  const n = Number(v ?? 0);
  const sign = n > 0 ? "+" : "";
  return `${sign}${n.toFixed(1)}%`;
}

export function KpiCard({
  label,
  value,
  hint,
  tone = "neutral",
  delta,
  deltaTone,
}: {
  label: string;
  value: string;
  hint?: string;
  tone?: "neutral" | "good" | "warn" | "bad" | "accent";
  delta?: string;
  deltaTone?: "good" | "bad" | "neutral";
}) {
  const tones = {
    neutral: "border-slate-200 bg-white",
    good: "border-emerald-200 bg-gradient-to-br from-emerald-50 to-white",
    warn: "border-amber-200 bg-gradient-to-br from-amber-50 to-white",
    bad: "border-rose-200 bg-gradient-to-br from-rose-50 to-white",
    accent: "border-sky-700 bg-gradient-to-br from-sky-700 to-slate-900 text-white",
  };
  const hintTone =
    tone === "accent"
      ? "text-sky-100"
      : tone === "good"
        ? "text-emerald-600"
        : tone === "bad"
          ? "text-rose-600"
          : "text-slate-400";
  const dTone =
    deltaTone === "good" ? "text-emerald-600" : deltaTone === "bad" ? "text-rose-600" : "text-slate-500";
  return (
    <div className={`rounded-2xl border px-5 py-4 shadow-sm ${tones[tone]}`}>
      <div
        className={`text-[11px] font-medium uppercase tracking-wide ${
          tone === "accent" ? "text-sky-100" : "text-slate-400"
        }`}
      >
        {label}
      </div>
      <div
        className={`mt-1 text-2xl font-semibold tabular-nums tracking-tight ${
          tone === "accent" ? "text-white" : "text-slate-900"
        }`}
      >
        {value}
      </div>
      {(delta || hint) && (
        <div className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs">
          {delta && <span className={`font-semibold ${tone === "accent" ? "text-emerald-300" : dTone}`}>{delta}</span>}
          {hint && <span className={hintTone}>{hint}</span>}
        </div>
      )}
    </div>
  );
}

export function Panel({
  title,
  action,
  children,
  className = "",
}: {
  title: string;
  action?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section className={`rounded-2xl border border-slate-200 bg-white p-5 shadow-sm ${className}`}>
      <div className="mb-4 flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold text-slate-800">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}

export function StatusBadge({
  status,
}: {
  status: string;
}) {
  const s = status.toUpperCase();
  const style =
    s.includes("RECEIVED") || s.includes("PAID") || s.includes("COMPLETED") || s === "POSTED"
      ? "bg-emerald-50 text-emerald-700"
      : s.includes("PENDING") || s.includes("DUE")
        ? "bg-amber-50 text-amber-700"
        : s.includes("OVERDUE") || s.includes("VOID")
          ? "bg-rose-50 text-rose-700"
          : s.includes("INCOME")
            ? "bg-emerald-50 text-emerald-700"
            : s.includes("EXPENSE")
              ? "bg-rose-50 text-rose-700"
              : "bg-slate-100 text-slate-600";
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${style}`}>
      {status}
    </span>
  );
}

/** Grouped bars + optional profit line (CSS). */
export function TrendChart({
  points,
}: {
  points: { label: string; revenue: number; expense: number; profit: number }[];
}) {
  const max = Math.max(1, ...points.flatMap((p) => [p.revenue, p.expense, Math.abs(p.profit)]));
  return (
    <div>
      <div className="mb-3 flex flex-wrap gap-3 text-[11px] text-slate-500">
        <span className="inline-flex items-center gap-1">
          <span className="h-2 w-2 rounded-sm bg-emerald-500" /> Revenue
        </span>
        <span className="inline-flex items-center gap-1">
          <span className="h-2 w-2 rounded-sm bg-rose-400" /> Expense
        </span>
        <span className="inline-flex items-center gap-1">
          <span className="h-2 w-2 rounded-sm bg-sky-500" /> Profit
        </span>
      </div>
      <div className="relative flex h-56 items-end gap-2">
        <svg className="pointer-events-none absolute inset-0 h-full w-full" viewBox="0 0 100 100" preserveAspectRatio="none">
          <polyline
            fill="none"
            stroke="#0ea5e9"
            strokeWidth="1.5"
            vectorEffect="non-scaling-stroke"
            points={points
              .map((p, i) => {
                const x = points.length <= 1 ? 50 : (i / (points.length - 1)) * 100;
                const y = 100 - (Math.max(0, p.profit) / max) * 88 - 4;
                return `${x},${y}`;
              })
              .join(" ")}
          />
        </svg>
        {points.map((p) => (
          <div key={p.label} className="relative z-10 flex min-w-0 flex-1 flex-col items-center gap-2">
            <div className="flex h-44 w-full items-end justify-center gap-0.5">
              <div
                className="w-[40%] rounded-t-md bg-emerald-500/90 transition-all"
                style={{ height: `${(p.revenue / max) * 100}%` }}
                title={`Revenue: ${inr(p.revenue)}`}
              />
              <div
                className="w-[40%] rounded-t-md bg-rose-400/90 transition-all"
                style={{ height: `${(p.expense / max) * 100}%` }}
                title={`Expense: ${inr(p.expense)}`}
              />
            </div>
            <div className="truncate text-[10px] font-medium text-slate-500">
              {p.label.length > 7 ? p.label.slice(5) : p.label}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function BarSeries({
  points,
}: {
  points: { label: string; a: number; b: number; aLabel?: string; bLabel?: string }[];
}) {
  const max = Math.max(1, ...points.flatMap((p) => [p.a, p.b]));
  return (
    <div className="flex h-56 items-end gap-3">
      {points.map((p) => (
        <div key={p.label} className="flex min-w-0 flex-1 flex-col items-center gap-2">
          <div className="flex h-44 w-full items-end justify-center gap-1">
            <div
              className="w-[42%] rounded-t-md bg-emerald-600 transition-all"
              style={{ height: `${(p.a / max) * 100}%` }}
              title={`${p.aLabel ?? "A"}: ${inr(p.a)}`}
            />
            <div
              className="w-[42%] rounded-t-md bg-rose-400 transition-all"
              style={{ height: `${(p.b / max) * 100}%` }}
              title={`${p.bLabel ?? "B"}: ${inr(p.b)}`}
            />
          </div>
          <div className="truncate text-[10px] font-medium text-slate-500">
            {p.label.slice(5) || p.label}
          </div>
        </div>
      ))}
    </div>
  );
}

export function CashflowBars({
  points,
}: {
  points: { label: string; inflow: number; outflow: number; net: number }[];
}) {
  const max = Math.max(1, ...points.flatMap((p) => [p.inflow, p.outflow]));
  return (
    <div>
      <div className="mb-3 flex flex-wrap gap-3 text-[11px] text-slate-500">
        <span className="inline-flex items-center gap-1">
          <span className="h-2 w-2 rounded-sm bg-emerald-500" /> Inflow
        </span>
        <span className="inline-flex items-center gap-1">
          <span className="h-2 w-2 rounded-sm bg-rose-400" /> Outflow
        </span>
        <span className="inline-flex items-center gap-1">
          <span className="h-2 w-2 rounded-sm bg-sky-500" /> Net
        </span>
      </div>
      <div className="relative flex h-48 items-end gap-1.5">
        <svg className="pointer-events-none absolute inset-0 h-full w-full" viewBox="0 0 100 100" preserveAspectRatio="none">
          <polyline
            fill="none"
            stroke="#0ea5e9"
            strokeWidth="1.4"
            vectorEffect="non-scaling-stroke"
            points={points
              .map((p, i) => {
                const x = points.length <= 1 ? 50 : (i / (points.length - 1)) * 100;
                const y = 100 - ((p.net + max) / (max * 2)) * 88 - 4;
                return `${x},${Math.min(96, Math.max(4, y))}`;
              })
              .join(" ")}
          />
        </svg>
        {points.map((p) => (
          <div key={p.label} className="relative z-10 flex min-w-0 flex-1 flex-col items-center gap-1">
            <div className="flex h-36 w-full items-end justify-center gap-0.5">
              <div
                className="w-[40%] rounded-t bg-emerald-500/85"
                style={{ height: `${(p.inflow / max) * 100}%` }}
                title={`In: ${inr(p.inflow)}`}
              />
              <div
                className="w-[40%] rounded-t bg-rose-400/85"
                style={{ height: `${(p.outflow / max) * 100}%` }}
                title={`Out: ${inr(p.outflow)}`}
              />
            </div>
            <div className="truncate text-[9px] text-slate-400">{p.label.slice(5) || p.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

const DONUT_COLORS = ["#059669", "#0ea5e9", "#8b5cf6", "#f59e0b", "#ef4444", "#64748b", "#14b8a6"];

export function DonutChart({
  slices,
}: {
  slices: { name: string; amount: number; color?: string }[];
}) {
  const total = slices.reduce((s, x) => s + Number(x.amount), 0) || 1;
  let cursor = 0;
  const stops = slices.map((sl, i) => {
    const pctShare = (Number(sl.amount) / total) * 100;
    const start = cursor;
    cursor += pctShare;
    const color = sl.color ?? DONUT_COLORS[i % DONUT_COLORS.length];
    return `${color} ${start}% ${cursor}%`;
  });
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
      <div
        className="mx-auto h-36 w-36 shrink-0 rounded-full"
        style={{
          background: `conic-gradient(${stops.join(", ") || "#e2e8f0 0% 100%"})`,
          mask: "radial-gradient(circle at center, transparent 52%, black 53%)",
          WebkitMask: "radial-gradient(circle at center, transparent 52%, black 53%)",
        }}
      />
      <div className="min-w-0 flex-1 space-y-1.5">
        {slices.map((sl, i) => {
          const share = ((Number(sl.amount) / total) * 100).toFixed(1);
          return (
            <div key={sl.name} className="flex items-center justify-between gap-2 text-xs">
              <span className="flex min-w-0 items-center gap-2">
                <span
                  className="h-2 w-2 shrink-0 rounded-sm"
                  style={{ background: sl.color ?? DONUT_COLORS[i % DONUT_COLORS.length] }}
                />
                <span className="truncate font-medium text-slate-700">{sl.name.replaceAll("_", " ")}</span>
              </span>
              <span className="shrink-0 tabular-nums text-slate-500">
                {share}% · {inr(sl.amount)}
              </span>
            </div>
          );
        })}
        {slices.length === 0 && <p className="text-sm text-slate-400">No data yet</p>}
      </div>
    </div>
  );
}

export function HBar({
  rows,
  color = "emerald",
}: {
  rows: { name: string; amount: number; max: number; pctLabel?: string }[];
  color?: "emerald" | "sky" | "dual";
}) {
  return (
    <div className="space-y-3">
      {rows.map((r) => {
        const used = Math.max(4, (r.amount / Math.max(r.max, 1)) * 100);
        return (
          <div key={r.name}>
            <div className="mb-1 flex justify-between text-xs">
              <span className="font-medium text-slate-700">{r.name.replaceAll("_", " ")}</span>
              <span className="tabular-nums text-slate-500">
                {inr(r.amount)}
                {r.pctLabel ? ` · ${r.pctLabel}` : ""}
              </span>
            </div>
            <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
              {color === "dual" ? (
                <div className="relative h-full w-full">
                  <div className="absolute inset-y-0 left-0 rounded-full bg-sky-200" style={{ width: "100%" }} />
                  <div
                    className="absolute inset-y-0 left-0 rounded-full bg-sky-600"
                    style={{ width: `${Math.min(100, used)}%` }}
                  />
                </div>
              ) : (
                <div
                  className={`h-full rounded-full ${
                    color === "sky"
                      ? "bg-gradient-to-r from-sky-600 to-sky-400"
                      : "bg-gradient-to-r from-emerald-600 to-teal-400"
                  }`}
                  style={{ width: `${Math.min(100, used)}%` }}
                />
              )}
            </div>
          </div>
        );
      })}
      {rows.length === 0 && <p className="text-sm text-slate-400">No data yet</p>}
    </div>
  );
}

export function ActionCard({
  title,
  amount,
  meta,
  href,
  cta,
  tone = "neutral",
}: {
  title: string;
  amount: string;
  meta: string;
  href: string;
  cta: string;
  tone?: "neutral" | "warn" | "bad" | "info" | "good";
}) {
  const border =
    tone === "warn"
      ? "border-amber-200 bg-amber-50/60"
      : tone === "bad"
        ? "border-rose-200 bg-rose-50/60"
        : tone === "info"
          ? "border-sky-200 bg-sky-50/60"
          : tone === "good"
            ? "border-emerald-200 bg-emerald-50/60"
            : "border-slate-200 bg-white";
  return (
    <div className={`rounded-xl border p-4 shadow-sm ${border}`}>
      <div className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">{title}</div>
      <div className="mt-1 text-lg font-semibold tabular-nums text-slate-900">{amount}</div>
      <div className="mt-0.5 text-xs text-slate-500">{meta}</div>
      <Link href={href} className="mt-2 inline-block text-xs font-semibold text-sky-700 hover:underline">
        {cta} →
      </Link>
    </div>
  );
}
