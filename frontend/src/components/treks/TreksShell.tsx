"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Sidebar } from "@/components/Sidebar";
import { TREKS_ERP_TABS } from "@/lib/rbac";

export function TreksShell({
  title,
  subtitle,
  breadcrumb = "Manage Treks",
  actions,
  children,
  showTabs = true,
}: {
  title: string;
  subtitle?: string;
  breadcrumb?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
  showTabs?: boolean;
}) {
  const pathname = usePathname();

  return (
    <div className="flex min-h-screen bg-[#f4f6f9]">
      <Sidebar />
      <main className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 border-b border-slate-200/80 bg-white/95 px-6 py-4 backdrop-blur">
          <div className="mb-1 text-[11px] font-medium text-slate-400">
            Treks &amp; Packages <span className="mx-1 text-slate-300">›</span> {breadcrumb}
          </div>
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <h1 className="text-xl font-semibold tracking-tight text-slate-900">{title}</h1>
              {subtitle && <p className="mt-0.5 max-w-2xl text-sm text-slate-500">{subtitle}</p>}
            </div>
            {actions}
          </div>
          {showTabs && (
            <div className="mt-4 -mb-4 flex gap-1 overflow-x-auto border-t border-slate-100 pt-3">
              {TREKS_ERP_TABS.map((tab) => {
                const active =
                  tab.href === "/trips"
                    ? pathname === "/trips"
                    : pathname === tab.href || pathname.startsWith(`${tab.href}/`);
                return (
                  <Link
                    key={`${tab.href}-${tab.label}`}
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
          )}
        </header>
        <div className="flex-1 p-6">{children}</div>
      </main>
    </div>
  );
}
