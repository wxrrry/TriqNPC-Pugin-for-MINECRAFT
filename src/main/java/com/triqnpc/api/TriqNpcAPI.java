package com.triqnpc.api;

import com.triqnpc.TriqNPCPlugin;
import com.triqnpc.npc.NpcEntity;

import java.util.Collection;

public final class TriqNpcAPI {
    private TriqNpcAPI() {}

    public static Collection<NpcEntity> getAllNpcs() {
        return TriqNPCPlugin.getInstance().getNpcManager().getAll();
    }
}


