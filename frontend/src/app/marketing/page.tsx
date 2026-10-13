"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function MarketingPage() {
  return (
    <ModuleStub
      title="Marketing"
      subtitle="Channels, creatives and trek demand generation"
      allowedRoles={MANAGERS}
      bullets={[
        "Track Instagram, Meta and Google Ads lead volume from Leads Management source tabs.",
        "Use Reports & Analytics for funnel conversion by channel.",
        "Website webhook intake creates leads automatically into the CRM.",
      ]}
    />
  );
}
