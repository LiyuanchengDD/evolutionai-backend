package com.example.grpcdemo.location;

import com.neovisionaries.i18n.CountrySubdivision;
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
    private final Map<String, List<CountrySubdivision>> subdivisionsByCountry;

    public LocationCatalog() {
        this.countryCodes = Arrays.stream(Locale.getISOCountries())
                .map(code -> code.toUpperCase(Locale.ROOT))
                .sorted()
                .toList();
        this.countryCodeSet = new HashSet<>(countryCodes);
        this.subdivisionsByCountry = Arrays.stream(CountrySubdivision.values())
                .filter(subdivision -> subdivision.getCountry() != null)
                .filter(subdivision -> normalizeCode(subdivision.getCountry().getAlpha2()) != null)
                .collect(Collectors.groupingBy(subdivision -> normalizeCode(subdivision.getCountry().getAlpha2())));
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

    private static String localizedSubdivisionName(CountrySubdivision subdivision, Locale locale) {
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

    private static String subdivisionCode(CountrySubdivision subdivision) {
        try {
            Object value = CountrySubdivision.class.getMethod("getCode").invoke(subdivision);
            if (value != null) {
                return value.toString();
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall back to enum name if the method is unavailable in the runtime version.
        }
        return humanizeCode(subdivision.name());
    }

    private static String invokeSubdivisionName(CountrySubdivision subdivision, Locale locale) {
        try {
            Object value = CountrySubdivision.class.getMethod("getName", Locale.class).invoke(subdivision, locale);
            if (value != null) {
                return value.toString();
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall back to default name.
        }
        try {
            Object value = CountrySubdivision.class.getMethod("getName").invoke(subdivision);
            if (value != null) {
                return value.toString();
            }
        } catch (ReflectiveOperationException ignored) {
            // Ignore and fall back to code-based humanisation.
        }
        return null;
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
}
