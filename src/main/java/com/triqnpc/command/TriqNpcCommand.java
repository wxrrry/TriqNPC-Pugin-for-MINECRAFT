package com.triqnpc.command;

import com.triqnpc.TriqNPCPlugin;
import com.triqnpc.npc.NpcEntity;
import com.triqnpc.npc.NpcManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class TriqNpcCommand implements CommandExecutor, TabCompleter {
    private final TriqNPCPlugin plugin;

    public TriqNpcCommand(TriqNPCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("triqnpc.admin")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }

        NpcManager mgr = plugin.getNpcManager();
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "settype": {
                if (args.length < 3) { sender.sendMessage("Использование: /" + label + " settype <id> <PLAYER|COW|PIG|VILLAGER>"); return true; }
                NpcEntity e = mgr.get(args[1]);
                if (e == null) { sender.sendMessage("§cNPC не найден."); return true; }
                e.getData().entityType = args[2].toUpperCase(Locale.ROOT);
                e.despawn();
                e.spawn();
                mgr.save(e);
                sender.sendMessage("§aТип NPC обновлён: " + e.getData().entityType);
                return true;
            }
            case "create": {
                if (!(sender instanceof Player)) { sender.sendMessage("Только игрок."); return true; }
                if (args.length < 3) { sender.sendMessage("Использование: /" + label + " create <id> <skin> [name]"); return true; }
                String id = args[1];
                String skin = args[2];
                String name = args.length >= 4 ? joinFrom(args, 3) : id;
                Player p = (Player) sender;
                if (mgr.exists(id)) { sender.sendMessage("§cNPC с таким id уже существует."); return true; }
                NpcEntity e = mgr.create(id, skin, name, p.getLocation());
                mgr.save(e);
                sender.sendMessage("§aNPC создан: " + id);
                return true;
            }
            case "remove": {
                if (args.length < 2) { sender.sendMessage("Использование: /" + label + " remove <id>"); return true; }
                String id = args[1];
                if (mgr.remove(id)) sender.sendMessage("§aNPC удален: " + id);
                else sender.sendMessage("§cNPC не найден: " + id);
                return true;
            }
            case "list": {
                if (mgr.getAll().isEmpty()) { sender.sendMessage("§7NPC нет."); return true; }
                for (NpcEntity e : mgr.getAll()) {
                    Location l = e.getData().location;
                    String loc = l == null ? "unknown" : String.format("%s %.1f %.1f %.1f", l.getWorld().getName(), l.getX(), l.getY(), l.getZ());
                    sender.sendMessage("§e" + e.getData().id + " §7@ §f" + loc);
                }
                return true;
            }
            case "setskin": {
                if (args.length < 3) { sender.sendMessage("Использование: /" + label + " setskin <id> <skin/url>"); return true; }
                NpcEntity e = mgr.get(args[1]);
                if (e == null) { sender.sendMessage("§cNPC не найден."); return true; }
                e.getData().skin = args[2];
                if (e.getArmorStand() != null) {
                    e.getArmorStand().getEquipment().setHelmet(com.triqnpc.npc.SkinUtil.createPlayerHead(args[2]));
                }
                mgr.save(e);
                sender.sendMessage("§aСкин обновлен.");
                return true;
            }
            case "setpose": {
                if (args.length < 4) { sender.sendMessage("Использование: /" + label + " setpose <id> <arm|head|body> <angle>"); return true; }
                NpcEntity e = mgr.get(args[1]);
                if (e == null) { sender.sendMessage("§cNPC не найден."); return true; }
                String part = args[2].toLowerCase(Locale.ROOT);
                float angle;
                try { angle = Float.parseFloat(args[3]); } catch (NumberFormatException ex) { sender.sendMessage("§cУгол не число."); return true; }
                if (part.equals("head")) {
                    e.setHeadPose(angle, e.getData().headYaw);
                } else if (part.equals("body")) {
                    e.setHeadPose(e.getData().headPitch, angle);
                } else if (part.equals("arm")) {
                    e.setArmPitch(angle);
                } else {
                    sender.sendMessage("§cПоддерживается head/body/arm.");
                    return true;
                }
                mgr.save(e);
                sender.sendMessage("§aПоза обновлена.");
                return true;
            }
            case "setitem": {
                if (args.length < 4) { sender.sendMessage("Использование: /" + label + " setitem <id> <main|off> <material>"); return true; }
                NpcEntity e = mgr.get(args[1]);
                if (e == null) { sender.sendMessage("§cNPC не найден."); return true; }
                String hand = args[2].toLowerCase(Locale.ROOT);
                Material mat = Material.matchMaterial(args[3]);
                if (mat == null) { sender.sendMessage("§cМатериал не найден."); return true; }
                ItemStack is = new ItemStack(mat);
                if (hand.equals("main")) e.setItem(EquipmentSlot.HAND, is);
                else if (hand.equals("off")) e.setItem(EquipmentSlot.OFF_HAND, is);
                else { sender.sendMessage("§cРука: main/off"); return true; }
                mgr.save(e);
                sender.sendMessage("§aПредмет выдан.");
                return true;
            }
            case "look": {
                if (args.length < 3) { sender.sendMessage("Использование: /" + label + " look <id> <player|none>"); return true; }
                NpcEntity e = mgr.get(args[1]);
                if (e == null) { sender.sendMessage("§cNPC не найден."); return true; }
                String t = args[2];
                if (t.equalsIgnoreCase("none")) {
                    e.setLookAtPlayer(false);
                } else {
                    e.setLookAtPlayer(true);
                }
                mgr.save(e);
                sender.sendMessage("§aРежим взгляда обновлен.");
                return true;
            }
            case "action": {
                if (args.length < 4) { sender.sendMessage("Использование: /" + label + " action <id> add <command|message> <text...>"); return true; }
                String id = args[1];
                NpcEntity e = mgr.get(id);
                if (e == null) { sender.sendMessage("§cNPC не найден."); return true; }
                String mode = args[2];
                if (!mode.equalsIgnoreCase("add")) { sender.sendMessage("§cПоддерживается только add."); return true; }
                String type = args[3].toLowerCase(Locale.ROOT);
                String text = joinFrom(args, 4);
                if (type.equals("command")) e.getData().actions.add("command:" + text);
                else if (type.equals("message")) e.getData().actions.add("message:" + text);
                else { sender.sendMessage("§cТип: command|message"); return true; }
                mgr.save(e);
                sender.sendMessage("§aДействие добавлено.");
                return true;
            }
            case "follow": {
                if (args.length < 3) { sender.sendMessage("Использование: /" + label + " follow <id> <player|none> [speed]"); return true; }
                NpcEntity e = mgr.get(args[1]);
                if (e == null) { sender.sendMessage("§cNPC не найден."); return true; }
                String target = args[2];
                if (target.equalsIgnoreCase("none")) {
                    e.getData().followPlayer = null;
                } else {
                    e.getData().followPlayer = target;
                    if (args.length >= 4) {
                        try { e.getData().followSpeed = Double.parseDouble(args[3]); } catch (NumberFormatException ignored) {}
                    }
                }
                mgr.save(e);
                sender.sendMessage("§aСледование обновлено.");
                return true;
            }
            default:
                help(sender);
                return true;
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§6/triqnpc create <id> <skin> [name]");
        sender.sendMessage("§6/triqnpc remove <id>");
        sender.sendMessage("§6/triqnpc list");
        sender.sendMessage("§6/triqnpc setskin <id> <skin/url>");
        sender.sendMessage("§6/triqnpc settype <id> <PLAYER|COW|PIG|VILLAGER>");
        sender.sendMessage("§6/triqnpc setpose <id> <head|body> <angle>");
        sender.sendMessage("§6/triqnpc setitem <id> <main|off> <material>");
        sender.sendMessage("§6/triqnpc look <id> <player|none>");
        sender.sendMessage("§6/triqnpc action <id> add <command|message> <text...>");
        sender.sendMessage("§6/triqnpc follow <id> <player|none> [speed]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("triqnpc.admin")) return Collections.emptyList();
        NpcManager mgr = plugin.getNpcManager();
        if (args.length == 1) return filter(Arrays.asList("create","remove","list","setskin","setpose","setitem","look","action","follow"), args[0]);
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("remove","setskin","setpose","setitem","look","action","follow").contains(sub))
                return filter(mgr.getAll().stream().map(e -> e.getData().id).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "setpose": return filter(Arrays.asList("head","body","arm"), args[2]);
                case "setitem": return filter(Arrays.asList("main","off"), args[2]);
                case "look": return filter(Arrays.asList("player","none"), args[2]);
                case "action": return filter(Arrays.asList("add"), args[2]);
                case "follow": return filter(suggestPlayersWithNone(), args[2]);
            }
        }
        if (args.length == 4) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("action")) return filter(Arrays.asList("command","message"), args[3]);
        }
        return Collections.emptyList();
    }

    private static List<String> filter(List<String> base, String token) {
        String t = token.toLowerCase(Locale.ROOT);
        return base.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(t)).collect(Collectors.toList());
    }

    private static List<String> suggestPlayersWithNone() {
        List<String> res = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) res.add(p.getName());
        res.add("none");
        return res;
    }

    private static String joinFrom(String[] arr, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }
}


