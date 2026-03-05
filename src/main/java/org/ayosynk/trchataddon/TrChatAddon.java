package org.ayosynk.trchataddon;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.arasple.mc.trchat.api.event.TrChatReceiveEvent;
import me.arasple.mc.trchat.module.display.ChatSession;
import me.arasple.mc.trchat.module.display.channel.Channel;
import me.arasple.mc.trchat.module.display.channel.obj.ChannelRange;
import me.arasple.mc.trchat.taboolib.module.chat.ComponentText;
import me.arasple.mc.trchat.taboolib.module.chat.Components;

import me.clip.placeholderapi.PlaceholderAPI;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import org.ayosynk.trchataddon.listeners.AuraSkillsListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

public class TrChatAddon extends JavaPlugin implements Listener {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("(?i)[&§]([0-9A-FK-OR])");

    private String noOneHeardMessage;
    private String directionFormat;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        noOneHeardMessage = getConfig().getString("no-one-heard-message",
                "&c[!] Вас никто не услышал... Рядом никого нет.");
        directionFormat = getConfig().getString("direction-format", "&8[&b%dist%m %arrow%&8] ");
        msg("§aTrChatAddon enabled.");
        getServer().getPluginManager().registerEvents(this, this);

        if (getServer().getPluginManager().getPlugin("AuraSkills") != null) {
            getServer().getPluginManager().registerEvents(new AuraSkillsListener(this), this);
            msg("§aTrChatAddon: Hooked into AuraSkills successfully.");
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DirectionExpansion(this).register();
            msg("§aTrChatAddon: Registered PlaceholderAPI expansion.");
        }

        getServer().getMessenger().registerIncomingPluginChannel(this, "trchataddon:main",
                new org.ayosynk.trchataddon.listeners.ProxyMessageListener(this));
    }

    @Override
    public void onDisable() {
        msg("§cTrChatAddon disabled.");
    }

    private void msg(String log) {
        getServer().getConsoleSender().sendMessage(log);
    }

    // === "No one heard you" feature ===
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (event.viewers().size() <= 1) {
            event.getPlayer().sendMessage(LEGACY_SERIALIZER.deserialize(noOneHeardMessage));
        }
    }

    // === Chat Color Injection via LuckPerms meta ===
    // Runs at HIGH priority before TrChat processes the message
    @EventHandler(priority = EventPriority.HIGH)
    public void onChatColor(AsyncChatEvent event) {
        Player sender = event.getPlayer();

        // Get chat color from LuckPerms meta
        String chatColor = PlaceholderAPI.setPlaceholders(sender, "%luckperms_meta_chat_color%");
        if (chatColor == null || chatColor.trim().isEmpty() || chatColor.equals("%luckperms_meta_chat_color%")) {
            // Fallback to extracting from prefix
            String prefix = PlaceholderAPI.setPlaceholders(sender, "%luckperms_prefix%");
            if (prefix == null || prefix.trim().isEmpty()) {
                chatColor = "&7";
            } else {
                String lastColors = extractLastLegacyColors(prefix);
                if (lastColors == null || lastColors.isEmpty()) {
                    chatColor = "&7";
                } else {
                    chatColor = lastColors;
                }
            }
        }

        // Inject color after any TrChat channel prefix (!, @, etc) to not break channel detection
        String originalMessage = PLAIN_TEXT_SERIALIZER.serialize(event.message());
        String coloredPrefix = chatColor;
        String coloredMessage;

        // Check for channel prefixes and inject color after them
        if (originalMessage.startsWith("!")) {
            coloredMessage = "!" + coloredPrefix + originalMessage.substring(1);
        } else if (originalMessage.startsWith("@")) {
            coloredMessage = "@" + coloredPrefix + originalMessage.substring(1);
        } else {
            // No channel prefix - prepend color normally
            coloredMessage = coloredPrefix + originalMessage;
        }
        event.message(LEGACY_SERIALIZER.deserialize(coloredMessage));
    }

    private String extractLastLegacyColors(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace('§', '&');
        Matcher matcher = LEGACY_CODE_PATTERN.matcher(normalized);
        String lastColor = "";
        StringBuilder formats = new StringBuilder();
        while (matcher.find()) {
            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                lastColor = "&" + code;
                formats.setLength(0);
            } else if (code == 'r') {
                lastColor = "";
                formats.setLength(0);
            } else if ("klmno".indexOf(code) >= 0) {
                String formatToken = "&" + code;
                if (formats.indexOf(formatToken) < 0) {
                    formats.append(formatToken);
                }
            }
        }
        return lastColor + formats;
    }

    // === Per-receiver Direction Arrow via TrChatReceiveEvent ===
    @EventHandler
    public void onTrChatReceive(TrChatReceiveEvent event) {
        CommandSender receiverSender = event.getReceiver();
        UUID senderUUID = event.getSender();

        if (!(receiverSender instanceof Player))
            return;
        if (senderUUID == null)
            return;

        Player receiver = (Player) receiverSender;
        Player sender = getServer().getPlayer(senderUUID);

        if (sender == null)
            return;
        // Don't show arrow to the sender themselves
        if (receiver.getUniqueId().equals(senderUUID))
            return;

        // Only show direction arrow for distance-based (local) chat channels
        ChatSession session = event.getSession();
        if (session == null)
            return;
        Channel channel = session.getLastChannel();
        if (channel == null)
            return;
        ChannelRange range = channel.getSettings().getRange();
        if (range == null || range.getType() != ChannelRange.Type.DISTANCE)
            return;

        Location locV = receiver.getLocation();
        Location locT = sender.getLocation();

        if (!locV.getWorld().equals(locT.getWorld()))
            return;

        double distance = locV.distance(locT);
        double dx = locT.getX() - locV.getX();
        double dz = locT.getZ() - locV.getZ();

        double angleToTarget = Math.toDegrees(Math.atan2(dz, dx)) - 90;
        if (angleToTarget < 0)
            angleToTarget += 360;

        double viewerYaw = locV.getYaw();
        if (viewerYaw < 0)
            viewerYaw += 360;
        viewerYaw = viewerYaw % 360;

        double diff = angleToTarget - viewerYaw;
        if (diff < -180)
            diff += 360;
        if (diff > 180)
            diff -= 360;

        String arrow = getArrowFromAngle(diff);
        String distStr = String.format("%.0f", distance);

        String prefix = directionFormat
                .replace("%dist%", distStr)
                .replace("%arrow%", arrow);
        if (!prefix.isEmpty() && !Character.isWhitespace(prefix.charAt(prefix.length() - 1))) {
            prefix = prefix + " ";
        }

        // Build the prefix using TrChat's ComponentText system and prepend to message
        String coloredPrefix = prefix;
        ComponentText prefixComponent = Components.INSTANCE.text(coloredPrefix);
        ComponentText original = event.getMessage();
        event.setMessage(prefixComponent.append(original));
    }

    private String getArrowFromAngle(double angle) {
        if (angle >= -22.5 && angle < 22.5)
            return "⬆";
        if (angle >= 22.5 && angle < 67.5)
            return "⬈";
        if (angle >= 67.5 && angle < 112.5)
            return "➡";
        if (angle >= 112.5 && angle < 157.5)
            return "⬊";
        if (angle >= 157.5 || angle < -157.5)
            return "⬇";
        if (angle >= -157.5 && angle < -112.5)
            return "⬋";
        if (angle >= -112.5 && angle < -67.5)
            return "⬅";
        if (angle >= -67.5 && angle < -22.5)
            return "⬉";
        return "•";
    }
}
