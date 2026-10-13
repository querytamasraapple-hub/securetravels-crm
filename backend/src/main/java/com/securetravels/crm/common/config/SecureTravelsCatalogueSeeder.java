package com.securetravels.crm.common.config;

import com.securetravels.crm.trip.Trip;
import com.securetravels.crm.trip.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Upserts the live Secure Travels catalogue (https://securetravels.in/) so CRM
 * treks/packages stay usable for itineraries, quotations and batches even when
 * {@code app.bootstrap-demo-data=false} (Docker prod profile).
 */
@Component
@Order(200)
public class SecureTravelsCatalogueSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecureTravelsCatalogueSeeder.class);

    private final TripRepository trips;

    public SecureTravelsCatalogueSeeder(TripRepository trips) {
        this.trips = trips;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int created = 0;
        int updated = 0;
        for (CatalogueTrip c : CATALOGUE) {
            var existing = trips.findBySlug(c.slug());
            if (existing.isPresent()) {
                Trip t = existing.get();
                apply(t, c);
                trips.save(t);
                updated++;
            } else {
                Trip t = new Trip();
                apply(t, c);
                trips.save(t);
                created++;
            }
        }
        log.info("[catalogue] Secure Travels treks synced: {} created, {} updated (source securetravels.in)",
                created, updated);
    }

    private void apply(Trip t, CatalogueTrip c) {
        t.setName(c.name());
        t.setSlug(c.slug());
        t.setCategory(c.category());
        t.setBookingType(c.bookingType());
        t.setDifficulty(c.difficulty());
        t.setBaseCost(new BigDecimal(c.baseCost()));
        t.setDurationDays(c.durationDays());
        t.setItinerary(c.itinerary());
        t.setInclusions(c.inclusions());
        t.setExclusions(c.exclusions());
        t.setActive(true);
    }

    private record CatalogueTrip(
            String name,
            String slug,
            Trip.Category category,
            Trip.BookingType bookingType,
            Trip.Difficulty difficulty,
            String baseCost,
            int durationDays,
            String itinerary,
            String inclusions,
            String exclusions
    ) {}

    private static CatalogueTrip trek(String name, String slug, Trip.Difficulty diff,
                                      String cost, int days, String region, String season) {
        return new CatalogueTrip(
                name, slug, Trip.Category.TREK, Trip.BookingType.FIXED_BATCH, diff, cost, days,
                """
                Day 1: Assemble / drive to base (%s). Briefing & gear check.
                Day 2–%d: Trek as per Secure Travels field itinerary — camps, meals, summit / high point.
                Final day: Descend to roadhead and depart.

                Region: %s · Best season: %s
                Source: https://securetravels.in/
                """.formatted(region, Math.max(2, days - 1), region, season),
                "Trek guide, forest permits (where applicable), camping / stay as per plan, meals on trek days, first-aid support",
                "Personal trekking gear, travel to pickup point, travel insurance, tips, personal expenses"
        );
    }

    private static CatalogueTrip pilgrimage(String name, String slug, Trip.Difficulty diff,
                                            String cost, int days, String region) {
        return new CatalogueTrip(
                name, slug, Trip.Category.PILGRIMAGE, Trip.BookingType.FIXED_BATCH, diff, cost, days,
                """
                Day 1: Pickup & drive toward %s region. Hotel / camp check-in.
                Mid days: Temple visits, trek legs (if any), puja assistance as per package.
                Final day: Return transfer / drop.

                Region: %s
                Source: https://securetravels.in/
                """.formatted(region, region),
                "Accommodation as per itinerary, transfers, guide / local support, breakfast & dinner (package specific)",
                "Flights / trains to joining point, personal offerings, tips, travel insurance"
        );
    }

    private static CatalogueTrip leisure(String name, String slug, Trip.Difficulty diff,
                                         String cost, int days, String region, boolean customFit) {
        return new CatalogueTrip(
                name, slug, Trip.Category.LEISURE,
                customFit ? Trip.BookingType.CUSTOM_FIT : Trip.BookingType.FIXED_BATCH,
                diff, cost, days,
                """
                Day 1: Arrival / start of road trip (%s).
                Mid days: Sightseeing, stays and activities as per Secure Travels package.
                Final day: Departure transfer.

                Region: %s
                Source: https://securetravels.in/
                """.formatted(region, region),
                "Hotels / camps as per plan, sightseeing transfers, breakfast (package specific), trip captain support",
                "Flights, lunches unless mentioned, adventure activities marked optional, personal expenses"
        );
    }

    /** Catalogue mirrored from https://securetravels.in/ (selling prices where listed). */
    private static final List<CatalogueTrip> CATALOGUE = List.of(
            // —— Uttarakhand treks ——
            trek("Kedarkantha Trek", "kedarkantha-trek", Trip.Difficulty.MODERATE,
                    "3999.00", 5, "Uttarakhand (Sankri)", "Nov–March"),
            trek("Kedarkantha Trek (Hometown)", "kedarkantha-trek-hometown", Trip.Difficulty.MODERATE,
                    "9999.00", 9, "Uttarakhand", "Nov–March"),
            trek("Dayara Bugyal Trek", "dayara-bugyal-trek", Trip.Difficulty.MODERATE,
                    "4500.00", 4, "Uttarakhand", "Dec–March"),
            trek("Valley of Flowers Trek", "valley-of-flowers-trek", Trip.Difficulty.MODERATE,
                    "12499.00", 6, "Uttarakhand (Govindghat)", "Jul–Sep"),
            trek("Nag Tibba Trek", "nag-tibba-trek", Trip.Difficulty.EASY,
                    "3499.00", 3, "Uttarakhand (Dehradun)", "Year-round (best Oct–Jun)"),
            trek("Chopta Tungnath Trek", "chopta-tungnath-trek", Trip.Difficulty.EASY,
                    "6500.00", 3, "Uttarakhand (Chopta)", "Dec–February"),
            trek("Har Ki Dun Trek", "har-ki-dun-trek", Trip.Difficulty.MODERATE,
                    "9499.00", 7, "Uttarakhand (Sankri)", "Dec–March"),
            trek("Kuari Pass Trek", "kuari-pass-trek", Trip.Difficulty.MODERATE,
                    "7999.00", 6, "Uttarakhand (Auli / Joshimath)", "Dec–March"),
            trek("Brahmatal Trek", "brahmatal-trek", Trip.Difficulty.MODERATE,
                    "6999.00", 6, "Uttarakhand", "Dec–March"),
            trek("Phulara Ridge Trek", "phulara-ridge-trek", Trip.Difficulty.MODERATE,
                    "8999.00", 6, "Uttarakhand", "Dec–March"),
            leisure("The Best of Uttarakhand", "best-of-uttarakhand", Trip.Difficulty.EASY,
                    "18999.00", 8, "Uttarakhand", false),

            // —— Himachal ——
            trek("Hampta Pass Trek", "hampta-pass-trek", Trip.Difficulty.MODERATE,
                    "5999.00", 5, "Himachal (Manali / Jobra)", "Jun–Sep"),
            trek("Sar Pass Trek", "sar-pass-trek", Trip.Difficulty.MODERATE,
                    "8499.00", 5, "Himachal (Kasol)", "May–Jun"),
            leisure("Jibhi Tirthan Valley Road Trip", "jibhi-tirthan-valley-road-trip", Trip.Difficulty.EASY,
                    "12999.00", 5, "Himachal Pradesh", false),
            leisure("Kasol – Manali – Solang Valley Tour", "kasol-manali-solang-valley-tour", Trip.Difficulty.EASY,
                    "9999.00", 4, "Himachal Pradesh", false),
            leisure("Mcleodganj – Bir – Kasol – Manali Tour", "mcleodganj-bir-kasol-manali-tour", Trip.Difficulty.EASY,
                    "14999.00", 6, "Himachal Pradesh", false),
            leisure("Amritsar – McLeodganj – Bir – Kasol – Manali Tour", "amritsar-mcleodganj-bir-kasol-manali-tour",
                    Trip.Difficulty.EASY, "16999.00", 8, "Punjab & Himachal", false),

            // —— Maharashtra weekend treks ——
            trek("Kalsubai Trek", "kalsubai-trek", Trip.Difficulty.EASY,
                    "1399.00", 2, "Maharashtra", "Jun–February"),
            trek("Andharban Jungle Trek", "andharban-jungle-trek", Trip.Difficulty.EASY,
                    "1549.00", 1, "Maharashtra", "Jun–February"),
            trek("Devkund Waterfall Trek", "devkund-waterfall-trek", Trip.Difficulty.EASY,
                    "1549.00", 1, "Maharashtra", "Jun–February"),
            trek("Rajmachi Fireflies Camping Trek", "rajmachi-fireflies-camping-trek", Trip.Difficulty.EASY,
                    "1999.00", 2, "Maharashtra", "Jun–Jul (fireflies)"),
            trek("Harishchandragad Night Trek", "harishchandragad-night-trek", Trip.Difficulty.MODERATE,
                    "1449.00", 2, "Maharashtra", "Jun–February"),

            // —— Kerala ——
            leisure("Kerala Full Circuit Tours", "kerala-full-circuit-tours", Trip.Difficulty.EASY,
                    "17499.00", 10, "Kerala", false),
            leisure("Kerala Short Getaway", "kerala-short-getaway", Trip.Difficulty.EASY,
                    "10499.00", 8, "Kerala", false),

            // —— Spiritual / Pilgrimage ——
            pilgrimage("Kedarnath Yatra", "kedarnath-yatra", Trip.Difficulty.DIFFICULT,
                    "9499.00", 4, "Uttarakhand"),
            pilgrimage("Kedarnath with Chopta Tungnath Trek", "kedarnath-with-chopta-tungnath-trek",
                    Trip.Difficulty.DIFFICULT, "11500.00", 5, "Uttarakhand"),
            pilgrimage("Do Dham Yatra", "do-dham-yatra", Trip.Difficulty.DIFFICULT,
                    "13499.00", 6, "Uttarakhand"),
            pilgrimage("Kedar & Badri with Chopta Tungnath Trek", "kedar-badri-with-chopta-tungnath-trek",
                    Trip.Difficulty.DIFFICULT, "13499.00", 6, "Uttarakhand"),
            pilgrimage("Adi Kailash & Om Parvat Yatra", "adi-kailash-om-parvat-yatra",
                    Trip.Difficulty.DIFFICULT, "36999.00", 6, "Uttarakhand / Pithoragarh"),
            pilgrimage("Char Dham Yatra", "char-dham-yatra", Trip.Difficulty.VERY_DIFFICULT,
                    "19999.00", 10, "Uttarakhand"),

            // —— Ladakh ——
            leisure("Ladakh Bike Trip 6D/5N", "ladakh-bike-trip-6d-5n", Trip.Difficulty.DIFFICULT,
                    "10999.00", 6, "Ladakh", false),
            leisure("Ladakh Bike Trip 7D/6N", "ladakh-bike-trip-7d-6n", Trip.Difficulty.DIFFICULT,
                    "13999.00", 7, "Ladakh", false),
            leisure("Ladakh Bike Trip 8D/7N", "ladakh-bike-trip-8d-7n", Trip.Difficulty.DIFFICULT,
                    "19999.00", 8, "Ladakh", false),

            // —— Spiti ——
            leisure("Summer Spiti Valley Tour", "summer-spiti-valley-tour", Trip.Difficulty.MODERATE,
                    "17999.00", 8, "Himachal – Spiti", false),
            leisure("Winter Spiti Valley Tour", "winter-spiti-valley-tour", Trip.Difficulty.DIFFICULT,
                    "20999.00", 8, "Himachal – Spiti", false),
            leisure("Spiti Valley Tour", "spiti-valley-tour", Trip.Difficulty.MODERATE,
                    "17999.00", 7, "Himachal – Spiti", true)
    );
}
