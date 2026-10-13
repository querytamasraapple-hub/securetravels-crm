"use client";

import { FinanceModuleStub } from "@/components/finance/FinanceModuleStub";

export default function FinanceAccountingPage() {
  return (
    <FinanceModuleStub
      title="Accounting"
      subtitle="Chart of accounts, journal entries, and period close for trek operations"
      bullets={[
        "Track trek revenue, deposits, and refunds against receivable ledgers",
        "Post vendor bills, guide payouts, and operating expenses to expense accounts",
        "Reconcile bank / UPI collections with booking payments",
        "Month-end close with P&L and balance-sheet summaries",
      ]}
      links={[
        { href: "/finance/pnl", label: "P&L Statement" },
        { href: "/finance/invoices", label: "Invoices" },
        { href: "/finance/cashflow", label: "Cash Flow" },
        { href: "/finance/reports", label: "Reports" },
      ]}
    />
  );
}
