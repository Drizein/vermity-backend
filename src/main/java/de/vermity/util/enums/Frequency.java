package de.vermity.util.enums;

import lombok.Getter;

/**
 * @author Drizein
 */
@Getter
public enum Frequency {
    MONTHLY(12, "monatlich"), QUARTERLY(4, "quartalsweise"), YEARLY(1, "jährlich");

    private final String description;
    private final int    factor;

    Frequency(int i, String description) {
        this.description = description;
        this.factor = i;
    }

}
