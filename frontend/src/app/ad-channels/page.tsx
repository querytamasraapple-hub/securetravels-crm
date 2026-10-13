"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function AdChannelsPage() {
  return (
    <ModuleStub
      title="Ad Channels"
      subtitle="Instagram, Google Ads, Meta and WhatsApp intake"
      allowedRoles={MANAGERS}
      bullets={[
        "Source tabs on Leads Management isolate each ad channel.",
        "Webhook + WhatsApp hub capture paid and organic traffic.",
        "Compare channel conversion in the Top Performing Campaigns panel.",
      ]}
    />
  );
}
