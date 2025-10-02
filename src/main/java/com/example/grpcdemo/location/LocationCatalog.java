package com.example.grpcdemo.location;

import com.neovisionaries.i18n.CountryCode;
import com.neovisionaries.i18n.Subdivision;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provides ISO-3166 country and subdivision (city/province) lookups for onboarding forms.
 */
@Component
public class LocationCatalog {

    private final Map<String, List<LocationOption>> subdivisionCache = new ConcurrentHashMap<>();

    public List<LocationOption> getCountries(Locale locale) {
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        List<LocationOption> options = new ArrayList<>();
        for (CountryCode code : CountryCode.values()) {
            if (!code.isAssigned()) {
                continue;
            }
            String alpha2 = code.getAlpha2();
            if (alpha2 == null) {
                continue;
            }
            options.add(new LocationOption(alpha2, code.getName(displayLocale)));
        }
        options.sort(Comparator.comparing(LocationOption::name, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableList(options);
    }

    public List<LocationOption> getCities(String countryCode, Locale locale) {
        if (countryCode == null) {
            return Collections.emptyList();
        }
        CountryCode code = CountryCode.getByCode(countryCode.toUpperCase(Locale.ROOT));
        if (code == null || !code.isAssigned()) {
            return Collections.emptyList();
        }
        List<LocationOption> cached = subdivisionCache.computeIfAbsent(code.getAlpha2(), key -> {
            List<Subdivision> subdivisions = code.getSubdivisionList();
            if (subdivisions == null) {
                return new ArrayList<>();
            }
            return subdivisions.stream()
                    .map(subdivision -> new LocationOption(subdivision.getCode(), subdivision.getName()))
                    .collect(Collectors.toCollection(ArrayList::new));
        });
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        return cached.stream()
                .map(option -> new LocationOption(option.code(), localizeSubdivision(option.code(), displayLocale)))
                .sorted(Comparator.comparing(LocationOption::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toUnmodifiableList());
    }

    public Optional<LocationOption> findCountry(String countryCode, Locale locale) {
        if (countryCode == null) {
            return Optional.empty();
        }
        CountryCode code = CountryCode.getByCode(countryCode.toUpperCase(Locale.ROOT));
        if (code == null || !code.isAssigned()) {
            return Optional.empty();
        }
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        return Optional.of(new LocationOption(code.getAlpha2(), code.getName(displayLocale)));
    }

    public Optional<LocationOption> findCity(String countryCode, String subdivisionCode, Locale locale) {
        if (countryCode == null || subdivisionCode == null) {
            return Optional.empty();
        }
        CountryCode code = CountryCode.getByCode(countryCode.toUpperCase(Locale.ROOT));
        if (code == null || !code.isAssigned()) {
            return Optional.empty();
        }
        String normalizedSubdivision = subdivisionCode.toUpperCase(Locale.ROOT);
        List<Subdivision> subdivisions = code.getSubdivisionList();
        if (subdivisions == null) {
            return Optional.empty();
        }
        for (Subdivision subdivision : subdivisions) {
            if (normalizedSubdivision.equalsIgnoreCase(subdivision.getCode())) {
                Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
                return Optional.of(new LocationOption(subdivision.getCode(),
                        subdivision.getName(displayLocale)));
            }
        }
        return Optional.empty();
    }

    private String localizeSubdivision(String subdivisionCode, Locale locale) {
        String[] segments = subdivisionCode.split("-");
        if (segments.length != 2) {
            return subdivisionCode;
        }
        CountryCode country = CountryCode.getByCode(segments[0]);
        if (country == null) {
            return subdivisionCode;
        }
        List<Subdivision> subdivisions = country.getSubdivisionList();
        if (subdivisions == null) {
            return subdivisionCode;
        }
        for (Subdivision subdivision : subdivisions) {
            if (subdivision.getCode().equalsIgnoreCase(subdivisionCode)) {
                return subdivision.getName(locale != null ? locale : Locale.SIMPLIFIED_CHINESE);
            }
        }
        return subdivisionCode;
    }

    public record LocationOption(String code, String name) {
    }
}
