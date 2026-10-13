"use client";

import { FinanceModuleStub } from "@/components/finance/FinanceModuleStub";

export default function FinanceReportsPage() {
  return (
    <FinanceModuleStub
      title="Reports"
      subtitle="Exportable finance packs — P&L, cash flow, aging, GST, and budgets"
      bullets={[
        "Profit & loss for any date range",
        "Cash flow and AR aging for collections follow-up",
        "GST output / input credit and net payable",
        "Budget utilisation and trek profitability packs",
      ]}
      links={[
        { href: "/finance/pnl", label: "P&L / Reports" },
        { href: "/finance/cashflow", label: "Cash Flow" },
        { href: "/finance/aging", label: "AR Aging" },
        { href: "/finance/tax", label: "GST Report" },
        { href: "/finance/trips", label: "Trek P&L" },
      ]}
    />
  );
}
