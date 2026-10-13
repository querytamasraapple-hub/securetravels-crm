"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Transport Management"}
      subtitle={"Vehicles, routes and on-road status"}
      allowedRoles={OPS_DESK}
      bullets={[
        "Overview Transport card shows fleet utilisation.",
        "Vendors directory holds transport partners.",
        "Mark transport CONFIRMED on Bookings Operations desk.",
      ]}
    />
  );
}
