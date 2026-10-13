"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Customer On-trip Support"}
      subtitle={"Live traveller help during treks"}
      allowedRoles={OPS_DESK}
      bullets={[
        "Overview Support card shows open conversations.",
        "WhatsApp & Email hub for traveller messages.",
        "Escalate issues to Incident Management.",
      ]}
    />
  );
}
