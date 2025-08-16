package com.triqnpc.npc;

import com.triqnpc.TriqNPCPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NpcManager {
    private final TriqNPCPlugin plugin;
    private final TriqNpcContext context;
    private final Map<String, NpcEntity> idToNpc = new HashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;

    public NpcManager(TriqNPCPlugin plugin) {
        this.plugin = plugin;
        this.context = new TriqNpcContext(plugin);
        this.dataFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to create NPC data file: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        try {
            Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to start NPC tick scheduler: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public Collection<NpcEntity> getAll() {
        return Collections.unmodifiableCollection(idToNpc.values());
    }

    public NpcEntity get(String id) { return idToNpc.get(id.toLowerCase(Locale.ROOT)); }

    public boolean exists(String id) { return idToNpc.containsKey(id.toLowerCase(Locale.ROOT)); }

    public NpcEntity create(String id, String skin, String name, Location loc) {
        String key = id.toLowerCase(Locale.ROOT);
        if (exists(key)) return null;
        
        try {
            NpcData d = new NpcData();
            d.id = key;
            d.skin = skin;
            d.displayName = name == null || name.isEmpty() ? id : name;
            d.location = loc.clone();
            d.entityType = "PLAYER"; // Default to PLAYER type for full player model
            d.followSpeed = 0.3d;
            NpcEntity e = new NpcEntity(d, context);
            e.spawn();
            idToNpc.put(key, e);
            return e;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to create NPC " + id + ": " + ex.getMessage());
            return null;
        }
    }

    public boolean remove(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        NpcEntity e = idToNpc.remove(key);
        if (e == null) return false;
        
        try {
            e.despawn();
            if (dataConfig.isConfigurationSection(key)) {
                dataConfig.set(key, null);
                saveFile();
            }
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to remove NPC " + id + ": " + ex.getMessage());
            return false;
        }
    }

    public void despawnAll() {
        for (NpcEntity e : idToNpc.values()) {
            try {
                e.despawn();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to despawn NPC " + e.getData().id + ": " + ex.getMessage());
            }
        }
    }

    public void loadAll() {
        idToNpc.clear();
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : dataConfig.getKeys(false)) {
            ConfigurationSection sec = dataConfig.getConfigurationSection(key);
            if (sec == null) continue;
            
            try {
                NpcData d = NpcData.fromConfig(key, sec);
                NpcEntity e = new NpcEntity(d, context);
                e.spawn();
                idToNpc.put(key, e);
                
                // Load items
                if (sec.isConfigurationSection("items")) {
                    ConfigurationSection items = sec.getConfigurationSection("items");
                    if (items != null) {
                        ItemStack main = items.getItemStack("main");
                        ItemStack off = items.getItemStack("off");
                        if (main != null) e.setItem(EquipmentSlot.HAND, main);
                        if (off != null) e.setItem(EquipmentSlot.OFF_HAND, off);
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load NPC " + key + ": " + ex.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + idToNpc.size() + " NPCs");
        if (context.playerRenderer() != null && context.playerRenderer().available()) {
            plugin.getLogger().info("Packet rendering enabled - NPCs will spawn as full player models");
        } else {
            plugin.getLogger().warning("Packet rendering disabled - NPCs will spawn as ArmorStands with player heads");
        }
    }

    public void saveAll() {
        for (NpcEntity e : idToNpc.values()) {
            try {
                save(e);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to save NPC " + e.getData().id + ": " + ex.getMessage());
            }
        }
        saveFile();
    }

    public void save(NpcEntity e) {
        NpcData d = e.getData();
        String key = d.id;
        dataConfig.set(key + ".skin", d.skin);
        dataConfig.set(key + ".name", d.displayName);
        dataConfig.set(key + ".entityType", d.entityType);
        if (d.location != null && d.location.getWorld() != null) {
            dataConfig.set(key + ".world", d.location.getWorld().getName());
            dataConfig.set(key + ".x", d.location.getX());
            dataConfig.set(key + ".y", d.location.getY());
            dataConfig.set(key + ".z", d.location.getZ());
            dataConfig.set(key + ".yaw", d.location.getYaw());
            dataConfig.set(key + ".pitch", d.location.getPitch());
        }
        dataConfig.set(key + ".lookAt", d.lookAtPlayer);
        dataConfig.set(key + ".follow.player", d.followPlayer);
        dataConfig.set(key + ".follow.speed", d.followSpeed);
        dataConfig.set(key + ".pose.head.pitch", d.headPitch);
        dataConfig.set(key + ".pose.head.yaw", d.headYaw);
        dataConfig.set(key + ".pose.arm.pitch", d.armPitch);
        dataConfig.set(key + ".actions", new ArrayList<>(d.actions));
        // items
        if (d.mainHand != null || d.offHand != null) {
            dataConfig.set(key + ".items.main", d.mainHand);
            dataConfig.set(key + ".items.off", d.offHand);
        } else {
            dataConfig.set(key + ".items", null);
        }
        saveFile();
    }

    private void saveFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save NPC data: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void tickAll() {
        for (NpcEntity e : idToNpc.values()) {
            try {
                e.tick();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to tick NPC " + e.getData().id + ": " + ex.getMessage());
            }
        }
    }
}


