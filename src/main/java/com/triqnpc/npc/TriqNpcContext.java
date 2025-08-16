package com.triqnpc.npc;

import com.triqnpc.TriqNPCPlugin;
import org.bukkit.plugin.Plugin;

public class TriqNpcContext {
    private final TriqNPCPlugin plugin;
    private final PlayerNpcRenderer playerRenderer;

    public TriqNpcContext(TriqNPCPlugin plugin) {
        this.plugin = plugin;
        this.playerRenderer = tryCreateRenderer(plugin);
    }

    public TriqNPCPlugin plugin() {
        return plugin;
    }

    public PlayerNpcRenderer playerRenderer() {
        return playerRenderer;
    }

    private PlayerNpcRenderer tryCreateRenderer(TriqNPCPlugin plugin) {
        plugin.getLogger().info("=== STARTING PLAYER NPC RENDERER INITIALIZATION ===");
        
        try {
            boolean packetsEnabled = plugin.getConfig().getBoolean("packet-render", true);
            plugin.getLogger().info("Packet rendering config value: " + packetsEnabled);
            
            if (!packetsEnabled) {
                plugin.getLogger().info("Packet rendering disabled in config");
                return null;
            }
            
            plugin.getLogger().info("Attempting to initialize PlayerNpcRenderer...");
            
            // Let PlayerNpcRenderer handle ProtocolLib detection
            try {
                plugin.getLogger().info("Creating PlayerNpcRenderer instance...");
                PlayerNpcRenderer renderer = new PlayerNpcRenderer(plugin);
                plugin.getLogger().info("PlayerNpcRenderer instance created, checking availability...");
                
                if (renderer.available()) {
                    plugin.getLogger().info("=== PLAYER NPC RENDERER INITIALIZATION SUCCESSFUL ===");
                    plugin.getLogger().info("Packet rendering enabled successfully");
                    return renderer;
                } else {
                    plugin.getLogger().warning("=== PLAYER NPC RENDERER INITIALIZATION FAILED ===");
                    plugin.getLogger().warning("PlayerNpcRenderer not available - ProtocolLib may not be properly installed");
                    return null;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("=== PLAYER NPC RENDERER INITIALIZATION FAILED ===");
                plugin.getLogger().warning("Failed to initialize packet renderer: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("=== PLAYER NPC RENDERER INITIALIZATION FAILED ===");
            plugin.getLogger().warning("Error checking packet renderer availability: " + t.getMessage());
            t.printStackTrace();
            return null;
        }
    }
}


