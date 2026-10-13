"use client";

import Link from "next/link";
import { FINANCE_PORTAL, type Role } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { FinanceShell } from "@/components/finance/FinanceShell";

export function FinanceModuleStub({
  title,
  subtitle,
  bullets,
  links = [],
  allowedRoles = FINANCE_PORTAL,
}: {
  title: string;
  subtitle: string;
  bullets: string[];
  links?: { href: string; label: string }[];
  allowedRoles?: Role[];
}) {
  return (
    <Protected allowedRoles={allowedRoles}>
      <FinanceShell title={title} subtitle={subtitle}>
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="mb-4 text-sm text-slate-600">
            Module is live in the Finance & ERP navigation. Use the linked ledgers below while
            dedicated workflows continue to expand.
          </p>
          <ul className="mb-5 space-y-2 text-sm text-slate-700">
            {bullets.map((b) => (
              <li key={b} className="flex gap-2">
                <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-sky-500" />
                {b}
              </li>
            ))}
          </ul>
          {links.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {links.map((l) => (
                <Link
                  key={l.href}
                  href={l.href}
                  className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-700 hover:border-sky-300 hover:text-sky-800"
                >
                  {l.label}
                </Link>
              ))}
            </div>
          )}
        </div>
      </FinanceShell>
    </Protected>
  );
}
