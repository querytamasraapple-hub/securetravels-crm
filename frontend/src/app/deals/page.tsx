"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function DealsPage() {
  return (
    <ModuleStub
      title="Deals / Opportunities"
      subtitle="High-intent leads moving toward booking confirmation"
      allowedRoles={MANAGERS}
      bullets={[
        "Filter Leads Management to Proposal Sent and Interested stages.",
        "Use Convert to Booking from the lead detail drawer when ready.",
        "Track conversion in Reports & Forecasting.",
      ]}
    />
  );
}
