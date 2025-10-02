package com.example.grpcdemo.location;

import com.neovisionaries.i18n.CountryCode;
import com.neovisionaries.i18n.SubdivisionCode;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Provides ISO-3166 country and subdivision (city/province) lookups for onboarding forms.
 */
@Component
public class LocationCatalog {

    private static final Map<String, CountryCode> OFFICIAL_COUNTRIES = buildOfficialCountryMap();
    private static final List<String> SORTED_COUNTRY_CODES = buildSortedCountryCodes();
    private static final Map<String, List<LocalizedSubdivision>> SUBDIVISION_CATALOG = createSubdivisionCatalog();

    public List<LocationOption> getCountries(Locale locale) {
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        List<LocationOption> options = new ArrayList<>();
        for (String code : SORTED_COUNTRY_CODES) {
            Locale countryLocale = new Locale("", code);
            String displayName = normalizeCountryName(code, countryLocale.getDisplayCountry(displayLocale));
            options.add(new LocationOption(code, displayName));
        }
        return Collections.unmodifiableList(options);
    }

    public List<LocationOption> getCities(String countryCode, Locale locale) {
        if (countryCode == null) {
            return Collections.emptyList();
        }
        String normalizedCountry = countryCode.toUpperCase(Locale.ROOT);
        List<LocalizedSubdivision> subdivisions = SUBDIVISION_CATALOG.get(normalizedCountry);
        if (subdivisions == null || subdivisions.isEmpty()) {
            return Collections.emptyList();
        }
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        return subdivisions.stream()
                .map(subdivision -> new LocationOption(subdivision.code(), subdivision.displayName(displayLocale)))
                .sorted(Comparator.comparing(LocationOption::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toUnmodifiableList());
    }

    public Optional<LocationOption> findCountry(String countryCode, Locale locale) {
        if (countryCode == null) {
            return Optional.empty();
        }
        String normalizedCountry = countryCode.toUpperCase(Locale.ROOT);
        if (!OFFICIAL_COUNTRIES.containsKey(normalizedCountry)) {
            return Optional.empty();
        }
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        Locale countryLocale = new Locale("", normalizedCountry);
        String displayName = normalizeCountryName(normalizedCountry, countryLocale.getDisplayCountry(displayLocale));
        return Optional.of(new LocationOption(normalizedCountry, displayName));
    }

    public Optional<LocationOption> findCity(String countryCode, String subdivisionCode, Locale locale) {
        if (countryCode == null || subdivisionCode == null) {
            return Optional.empty();
        }
        String normalizedCountry = countryCode.toUpperCase(Locale.ROOT);
        List<LocalizedSubdivision> subdivisions = SUBDIVISION_CATALOG.get(normalizedCountry);
        if (subdivisions == null || subdivisions.isEmpty()) {
            return Optional.empty();
        }
        String normalizedSubdivision = subdivisionCode.toUpperCase(Locale.ROOT);
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        return subdivisions.stream()
                .filter(subdivision -> subdivision.code().equalsIgnoreCase(normalizedSubdivision))
                .findFirst()
                .map(subdivision -> new LocationOption(subdivision.code(), subdivision.displayName(displayLocale)));
    }

    public record LocationOption(String code, String name) {
    }

    private static Map<String, List<LocalizedSubdivision>> createSubdivisionCatalog() {
        Map<String, List<LocalizedSubdivision>> catalog = Arrays.stream(SubdivisionCode.values())
                .map(LocationCatalog::toLocalizedSubdivision)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(LocalizedSubdivision::countryCode,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), subdivisions -> {
                            subdivisions.sort(Comparator.comparing(LocalizedSubdivision::englishName, buildEnglishCollator())
                                    .thenComparing(LocalizedSubdivision::code));
                            return Collections.unmodifiableList(subdivisions);
                        })));
        return Collections.unmodifiableMap(catalog);
    }

    private static Map<String, CountryCode> buildOfficialCountryMap() {
        Map<String, CountryCode> countries = Arrays.stream(CountryCode.values())
                .filter(countryCode -> countryCode != null && countryCode.isOfficiallyAssigned())
                .map(countryCode -> Map.entry(normalizeAlpha2(countryCode.getAlpha2()), countryCode))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (existing, replacement) -> existing, TreeMap::new));
        return Collections.unmodifiableMap(countries);
    }

    private static List<String> buildSortedCountryCodes() {
        Collator englishCollator = buildEnglishCollator();
        return OFFICIAL_COUNTRIES.values().stream()
                .map(countryCode -> normalizeAlpha2(countryCode.getAlpha2()))
                .filter(Objects::nonNull)
                .distinct()
                .sorted((left, right) -> {
                    String leftName = normalizeCountryName(left, new Locale("", left).getDisplayCountry(Locale.ENGLISH));
                    String rightName = normalizeCountryName(right, new Locale("", right).getDisplayCountry(Locale.ENGLISH));
                    int comparison = englishCollator.compare(leftName, rightName);
                    if (comparison == 0) {
                        comparison = left.compareTo(right);
                    }
                    return comparison;
                })
                .collect(Collectors.toUnmodifiableList());
    }

    private static LocalizedSubdivision toLocalizedSubdivision(SubdivisionCode subdivisionCode) {
        if (subdivisionCode == null) {
            return null;
        }
        CountryCode countryCode = subdivisionCode.getCountry();
        String countryAlpha2 = countryCode != null ? normalizeAlpha2(countryCode.getAlpha2()) : null;
        if (countryAlpha2 == null || !OFFICIAL_COUNTRIES.containsKey(countryAlpha2)) {
            return null;
        }
        String code = subdivisionCode.getCode();
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalizedCode = countryAlpha2 + "-" + code.toUpperCase(Locale.ROOT);
        String englishName = safeSubdivisionName(subdivisionCode.getName(), normalizedCode);
        String localName = normalize(subdivisionCode.getLocalName());
        return new LocalizedSubdivision(countryAlpha2, normalizedCode, englishName, localName);
    }

    private static String normalizeCountryName(String alpha2, String candidate) {
        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }
        CountryCode countryCode = OFFICIAL_COUNTRIES.get(alpha2);
        if (countryCode != null) {
            String name = countryCode.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return alpha2;
    }

    private static String normalizeAlpha2(String alpha2) {
        if (alpha2 == null || alpha2.length() != 2) {
            return null;
        }
        String normalized = alpha2.toUpperCase(Locale.ROOT);
        for (int i = 0; i < normalized.length(); i++) {
            if (!Character.isLetter(normalized.charAt(i))) {
                return null;
            }
        }
        return normalized;
    }

    private static String safeSubdivisionName(String candidate, String fallback) {
        if (candidate != null) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return fallback;
    }

    private static String normalize(String candidate) {
        if (candidate == null) {
            return null;
        }
        String trimmed = candidate.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Collator buildEnglishCollator() {
        Collator collator = Collator.getInstance(Locale.ENGLISH);
        collator.setStrength(Collator.PRIMARY);
        return collator;
    }

    private record LocalizedSubdivision(String countryCode, String code, String englishName, String localName) {
        String displayName(Locale locale) {
            if (locale != null && "zh".equalsIgnoreCase(locale.getLanguage()) && localName != null) {
                return localName;
            }
            return englishName;
        }
    }
}
