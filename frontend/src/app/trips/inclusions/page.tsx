"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Inclusions & Exclusions"
      breadcrumb="Inclusions & Exclusions"
      subtitle="What is included on trek products for sales and website"
      bullets={[
        "Edit inclusions / exclusions from trek detail",
        "Keep sales scripts aligned with catalogue",
        "Reuse lists across similar packages",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
      ]}
    />
  );
}
