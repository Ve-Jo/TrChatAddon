package org.ayosynk.trchataddon.listeners;

import org.ayosynk.trchataddon.TrChatAddon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

public class ProxyMessageListener implements PluginMessageListener {

    private final TrChatAddon plugin;
    private final Random random = new Random();

    public ProxyMessageListener(TrChatAddon plugin) {
        this.plugin = plugin;
    }

    private String convertToMiniMessage(String text) {
        if (text == null)
            return "";
        // Replace markdown links with MiniMessage format
        text = text.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)",
                "<click:open_url:'$2'><hover:show_text:'<gray>Нажмите, чтобы открыть ссылку'>$1</hover></click>");

        // Convert legacy color codes to MiniMessage tags
        text = text.replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
        return text;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("trchataddon:main")) {
            return;
        }

        String subchannel = new String(message, StandardCharsets.UTF_8);
        if (subchannel.equals("ThematicBroadcast")) {
            List<String> messages = plugin.getConfig().getStringList("thematic-messages");
            if (messages == null || messages.isEmpty()) {
                return;
            }

            String msg = messages.get(random.nextInt(messages.size()));

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                msg = PlaceholderAPI.setPlaceholders(null, msg);
            }

            String mmString = convertToMiniMessage(msg);

            for (String line : mmString.split("\\n")) {
                Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(line));
            }
            plugin.getLogger().info("Broadcasted thematic message from proxy signal.");
        }
    }
}
