"use client";

import { MANAGERS } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function IntegrationsPage() {
  return (
    <ModuleStub
      title="Integrations"
      subtitle="Connect ads, WhatsApp, webhooks and Finance ERP"
      allowedRoles={MANAGERS}
      bullets={[
        "WhatsApp & Email hub for messaging providers.",
        "Lead webhook for website / form tools.",
        "Finance ERP for invoices, GST and payouts after conversion.",
      ]}
    />
  );
}
