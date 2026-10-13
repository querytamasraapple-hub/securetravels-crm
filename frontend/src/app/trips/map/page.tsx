"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Map & Route Builder"
      breadcrumb="Map & Route Builder"
      subtitle="Route maps and GPS waypoints for trek pages"
      bullets={[
        "Plot start / camp / summit points",
        "Embed maps on live trek pages",
        "Share route packs with operations",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
        { href: "/operations", label: "Operations" },
      ]}
    />
  );
}
