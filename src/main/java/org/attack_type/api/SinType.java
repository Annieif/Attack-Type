package org.attack_type.api;

public enum SinType {
    WRATH,
    LUST,
    SLOTH,
    GLUTTONY,
    GLOOM,
    PRIDE,
    ENVY;

    public String getTranslationKey() {
        return "sin.attack_type." + name().toLowerCase();
    }
}