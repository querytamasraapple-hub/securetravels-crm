"use client";

import { AdminPage } from "@/components/AdminPage";
import type { Role } from "@/lib/rbac";

export function ModuleStub({
  title,
  subtitle,
  allowedRoles,
  bullets,
}: {
  title: string;
  subtitle: string;
  allowedRoles: Role[];
  bullets: string[];
}) {
  return (
    <AdminPage title={title} subtitle={subtitle} allowedRoles={allowedRoles}>
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="mb-4 text-sm text-slate-600">
          This module is wired into Basecamp for managers and admins. Core actions below are
          available through existing CRM tools until dedicated screens ship.
        </p>
        <ul className="space-y-2 text-sm text-slate-700">
          {bullets.map((b) => (
            <li key={b} className="flex gap-2">
              <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-sky-500" />
              {b}
            </li>
          ))}
        </ul>
      </div>
    </AdminPage>
  );
}
