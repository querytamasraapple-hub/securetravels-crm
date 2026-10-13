"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function CampaignsPage() {
  return (
    <ModuleStub
      title="Campaigns"
      subtitle="Plan and measure trek promotion campaigns"
      allowedRoles={MANAGERS}
      bullets={[
        "Segment leads by source in Leads Management (Instagram, Meta, Google, Website, Referral).",
        "Compare campaign performance using the Source Performance chart on Leads.",
        "Promote high-converting treks via Bookings and Trek catalogue.",
      ]}
    />
  );
}
