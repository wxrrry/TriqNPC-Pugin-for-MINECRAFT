package com.triqnpc;

import com.triqnpc.listener.NpcInteractListener;

public final class Bootstrap {
    public static void wire(TriqNPCPlugin plugin) {
        new NpcInteractListener(plugin);
    }
}


