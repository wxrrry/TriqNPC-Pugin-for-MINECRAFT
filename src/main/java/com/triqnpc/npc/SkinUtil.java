package com.triqnpc.npc;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.Base64;

public final class SkinUtil {
    private SkinUtil() {}

    public static ItemStack createPlayerHead(String skin) {
        if (skin == null || skin.trim().isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;
        
        String trimmedSkin = skin.trim();
        
        try {
            if (looksLikeUrl(trimmedSkin)) {
                // Handle URL-based skins
                URL url = new URL(trimmedSkin);
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(url);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } else if (looksLikeBase64(trimmedSkin)) {
                // Handle base64-encoded skins
                try {
                    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                    PlayerTextures textures = profile.getTextures();
                    URL skinUrl = new URL("https://textures.minecraft.net/texture/" + trimmedSkin);
                    textures.setSkin(skinUrl);
                    profile.setTextures(textures);
                    meta.setOwnerProfile(profile);
                } catch (Exception e) {
                    // Fallback to offline player
                    OfflinePlayer off = Bukkit.getOfflinePlayer(trimmedSkin);
                    meta.setOwningPlayer(off);
                }
            } else {
                // Handle player names
                OfflinePlayer off = Bukkit.getOfflinePlayer(trimmedSkin);
                meta.setOwningPlayer(off);
            }
        } catch (Exception e) {
            // Fallback: try to use as player name
            try {
                OfflinePlayer off = Bukkit.getOfflinePlayer(trimmedSkin);
                meta.setOwningPlayer(off);
            } catch (Exception ignored) {
                // Leave as default player head
            }
        }
        
        head.setItemMeta(meta);
        return head;
    }

    private static boolean looksLikeUrl(String s) {
        try {
            new URL(s);
            return s.startsWith("http://") || s.startsWith("https://");
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    private static boolean looksLikeBase64(String s) {
        if (s == null || s.length() < 10) return false;
        try {
            Base64.getDecoder().decode(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}


