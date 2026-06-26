package com.loyaltap.business.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class BusinessSlugUtils {

    private static final int MAX_SLUG_LENGTH = 120;

    private BusinessSlugUtils() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String slug = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.length() <= MAX_SLUG_LENGTH) {
            return slug;
        }

        return slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-$", "");
    }
}
