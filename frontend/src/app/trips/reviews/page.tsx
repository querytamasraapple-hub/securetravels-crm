"use client";

import { TreksModuleStub } from "@/components/treks/TreksModuleStub";

export default function Page() {
  return (
    <TreksModuleStub
      title="Reviews & Ratings"
      breadcrumb="Reviews & Ratings"
      subtitle="Customer ratings and testimonials for trek products"
      bullets={[
        "Average rating on Manage Treks KPIs",
        "Moderate published reviews",
        "Surface bestsellers from review volume",
      ]}
      links={[
        { href: "/trips", label: "Manage Treks" },
      ]}
    />
  );
}
