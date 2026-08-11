package org.attack_type.network;

import net.minecraft.util.Identifier;
import org.attack_type.Attack_type;

public class ModPackets {
    public static final Identifier RESISTANCE_SYNC = new Identifier(Attack_type.MOD_ID, "resistance_sync");
    public static final Identifier RESISTANCE_UPDATE = new Identifier(Attack_type.MOD_ID, "resistance_update");
    public static final Identifier FRAGMENT_SYNC = new Identifier(Attack_type.MOD_ID, "fragment_sync");
    public static final Identifier FRAGMENT_TRIGGER = new Identifier(Attack_type.MOD_ID, "fragment_trigger");
}