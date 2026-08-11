package org.attack_type.network;

import net.minecraft.util.Identifier;
import org.attack_type.Attack_type;

/**
 * 网络包通道标识符常量。
 * <p>
 * 定义 4 个网络通道：
 * <ul>
 *   <li>{@link #RESISTANCE_SYNC} — 服务端→客户端：同步完整抗性配置</li>
 *   <li>{@link #RESISTANCE_UPDATE} — 客户端→服务端：提交抗性修改</li>
 *   <li>{@link #FRAGMENT_SYNC} — 服务端→客户端：同步碎片数据</li>
 *   <li>{@link #FRAGMENT_TRIGGER} — 客户端→服务端：请求手动触发罪孽</li>
 * </ul>
 */
public class ModPackets {
    /** 服务端→客户端：抗性配置全量同步 */
    public static final Identifier RESISTANCE_SYNC = new Identifier(Attack_type.MOD_ID, "resistance_sync");
    /** 客户端→服务端：抗性配置更新 */
    public static final Identifier RESISTANCE_UPDATE = new Identifier(Attack_type.MOD_ID, "resistance_update");
    /** 服务端→客户端：碎片数据全量同步 */
    public static final Identifier FRAGMENT_SYNC = new Identifier(Attack_type.MOD_ID, "fragment_sync");
    /** 客户端→服务端：手动触发罪孽请求 */
    public static final Identifier FRAGMENT_TRIGGER = new Identifier(Attack_type.MOD_ID, "fragment_trigger");
}