"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function LandingPagesPage() {
  return (
    <ModuleStub
      title="Landing Pages"
      subtitle="Trek landing experiences that feed the CRM"
      allowedRoles={MANAGERS}
      bullets={[
        "Website forms post into POST /api/webhook/lead with HMAC.",
        "Leads arrive with source WEBSITE and optional campaign notes.",
        "Use Forms & Lead Capture for field mapping guidance.",
      ]}
    />
  );
}
