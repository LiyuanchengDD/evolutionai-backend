package com.example.grpcdemo.location;

import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides ISO 3166 country and subdivision data sourced from the nv-i18n library.
 * <p>
 * The catalog lazily resolves localized display names for a caller supplied {@link Locale}
 * ensuring that new locales automatically reuse the underlying ISO data set.
 */
@Component
public class LocationCatalog {

    public record LocationOption(String code, String name) { }

    private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    private final List<String> countryCodes;
    private final Set<String> countryCodeSet;
    private final Map<String, List<Object>> subdivisionsByCountry;

    public LocationCatalog() {
        this.countryCodes = Arrays.stream(Locale.getISOCountries())
                .map(code -> code.toUpperCase(Locale.ROOT))
                .sorted()
                .toList();
        this.countryCodeSet = new HashSet<>(countryCodes);
        this.subdivisionsByCountry = loadSubdivisions();
    }

    public List<LocationOption> listCountries(Locale locale) {
        Locale targetLocale = normalizeLocale(locale);
        Collator collator = Collator.getInstance(targetLocale);
        return countryCodes.stream()
                .map(code -> new LocationOption(code, localizedCountryName(code, targetLocale)))
                .sorted(Comparator.comparing(LocationOption::name, collator))
                .toList();
    }

    public List<LocationOption> listCities(String countryCode, Locale locale) {
        Locale targetLocale = normalizeLocale(locale);
        Collator collator = Collator.getInstance(targetLocale);
        return subdivisionsByCountry.getOrDefault(normalizeCode(countryCode), List.of()).stream()
                .map(subdivision -> new LocationOption(subdivisionCode(subdivision), localizedSubdivisionName(subdivision, targetLocale)))
                .sorted(Comparator.comparing(LocationOption::name, collator))
                .toList();
    }

    public Optional<LocationOption> findCountry(String countryCode, Locale locale) {
        Locale targetLocale = normalizeLocale(locale);
        String normalizedCode = normalizeCode(countryCode);
        if (normalizedCode == null || !countryCodeSet.contains(normalizedCode)) {
            return Optional.empty();
        }
        return Optional.of(new LocationOption(normalizedCode, localizedCountryName(normalizedCode, targetLocale)));
    }

    public Optional<LocationOption> findCity(String countryCode, String cityCode, Locale locale) {
        Locale targetLocale = normalizeLocale(locale);
        String normalizedCountry = normalizeCode(countryCode);
        String normalizedCity = normalizeCode(cityCode);
        return subdivisionsByCountry.getOrDefault(normalizedCountry, List.of()).stream()
                .filter(subdivision -> Objects.equals(subdivisionCode(subdivision), normalizedCity))
                .findFirst()
                .map(subdivision -> new LocationOption(subdivisionCode(subdivision), localizedSubdivisionName(subdivision, targetLocale)));
    }

    private static String localizedCountryName(String countryCode, Locale locale) {
        Locale displayLocale = locale == null ? DEFAULT_LOCALE : locale;
        Locale countryLocale = new Locale("", countryCode);
        String localized = countryLocale.getDisplayCountry(displayLocale);
        if (localized == null || localized.isBlank() || localized.equalsIgnoreCase(countryCode)) {
            localized = countryLocale.getDisplayCountry(Locale.ENGLISH);
        }
        return localized;
    }

    private static Map<String, List<Object>> loadSubdivisions() {
        Class<?> subdivisionClass = loadSubdivisionClass();
        if (subdivisionClass == null) {
            return Map.of();
        }
        Object[] constants = subdivisionClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return Map.of();
        }

        return Arrays.stream(constants)
                .map(subdivision -> new SubdivisionEntry(subdivision, extractCountryCode(subdivision)))
                .filter(entry -> entry.countryCode() != null)
                .collect(Collectors.groupingBy(SubdivisionEntry::countryCode,
                        Collectors.mapping(SubdivisionEntry::value, Collectors.toList())));
    }

    private static Class<?> loadSubdivisionClass() {
        try {
            return Class.forName("com.neovisionaries.i18n.CountrySubdivision");
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private static String extractCountryCode(Object subdivision) {
        if (subdivision == null) {
            return null;
        }
        Object country = invokeMethod(subdivision, "getCountry");
        if (country == null) {
            return null;
        }
        Object alpha2 = invokeMethod(country, "getAlpha2");
        if (alpha2 == null) {
            return null;
        }
        return normalizeCode(alpha2.toString());
    }

    private static String localizedSubdivisionName(Object subdivision, Locale locale) {
        Locale targetLocale = locale == null ? DEFAULT_LOCALE : locale;
        String localized = invokeSubdivisionName(subdivision, targetLocale);
        if (localized == null || localized.isBlank()) {
            localized = invokeSubdivisionName(subdivision, Locale.ENGLISH);
        }
        if (localized == null || localized.isBlank()) {
            localized = humanizeCode(subdivisionCode(subdivision));
        }
        return localized;
    }

    private static String subdivisionCode(Object subdivision) {
        Object value = invokeMethod(subdivision, "getCode");
        if (value != null) {
            return value.toString();
        }
        value = invokeMethod(subdivision, "name");
        if (value != null) {
            return humanizeCode(value.toString());
        }
        return subdivision == null ? "" : humanizeCode(subdivision.toString());
    }

    private static String invokeSubdivisionName(Object subdivision, Locale locale) {
        Object value = invokeMethod(subdivision, "getName", new Class<?>[]{Locale.class}, new Object[]{locale});
        if (value != null) {
            return value.toString();
        }
        value = invokeMethod(subdivision, "getName");
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    private static Object invokeMethod(Object target, String methodName) {
        return invokeMethod(target, methodName, new Class<?>[0], new Object[0]);
    }

    private static Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String humanizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.replace('_', '-');
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private static Locale normalizeLocale(Locale locale) {
        return locale == null ? DEFAULT_LOCALE : locale;
    }

    private record SubdivisionEntry(Object value, String countryCode) { }
}
