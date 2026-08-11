package org.attack_type.fragment;

/**
 * 罪孽碎片系统配置常量。
 * <p>
 * 集中管理碎片获取、消耗和阈值的数值参数，便于统一调试。
 */
public class SinFragmentConfig {
    /** 击杀普通生物掉落碎片数 */
    public static final int MOB_KILL_FRAGMENTS = 5;
    /** 击杀玩家掉落碎片数 */
    public static final int PLAYER_KILL_FRAGMENTS = 20;
    /** 受到伤害时获得的碎片数 */
    public static final int DAMAGE_TAKEN_FRAGMENTS = 1;
    /** 造成伤害时获得的碎片数 */
    public static final int DAMAGE_DEALT_FRAGMENTS = 1;

    /** L1 触发消耗碎片 */
    public static final int COST_LEVEL_1 = 40;
    /** L2 触发消耗碎片 */
    public static final int COST_LEVEL_2 = 70;
    /** L3 触发消耗碎片 */
    public static final int COST_LEVEL_3 = 100;

    /** 溢出阈值（≥500 自动触发） */
    public static final int OVERFLOW_THRESHOLD = 500;
    /** 即死阈值（≥1000 直接击杀） */
    public static final int KILL_THRESHOLD = 1000;

    /** L1 激活持续 tick（4 秒） */
    public static final int DURATION_L1_TICKS = 80;
    /** L2 激活持续 tick（7 秒） */
    public static final int DURATION_L2_TICKS = 140;
    /** L3 激活持续 tick（10 秒） */
    public static final int DURATION_L3_TICKS = 200;
}