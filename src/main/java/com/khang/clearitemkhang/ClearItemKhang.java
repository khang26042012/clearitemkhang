package com.khang.clearitemkhang;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClearItemKhang extends JavaPlugin {

    private int intervalMinutes;
    private int warningSeconds;
    private String warnMessage;
    private String warnEmptyMessage;
    private String countdownMessage;
    private String doneMessage;
    private String doneEmptyMessage;
    private boolean consoleLog;
    private BukkitTask cycleTask;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        startCycle();
        getLogger().info("ClearItemKhang v" + getDescription().getVersion() + " đã bật! Dọn mỗi " + intervalMinutes + " phút, cảnh báo trước " + warningSeconds + " giây.");
    }

    @Override
    public void onDisable() {
        if (cycleTask != null) {
            cycleTask.cancel();
        }
        getLogger().info("ClearItemKhang disabled.");
    }

    private void loadConfig() {
        reloadConfig();
        intervalMinutes = Math.max(1, getConfig().getInt("interval-minutes", 5));
        warningSeconds = Math.max(0, getConfig().getInt("warning-seconds", 10));
        warnMessage = getConfig().getString("warn-message", "&6[🧹 Lao Công] &eLao công may mắn của ngày hôm nay &b{player} &esẽ đến dọn dẹp sau &c{seconds} &egiây nữa, nhớ cất đồ kĩ vào!");
        warnEmptyMessage = getConfig().getString("warn-empty-message", "&6[🧹 Lao Công] &eSẽ đến dọn dẹp sau &c{seconds} &egiây nữa, nhớ cất đồ kĩ vào!");
        countdownMessage = getConfig().getString("countdown-message", "&6[🧹 Lao Công] &eDọn dẹp sau &c{seconds} &egiây...!");
        doneMessage = getConfig().getString("done-message", "&6[🧹 Lao Công] &b{player} &eđã dọn xong &c{count} &evật phẩm rơi!");
        doneEmptyMessage = getConfig().getString("done-empty-message", "&6[🧹 Lao Công] &eĐã dọn xong &c{count} &evật phẩm rơi!");
        consoleLog = getConfig().getBoolean("console-log", true);
    }

    private void startCycle() {
        if (cycleTask != null) {
            cycleTask.cancel();
        }
        long intervalTicks = 20L * 60L * intervalMinutes;
        long warnTicks = 20L * warningSeconds;
        // Chay warn truoc, clear chay sau warningSeconds
        cycleTask = new BukkitRunnable() {
            @Override
            public void run() {
                doWarnPhase(warnTicks);
            }
        }.runTaskTimer(this, intervalTicks, intervalTicks);
    }

    private String pickLuckyCleaner() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            return null;
        }
        return online.get(random.nextInt(online.size())).getName();
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private void doWarnPhase(long warnTicks) {
        String lucky = pickLuckyCleaner();
        String warn;
        if (lucky == null) {
            warn = color(warnEmptyMessage.replace("{seconds}", String.valueOf(warningSeconds)));
        } else {
            warn = color(warnMessage.replace("{player}", lucky).replace("{seconds}", String.valueOf(warningSeconds)));
        }
        Bukkit.broadcastMessage(warn);
        if (consoleLog) {
            getLogger().info(ChatColor.stripColor(warn));
        }
        // Dem nguoc 5..1 giay cuoi
        for (int s = 5; s >= 1; s--) {
            if (s > warningSeconds) {
                continue;
            }
            final int sec = s;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.broadcastMessage(color(countdownMessage.replace("{seconds}", String.valueOf(sec))));
                }
            }.runTaskLater(this, warnTicks - (20L * sec));
        }
        // Clear sau warningSeconds
        final String luckyFinal = lucky;
        new BukkitRunnable() {
            @Override
            public void run() {
                clearItems(luckyFinal);
            }
        }.runTaskLater(this, warnTicks);
    }

    private void clearItems(String lucky) {
        int count = 0;
        int skipped = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    // KHONG xoa Trung Rong (EggKhang) - de event truy lung tiep tuc
                    if (item.getItemStack() != null && item.getItemStack().getType() == org.bukkit.Material.DRAGON_EGG) {
                        skipped++;
                        continue;
                    }
                    entity.remove();
                    count++;
                }
            }
        }
        String msg;
        if (lucky == null) {
            msg = color(doneEmptyMessage.replace("{count}", String.valueOf(count)).replace("{minutes}", String.valueOf(intervalMinutes)));
        } else {
            msg = color(doneMessage.replace("{player}", lucky).replace("{count}", String.valueOf(count)).replace("{minutes}", String.valueOf(intervalMinutes)));
        }
        if (count > 0) {
            Bukkit.broadcastMessage(msg);
        }
        if (consoleLog) {
            getLogger().info("Lao công " + (lucky == null ? "(không ai online)" : lucky) + " đã dọn " + count + " vật phẩm rơi." + (skipped > 0 ? " (bỏ qua " + skipped + " Trứng Rồng)" : ""));
        }
    }
}
