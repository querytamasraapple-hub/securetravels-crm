"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function ActivitiesPage() {
  return (
    <ModuleStub
      title="Activities"
      subtitle="Calls, WhatsApp touches and follow-ups across the sales desk"
      allowedRoles={MANAGERS}
      bullets={[
        "Open Tasks & Follow Ups for the actionable queue.",
        "Lead activity timelines show status and field changes.",
        "Calendar & Tasks covers scheduled calls and departures.",
      ]}
    />
  );
}
