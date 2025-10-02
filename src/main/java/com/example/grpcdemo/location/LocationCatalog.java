package com.example.grpcdemo.location;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides ISO-3166 country and subdivision (city/province) lookups for onboarding forms.
 */
@Component
public class LocationCatalog {

    private static final Set<String> ISO_COUNTRY_CODES = Arrays.stream(Locale.getISOCountries())
            .map(code -> code.toUpperCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

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
        Map<String, List<LocalizedSubdivision>> catalog = new LinkedHashMap<>();
        catalog.put("CN", List.of(
                new LocalizedSubdivision("CN-AH", "Anhui", "安徽省"),
                new LocalizedSubdivision("CN-BJ", "Beijing", "北京市"),
                new LocalizedSubdivision("CN-GD", "Guangdong", "广东省"),
                new LocalizedSubdivision("CN-SH", "Shanghai", "上海市"),
                new LocalizedSubdivision("CN-SC", "Sichuan", "四川省"),
                new LocalizedSubdivision("CN-ZJ", "Zhejiang", "浙江省")
        ));
        catalog.put("US", List.of(
                new LocalizedSubdivision("US-CA", "California", "加利福尼亚州"),
                new LocalizedSubdivision("US-NY", "New York", "纽约州"),
                new LocalizedSubdivision("US-TX", "Texas", "得克萨斯州"),
                new LocalizedSubdivision("US-WA", "Washington", "华盛顿州"),
                new LocalizedSubdivision("US-FL", "Florida", "佛罗里达州")
        ));
        catalog.put("CA", List.of(
                new LocalizedSubdivision("CA-BC", "British Columbia", "不列颠哥伦比亚省"),
                new LocalizedSubdivision("CA-ON", "Ontario", "安大略省"),
                new LocalizedSubdivision("CA-QC", "Quebec", "魁北克省"),
                new LocalizedSubdivision("CA-AB", "Alberta", "艾伯塔省")
        ));
        catalog.put("JP", List.of(
                new LocalizedSubdivision("JP-13", "Tokyo", "东京都"),
                new LocalizedSubdivision("JP-27", "Osaka", "大阪府"),
                new LocalizedSubdivision("JP-23", "Aichi", "爱知县"),
                new LocalizedSubdivision("JP-01", "Hokkaido", "北海道")
        ));
        catalog.put("DE", List.of(
                new LocalizedSubdivision("DE-BE", "Berlin", "柏林州"),
                new LocalizedSubdivision("DE-BY", "Bavaria", "巴伐利亚州"),
                new LocalizedSubdivision("DE-NW", "North Rhine-Westphalia", "北莱茵-威斯特法伦州"),
                new LocalizedSubdivision("DE-HE", "Hesse", "黑森州")
        ));
        return Collections.unmodifiableMap(catalog);
    }

    private record LocalizedSubdivision(String code, String englishName, String simplifiedChineseName) {
        String displayName(Locale locale) {
            if (locale != null && "zh".equalsIgnoreCase(locale.getLanguage()) && simplifiedChineseName != null) {
                return simplifiedChineseName;
            }
            return englishName;
        }
    }
}
