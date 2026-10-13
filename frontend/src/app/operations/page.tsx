"use client";

import { OPS_DESK } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { Sidebar } from "@/components/Sidebar";
import { OpsDashboard } from "@/components/ops/OpsDashboard";

export default function OperationsPage() {
  return (
    <Protected allowedRoles={OPS_DESK}>
      <div className="flex min-h-screen bg-slate-50">
        <Sidebar />
        <OpsDashboard />
      </div>
    </Protected>
  );
}
