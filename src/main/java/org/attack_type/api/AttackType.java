package org.attack_type.api;

/**
 * 物理攻击类型枚举。
 * <p>
 * Minecraft 中所有直接攻击被归类为以下四种物理类型之一：
 * <ul>
 *   <li>{@link #SLASH} — 斩击（剑、斧、三叉戟近战）</li>
 *   <li>{@link #PIERCE} — 突刺（箭矢、投掷三叉戟）</li>
 *   <li>{@link #BLUNT} — 打击（空手、雪球、鸡蛋、其他物品）</li>
 *   <li>{@link #NONE} — 无物理类型（摔落、窒息、火焰、中毒等非直接攻击）</li>
 * </ul>
 */
public enum AttackType {
    SLASH,
    PIERCE,
    BLUNT,
    NONE;

    /**
     * 从字符串名称解析攻击类型，大小写不敏感。
     *
     * @param name 攻击类型名称（如 "slash"、"PIERCE"）
     * @return 对应的 AttackType 枚举值，未知名称返回 {@link #NONE}
     */
    public static AttackType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}