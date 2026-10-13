import type { ReactNode } from "react";
import type { NavIcon } from "@/lib/rbac";

/** Soft accent colors matching Basecamp SS icon chips. */
const ICON_COLOR: Partial<Record<NavIcon, string>> = {
  dashboard: "text-orange-400",
  leads: "text-rose-400",
  bookings: "text-sky-400",
  treks: "text-emerald-400",
  batches: "text-violet-400",
  customers: "text-cyan-400",
  payments: "text-amber-400",
  expenses: "text-lime-400",
  staff: "text-blue-400",
  attendance: "text-teal-400",
  vendors: "text-orange-300",
  documents: "text-indigo-400",
  inventory: "text-fuchsia-400",
  payroll: "text-green-400",
  leave: "text-red-400",
  analytics: "text-sky-300",
  visitors: "text-pink-400",
  notifications: "text-yellow-400",
  settings: "text-slate-300",
  audit: "text-emerald-300",
  tasks: "text-slate-300",
  messages: "text-sky-300",
  ops: "text-orange-300",
  calendar: "text-violet-300",
  guides: "text-teal-300",
  finance: "text-emerald-300",
};

function Svg({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={`h-4 w-4 shrink-0 ${className ?? ""}`}
      aria-hidden
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {children}
    </svg>
  );
}

