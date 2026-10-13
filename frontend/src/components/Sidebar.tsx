"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { NotificationBell } from "@/components/NotificationBell";
import { NavIconMark, initialsFromName } from "@/components/NavIcons";
import { navSectionsForRole, roleDisplayLabel } from "@/lib/rbac";

const COLLAPSE_KEY = "toh.sidebar.collapsed";

export function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const sections = navSectionsForRole(user?.role);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    try {
      setCollapsed(localStorage.getItem(COLLAPSE_KEY) === "1");
    } catch {
      /* ignore */
    }
  }, []);

  function toggleCollapsed() {
    setCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(COLLAPSE_KEY, next ? "1" : "0");
      } catch {
        /* ignore */
      }
      return next;
    });
  }

  function isActive(href: string) {
    if (pathname === href) return true;
    // Exact-only roots so /operations does not stay active on /operations/desk.
    if (href === "/finance" || href === "/operations" || href === "/trips") return false;
    return pathname.startsWith(`${href}/`);
  }

  const initials = initialsFromName(user?.fullName, user?.email);

  return (
    <aside
      className={`flex shrink-0 flex-col border-r border-slate-800 bg-slate-950 text-slate-200 transition-[width] ${
        collapsed ? "w-[72px]" : "w-60"
      }`}
    >
      <div className="flex items-center justify-between gap-2 border-b border-slate-800 px-3 py-4">
        <div className={`flex min-w-0 items-center gap-2 ${collapsed ? "justify-center w-full" : ""}`}>
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-orange-500 text-sm font-bold text-white">
            ▲
          </div>
          {!collapsed && (
            <div className="min-w-0">
              <div className="truncate text-sm font-semibold text-cyan-100">Trip On Himalaya</div>
              <div className="text-[10px] uppercase tracking-[0.14em] text-slate-500">Basecamp OS</div>
            </div>
          )}
        </div>
        {!collapsed && (
          <button
            type="button"
            onClick={toggleCollapsed}
            className="rounded p-1 text-slate-500 hover:bg-slate-900 hover:text-slate-200"
            aria-label="Collapse sidebar"
            title="Collapse"
          >
            ‹
          </button>
        )}
      </div>

      {collapsed && (
        <button
          type="button"
          onClick={toggleCollapsed}
          className="mx-auto mt-2 rounded p-1 text-slate-500 hover:bg-slate-900 hover:text-slate-200"
          aria-label="Expand sidebar"
          title="Expand"
        >
          ›
        </button>
      )}

      <nav className="flex-1 space-y-4 overflow-y-auto p-3">
        {sections.map((section) => (
          <div key={section.id}>
            {!collapsed && (
              <div className="mb-1 px-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                {section.label}
              </div>
            )}
            <div className="space-y-0.5">
              {section.items.map((item) => {
                const active = isActive(item.href);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    title={item.label}
                    className={`flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition ${
                      collapsed ? "justify-center px-2" : ""
                    } ${
                      active
                        ? "border-l-2 border-orange-500 bg-orange-950/50 text-orange-300"
                        : "border-l-2 border-transparent text-slate-300 hover:bg-slate-900 hover:text-cyan-100"
                    }`}
                  >
                    <NavIconMark name={item.icon} />
                    {!collapsed && <span>{item.label}</span>}
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      {!collapsed && (user?.role === "MANAGER" || user?.role === "ADMIN" || user?.role === "CEO") && (
        pathname.startsWith("/finance") ? (
          <div className="mx-3 mb-2 overflow-hidden rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900 to-sky-950/50 p-3">
            <div className="mb-2 h-12 rounded-lg bg-[linear-gradient(135deg,#0ea5e955,#22c55e55)]" />
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
        ) : pathname.startsWith("/trips") || pathname.startsWith("/batches") ? (
          <div className="mx-3 mb-2 overflow-hidden rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900 to-emerald-950/40 p-3">
            <div className="mb-2 h-12 rounded-lg bg-[linear-gradient(135deg,#05966966,#0ea5e955)]" />
            <div className="text-xs font-semibold text-slate-100">
              {pathname.startsWith("/batches")
                ? "Better batches, happier trekkers"
                : "Create amazing treks people love"}
            </div>
            <p className="mt-1 text-[10px] leading-relaxed text-slate-400">
              {pathname.startsWith("/batches")
                ? "Plan · Fill · Depart"
                : "Catalogue · Batches · Live site"}
            </p>
            <Link
              href={pathname.startsWith("/batches") ? "/batches" : "/trips"}
              className="mt-2 inline-block rounded-md bg-emerald-500 px-2.5 py-1 text-[10px] font-semibold text-white hover:bg-emerald-400"
            >
              {pathname.startsWith("/batches") ? "Plan New Batch" : "View Live Catalogue"}
            </Link>
          </div>
        ) : (
          <div className="mx-3 mb-2 overflow-hidden rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900 to-orange-950/40 p-3">
            <div className="mb-2 h-12 rounded-lg bg-[linear-gradient(135deg,#ea580c55,#0ea5e955)]" />
            <div className="text-xs font-semibold text-slate-100">Turn more leads into trekkers</div>
            <p className="mt-1 text-[10px] leading-relaxed text-slate-400">Manage · Nurture · Convert</p>
            <Link
              href="/leads"
              className="mt-2 inline-block rounded-md bg-orange-500 px-2.5 py-1 text-[10px] font-semibold text-white hover:bg-orange-400"
            >
              Explore CRM
            </Link>
          </div>
        )
      )}
      {!collapsed && user?.role === "OPS" && (
        <div className="mx-3 mb-2 overflow-hidden rounded-xl border border-slate-800 bg-gradient-to-br from-slate-900 to-sky-950/50 p-3">
          <div className="mb-2 h-12 rounded-lg bg-[linear-gradient(135deg,#0ea5e955,#22c55e55)]" />
          <div className="text-xs font-semibold text-slate-100">Smooth operations, happier trekkers</div>
          <p className="mt-1 text-[10px] leading-relaxed text-slate-400">Plan · Coordinate · Execute</p>
          <Link
            href="/operations"
            className="mt-2 inline-block rounded-md bg-sky-500 px-2.5 py-1 text-[10px] font-semibold text-white hover:bg-sky-400"
          >
            Ops dashboard
          </Link>
        </div>
      )}

      <div className="border-t border-slate-800 p-3">
        <div className={`mb-2 flex items-center gap-2 px-1 ${collapsed ? "flex-col" : "justify-between"}`}>
          {user && (
            <div className={`flex min-w-0 items-center gap-2 ${collapsed ? "justify-center" : ""}`}>
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-orange-500 text-[11px] font-bold text-white">
                {initials}
              </div>
              {!collapsed && (
                <div className="min-w-0 text-xs">
                  <div className="truncate font-medium text-slate-100">{user.fullName || user.email}</div>
                  <div className="uppercase tracking-wide text-teal-400/90">
                    {roleDisplayLabel(user.role)}
                  </div>
                </div>
              )}
            </div>
          )}
          <NotificationBell />
        </div>
        {!collapsed && (
          <button
            onClick={logout}
            className="w-full rounded-md px-3 py-2 text-left text-sm font-medium text-rose-400 hover:bg-slate-900"
          >
            Sign out
          </button>
        )}
      </div>
    </aside>
  );
}
