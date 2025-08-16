package com.triqnpc.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NpcData {
    public String id;
    public String skin; // player name or URL/base64
    public String displayName;
    public Location location;
    public String entityType = "PLAYER"; // PLAYER, COW, PIG, VILLAGER, etc.
    public boolean lookAtPlayer;
    public String followPlayer; // name or null
    public double followSpeed;
    public ItemStack mainHand;
    public ItemStack offHand;
    public float headPitch;
    public float headYaw;
    public float armPitch;
    public final List<String> actions = new ArrayList<>(); // prefix: command:..., message:...

    public static NpcData fromConfig(String id, ConfigurationSection sec) {
        NpcData d = new NpcData();
        d.id = id;
        d.skin = sec.getString("skin", id);
        d.displayName = sec.getString("name", id);
        d.entityType = sec.getString("entityType", "PLAYER"); // Default to PLAYER for full player model
        d.lookAtPlayer = sec.getBoolean("lookAt", false);
        d.followPlayer = sec.getString("follow.player");
        d.followSpeed = sec.getDouble("follow.speed", 0.3d);
        d.headPitch = (float) sec.getDouble("pose.head.pitch", 0.0);
        d.headYaw = (float) sec.getDouble("pose.head.yaw", 0.0);
        d.armPitch = (float) sec.getDouble("pose.arm.pitch", 0.0);
        if (sec.isList("actions")) d.actions.addAll(Objects.requireNonNull(sec.getStringList("actions")));
        if (sec.isString("world")) {
            String worldName = sec.getString("world");
            double x = sec.getDouble("x");
            double y = sec.getDouble("y");
            double z = sec.getDouble("z");
            float yaw = (float) sec.getDouble("yaw");
            float pitch = (float) sec.getDouble("pitch");
            if (Bukkit.getWorld(worldName) != null) {
                d.location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            }
        }
        // items left null; serialized by manager
        return d;
    }
}