export function NavIconMark({ name }: { name?: NavIcon }) {
  const color = (name && ICON_COLOR[name]) || "text-slate-400";
  switch (name) {
    case "dashboard":
      return (
        <Svg className={color}>
          <rect x="3" y="3" width="7" height="7" rx="1" />
          <rect x="14" y="3" width="7" height="7" rx="1" />
          <rect x="3" y="14" width="7" height="7" rx="1" />
          <rect x="14" y="14" width="7" height="7" rx="1" />
        </Svg>
      );
    case "leads":
      return (
        <Svg className={color}>
          <circle cx="12" cy="12" r="8" />
          <circle cx="12" cy="12" r="3" />
          <circle cx="12" cy="12" r="0.5" fill="currentColor" stroke="none" />
        </Svg>
      );
    case "bookings":
      return (
        <Svg className={color}>
          <path d="M4 5h7v14H4z" />
          <path d="M13 5h7v14h-7z" />
          <path d="M7 8h1M7 11h1M16 8h1M16 11h1" />
        </Svg>
      );
    case "treks":
      return (
        <Svg className={color}>
          <path d="M3 18L9 6l3 5 3-3 6 10H3z" />
        </Svg>
      );
    case "batches":
      return (
        <Svg className={color}>
          <rect x="5" y="4" width="14" height="16" rx="2" />
          <path d="M9 2v4M15 2v4M5 10h14" />
          <circle cx="16" cy="16" r="2.5" />
          <path d="M16 15v1.2l.8.8" />
        </Svg>
      );
    case "customers":
      return (
        <Svg className={color}>
          <circle cx="12" cy="8" r="3.5" />
          <path d="M5 19c1.5-3 4-4.5 7-4.5S17.5 16 19 19" />
        </Svg>
      );
    case "payments":
      return (
        <Svg className={color}>
          <rect x="3" y="6" width="18" height="12" rx="2" />
          <path d="M3 10h18" />
          <path d="M7 15h4" />
        </Svg>
      );
    case "expenses":
      return (
        <Svg className={color}>
          <rect x="3" y="7" width="18" height="11" rx="1.5" />
          <path d="M12 10v5M10.5 11.5c0-.8.7-1.4 1.5-1.4s1.5.6 1.5 1.4-.7 1.2-1.5 1.4-1.5.6-1.5 1.4.7 1.4 1.5 1.4 1.5-.6 1.5-1.4" />
        </Svg>
      );
    case "staff":
      return (
        <Svg className={color}>
          <rect x="4" y="8" width="16" height="11" rx="1.5" />
          <path d="M9 8V6a3 3 0 0 1 6 0v2" />
        </Svg>
      );
    case "attendance":
      return (
        <Svg className={color}>
          <rect x="5" y="3" width="14" height="18" rx="2" />
          <path d="M8 9h8M8 13h5" />
          <path d="M14 16l1.5 1.5L19 14" />
        </Svg>
      );
    case "vendors":
      return (
        <Svg className={color}>
          <path d="M3 14h13v5H3z" />
          <path d="M16 16h3l2 3v0H16z" />
          <circle cx="7" cy="19" r="1.5" />
          <circle cx="17" cy="19" r="1.5" />
          <path d="M3 14V9h10v5" />
        </Svg>
      );
    case "documents":
      return (
        <Svg className={color}>
          <path d="M7 3h7l4 4v14H7z" />
          <path d="M14 3v4h4" />
          <path d="M12 13l2 1.5v3l-2 1-2-1v-3z" />
        </Svg>
      );
    case "inventory":
      return (
        <Svg className={color}>
          <path d="M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3z" />
          <path d="M12 12l8-4.5M12 12v9M12 12L4 7.5" />
        </Svg>
      );
    case "payroll":
      return (
        <Svg className={color}>
          <rect x="5" y="4" width="14" height="16" rx="2" />
          <path d="M12 9v6M10 11c0-1 .9-1.8 2-1.8s2 .8 2 1.8-1 1.5-2 1.8-2 .8-2 1.8.9 1.8 2 1.8 2-.8 2-1.8" />
        </Svg>
      );
    case "leave":
      return (
        <Svg className={color}>
          <rect x="4" y="5" width="16" height="15" rx="2" />
          <path d="M4 10h16M9 3v4M15 3v4M8 14l8 4M16 14l-8 4" />
        </Svg>
      );
    case "analytics":
      return (
        <Svg className={color}>
          <path d="M4 19V5M4 19h16" />
          <path d="M8 15v-4M12 15V8M16 15v-7" />
        </Svg>
      );
    case "visitors":
      return (
        <Svg className={color}>
          <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6z" />
          <circle cx="12" cy="12" r="2.5" />
        </Svg>
      );
    case "notifications":
      return (
        <Svg className={color}>
          <path d="M6 9a6 6 0 1 1 12 0c0 4 1.5 5.5 1.5 5.5H4.5S6 13 6 9z" />
          <path d="M10 19a2 2 0 0 0 4 0" />
        </Svg>
      );
    case "settings":
      return (
        <Svg className={color}>
          <circle cx="12" cy="12" r="3" />
          <path d="M12 3v2M12 19v2M4.9 6.5l1.4 1.4M17.7 16.1l1.4 1.4M3 12h2M19 12h2M4.9 17.5l1.4-1.4M17.7 7.9l1.4-1.4" />
        </Svg>
      );
    case "audit":
      return (
        <Svg className={color}>
          <path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z" />
        </Svg>
      );
    case "tasks":
      return (
        <Svg className={color}>
          <rect x="4" y="4" width="16" height="16" rx="2" />
          <path d="M8 9h8M8 13h5" />
        </Svg>
      );
    case "messages":
      return (
        <Svg className={color}>
          <path d="M4 6h16v10H8l-4 3V6z" />
        </Svg>
      );
    case "ops":
      return (
        <Svg className={color}>
          <path d="M4 7h16M4 12h10M4 17h14" />
        </Svg>
      );
    case "calendar":
      return (
        <Svg className={color}>
          <rect x="4" y="5" width="16" height="15" rx="2" />
          <path d="M4 10h16M9 3v4M15 3v4" />
        </Svg>
      );
    case "guides":
      return (
        <Svg className={color}>
          <circle cx="12" cy="7" r="3" />
          <path d="M6 20c1-3.5 3.5-5 6-5s5 1.5 6 5" />
        </Svg>
      );
    case "finance":
      return (
        <Svg className={color}>
          <path d="M12 4v16M8 8c0-1.5 1.8-2.5 4-2.5s4 1 4 2.5-1.8 2.2-4 2.5-4 1-4 2.5 1.8 2.5 4 2.5 4-1 4-2.5" />
        </Svg>
      );
    default:
      return <span className="inline-block h-4 w-4" />;
  }
}

export function initialsFromName(name?: string | null, email?: string | null): string {
  const source = (name && name.trim()) || (email && email.split("@")[0]) || "?";
  const parts = source.split(/[\s._-]+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return source.slice(0, 2).toUpperCase();
}
