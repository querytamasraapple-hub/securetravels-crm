"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import {
  canAccessPath,
  homePathForRole,
  isFinancePath,
  loginPathForRole,
  type Role,
} from "@/lib/rbac";

type Props = {
  children: React.ReactNode;
  /** Optional override; by default the shared NAV matrix decides access. */
  allowedRoles?: Role[];
};

/**
 * Login + role gate.
 * Unauthenticated → /login (or /finance/login on finance routes).
 * Authenticated but role not allowed for this path → role home.
 */
export function Protected({ children, allowedRoles }: Props) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace(isFinancePath(pathname) ? "/finance/login" : "/login");
      return;
    }
    const allowed = allowedRoles
      ? allowedRoles.includes(user.role as Role)
      : canAccessPath(user.role, pathname);
    if (!allowed) {
      router.replace(homePathForRole(user.role));
    }
  }, [loading, user, pathname, allowedRoles, router]);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center text-slate-400">
        Loading…
      </div>
    );
  }
  if (!user) return null;

  const allowed = allowedRoles
    ? allowedRoles.includes(user.role as Role)
    : canAccessPath(user.role, pathname);
  if (!allowed) {
    return (
      <div className="flex h-screen items-center justify-center text-slate-400">
        Redirecting…
      </div>
    );
  }

  return <>{children}</>;
}

export { loginPathForRole };
