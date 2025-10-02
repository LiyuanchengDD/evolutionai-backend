package com.example.grpcdemo.location;

import com.neovisionaries.i18n.CountryCode;
import com.neovisionaries.i18n.SubdivisionCode;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ReflectiveOperationException;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides ISO-3166 country and subdivision (city/province) lookups for onboarding forms.
 */
@Component
public class LocationCatalog {

    private static final Method COUNTRY_GET_ALPHA2_METHOD = findCountryMethod("getAlpha2");
    private static final Method COUNTRY_IS_OFFICIALLY_ASSIGNED_METHOD = findCountryMethod("isOfficiallyAssigned");
    private static final Set<String> ISO_COUNTRY_CODES = Arrays.stream(CountryCode.values())
            .map(LocationCatalog::alpha2Code)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());

    private static final Method SUBDIVISION_GET_NAME_METHOD = findSubdivisionMethod("getName");
    private static final Method SUBDIVISION_GET_LOCAL_NAME_METHOD = findSubdivisionMethod("getLocalName");
    private static final Map<String, List<LocalizedSubdivision>> SUBDIVISION_CATALOG = createSubdivisionCatalog();

    public List<LocationOption> getCountries(Locale locale) {
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        List<LocationOption> options = new ArrayList<>();
        for (String code : ISO_COUNTRY_CODES) {
            Locale countryLocale = new Locale("", code);
            String name = countryLocale.getDisplayCountry(displayLocale);
            if (name == null || name.isBlank()) {
                continue;
            }
            options.add(new LocationOption(code, name));
        }
        options.sort(Comparator.comparing(LocationOption::name, String.CASE_INSENSITIVE_ORDER));
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
        if (!ISO_COUNTRY_CODES.contains(normalizedCountry)) {
            return Optional.empty();
        }
        Locale displayLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        Locale countryLocale = new Locale("", normalizedCountry);
        return Optional.of(new LocationOption(normalizedCountry, countryLocale.getDisplayCountry(displayLocale)));
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
                            subdivisions.sort(Comparator.comparing(LocalizedSubdivision::englishName, String.CASE_INSENSITIVE_ORDER));
                            return Collections.unmodifiableList(subdivisions);
                        })));
        return Collections.unmodifiableMap(catalog);
    }

    private static String alpha2Code(CountryCode countryCode) {
        if (countryCode == null) {
            return null;
        }
        if (!isOfficiallyAssigned(countryCode)) {
            return null;
        }
        String alpha2 = invokeCountryAlpha2(countryCode);
        if (alpha2 == null || alpha2.isBlank()) {
            return null;
        }
        String normalized = alpha2.toUpperCase(Locale.ROOT);
        if (normalized.length() != 2) {
            return null;
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (!Character.isLetter(normalized.charAt(i))) {
                return null;
            }
        }
        return normalized;
    }

    private static LocalizedSubdivision toLocalizedSubdivision(SubdivisionCode subdivisionCode) {
        if (subdivisionCode == null) {
            return null;
        }
        String isoCode = subdivisionCode.name().replace('_', '-');
        int separatorIndex = isoCode.indexOf('-');
        if (separatorIndex <= 0) {
            return null;
        }
        String countryAlpha2 = isoCode.substring(0, separatorIndex);
        if (!ISO_COUNTRY_CODES.contains(countryAlpha2)) {
            return null;
        }
        String englishName = invokeSubdivisionName(subdivisionCode, SUBDIVISION_GET_NAME_METHOD);
        if (englishName == null || englishName.isBlank()) {
            englishName = isoCode;
        }
        String localName = invokeSubdivisionName(subdivisionCode, SUBDIVISION_GET_LOCAL_NAME_METHOD);
        if (localName != null && localName.isBlank()) {
            localName = null;
        }
        return new LocalizedSubdivision(countryAlpha2, isoCode, englishName, localName);
    }

    private record LocalizedSubdivision(String countryCode, String code, String englishName, String localName) {
        String displayName(Locale locale) {
            if (locale != null && "zh".equalsIgnoreCase(locale.getLanguage()) && localName != null) {
                return localName;
            }
            return englishName;
        }
    }

    private static Method findSubdivisionMethod(String methodName) {
        try {
            return SubdivisionCode.class.getMethod(methodName);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findCountryMethod(String methodName) {
        try {
            return CountryCode.class.getMethod(methodName);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static boolean isOfficiallyAssigned(CountryCode countryCode) {
        if (COUNTRY_IS_OFFICIALLY_ASSIGNED_METHOD == null) {
            return true;
        }
        try {
            Object value = COUNTRY_IS_OFFICIALLY_ASSIGNED_METHOD.invoke(countryCode);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
        return true;
    }

    private static String invokeCountryAlpha2(CountryCode countryCode) {
        if (COUNTRY_GET_ALPHA2_METHOD == null) {
            return countryCode.name();
        }
        try {
            Object value = COUNTRY_GET_ALPHA2_METHOD.invoke(countryCode);
            if (value instanceof String stringValue) {
                return stringValue;
            }
        } catch (ReflectiveOperationException ignored) {
            return countryCode.name();
        }
        return countryCode.name();
    }

    private static String invokeSubdivisionName(SubdivisionCode subdivisionCode, Method method) {
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(subdivisionCode);
            if (value instanceof String stringValue) {
                return stringValue;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback handled by caller.
        }
        return null;
    }
}
