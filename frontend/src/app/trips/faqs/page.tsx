"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="FAQs"
      breadcrumb="FAQs"
      subtitle="Frequently asked questions for each trek and package"
      bullets={[
        "Common fitness, gear and permit questions",
        "Per-trek FAQ blocks for the live site",
        "Reuse templates across similar treks",
      ]}
      links={[
        { href: "/trips/highlights", label: "Highlights" },
        { href: "/trips", label: "Manage Treks" },
      ]}
    />
  );
}
