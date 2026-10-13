"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Travel Desk"}
      subtitle={"Pickups, transfers and traveller logistics"}
      allowedRoles={OPS_DESK}
      bullets={[
        "Today's Operations timeline on Overview covers pickup slots.",
        "Transport Management tracks vehicle allocation.",
        "Ops handoff desk confirms transfer status per booking.",
      ]}
    />
  );
}
