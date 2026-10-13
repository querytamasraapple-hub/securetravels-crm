"use client";

import { FinanceModuleStub } from "@/components/finance/FinanceModuleStub";

export default function FinanceBranchesPage() {
  return (
    <FinanceModuleStub
      title="Multi-Company / Branches"
      subtitle="Branch and entity books for multi-basecamp trek operations"
      bullets={[
        "Separate books per basecamp / company entity (Uttarakhand, Himachal, etc.)",
        "Consolidated revenue and expense views on the ERP dashboard",
        "Inter-branch transfers and shared vendor payouts",
        "GST registrations and place-of-supply by branch",
      ]}
      links={[
        { href: "/finance", label: "Consolidated dashboard" },
        { href: "/finance/tax", label: "GST by entity" },
        { href: "/settings", label: "Company settings" },
      ]}
    />
  );
}
