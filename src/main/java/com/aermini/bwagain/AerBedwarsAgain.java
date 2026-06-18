package com.aermini.bwagain;

import com.aermini.bwagain.listeners.BedwarsListener;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class AerBedwarsAgain extends JavaPlugin {

    private static AerBedwarsAgain instance;
    private Logger logger;
    public static final String BUNGEE_CHANNEL = "BungeeCord";

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, BUNGEE_CHANNEL);
        getServer().getPluginManager().registerEvents(new BedwarsListener(this), this);

        logger.info("=================================");
        logger.info("  AerBedwarsAgain 已启用!");
        logger.info("  作者: AerMini");
        logger.info("  版本: " + getDescription().getVersion());
        logger.info("=================================");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, BUNGEE_CHANNEL);
        logger.info("AerBedwarsAgain 已禁用!");
    }

    public static AerBedwarsAgain getInstance() {
        return instance;
    }
    public Logger getPluginLogger() {
        return logger;
    }

    public void sendBungeeMessage(org.bukkit.entity.Player player, String subchannel, String... data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        for (String item : data) out.writeUTF(item);
        player.sendPluginMessage(this, BUNGEE_CHANNEL, out.toByteArray());
    }
}