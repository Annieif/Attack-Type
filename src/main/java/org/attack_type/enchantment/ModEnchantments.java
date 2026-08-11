package org.attack_type.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import org.attack_type.api.AttackType;
import org.attack_type.api.SinType;

import java.util.HashMap;
import java.util.Map;

public class ModEnchantments {
    private static final Map<SinType, SinEnchantment> SIN_ENCHANTS = new HashMap<>();
    private static final Map<AttackType, PhysicalResistanceEnchantment> PHYS_ENCHANTS = new HashMap<>();

    public static final SinEnchantment WRATH = registerSin(SinType.WRATH);
    public static final SinEnchantment LUST = registerSin(SinType.LUST);
    public static final SinEnchantment SLOTH = registerSin(SinType.SLOTH);
    public static final SinEnchantment GLUTTONY = registerSin(SinType.GLUTTONY);
    public static final SinEnchantment GLOOM = registerSin(SinType.GLOOM);
    public static final SinEnchantment PRIDE = registerSin(SinType.PRIDE);
    public static final SinEnchantment ENVY = registerSin(SinType.ENVY);

    public static final PhysicalResistanceEnchantment SLASH_RESISTANCE = registerPhys(AttackType.SLASH);
    public static final PhysicalResistanceEnchantment PIERCE_RESISTANCE = registerPhys(AttackType.PIERCE);
    public static final PhysicalResistanceEnchantment BLUNT_RESISTANCE = registerPhys(AttackType.BLUNT);

    private static SinEnchantment registerSin(SinType sinType) {
        SinEnchantment ench = new SinEnchantment(sinType);
        Registry.register(Registries.ENCHANTMENT,
                new Identifier("attack_type", sinType.name().toLowerCase()),
                ench);
        SIN_ENCHANTS.put(sinType, ench);
        return ench;
    }

    private static PhysicalResistanceEnchantment registerPhys(AttackType attackType) {
        PhysicalResistanceEnchantment ench = new PhysicalResistanceEnchantment(attackType);
        String name = attackType.name().toLowerCase() + "_resistance";
        Registry.register(Registries.ENCHANTMENT,
                new Identifier("attack_type", name),
                ench);
        PHYS_ENCHANTS.put(attackType, ench);
        return ench;
    }

    public static SinEnchantment getSinEnchantment(SinType sinType) {
        return SIN_ENCHANTS.get(sinType);
    }

    public static PhysicalResistanceEnchantment getPhysicalEnchantment(AttackType attackType) {
        return PHYS_ENCHANTS.get(attackType);
    }

    public static void initialize() {
    }
}