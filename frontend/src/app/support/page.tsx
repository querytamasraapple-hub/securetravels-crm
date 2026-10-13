"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function SupportPage() {
  return (
    <ModuleStub
      title="Customer Support"
      subtitle="Post-sale and pre-departure traveller help"
      allowedRoles={MANAGERS}
      bullets={[
        "Use WhatsApp & Email inbox for traveller conversations.",
        "Open Customers for trip history and contact details.",
        "Documents module handles compliance uploads before departure.",
      ]}
    />
  );
}
