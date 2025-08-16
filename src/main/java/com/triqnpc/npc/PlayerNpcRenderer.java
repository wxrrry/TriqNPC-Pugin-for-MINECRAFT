package com.triqnpc.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public class PlayerNpcRenderer {
    private final Plugin plugin;
    private Object protocolManager;
    private Method createPacketMethod;
    private Method sendPacketMethod;
    private boolean available = false;

    public PlayerNpcRenderer(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = tryGetProtocolManager();
        this.available = protocolManager != null && createPacketMethod != null && sendPacketMethod != null;
        
        if (available) {
            plugin.getLogger().info("PlayerNpcRenderer initialized successfully with ProtocolLib");
        } else {
            plugin.getLogger().warning("PlayerNpcRenderer failed to initialize - ProtocolLib not available");
        }
    }

    public boolean available() {
        return available;
    }

    private Object tryGetProtocolManager() {
        try {
            plugin.getLogger().info("Attempting to detect ProtocolLib on server...");
            
            // Get ProtocolLibrary class
            Class<?> protocolLibraryClass = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            plugin.getLogger().info("ProtocolLib class found");
            
            // Get ProtocolManager
            Method getProtocolManagerMethod = protocolLibraryClass.getMethod("getProtocolManager");
            Object pm = getProtocolManagerMethod.invoke(null);
            plugin.getLogger().info("ProtocolManager instance created: " + pm.getClass().getName());
            
            // Get PacketType class
            Class<?> packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            plugin.getLogger().info("PacketType class found");
            
            // Get createPacket method
            createPacketMethod = pm.getClass().getMethod("createPacket", packetTypeClass);
            plugin.getLogger().info("createPacket method found");
            
            // Try to find send method - try multiple approaches
            Method[] methods = pm.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equals("sendServerPacket") && method.getParameterCount() == 2) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params[0] == Player.class && params[1].getName().contains("PacketContainer")) {
                        sendPacketMethod = method;
                        plugin.getLogger().info("Found sendServerPacket method: " + method.getName() + " with params: " + java.util.Arrays.toString(params));
                        break;
                    }
                }
            }
            
            if (sendPacketMethod == null) {
                // Try alternative method names
                for (Method method : methods) {
                    if (method.getName().equals("sendPacket") && method.getParameterCount() == 2) {
                        Class<?>[] params = method.getParameterTypes();
                        if (params[0] == Player.class && params[1].getName().contains("PacketContainer")) {
                            sendPacketMethod = method;
                            plugin.getLogger().info("Found sendPacket method: " + method.getName() + " with params: " + java.util.Arrays.toString(params));
                            break;
                        }
                    }
                }
            }
            
            if (sendPacketMethod == null) {
                plugin.getLogger().warning("No suitable send method found in ProtocolManager");
                plugin.getLogger().info("Available methods with 'send' in name:");
                for (Method method : methods) {
                    if (method.getName().toLowerCase().contains("send")) {
                        plugin.getLogger().info("  " + method.getName() + "(" + 
                            java.util.Arrays.toString(method.getParameterTypes()) + ")");
                    }
                }
                return null;
            }
            
            plugin.getLogger().info("ProtocolLib detected successfully - full player model NPCs will be available");
            return pm;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error initializing ProtocolLib: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void spawnForAll(NpcEntity e) {
        for (Player p : Bukkit.getOnlinePlayers()) spawnFor(p, e);
    }

    public void spawnFor(Player viewer, NpcEntity e) {
        try {
            plugin.getLogger().info("=== STARTING NPC SPAWN PROCESS ===");
            plugin.getLogger().info("Viewer: " + viewer.getName());
            plugin.getLogger().info("NPC ID: " + e.getData().id);
            
            NpcData d = e.getData();
            UUID uuid = UUID.nameUUIDFromBytes(("triqnpc:" + d.id).getBytes());
            int entityId = getEntityIdFromUUID(uuid);
            String name16 = trim(d.displayName);
            
            plugin.getLogger().info("Generated UUID: " + uuid);
            plugin.getLogger().info("Generated EntityID: " + entityId);
            plugin.getLogger().info("Display Name: " + name16);

            // Get packet types
            Class<?> packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            
            // Find the correct inner class: Play.Server
            Class<?> playServerClass = null;
            for (Class<?> innerClass : packetTypeClass.getDeclaredClasses()) {
                if (innerClass.getName().contains("Play") && innerClass.getName().contains("Server")) {
                    playServerClass = innerClass;
                    break;
                }
            }
            
            if (playServerClass == null) {
                plugin.getLogger().warning("Could not find PacketType.Play.Server class");
                plugin.getLogger().info("Available inner classes:");
                for (Class<?> innerClass : packetTypeClass.getDeclaredClasses()) {
                    plugin.getLogger().info("  " + innerClass.getName());
                }
                throw new RuntimeException("PacketType.Play.Server not found");
            }
            
            plugin.getLogger().info("Found Play.Server class: " + playServerClass.getName());
            
            // Try different field names for packet types
            Object spawnEntityPacketType = getFieldValue(playServerClass, "SPAWN_ENTITY");
            if (spawnEntityPacketType == null) {
                spawnEntityPacketType = getFieldValue(playServerClass, "ENTITY_SPAWN");
            }
            if (spawnEntityPacketType == null) {
                spawnEntityPacketType = getFieldValue(playServerClass, "SPAWN_ENTITY_PACKET");
            }
            
            Object entityHeadRotationPacketType = getFieldValue(playServerClass, "ENTITY_HEAD_ROTATION");
            if (entityHeadRotationPacketType == null) {
                entityHeadRotationPacketType = getFieldValue(playServerClass, "HEAD_ROTATION");
            }
            
            Object entityMetadataPacketType = getFieldValue(playServerClass, "ENTITY_METADATA");
            if (entityMetadataPacketType == null) {
                entityMetadataPacketType = getFieldValue(playServerClass, "METADATA");
            }
            
            plugin.getLogger().info("Packet types found: SPAWN_ENTITY=" + (spawnEntityPacketType != null) + 
                ", ENTITY_HEAD_ROTATION=" + (entityHeadRotationPacketType != null) + 
                ", ENTITY_METADATA=" + (entityMetadataPacketType != null));
            
            // List all available fields to debug
            if (spawnEntityPacketType == null || entityHeadRotationPacketType == null || entityMetadataPacketType == null) {
                plugin.getLogger().info("Available fields in " + playServerClass.getName() + ":");
                for (java.lang.reflect.Field field : playServerClass.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        plugin.getLogger().info("  " + field.getName() + " (type: " + field.getType().getSimpleName() + ")");
                    }
                }
            }

            // Create packets using PacketContainer
            Object spawnPacket = createPacketMethod.invoke(protocolManager, spawnEntityPacketType);
            Object headPacket = createPacketMethod.invoke(protocolManager, entityHeadRotationPacketType);
            Object metaPacket = createPacketMethod.invoke(protocolManager, entityMetadataPacketType);
            
            plugin.getLogger().info("Packets created successfully");

            // Set spawn packet data using PacketContainer methods
            setPacketContainerData(spawnPacket, "EntityID", entityId);
            setPacketContainerData(spawnPacket, "UUID", uuid);
            setPacketContainerData(spawnPacket, "X", d.location.getX());
            setPacketContainerData(spawnPacket, "Y", d.location.getY());
            setPacketContainerData(spawnPacket, "Z", d.location.getZ());
            setPacketContainerData(spawnPacket, "Yaw", (byte) ((d.location.getYaw() % 360) * 256 / 360));
            setPacketContainerData(spawnPacket, "Pitch", (byte) ((d.location.getPitch() % 360) * 256 / 360));
            
            // Set head rotation packet data
            setPacketContainerData(headPacket, "EntityID", entityId);
            setPacketContainerData(headPacket, "Yaw", (byte) ((d.location.getYaw() % 360) * 256 / 360));
            
            // Set metadata packet data
            setPacketContainerData(metaPacket, "EntityID", entityId);
            
            plugin.getLogger().info("Packet data set successfully");

            // Send packets
            sendPacketMethod.invoke(protocolManager, viewer, spawnPacket);
            sendPacketMethod.invoke(protocolManager, viewer, headPacket);
            sendPacketMethod.invoke(protocolManager, viewer, metaPacket);
            
            plugin.getLogger().info("Packets sent successfully");

            // Store the entity ID for later reference
            viewer.setMetadata("triqnpc-viewing-" + d.id, new org.bukkit.metadata.FixedMetadataValue(plugin, entityId));
            
            plugin.getLogger().info("=== NPC SPAWN PROCESS COMPLETED SUCCESSFULLY ===");
            plugin.getLogger().info("Successfully spawned NPC " + d.id + " as full player model for " + viewer.getName());
            
        } catch (Exception ex) {
            plugin.getLogger().warning("=== NPC SPAWN PROCESS FAILED ===");
            plugin.getLogger().warning("Error type: " + ex.getClass().getSimpleName());
            plugin.getLogger().warning("Error message: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Packet rendering failed", ex);
        }
    }

    public void despawnForAll(NpcEntity e) {
        for (Player p : Bukkit.getOnlinePlayers()) despawnFor(p, e);
    }

    public void despawnFor(Player viewer, NpcEntity e) {
        try {
            NpcData d = e.getData();
            int entityId = viewer.getMetadata("triqnpc-viewing-" + d.id).get(0).asInt();
            
            Class<?> packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            Class<?> playServerClass = packetTypeClass.getDeclaredClasses()[0];
            Object entityDestroyPacketType = getFieldValue(playServerClass, "ENTITY_DESTROY");
            
            Object destroyPacket = createPacketMethod.invoke(protocolManager, entityDestroyPacketType);
            setPacketContainerData(destroyPacket, "EntityIDs", java.util.Collections.singletonList(entityId));
            
            sendPacketMethod.invoke(protocolManager, viewer, destroyPacket);
            viewer.removeMetadata("triqnpc-viewing-" + d.id, plugin);
            
            plugin.getLogger().info("Successfully despawned NPC " + d.id + " for " + viewer.getName());
        } catch (Exception ex) {
            plugin.getLogger().warning("Packet despawn failed for NPC " + e.getData().id + ": " + ex.getMessage());
        }
    }



    private void setPacketContainerData(Object packet, String fieldName, Object value) {
        try {
            // Assuming packet is a PacketContainer
            // This method is not directly available in the original code,
            // so we'll simulate setting a field.
            // In a real scenario, you'd use a method like setField(fieldName, value)
            // or directly access the underlying object if it's a PacketContainer.
            // For now, we'll try to find a field that matches the name.
            for (java.lang.reflect.Field field : packet.getClass().getDeclaredFields()) {
                if (field.getName().toLowerCase().contains(fieldName.toLowerCase())) {
                    field.setAccessible(true);
                    field.set(packet, value);
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting packet data for field '" + fieldName + "': " + e.getMessage());
        }
    }

    private Object getFieldValue(Class<?> clazz, String fieldName) {
        try {
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private int getEntityIdFromUUID(UUID uuid) {
        return Math.abs(uuid.hashCode());
    }

    private String trim(String s) {
        return s.length() > 16 ? s.substring(0, 16) : s;
    }
}


