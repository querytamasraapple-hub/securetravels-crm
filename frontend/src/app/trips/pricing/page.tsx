"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Pricing & Discounts"
      breadcrumb="Pricing & Discounts"
      subtitle="Base price, seasonal rates and discount rules"
      bullets={[
        "Base cost per person from trek catalogue",
        "Seasonal and early-bird discounts",
        "Connect pricing to batch departures",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
        { href: "/batches", label: "Batches & Departures" },
      ]}
    />
  );
}
