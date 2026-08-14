package org.attack_type.api;

/**
 * 七大罪孽属性枚举。
 * <p>
 * 每种罪孽对应一种伤害属性，可通过附魔附加到武器上，在攻击时触发额外罪孽伤害。
 * 玩家可通过积累碎片来手动/自动触发罪孽攻击。
 * <ul>
 *   <li>{@link #WRATH} — 暴怒（红色）</li>
 *   <li>{@link #LUST} — 色欲（橙色）</li>
 *   <li>{@link #SLOTH} — 怠惰（黄色）</li>
 *   <li>{@link #GLUTTONY} — 暴食（草绿）</li>
 *   <li>{@link #GLOOM} — 忧郁（天蓝）</li>
 *   <li>{@link #PRIDE} — 傲慢（深蓝）</li>
 *   <li>{@link #ENVY} — 嫉妒（紫色）</li>
 * </ul>
 */
public enum SinType {
    WRATH(0xFF4444),
    LUST(0xFF8800),
    SLOTH(0xFFFF44),
    GLUTTONY(0x44FF44),
    GLOOM(0x44CCFF),
    PRIDE(0x4444FF),
    ENVY(0xCC44FF);

    private final int color;

    SinType(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    /**
     * @return 国际化翻译键，格式为 {@code sin.attack_type.<小写名称>}
     */
    public String getTranslationKey() {
        return "sin.attack_type." + name().toLowerCase();
    }
}