"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Images & Media"
      breadcrumb="Images & Media"
      subtitle="Cover images, galleries and media for trek pages"
      bullets={[
        "Cover image on Manage Treks quick panel",
        "Gallery slots for website and WhatsApp",
        "Keep media linked to trek slug",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
      ]}
    />
  );
}
