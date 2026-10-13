"use client";

import { ADMINS } from "@/lib/rbac";
import { AdminPage } from "@/components/AdminPage";
import { AuditLog } from "@/components/report/auditLog";

export default function AuditPage() {
  return (
    <AdminPage
      title="Audit log"
      subtitle="ADMIN/CEO only — full mutation trail including before/after values"
      allowedRoles={ADMINS}
    >
      <AuditLog />
    </AdminPage>
  );
}
