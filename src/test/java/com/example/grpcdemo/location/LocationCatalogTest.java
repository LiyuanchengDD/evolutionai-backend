package com.example.grpcdemo.location;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationCatalogTest {

    private final LocationCatalog catalog = new LocationCatalog();

    @Test
    void listCountriesReturnsInternationalizedOptions() {
        List<LocationCatalog.LocationOption> countries = catalog.listCountries(Locale.ENGLISH);
        assertFalse(countries.isEmpty(), "Country list should not be empty");
        assertTrue(countries.stream().anyMatch(option -> option.code().equals("CN")), "China should be present in the list");
    }

    @Test
    void listCitiesReturnsSubdivisionsForKnownCountry() {
        List<LocationCatalog.LocationOption> cities = catalog.listCities("CN", Locale.SIMPLIFIED_CHINESE);
        assertFalse(cities.isEmpty(), "Chinese subdivisions should be available");
        assertTrue(cities.stream().anyMatch(option -> option.code().equals("CN-11")), "Beijing subdivision should exist");
    }
}
