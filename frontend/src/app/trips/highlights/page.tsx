"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Highlights & FAQs"
      breadcrumb="Highlights"
      subtitle="Trek highlights and selling points for cards and landing pages"
      bullets={[
        "Featured highlight chips on trek cards",
        "Homepage and SEO highlight copy",
        "Pair with FAQs for conversion",
      ]}
      links={[
        { href: "/trips/faqs", label: "FAQs" },
        { href: "/trips", label: "Manage Treks" },
      ]}
    />
  );
}
