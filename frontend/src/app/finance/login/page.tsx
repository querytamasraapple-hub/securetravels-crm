"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { homePathForRole } from "@/lib/rbac";

export default function FinanceLoginPage() {
  const { user, login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("finance@securetravels.in");
  const [password, setPassword] = useState("finance123");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    router.replace(homePathForRole(user.role));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden px-4">
      <div
        className="absolute inset-0 bg-cover bg-center"
        style={{
          backgroundImage:
            "linear-gradient(135deg, rgba(15,23,42,0.92), rgba(6,78,59,0.75)), url('https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1920&q=80')",
        }}
      />
      <form
        onSubmit={onSubmit}
        className="relative w-full max-w-md rounded-2xl border border-white/15 bg-white/95 p-8 shadow-2xl backdrop-blur"
      >
        <div className="text-[11px] font-semibold uppercase tracking-[0.16em] text-emerald-700">
          Trip On Himalaya
        </div>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">Finance ERP</h1>
        <p className="mt-1 text-sm text-slate-500">
          Separate portal for company revenue, receivables, expenses & trek P&L
        </p>

        <label className="mt-6 block text-sm font-medium text-slate-700">
          Email
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-emerald-700"
          />
        </label>

        <label className="mt-4 block text-sm font-medium text-slate-700">
          Password
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-emerald-700"
          />
        </label>

        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="mt-6 w-full rounded-lg bg-slate-900 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-50"
        >
          {submitting ? "Signing in…" : "Enter Finance ERP"}
        </button>

        <p className="mt-4 text-center text-xs text-slate-400">
          Demo: finance@securetravels.in · finance123
        </p>
        <p className="mt-3 text-center text-xs text-slate-500">
          Sales / ops desk?{" "}
          <Link href="/login" className="font-medium text-emerald-700 hover:underline">
            CRM login
          </Link>
        </p>
      </form>
    </div>
  );
}
