package com.triqnpc.npc;

import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SkinFetcherUtil {
    private static final Map<String, Object> skinCache = new HashMap<>();
    
    public static Object getSkin(String playerName, Plugin plugin) {
        if (skinCache.containsKey(playerName)) {
            return skinCache.get(playerName);
        }
        
        try {
            // Get UUID from player name
            String uuid = getUUIDFromName(playerName);
            if (uuid == null) {
                plugin.getLogger().warning("Could not get UUID for player: " + playerName);
                return null;
            }
            
            // Fetch skin data
            Object skin = fetchTextures(uuid);
            if (skin != null) {
                skinCache.put(playerName, skin);
                plugin.getLogger().info("Successfully fetched skin for: " + playerName);
            }
            
            return skin;
        } catch (Exception e) {
            plugin.getLogger().warning("Error fetching skin for " + playerName + ": " + e.getMessage());
            return null;
        }
    }
    
    private static String getUUIDFromName(String playerName) throws Exception {
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON response to get UUID
            String json = response.toString();
            int uuidStart = json.indexOf("\"id\":\"") + 6;
            int uuidEnd = json.indexOf("\"", uuidStart);
            return json.substring(uuidStart, uuidEnd);
        }
        
        return null;
    }
    
    private static Object fetchTextures(String uuidNoHyphen) throws Exception {
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidNoHyphen + "?unsigned=false");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        String body = response.toString();
        
        // Parse the response to extract texture data
        int vStart = body.indexOf("\"value\":\"") + 9;
        int vEnd = body.indexOf("\"", vStart);
        int sStart = body.indexOf("\"signature\":\"") + 12;
        int sEnd = body.indexOf("\"", sStart);
        
        String value = body.substring(vStart, vEnd);
        String signature = body.substring(sStart, sEnd);
        
        // Create WrappedSignedProperty via reflection
        try {
            Class<?> wrappedSignedPropertyClass = Class.forName("com.comphenix.protocol.wrappers.WrappedSignedProperty");
            return wrappedSignedPropertyClass.getConstructor(String.class, String.class, String.class)
                .newInstance("textures", value, signature);
        } catch (Exception e) {
            // If ProtocolLib is not available, return null
            return null;
        }
    }
}


