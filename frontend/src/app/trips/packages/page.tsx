"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Manage Packages"
      breadcrumb="Manage Packages"
      subtitle="Group, custom and seasonal packages built on trek products"
      bullets={[
        "Map package variants to base treks",
        "Group / custom / seasonal packaging",
        "Link packages to pricing and batches",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
        { href: "/trips/pricing", label: "Pricing" },
        { href: "/batches", label: "Batches" },
      ]}
    />
  );
}
