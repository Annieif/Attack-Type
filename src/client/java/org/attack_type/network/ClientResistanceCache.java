package org.attack_type.network;

import org.attack_type.api.ResistanceProfile;

public class ClientResistanceCache {
    private static ResistanceProfile cachedProfile = new ResistanceProfile();

    public static ResistanceProfile getProfile() {
        return cachedProfile;
    }

    public static void setProfile(ResistanceProfile profile) {
        cachedProfile = profile;
    }
}