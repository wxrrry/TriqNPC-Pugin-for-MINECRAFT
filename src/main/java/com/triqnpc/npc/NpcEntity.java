package com.triqnpc.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.Locale;
import org.bukkit.Material;

public class NpcEntity {
    private final NpcData data;
    private final TriqNpcContext context;
    private ArmorStand armorStand;

    public NpcEntity(NpcData data, TriqNpcContext context) {
        this.data = data;
        this.context = context;
    }

    public boolean isSpawned() {
        // For packet-rendered player NPCs, they are always considered spawned if they have a location
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            return data.location != null;
        }
        
        // For ArmorStand NPCs, check if the entity exists and is alive
        return armorStand != null && !armorStand.isDead();
    }

    public void spawn() {
        if (data.location == null) return;
        World world = data.location.getWorld();
        if (world == null) return;

        // Always try to use packet-based player model for PLAYER type
        if ("PLAYER".equalsIgnoreCase(data.entityType)) {
            if (context.playerRenderer() != null && context.playerRenderer().available()) {
                try {
                    context.playerRenderer().spawnForAll(this);
                    return;
                } catch (Exception e) {
                    // If packet rendering fails, fall back to ArmorStand
                    context.plugin().getLogger().warning("Packet rendering failed for NPC " + data.id + ", falling back to ArmorStand: " + e.getMessage());
                }
            } else {
                context.plugin().getLogger().warning("PlayerNpcRenderer not available for NPC " + data.id + ", using ArmorStand fallback");
            }
        }

        // Fallback to ArmorStand for non-PLAYER types or when packet renderer fails
        if (isSpawned()) return;
        armorStand = (ArmorStand) world.spawnEntity(data.location, EntityType.ARMOR_STAND);
        armorStand.setVisible(true);
        armorStand.setGravity(false);
        armorStand.setMarker(false);
        armorStand.setArms(true);
        armorStand.setSmall(false);
        armorStand.setBasePlate(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setCustomName(data.displayName);
        if ("PLAYER".equalsIgnoreCase(data.entityType)) {
            armorStand.getEquipment().setHelmet(SkinUtil.createPlayerHead(data.skin));
        } else {
            // For mob types, set appropriate mob head
            armorStand.getEquipment().setHelmet(getMobHead(data.entityType));
        }
        armorStand.setHeadPose(new EulerAngle(Math.toRadians(data.headPitch), Math.toRadians(data.headYaw), 0));
        armorStand.setRightArmPose(new EulerAngle(Math.toRadians(data.armPitch), 0, 0));
        armorStand.setMetadata("triqnpc-id", new FixedMetadataValue(context.plugin(), data.id));
        if (data.mainHand != null) armorStand.getEquipment().setItem(EquipmentSlot.HAND, data.mainHand);
        if (data.offHand != null) armorStand.getEquipment().setItem(EquipmentSlot.OFF_HAND, data.offHand);
    }

    private ItemStack getMobHead(String entityType) {
        switch (entityType.toUpperCase(Locale.ROOT)) {
            case "COW": return new ItemStack(Material.COW_SPAWN_EGG);
            case "PIG": return new ItemStack(Material.PIG_SPAWN_EGG);
            case "VILLAGER": return new ItemStack(Material.VILLAGER_SPAWN_EGG);
            case "ZOMBIE": return new ItemStack(Material.ZOMBIE_SPAWN_EGG);
            case "SKELETON": return new ItemStack(Material.SKELETON_SPAWN_EGG);
            case "CREEPER": return new ItemStack(Material.CREEPER_SPAWN_EGG);
            default: return new ItemStack(Material.PLAYER_HEAD);
        }
    }

    public void despawn() {
        // If this is a packet-rendered player NPC, despawn via packets
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            try {
                context.playerRenderer().despawnForAll(this);
                return;
            } catch (Exception e) {
                context.plugin().getLogger().warning("Packet despawn failed for NPC " + data.id + ", falling back to entity removal: " + e.getMessage());
            }
        }
        
        // Fallback to entity removal
        if (armorStand != null) {
            armorStand.removeMetadata("triqnpc-id", context.plugin());
            armorStand.remove();
        }
        armorStand = null;
    }

    public void teleport(Location location) {
        data.location = location.clone();
        
        // If this is a packet-rendered player NPC, teleport via packets
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            try {
                // For packet NPCs, we need to respawn them at the new location
                context.playerRenderer().despawnForAll(this);
                context.playerRenderer().spawnForAll(this);
                return;
            } catch (Exception e) {
                context.plugin().getLogger().warning("Packet teleport failed for NPC " + data.id + ", falling back to entity teleport: " + e.getMessage());
            }
        }
        
        // Fallback to entity teleport
        if (isSpawned()) {
            armorStand.teleport(location);
        }
    }

    public void setName(String name) {
        data.displayName = name;
        
        // If this is a packet-rendered player NPC, update via packets
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            try {
                // For packet NPCs, we need to respawn them with the new name
                context.playerRenderer().despawnForAll(this);
                context.playerRenderer().spawnForAll(this);
                return;
            } catch (Exception e) {
                context.plugin().getLogger().warning("Packet name update failed for NPC " + data.id + ", falling back to entity update: " + e.getMessage());
            }
        }
        
        // Fallback to entity update
        if (isSpawned()) {
            armorStand.setCustomName(name);
        }
    }

    public void setHeadPose(float pitch, float yaw) {
        data.headPitch = pitch;
        data.headYaw = yaw;
        
        // If this is a packet-rendered player NPC, update via packets
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            try {
                // For packet NPCs, we need to respawn them with new head rotation
                context.playerRenderer().despawnForAll(this);
                context.playerRenderer().spawnForAll(this);
                return;
            } catch (Exception e) {
                context.plugin().getLogger().warning("Packet head pose update failed for NPC " + data.id + ", falling back to entity update: " + e.getMessage());
            }
        }
        
        // Fallback to entity update
        if (isSpawned()) {
            armorStand.setHeadPose(new EulerAngle(Math.toRadians(pitch), Math.toRadians(yaw), 0));
        }
    }

    public void setArmPitch(float pitch) {
        data.armPitch = pitch;
        
        // If this is a packet-rendered player NPC, update via packets
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            try {
                // For packet NPCs, we need to respawn them with new arm rotation
                context.playerRenderer().despawnForAll(this);
                context.playerRenderer().spawnForAll(this);
                return;
            } catch (Exception e) {
                context.plugin().getLogger().warning("Packet arm pose update failed for NPC " + data.id + ", falling back to entity update: " + e.getMessage());
            }
        }
        
        // Fallback to entity update
        if (isSpawned()) {
            armorStand.setRightArmPose(new EulerAngle(Math.toRadians(pitch), 0, 0));
        }
    }

    public void setLookAtPlayer(boolean enabled) {
        data.lookAtPlayer = enabled;
    }

    public void setItem(EquipmentSlot slot, ItemStack itemStack) {
        if (slot == EquipmentSlot.HAND) data.mainHand = itemStack;
        if (slot == EquipmentSlot.OFF_HAND) data.offHand = itemStack;
        
        // If this is a packet-rendered player NPC, update via packets
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            try {
                // For packet NPCs, we need to respawn them with the new items
                context.playerRenderer().despawnForAll(this);
                context.playerRenderer().spawnForAll(this);
                return;
            } catch (Exception e) {
                context.plugin().getLogger().warning("Packet item update failed for NPC " + data.id + ", falling back to entity update: " + e.getMessage());
            }
        }
        
        // Fallback to entity update
        if (isSpawned()) {
            armorStand.getEquipment().setItem(slot, itemStack);
        }
    }

    public void tick() {
        if (!isSpawned()) return;
        
        // Handle look at player for packet NPCs
        if (data.lookAtPlayer && "PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            Player target = closestPlayer(data.location);
            if (target != null) {
                // For packet NPCs, we need to respawn them with new head rotation
                float yaw = calculateYaw(data.location, target.getEyeLocation());
                float pitch = calculatePitch(data.location, target.getEyeLocation());
                if (Math.abs(yaw - data.headYaw) > 5 || Math.abs(pitch - data.headPitch) > 5) {
                    data.headYaw = yaw;
                    data.headPitch = pitch;
                    try {
                        context.playerRenderer().despawnForAll(this);
                        context.playerRenderer().spawnForAll(this);
                    } catch (Exception e) {
                        context.plugin().getLogger().warning("Packet head rotation update failed for NPC " + data.id + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Handle look at player for ArmorStand NPCs
        if (data.lookAtPlayer && armorStand != null) {
            Player target = closestPlayer(data.location);
            if (target != null) lookAt(armorStand, target.getEyeLocation());
        }
        
        if (data.followPlayer != null) {
            Player player = Bukkit.getPlayerExact(data.followPlayer);
            if (player != null && player.isOnline()) {
                followTowards(player.getLocation(), data.followSpeed);
            }
        }
    }

    private float calculateYaw(Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector());
        return (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
    }

    private float calculatePitch(Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector());
        return (float) Math.toDegrees(-Math.atan2(dir.getY(), Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ())));
    }

    private void followTowards(Location target, double speed) {
        Location current = data.location;
        Vector direction = target.toVector().subtract(current.toVector());
        double distance = direction.length();
        if (distance < 0.5) return;
        Vector step = direction.normalize().multiply(speed * 0.2); // per tick
        Location next = current.add(step);
        
        // If this is a packet-rendered player NPC, update via packets
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            try {
                data.location = next.clone();
                // For packet NPCs, we need to respawn them at the new location
                context.playerRenderer().despawnForAll(this);
                context.playerRenderer().spawnForAll(this);
                return;
            } catch (Exception e) {
                context.plugin().getLogger().warning("Packet follow update failed for NPC " + data.id + ", falling back to entity update: " + e.getMessage());
            }
        }
        
        // Fallback to entity update
        if (isSpawned()) {
            armorStand.teleport(next);
        }
    }

    private static Player closestPlayer(Location from) {
        double best = Double.MAX_VALUE;
        Player bestPlayer = null;
        for (Player p : from.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(from);
            if (d < best) {
                best = d;
                bestPlayer = p;
            }
        }
        return bestPlayer;
    }

    private static void lookAt(ArmorStand stand, Location target) {
        Location loc = stand.getLocation();
        Vector dir = target.toVector().subtract(loc.toVector());
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        float pitch = (float) Math.toDegrees(-Math.atan2(dir.getY(), Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ())));
        stand.setHeadPose(new EulerAngle(Math.toRadians(pitch), Math.toRadians(yaw), 0));
    }

    public ArmorStand getArmorStand() {
        // For packet-rendered player NPCs, return null as they don't have a physical entity
        if ("PLAYER".equalsIgnoreCase(data.entityType) && context.playerRenderer() != null && context.playerRenderer().available()) {
            return null;
        }
        
        return armorStand;
    }

    public NpcData getData() {
        return data;
    }
}


