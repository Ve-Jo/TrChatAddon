package org.ayosynk.trchataddon.listeners;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import org.ayosynk.trchataddon.TrChatAddon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AuraSkillsListener implements Listener {

    private final TrChatAddon plugin;
    private final String broadcastMessage;

    public AuraSkillsListener(TrChatAddon plugin) {
        this.plugin = plugin;
        this.broadcastMessage = plugin.getConfig().getString("auraskills-broadcast.message",
                "&8[&bНавыки&8] &7Игрок &b%player% &7повысил навык &b%skill% &7до уровня &b%level%&7! Общая сила: &b%power%");
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onSkillLevelUp(SkillLevelUpEvent event) {
        if (!plugin.getConfig().getBoolean("auraskills-broadcast.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null)
            return;

        String skillName = getLocalizedSkillName(event.getSkill().name());
        int newLevel = event.getLevel();
        int totalPower = AuraSkillsApi.get().getUser(player.getUniqueId()).getPowerLevel();

        String message = broadcastMessage
                .replace("%player%", player.getName())
                .replace("%skill%", skillName)
                .replace("%level%", String.valueOf(newLevel))
                .replace("%power%", String.valueOf(totalPower));

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private String getLocalizedSkillName(String enumName) {
        switch (enumName.toUpperCase()) {
            case "FARMING":
                return "Фермерство";
            case "FORAGING":
                return "Рубка дерева";
            case "MINING":
                return "Шахтёрство";
            case "FISHING":
                return "Рыбалка";
            case "EXCAVATION":
                return "Раскопки";
            case "ARCHERY":
                return "Стрельба из лука";
            case "DEFENSE":
                return "Защита";
            case "FIGHTING":
                return "Сражения";
            case "ENDURANCE":
                return "Выносливость";
            case "AGILITY":
                return "Ловкость";
            case "ALCHEMY":
                return "Алхимия";
            case "ENCHANTING":
                return "Наложение чар";
            case "SORCERY":
                return "Колдовство";
            case "HEALING":
                return "Лечение";
            case "FORGING":
                return "Ковка";
            default:
                return enumName;
        }
    }
}
