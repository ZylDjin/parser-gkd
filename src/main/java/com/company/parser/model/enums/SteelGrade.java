package com.company.parser.model.enums;

/**
 * Марки стали
 */
public enum SteelGrade {
    ST3("Ст3", "Сталь обыкновенного качества"),
    ST3SP("Ст3сп", "Сталь спокойная"),
    ST3PS("Ст3пс", "Сталь полуспокойная"),
    GRADE_20("20", "Качественная конструкционная сталь"),
    GRADE_35("35", "Качественная конструкционная сталь"),
    GRADE_45("45", "Качественная конструкционная сталь"),
    GRADE_09G2S("09Г2С", "Низколегированная сталь"),
    GRADE_10HSND("10ХСНД", "Низколегированная атмосферостойкая"),
    GRADE_15HSND("15ХСНД", "Низколегированная атмосферостойкая"),
    AISI_304("AISI 304", "Нержавеющая сталь"),
    AISI_316("AISI 316", "Нержавеющая сталь"),
    AISI_321("AISI 321", "Нержавеющая сталь"),
    UNKNOWN("", "Марка не указана");

    private final String code;
    private final String description;

    SteelGrade(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Получение марки стали по коду
     */
    public static SteelGrade fromCode(String code) {
        if (code == null || code.isBlank()) return UNKNOWN;

        String normalized = code.toUpperCase().trim();

        for (SteelGrade grade : values()) {
            if (grade.code.equalsIgnoreCase(normalized)) {
                return grade;
            }
        }

        // Дополнительные варианты написания
        return switch (normalized) {
            case "СТ3", "CT3" -> ST3;
            case "СТ3СП", "CT3SP" -> ST3SP;
            case "СТ3ПС", "CT3PS" -> ST3PS;
            case "09Г2С", "09G2S" -> GRADE_09G2S;
            case "10ХСНД", "10HSND" -> GRADE_10HSND;
            case "15ХСНД", "15HSND" -> GRADE_15HSND;
            case "304", "08Х18Н10" -> AISI_304;
            case "316", "03Х17Н14М3" -> AISI_316;
            case "321", "08Х18Н10Т" -> AISI_321;
            default -> UNKNOWN;
        };
    }
}
