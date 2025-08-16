package com.triqnpc;

import com.triqnpc.command.TriqNpcCommand;
import com.triqnpc.npc.NpcManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TriqNPCPlugin extends JavaPlugin {
    private static TriqNPCPlugin instance;
    private NpcManager npcManager;

    public static TriqNPCPlugin getInstance() {
        return instance;
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.npcManager = new NpcManager(this);
        this.npcManager.loadAll();

        getCommand("triqnpc").setExecutor(new TriqNpcCommand(this));
        getCommand("triqnpc").setTabCompleter(new TriqNpcCommand(this));
        Bootstrap.wire(this);

        Bukkit.getLogger().info("TriqNPC enabled");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.saveAll();
            npcManager.despawnAll();
        }
    }
}


