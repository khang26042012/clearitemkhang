package com.khang.clearitemkhang;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

public class ClearItemKhang extends JavaPlugin {

    private int intervalMinutes;
    private String broadcastMessage;
    private boolean consoleLog;
    private BukkitRunnable task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        startTask();
        getLogger().info("ClearItemKhang enabled! Clears items every " + intervalMinutes + " minute(s).");
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
        }
        getLogger().info("ClearItemKhang disabled.");
    }

    private void loadConfig() {
        reloadConfig();
        intervalMinutes = getConfig().getInt("interval-minutes", 5);
        if (intervalMinutes < 1) {
            intervalMinutes = 1;
        }
        broadcastMessage = getConfig().getString("broadcast-message",
            "&6[ClearItemKhang] &eCleared &c{count} &edropped items!");
        consoleLog = getConfig().getBoolean("console-log", true);
    }

    private void startTask() {
        if (task != null) {
            task.cancel();
        }
        // 20 ticks/sec * 60 sec * minutes = ticks
        long ticks = 20L * 60L * intervalMinutes;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                clearItems();
            }
        };
        task.runTaskTimer(this, ticks, ticks);
    }

    private void clearItems() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    entity.remove();
                    count++;
                }
            }
        }

        String msg = broadcastMessage
            .replace("{count}", String.valueOf(count))
            .replace("{minutes}", String.valueOf(intervalMinutes));
        msg = ChatColor.translateAlternateColorCodes('&', msg);

        if (count > 0) {
            Bukkit.broadcastMessage(msg);
        }
        if (consoleLog) {
            getLogger().info("Cleared " + count + " dropped items.");
        }
    }
}
