package org.ayosynk.trchataddon;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.Relational;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class DirectionExpansion extends PlaceholderExpansion implements Relational {

    private final TrChatAddon plugin;

    public DirectionExpansion(TrChatAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "ayosynk";
    }

    @Override
    public String getIdentifier() {
        return "trchataddon";
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player viewer, Player target, String identifier) {
        if (viewer == null || target == null)
            return "";
        if (!identifier.equals("dirptr"))
            return null; // unknown placeholder

        try {
            Location locV = viewer.getLocation();
            Location locT = target.getLocation();

            // Null-safe world comparison (players can be mid-transition)
            if (locV.getWorld() == null || locT.getWorld() == null)
                return "";
            if (!locV.getWorld().equals(locT.getWorld()))
                return "";

            // Use distanceSquared to avoid sqrt and the cross-world
            // IllegalArgumentException
            double maxDistance = plugin.getConfig().getDouble("max-distance", 150.0);
            double distSq = locV.distanceSquared(locT);
            if (distSq > maxDistance * maxDistance)
                return "";

            double distance = Math.sqrt(distSq);

            double dx = locT.getX() - locV.getX();
            double dz = locT.getZ() - locV.getZ();

            // Angle from viewer to target in world space (0 = north = -Z in Minecraft)
            double angleToTarget = Math.toDegrees(Math.atan2(dx, -dz)); // range (-180, 180]

            // Minecraft yaw: 0 = south (+Z), increases clockwise. Convert to north-based
            // bearing.
            double viewerYaw = locV.getYaw(); // raw yaw, can be any float

            // How far right/left is the target relative to where the viewer is facing?
            double diff = angleToTarget - viewerYaw;

            // Normalise diff to [-180, 180] robustly
            diff = ((diff % 360) + 360) % 360; // now in [0, 360)
            if (diff > 180)
                diff -= 360; // now in (-180, 180]

            String arrow = getArrowFromAngle(diff);
            String formattedDist = String.format("%.0f", distance);
            String format = plugin.getConfig().getString("direction-format", "\u00268[\u0026b%dist%m %arrow%\u00268]");
            return format.replace("%dist%", formattedDist).replace("%arrow%", arrow);

        } catch (Exception e) {
            // Never let a fringe exception break the placeholder silently
            return "";
        }
    }

    @Override
    public String onRequest(OfflinePlayer p, String params) {
        if (params == null) {
            return null;
        }

        // Provide a non-relational fallback if needed
        if (params.equals("dirptr")) {
            return "&c[Requires Viewer]";
        }

        if (p == null) {
            return "";
        }

        if (params.equals("lp_prefix_space")) {
            String prefix = PlaceholderAPI.setPlaceholders(p, "%luckperms_prefix%");
            if (prefix == null || prefix.trim().isEmpty()) {
                return "";
            }
            return prefix;
        }

        if (params.equals("lp_gap")) {
            String prefix = PlaceholderAPI.setPlaceholders(p, "%luckperms_prefix%");
            if (prefix == null || prefix.trim().isEmpty()) {
                return "";
            }
            return " ";
        }

        if (params.equals("world_gap")) {
            String world = PlaceholderAPI.setPlaceholders(p, "%javascript_world%");
            if (world == null || world.trim().isEmpty()) {
                return "";
            }
            return " ";
        }

        if (params.equals("world_prefix_gap")) {
            String world = PlaceholderAPI.setPlaceholders(p, "%javascript_world%");
            if (world == null || world.trim().isEmpty()) {
                return "";
            }
            String prefix = PlaceholderAPI.setPlaceholders(p, "%luckperms_prefix%");
            if (prefix == null || prefix.trim().isEmpty()) {
                return "";
            }
            return " ";
        }

        if (params.equals("lp_suffix_space")) {
            String suffix = PlaceholderAPI.setPlaceholders(p, "%luckperms_suffix%");
            if (suffix == null || suffix.trim().isEmpty()) {
                return "";
            }
            return " " + suffix;
        }

        if (params.equals("name_color")) {
            String prefix = PlaceholderAPI.setPlaceholders(p, "%luckperms_prefix%");
            String metaColor = PlaceholderAPI.setPlaceholders(p, "%luckperms_meta_chat_color%");
            if (prefix == null || prefix.trim().isEmpty()) {
                return metaColor == null ? "" : metaColor;
            }
            String translated = ChatColor.translateAlternateColorCodes('&', prefix);
            String lastColors = ChatColor.getLastColors(translated);
            if (lastColors == null || lastColors.isEmpty()) {
                return metaColor == null ? "" : metaColor;
            }
            return lastColors.replace('§', '&');
        }

        if (params.equals("chat_color")) {
            String metaColor = PlaceholderAPI.setPlaceholders(p, "%luckperms_meta_chat_color%");
            if (metaColor != null && !metaColor.trim().isEmpty()) {
                return metaColor;
            }

            String prefix = PlaceholderAPI.setPlaceholders(p, "%luckperms_prefix%");
            if (prefix == null || prefix.trim().isEmpty()) {
                return "&7";
            }

            String translated = ChatColor.translateAlternateColorCodes('&', prefix);
            String lastColors = ChatColor.getLastColors(translated);
            if (lastColors == null || lastColors.isEmpty()) {
                return "&7";
            }
            return lastColors.replace('§', '&');
        }

        return null; //
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
