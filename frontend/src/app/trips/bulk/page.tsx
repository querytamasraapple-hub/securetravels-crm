"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Clone & Bulk Actions"
      breadcrumb="Clone & Bulk Actions"
      subtitle="Clone treks, bulk activate and multi-select actions"
      bullets={[
        "Clone trek from quick actions panel",
        "Bulk select rows on Manage Treks table",
        "Import treks from CSV (coming online)",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
      ]}
    />
  );
}
