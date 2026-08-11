package org.attack_type.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import org.attack_type.api.AttackType;
import org.attack_type.api.SinType;

import java.util.HashMap;
import java.util.Map;

/**
 * 附魔注册中心。
 * <p>
 * 注册并管理 10 种模组附魔：
 * <ul>
 *   <li>7 种罪孽武器附魔（{@link SinEnchantment}）：最高 5 级，附在武器上
 *     <ul><li>暴怒(WRATH)、色欲(LUST)、怠惰(SLOTH)、暴食(GLUTTONY)、忧郁(GLOOM)、傲慢(PRIDE)、嫉妒(ENVY)</li></ul>
 *   </li>
 *   <li>3 种物理抗性护甲附魔（{@link PhysicalResistanceEnchantment}）：最高 4 级，附在护甲上
 *     <ul><li>斩击抗性(SLASH)、突刺抗性(PIERCE)、打击抗性(BLUNT)</li></ul>
 *   </li>
 * </ul>
 * <p>
 * {@link #initialize()} 方法在模组初始化时调用，触发类加载从而执行静态字段初始化。
 */
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

    /**
     * 注册罪孽附魔到原版注册表。
     *
     * @param sinType 罪孽类型
     * @return 注册的附魔实例
     */
    private static SinEnchantment registerSin(SinType sinType) {
        SinEnchantment ench = new SinEnchantment(sinType);
        Registry.register(Registries.ENCHANTMENT,
                new Identifier("attack_type", sinType.name().toLowerCase()),
                ench);
        SIN_ENCHANTS.put(sinType, ench);
        return ench;
    }

    /**
     * 注册物理抗性附魔到原版注册表。
     *
     * @param attackType 攻击类型
     * @return 注册的附魔实例
     */
    private static PhysicalResistanceEnchantment registerPhys(AttackType attackType) {
        PhysicalResistanceEnchantment ench = new PhysicalResistanceEnchantment(attackType);
        String name = attackType.name().toLowerCase() + "_resistance";
        Registry.register(Registries.ENCHANTMENT,
                new Identifier("attack_type", name),
                ench);
        PHYS_ENCHANTS.put(attackType, ench);
        return ench;
    }

    /**
     * 根据罪孽类型获取对应的附魔。
     *
     * @param sinType 罪孽类型
     * @return 对应的 SinEnchantment，无则 null
     */
    public static SinEnchantment getSinEnchantment(SinType sinType) {
        return SIN_ENCHANTS.get(sinType);
    }

    /**
     * 根据攻击类型获取对应的物理抗性附魔。
     *
     * @param attackType 攻击类型
     * @return 对应的 PhysicalResistanceEnchantment，无则 null
     */
    public static PhysicalResistanceEnchantment getPhysicalEnchantment(AttackType attackType) {
        return PHYS_ENCHANTS.get(attackType);
    }

    /**
     * 空方法，触发类加载从而执行静态初始化。
     */
    public static void initialize() {
    }
}