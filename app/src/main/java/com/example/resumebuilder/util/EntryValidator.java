package com.example.resumebuilder.util;

import com.example.resumebuilder.model.FieldSpec;
import com.example.resumebuilder.model.SectionItem;
import com.example.resumebuilder.model.SectionType;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checks one section entry: required fields, formats, future dates and date order.
 * Optional fields are never required.
 */
public final class EntryValidator {

    public enum Error {
        REQUIRED, DATE_FORMAT, DATE_ORDER, DATE_FUTURE, EMAIL_FORMAT, PHONE_FORMAT
    }

    private static final String[][] DATE_PAIRS = {
            {"start_date", "end_date"},
            {"issue_date", "expiry_date"}
    };

    private EntryValidator() {
    }

    /** Validates against today's month. */
    public static Map<String, Error> validate(SectionItem item) {
        YearMonth now = YearMonth.now();
        return validate(item, now.getYear(), now.getMonthValue());
    }

    /** Returns the problems by field key, in form order. An empty map means the entry is valid. */
    public static Map<String, Error> validate(SectionItem item, int currentYear,
                                              int currentMonth) {
        Map<String, Error> errors = new LinkedHashMap<>();
        boolean current = item.isChecked("current");

        for (FieldSpec spec : item.type.fields) {
            if (current && spec.key.equals("end_date")) {
                continue;
            }
            String value = item.get(spec.key);
            if (value.isEmpty()) {
                if (spec.required) {
                    errors.put(spec.key, Error.REQUIRED);
                }
                continue;
            }
            switch (spec.kind) {
                case DATE:
                    if (!DateFormatter.isValidOrBlank(value)) {
                        errors.put(spec.key, Error.DATE_FORMAT);
                    } else if (!futureAllowed(item.type, spec.key)
                            && DateFormatter.isFuture(value, currentYear, currentMonth)) {
                        errors.put(spec.key, Error.DATE_FUTURE);
                    }
                    break;
                case EMAIL:
                    if (!ContactValidator.isValidEmail(value)) {
                        errors.put(spec.key, Error.EMAIL_FORMAT);
                    }
                    break;
                case PHONE:
                    if (!ContactValidator.isValidPhone(value)) {
                        errors.put(spec.key, Error.PHONE_FORMAT);
                    }
                    break;
                default:
                    break;
            }
        }

        for (String[] pair : DATE_PAIRS) {
            String start = pair[0];
            String end = pair[1];
            if (!hasField(item.type, start) || !hasField(item.type, end)) {
                continue;
            }
            if (current && end.equals("end_date")) {
                continue;
            }
            if (errors.containsKey(start) || errors.containsKey(end)) {
                continue;
            }
            if (!DateFormatter.isOrdered(item.get(start), item.get(end))) {
                errors.put(end, Error.DATE_ORDER);
            }
        }
        return errors;
    }

    /**
     * Certificate expiry dates and the end date of education (expected graduation) may lie in
     * the future. Every other date describes something that has already happened.
     */
    private static boolean futureAllowed(SectionType type, String key) {
        if (key.equals("expiry_date")) {
            return true;
        }
        return key.equals("end_date") && type == SectionType.EDUCATION;
    }

    private static boolean hasField(SectionType type, String key) {
        for (FieldSpec spec : type.fields) {
            if (spec.key.equals(key)) {
                return true;
            }
        }
        return false;
    }
}