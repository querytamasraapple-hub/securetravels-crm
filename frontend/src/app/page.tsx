"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { homePathForRole } from "@/lib/rbac";

/** Send each role to their first allowed module (or login). */
export default function Home() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace("/login");
      return;
    }
    router.replace(homePathForRole(user.role));
  }, [user, loading, router]);

  return (
    <div className="flex h-screen items-center justify-center text-slate-400">
      Loading…
    </div>
  );
}
