"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Itineraries & Days"
      breadcrumb="Itineraries & Days"
      subtitle="Day-wise trek plans, camps and activity blocks"
      bullets={[
        "Edit day-by-day itinerary copy on each trek",
        "Align stay and transport legs with operations",
        "Preview itinerary on the live trek page",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
        { href: "/operations/itinerary", label: "Ops Itinerary" },
      ]}
    />
  );
}
