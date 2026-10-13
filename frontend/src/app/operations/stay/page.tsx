"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Stay Management"}
      subtitle={"Camps, hotels and base stays"}
      allowedRoles={OPS_DESK}
      bullets={[
        "Overview Stay & Camps card tracks allotments.",
        "Hotel status is updated on Bookings Operations.",
        "Vendors holds hotel partners.",
      ]}
    />
  );
}
