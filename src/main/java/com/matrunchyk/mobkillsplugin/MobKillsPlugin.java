//noinspection unused
package com.matrunchyk.mobkillsplugin;

import com.destroystokyo.paper.event.entity.CreeperIgniteEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class MobKillsPlugin extends JavaPlugin {
    private YamlConfiguration defaultLangConfig;
    private final Map<UUID, YamlConfiguration> playerLangConfigs = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(MobKillsPlugin.class.getName());
    public static final String MOBKILLS_YML = "mobkills.yml";
    public static final String MOB_KILLS = "MobKills";
    // Store the number of mob kills for each player
    private final Map<String, Integer> mobKills = new HashMap<>();

    // Called when the plugin is enabled
    @Override
    public void onEnable() {
        LOGGER.info(ChatColor.LIGHT_PURPLE + "MobKillsPlugin has been enabled!");

        loadConfig();

        // Load the saved scores from file
        loadMobKills();

        // Register the EntityDeathListener to handle mob kills
        getServer().getPluginManager().registerEvents(new MobKillsEventListener(), this);
    }

    // Called when the plugin is disabled
    @Override
    public void onDisable() {
        // Save the current scores to file
        saveMobKills();

        LOGGER.info(ChatColor.LIGHT_PURPLE + "MobKillsPlugin has been disabled!");
    }

    private void loadConfig() {
        saveDefaultConfig();

        File langFile = new File(getDataFolder(), "lang_en.yml");
        defaultLangConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    // Load the saved scores from file
    private void loadMobKills() {
        // Get the file path for the scores file
        File file = new File(getDataFolder(), MOBKILLS_YML);

        // If the file doesn't exist, return (there are no saved scores)
        if (!file.exists()) {
            LOGGER.warning(ChatColor.RED + "File " + file.getAbsolutePath() + " does not exist!");
            return;
        }

        // Load the YAML configuration from the scores file
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Loop through each player name in the configuration
        for (String key : config.getKeys(false)) {
            // Get the number of mob kills for the player and add it to the map
            mobKills.put(key, config.getInt(key));
        }
    }

    // Save the current scores to file
    private void saveMobKills() {
        LOGGER.info(ChatColor.DARK_PURPLE + "Saving killed mobs...");

        // Get the file path for the scores file
        File file = new File(getDataFolder(), MOBKILLS_YML);

        // Create a new YAML configuration to store the scores
        YamlConfiguration config = new YamlConfiguration();

        // Loop through each player in the mobKills map
        for (Map.Entry<String, Integer> entry : mobKills.entrySet()) {
            // Add the player's name and score to the YAML configuration
            config.set(entry.getKey(), entry.getValue());
        }

        // Save the YAML configuration to the scores file
        try {
            config.save(file);
        } catch (IOException e) {
            // If there was an error saving the file, print the stack trace
            e.printStackTrace();
        }
    }

    // Listener class to handle EntityDeathEvents (i.e. mob kills)
    private class MobKillsEventListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            Locale locale = player.locale();
            UUID playerId = event.getPlayer().getUniqueId();

            // Load the language file for the player's preferred language
            File playerLangFile = new File(getDataFolder(), "lang_" + locale.getLanguage() + ".yml");

            YamlConfiguration playerLangConfig;

            if (playerLangFile.exists()) {
                playerLangConfig = YamlConfiguration.loadConfiguration(playerLangFile);
            } else {
                playerLangConfig = new YamlConfiguration();
                playerLangConfig.setDefaults(defaultLangConfig);
            }
            playerLangConfigs.put(playerId, playerLangConfig);
        }

        // Called when a mob is killed by a player
        @EventHandler
        public void onEntityDeath(EntityDeathEvent event) {
            // Check if the killer is a player and the entity is a living entity (not an item or projectile, etc.)
            //noinspection ConstantConditions
            if (event.getEntity().getKiller() instanceof Player && event.getEntityType().isAlive()) {
                updateScoreboard(event);
            }

            // If creeper killed by a wooden shovel
            //noinspection ConstantConditions
            if (event.getEntity().getKiller() instanceof Player && event.getEntity().getType() == EntityType.CREEPER && event.getEntity().getKiller().getInventory().getItemInMainHand().getType() == Material.WOODEN_SHOVEL) {
                tameCreeper(event);
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            Entity entity = event.getEntity();
            String entityName = entity.getName();
            Bukkit.broadcast(Component.text("Entity event occurred: " + event.getEventName() + " for entity: " + entityName));
        }

        @EventHandler
        public void onEntityTarget(EntityTargetEvent event) {
            Entity entity = event.getEntity();
            String entityName = entity.getName();
            Bukkit.broadcast(Component.text("Entity event occurred: " + event.getEventName() + " for entity: " + entityName));
        }

        @EventHandler
        public void onCreeperIgnite(CreeperIgniteEvent event) {
            Entity entity = event.getEntity();
            String entityName = entity.getName();
            Bukkit.broadcast(Component.text("Entity event occurred: " + event.getEventName() + " for entity: " + entityName));
        }

        @EventHandler
        public void onCreeperPower(CreeperPowerEvent event) {
            Entity entity = event.getEntity();
            String entityName = entity.getName();
            Bukkit.broadcast(Component.text("Entity event occurred: " + event.getEventName() + " for entity: " + entityName));
        }

        @EventHandler
        public void onEntityExplode(EntityExplodeEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof Creeper creeper) {

                MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);
                PersistentDataContainer dataContainer = creeper.getPersistentDataContainer();
                assert plugin != null;
                NamespacedKey tamedByKey = new NamespacedKey(plugin, "tamedBy");
                String tamedByValue = dataContainer.get(tamedByKey, PersistentDataType.STRING);

                if (tamedByValue == null) {
                    LOGGER.info(ChatColor.DARK_PURPLE + "Creeper is not tamed by anyone");
                } else {
                    LOGGER.info(ChatColor.DARK_PURPLE + "tamedByValue: " + tamedByValue);
                    UUID steamUUID = UUID.fromString(tamedByValue);
                    Player player = Bukkit.getPlayer(steamUUID);

                    if (player == null) {
                        LOGGER.info(ChatColor.DARK_PURPLE + "Failed to find a player by UUID. Is he offline?");
                    } else if (creeper.getTarget() != player) {
                        player.sendMessage(getMessage(player, ChatColor.RED + "creeper_is_not_targeting"));
                    } else if (creeper.getLocation().distance(player.getLocation()) > 10) {
                        player.sendMessage(getMessage(player,ChatColor.RED + "creeper_is_too_far_away_the_player"));
                    } else {
                        player.sendMessage(getMessage(player,ChatColor.GREEN + "creeper_is_tamed_by_you_cancelling_explosion"));
                        event.setCancelled(true);
                        event.setYield(0);
                        creeper.setIgnited(false);
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
            //noinspection ConstantConditions
            if (event.getRightClicked() instanceof Creeper creeper && event.getPlayer() != null) {
                Player interactedPlayer = event.getPlayer();
                UUID interactedSteamUUID = interactedPlayer.getUniqueId();

                MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);
                PersistentDataContainer dataContainer = creeper.getPersistentDataContainer();
                assert plugin != null;
                NamespacedKey tamedByKey = new NamespacedKey(plugin, "tamedBy");
                NamespacedKey selfDestructionKey = new NamespacedKey(plugin, "selfDestruction");
                String tamedByValue = dataContainer.get(tamedByKey, PersistentDataType.STRING);

                boolean isAboutToSelfDestruct = !dataContainer.getOrDefault(tamedByKey, PersistentDataType.STRING, "").isEmpty();

                if (tamedByValue == null) {
                    interactedPlayer.sendMessage(getMessage(interactedPlayer, ChatColor.RED + "creeper_is_not_tamed_by_anyone"));
                } else if (isAboutToSelfDestruct) {
                    interactedPlayer.sendMessage(getMessage(interactedPlayer, ChatColor.RED + "creeper_is_already_about_to_destruct"));
                } else {
                    LOGGER.info(ChatColor.DARK_PURPLE + "tamedByValue: " + tamedByValue);
                    UUID steamUUID = UUID.fromString(tamedByValue);
                    Player player = Bukkit.getPlayer(steamUUID);

                    if (player == null) {
                        interactedPlayer.sendMessage(getMessage(interactedPlayer, ChatColor.RED + "failed_to_find_a_tamed_player_by_uuid"));
                    } else if (creeper.getTarget() != player) {
                        interactedPlayer.sendMessage(getMessage(interactedPlayer, ChatColor.RED + "creeper_is_not_targeting_the_player"));
                    } else if (creeper.getLocation().distance(player.getLocation()) > 10) {
                        interactedPlayer.sendMessage(getMessage(interactedPlayer, ChatColor.RED + "creeper_is_too_far_away_the_player"));
                    } else if (interactedSteamUUID != steamUUID) {
                        interactedPlayer.sendMessage(getMessage(interactedPlayer, ChatColor.RED + "interacting_player_is_not_the_same"));
                    } else {
                        LOGGER.info(ChatColor.DARK_PURPLE + "Setting CREEPER explosion timeout!");
                        player.sendMessage(getMessage(player, ChatColor.RED + "creeper_is_about_to_explode"));
                        dataContainer.set(selfDestructionKey, PersistentDataType.STRING, "1");

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                LOGGER.info(ChatColor.DARK_PURPLE + "Explosion!!!");
                                if (creeper.isValid()) { // Check if creeper is still valid
                                    Location loc = creeper.getLocation();
                                    World world = loc.getWorld();
                                    world.createExplosion(
                                        loc,
                                        1f,
                                        false,
                                        true,
                                        player
                                    ); // Explode the creeper
                                    creeper.remove(); // Remove the creeper

                                    world.setStorm(true);
                                    world.setThundering(true);
                                    world.strikeLightning(loc);
                                }
                            }
                        }.runTaskLater(plugin, 20 * 10); // 20 ticks per second, 10 seconds delay
                    }
                }
            }
        }

        private void tameCreeper(EntityDeathEvent event) {
            Entity entity = event.getEntity();
            Creeper creeper = (Creeper) entity;
            Player player = event.getEntity().getKiller();
            assert player != null;
            UUID steamUUID = player.getUniqueId();

            event.setCancelled(true); // Prevent the entity from dying

            creeper.setPowered(true);
            creeper.setExplosionRadius(1);
            creeper.setIgnited(false);
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 4f, 1f);

            MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);
            PersistentDataContainer dataContainer = creeper.getPersistentDataContainer();
            assert plugin != null;
            NamespacedKey tamedByKey = new NamespacedKey(plugin, "tamedBy");
            String tamedByValue = steamUUID.toString();
            dataContainer.set(tamedByKey, PersistentDataType.STRING, tamedByValue);
        }

        /**
         * Get the player who killed the mob and increment their kill count in the mobKills map
         *
         * @param event EntityDeathEvent
         */
        private void updateScoreboard(EntityDeathEvent event) {
            Player player = event.getEntity().getKiller();

            assert player != null;
            int newScore = mobKills.getOrDefault(player.getName(), 0) + 1;
            mobKills.put(player.getName(), newScore);

            LOGGER.info(ChatColor.DARK_PURPLE + player.getName() + "Killed " + event.getEntity().getName() + " — incrementing MobKill score");

            // Update the player's scoreboard with their new score
            Scoreboard scoreboard = player.getScoreboard();
            Objective objective = scoreboard.getObjective(MOB_KILLS);

            // If the objective doesn't exist, create it
            if (objective == null) {
                LOGGER.warning(ChatColor.DARK_PURPLE + MOB_KILLS + " scoreboard doesn't exist, creating...");
                objective = scoreboard.registerNewObjective(MOB_KILLS, Criteria.DUMMY, Component.text("Mob Kills"));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }

            // Update the player's score in the objective
            Score score = objective.getScore(player.getName());
            score.setScore(newScore);

            // Notify the player of their new score
            player.sendMessage(getMessage(player, ChatColor.GREEN + "you_killed_mobs", newScore));

            // Loop through all online players and update their scoreboards with the latest scores
            for (Player p : Bukkit.getOnlinePlayers()) {
                Objective obj = p.getScoreboard().getObjective(MOB_KILLS);

                // If the objective doesn't exist, create it
                if (obj == null) {
                    obj = p.getScoreboard().registerNewObjective(MOB_KILLS, Criteria.DUMMY, Component.text("Mob Kills"));
                    obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                }

                // Get the player's current score (or 0 if they don't have a score yet)
                int scoreValue = mobKills.getOrDefault(p.getName(), 0);

                // Update the player's score in the objective
                Score objectiveScore = obj.getScore(player.getName());
                objectiveScore.setScore(scoreValue);
            }
        }
    }

    public String getMessage(Player player, String key, Object... placeholders) {
        UUID playerId = player.getUniqueId();
        YamlConfiguration langConfig = playerLangConfigs.get(playerId);

        if (langConfig == null) {
            langConfig = defaultLangConfig;
        }

        String message = langConfig.getString(key);
        if (message == null) {
            return "";
        }

        // Replace placeholders
        if (placeholders != null && placeholders.length > 0) {
            message = String.format(message, placeholders);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}