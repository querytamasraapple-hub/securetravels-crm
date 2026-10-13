"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function ForecastingPage() {
  return (
    <ModuleStub
      title="Forecasting"
      subtitle="Expected bookings and revenue from the live lead funnel"
      allowedRoles={MANAGERS}
      bullets={[
        "Leads Funnel and Source Performance on Leads Management drive the short-term outlook.",
        "Reports & Analytics has team conversion and trip performance.",
        "Finance ERP Trek P&L closes the loop after bookings confirm.",
      ]}
    />
  );
}
