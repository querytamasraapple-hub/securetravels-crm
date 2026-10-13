"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="SEO & Content"
      breadcrumb="SEO & Content"
      subtitle="Meta titles, descriptions and content for trek landing pages"
      bullets={[
        "Slug and meta description per trek",
        "Open-graph imagery from media library",
        "Content briefs for marketing",
      ]}
      links={[
        { href: "/trips/media", label: "Media" },
        { href: "/trips", label: "Manage Treks" },
      ]}
    />
  );
}
