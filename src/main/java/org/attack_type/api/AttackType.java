package org.attack_type.api;

public enum AttackType {
    SLASH,
    PIERCE,
    BLUNT,
    NONE;

    public static AttackType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}