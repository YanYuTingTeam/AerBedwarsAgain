package com.aermini.bwagain.utils;

import com.aermini.bwagain.AerBedwarsAgain;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

public class BungeeCordUtils {

    public static void sendToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(AerBedwarsAgain.getInstance(), AerBedwarsAgain.BUNGEE_CHANNEL, out.toByteArray());
    }

    public static void sendEvent(Player player, String eventName, String... params) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("AerBedwarsAgain");
        StringBuilder message = new StringBuilder("event=" + eventName);
        for (String param : params) message.append(",").append(param);
        out.writeUTF(message.toString());
        player.sendPluginMessage(AerBedwarsAgain.getInstance(), AerBedwarsAgain.BUNGEE_CHANNEL, out.toByteArray());
    }

    public static void sendPlayAgain(Player player, String group) {
        sendEvent(player, "AerJoinerR", "player=" + player.getName(), "group=" + group);
    }

    public static void sendToHub(Player player, String hubServer) {
        sendToServer(player, hubServer);
    }
}