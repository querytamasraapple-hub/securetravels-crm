"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function FormsPage() {
  return (
    <ModuleStub
      title="Forms & Lead Capture"
      subtitle="Multi-channel capture into Leads Management"
      allowedRoles={MANAGERS}
      bullets={[
        "Manual Add Lead and CSV Import for desk capture.",
        "Website webhook for organic / landing page forms.",
        "WhatsApp inbound creates WHATSAPP-sourced leads automatically.",
      ]}
    />
  );
}
