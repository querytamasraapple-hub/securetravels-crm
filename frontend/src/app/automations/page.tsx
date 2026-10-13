"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function AutomationsPage() {
  return (
    <ModuleStub
      title="Automations"
      subtitle="Follow-ups, assignment and nurture workflows"
      allowedRoles={MANAGERS}
      bullets={[
        "Follow-up tasks auto-create when lead status changes.",
        "Inbound WhatsApp / webhooks assign owners via round-robin.",
        "Use Tasks & Follow Ups to work the automated queue daily.",
      ]}
    />
  );
}
