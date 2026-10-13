"use client";

import { FinanceModuleStub } from "@/components/finance/FinanceModuleStub";

export default function FinanceProfitabilityPage() {
  return (
    <FinanceModuleStub
      title="Profitability & Analytics"
      subtitle="Trek-level and company margin analytics for managers and finance"
      bullets={[
        "Trek P&L by departure, package, and season",
        "Gross vs net margin trends from the Finance Dashboard",
        "Budget vs actual variance by cost centre",
        "Compare guide / transport / stay cost ratios across treks",
      ]}
      links={[
        { href: "/finance/trips", label: "Trek P&L" },
        { href: "/finance/pnl", label: "Company P&L" },
        { href: "/finance/budgets", label: "Budgets" },
        { href: "/finance", label: "ERP Dashboard" },
      ]}
    />
  );
}
