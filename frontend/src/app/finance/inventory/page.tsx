"use client";

import { FinanceModuleStub } from "@/components/finance/FinanceModuleStub";

export default function FinanceInventoryPage() {
  return (
    <FinanceModuleStub
      title="Inventory & Assets"
      subtitle="Gear, rental stock, and fixed assets with cost and depreciation visibility"
      bullets={[
        "Link equipment purchases to expense and asset categories",
        "Track rental income from gear against inventory SKUs",
        "Capitalise high-value assets for trek operations",
        "Budget gear refresh cycles under Budgeting & Forecasting",
      ]}
      links={[
        { href: "/inventory", label: "Ops Inventory" },
        { href: "/finance/expenses", label: "Gear expenses" },
        { href: "/finance/budgets", label: "Budgets" },
      ]}
    />
  );
}
