"use client";

import { FinanceModuleStub } from "@/components/finance/FinanceModuleStub";

export default function FinancePayrollPage() {
  return (
    <FinanceModuleStub
      title="Payroll & HR"
      subtitle="Salaries, guide retainers, TDS, and payroll scheduling tied to Finance ERP"
      bullets={[
        "Next payroll run amount surfaces on the Finance Dashboard action cards",
        "Map salary and guide fee categories into expense ledgers",
        "Coordinate attendance / leave from HR modules with cost centres",
        "Hold TDS estimates alongside GST compliance",
      ]}
      links={[
        { href: "/payroll", label: "Open Payroll (CRM)" },
        { href: "/finance/expenses", label: "Salary expenses" },
        { href: "/finance/tax", label: "GST & Tax" },
        { href: "/attendance", label: "Attendance" },
      ]}
    />
  );
}
