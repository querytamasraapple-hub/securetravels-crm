"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Permits & Compliance"}
      subtitle={"Forest permits and traveller documents"}
      allowedRoles={OPS_DESK}
      bullets={[
        "Documents module stores compliance uploads.",
        "Alerts panel flags pending permits.",
        "Verify IDs before departure on the travel desk.",
      ]}
    />
  );
}
