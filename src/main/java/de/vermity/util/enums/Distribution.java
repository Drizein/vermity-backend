package de.vermity.util.enums;

import lombok.Getter;

@Getter
public enum Distribution {
  SQUARE_METERS("m²"),
  FLAT("Woheinheit"),
  PERSON("Person"),
  NONE("Keine");

  private final String unit;

  Distribution(String unit) {
    this.unit = unit;
  }

}
