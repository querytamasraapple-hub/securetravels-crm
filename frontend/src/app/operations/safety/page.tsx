"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Risk & Safety"}
      subtitle={"Altitude risk, gear and emergency readiness"}
      allowedRoles={OPS_DESK}
      bullets={[
        "High altitude alerts appear on Operations Overview.",
        "Inventory tracks oxygen and first-aid stock.",
        "Incident Management logs field events.",
      ]}
    />
  );
}
