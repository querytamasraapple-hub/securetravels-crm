"use client";

import { Protected } from "@/components/Protected";
import { Sidebar } from "@/components/Sidebar";
import type { Role } from "@/lib/rbac";

export function AdminPage({
  title,
  subtitle,
  allowedRoles,
  actions,
  children,
}: {
  title: string;
  subtitle?: string;
  allowedRoles?: Role[];
  actions?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <Protected allowedRoles={allowedRoles}>
      <div className="flex min-h-screen bg-slate-50">
        <Sidebar />
        <main className="flex-1 overflow-y-auto p-6">
          <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
            <div>
              <h1 className="text-xl font-semibold text-slate-900">{title}</h1>
              {subtitle && <p className="text-sm text-slate-500">{subtitle}</p>}
            </div>
            {actions}
          </div>
          {children}
        </main>
      </div>
    </Protected>
  );
}
