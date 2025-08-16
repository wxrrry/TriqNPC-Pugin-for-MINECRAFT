package com.triqnpc.listener;

import com.triqnpc.TriqNPCPlugin;
import com.triqnpc.npc.NpcEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class NpcInteractListener implements Listener {
    private final TriqNPCPlugin plugin;

    public NpcInteractListener(TriqNPCPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand)) return;
        if (!event.getRightClicked().hasMetadata("triqnpc-id")) return;
        String id = event.getRightClicked().getMetadata("triqnpc-id").get(0).asString();
        NpcEntity e = plugin.getNpcManager().get(id);
        if (e == null) return;
        Player p = event.getPlayer();
        for (String action : e.getData().actions) {
            if (action.startsWith("command:")) {
                String cmd = action.substring("command:".length()).trim().replace("{player}", p.getName());
                p.performCommand(cmd);
            } else if (action.startsWith("message:")) {
                String msg = action.substring("message:".length()).trim().replace("{player}", p.getName());
                p.sendMessage(msg);
            }
        }
    }
}


