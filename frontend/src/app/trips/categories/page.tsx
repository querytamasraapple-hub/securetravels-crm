"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Categories & Tags"
      breadcrumb="Categories & Tags"
      subtitle="Trek categories, difficulty tags and catalogue taxonomy"
      bullets={[
        "Category filters on Manage Treks",
        "Terrain and season tags on cards",
        "Settings for featured taxonomy",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
      ]}
    />
  );
}
