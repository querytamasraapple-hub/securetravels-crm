"use client";

import Link from "next/link";
import { Protected } from "@/components/Protected";
import { TreksShell } from "@/components/treks/TreksShell";

export function TreksModuleStub({
  title,
  breadcrumb,
  subtitle,
  bullets,
  links = [],
}: {
  title: string;
  breadcrumb: string;
  subtitle: string;
  bullets: string[];
  links?: { href: string; label: string }[];
}) {
  return (
    <Protected>
      <TreksShell title={title} subtitle={subtitle} breadcrumb={breadcrumb}>
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="mb-4 text-sm text-slate-600">
            This Treks &amp; Packages module is wired into Basecamp navigation. Use the linked
            tools below while specialised editors continue to expand.
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
                  key={l.href + l.label}
                  href={l.href}
                  className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-700 hover:border-sky-300 hover:text-sky-800"
                >
                  {l.label}
                </Link>
              ))}
            </div>
          )}
        </div>
      </TreksShell>
    </Protected>
  );
}
