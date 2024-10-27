package de.vermity.util.enums;

import lombok.Getter;

/**
 * @author Jan Tiedt
 */
@Getter
public enum MeterType {
  STROM("Kw/h", "Strom"),
  GAS("Kw/h", "Gas"),
  WASSERWARM("m³", "Wasser warm"),
  ABWASSER("m³", "Abwasser"),
  WASSERKALT("m³", "Wasser kalt");

  private final String unit;
  private final String description;

  MeterType(String unit, String description) {
    this.unit = unit;
    this.description = description;
  }


}