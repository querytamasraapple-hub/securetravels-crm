"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Incident Management"}
      subtitle={"Field incidents and resolution tracking"}
      allowedRoles={OPS_DESK}
      bullets={[
        "Log altitude, medical or weather incidents here.",
        "Link affected batches from Departures.",
        "Notify managers via Notifications.",
      ]}
    />
  );
}
