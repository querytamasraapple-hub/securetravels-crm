"use client";

import { SALES_DESK } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { Sidebar } from "@/components/Sidebar";
import { LeadsManagement } from "@/components/lead/LeadsManagement";

export default function LeadsPage() {
  return (
    <Protected allowedRoles={SALES_DESK}>
      <div className="flex min-h-screen bg-slate-50">
        <Sidebar />
        <LeadsManagement />
      </div>
    </Protected>
  );
}
