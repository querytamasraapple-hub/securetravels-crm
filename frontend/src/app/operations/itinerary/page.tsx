"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Trip Itinerary Management"}
      subtitle={"Day-wise trek plans and camp stops"}
      allowedRoles={OPS_DESK}
      bullets={[
        "Trek catalogue holds package structure.",
        "Live map on Overview shows next stops for active batches.",
        "Update guides via Guide & Staff Management.",
      ]}
    />
  );
}
