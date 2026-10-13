"use client";

import { OPS_DESK } from "@/lib/rbac";
import { ModuleStub } from "@/components/ModuleStub";

export default function Page() {
  return (
    <ModuleStub
      title={"Departures & Trips"}
      subtitle={"Upcoming and live trek departures"}
      allowedRoles={OPS_DESK}
      bullets={[
        "Use Operations Overview batch table for departure status.",
        "Calendar shows seat fill by departure date.",
        "Bookings Operations desk manages hotel and transport handoffs.",
      ]}
    />
  );
}
