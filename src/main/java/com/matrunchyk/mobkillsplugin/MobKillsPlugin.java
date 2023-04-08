//noinspection unused
package com.matrunchyk.mobkillsplugin;

import com.destroystokyo.paper.event.entity.CreeperIgniteEvent;
import com.matrunchyk.mobkillsplugin.models.DelayedNote;
import com.matrunchyk.mobkillsplugin.models.NoteDelay;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Score;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class MobKillsPlugin extends JavaPlugin {
    private YamlConfiguration defaultLangConfig;
    private FileConfiguration config;
    private final Map<UUID, YamlConfiguration> playerLangConfigs = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(MobKillsPlugin.class.getName());
    public static final String MOBKILLS_YML = "mobkills.yml";
    public static final String MOB_KILLS = "MobKills";
    // Store the number of mob kills for each player
    private final Map<String, Integer> mobKills = new HashMap<>();

    private final Map<UUID, Integer> skeletonDeathCountMap = new HashMap<>();

    // Called when the plugin is enabled
    @Override
    public void onEnable() {
        LOGGER.info("MobKillsPlugin v2.2 has been enabled!");

        loadConfig();

        // Load the saved scores from file
        loadMobKills();

        // Register the EntityDeathListener to handle mob kills
        getServer().getPluginManager().registerEvents(new MobKillsEventListener(), this);

        // Register the DaddyPowerCommand
        PluginCommand daddyPowerCommand = getCommand("daddy-power");

        if (daddyPowerCommand != null) {
            daddyPowerCommand.setExecutor(new DaddyPowerCommand(this));
        }

        // Register the DaddyPowerCommand
        PluginCommand happyBirthdayCommand = getCommand("happy-birthday");

        if (happyBirthdayCommand != null) {
            happyBirthdayCommand.setExecutor(new HappyBirthdayCommand(this));
        }

        // Register the DebugCommand
        PluginCommand debugCommand = getCommand("debug");

        if (debugCommand != null) {
            debugCommand.setExecutor(new DebugCommand(this));
        }
    }

    // Called when the plugin is disabled
    @Override
    public void onDisable() {
        // Save the current scores to file
        saveMobKills();

        LOGGER.info("MobKillsPlugin has been disabled!");
    }

    private void loadConfig() {
        LOGGER.info("Loading config...");

        // Force saving defaults from resource files, overriding existing ones, if any
        saveDefaultConfig();
        saveResource("lang/lang_en.yml", true);

        // Load lang_en.yml
        File langFile = new File(getDataFolder(), "lang_en.yml");
        defaultLangConfig = YamlConfiguration.loadConfiguration(langFile);

        LOGGER.info("Default language configuration:");
        LOGGER.info(defaultLangConfig.saveToString());
    }

    // Load the saved scores from file
    private void loadMobKills() {
        LOGGER.info("Loading stats...");

        // Get the file path for the scores file
        File file = new File(getDataFolder(), MOBKILLS_YML);

        // If the file doesn't exist, return (there are no saved scores)
        if (!file.exists()) {
            LOGGER.warning("File " + file.getAbsolutePath() + " does not exist!");
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
        LOGGER.info("Saving killed mobs...");

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

    public int getSkeletonCount(Player player) {
        UUID uuid = player.getUniqueId();

        if (!skeletonDeathCountMap.containsKey(uuid)) {
            return 0;
        }
        return skeletonDeathCountMap.get(uuid);
    }

    public void incrementSkeletonCount(Player player) {
        UUID uuid = player.getUniqueId();
        int deathCount = skeletonDeathCountMap.getOrDefault(uuid, 0);

        LOGGER.info("Skeleton death count of player " + player.getName() + " is increased to " + (deathCount + 1));
        skeletonDeathCountMap.put(uuid, deathCount + 1);
    }

    public void resetSkeletonCount(Player player) {
        LOGGER.info("Resetting death count of player " + player.getName());
        skeletonDeathCountMap.put(player.getUniqueId(), 0);
    }

    // Listener class to handle EntityDeathEvents (i.e. mob kills)
    private class MobKillsEventListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            Locale locale = player.locale();
            UUID playerId = event.getPlayer().getUniqueId();

            // Load the language file for the player's preferred language
            String langFilePath = "lang/lang_" + locale.getLanguage() + ".yml";
            YamlConfiguration playerLangConfig;

            LOGGER.info("Player " + player.getName() + " joined the game, loading localization file: " + langFilePath);

            if (getResource(langFilePath) == null) {
                playerLangConfig = new YamlConfiguration();
                playerLangConfig.setDefaults(defaultLangConfig);
            } else {
                saveResource(langFilePath, true);

                File playerLangFile = new File(getDataFolder(), langFilePath);
                playerLangConfig = YamlConfiguration.loadConfiguration(playerLangFile);
            }

            playerLangConfigs.put(playerId, playerLangConfig);
        }

        // Called when a mob is killed by a player
        @EventHandler
        public void onEntityDeath(EntityDeathEvent event) {
            Player killer = event.getEntity().getKiller();

            if (killer == null) {
                return;
            }

            // Check if the killer is a player and the entity is a living entity (not an item or projectile, etc.)
            //noinspection ConstantConditions
            if (killer instanceof Player && event.getEntityType().isAlive()) {
                updateScoreboard(event);
            }

            Material killedWith = killer.getInventory().getItemInMainHand().getType();
            EntityType killedEntityType = event.getEntity().getType();

            // If entity killed by a special weapon (wooden shovel or crossbow)
            //noinspection ConstantConditions
            if (event.getEntity().getKiller() instanceof Player
                    && (killedEntityType == EntityType.CREEPER || killedEntityType == EntityType.ZOMBIE)
                    && killedWith == Material.WOODEN_SHOVEL
            ) {
                tameMob(event);
            }

            if (event.getEntity().getKiller() instanceof Player && killedEntityType == EntityType.SKELETON) {
                incrementSkeletonCount(event.getEntity().getKiller());
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            Entity entity = event.getEntity();
            String entityName = entity.getName();
            String damagedBy = event.getDamager().getName();

            if (event.getDamager() instanceof Creeper && entity instanceof Player) {
                Creeper creeper = (Creeper) event.getDamager();
                Player player = (Player) entity;
                UUID interactedSteamUUID = player.getUniqueId();

                MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);
                PersistentDataContainer dataContainer = creeper.getPersistentDataContainer();
                assert plugin != null;
                NamespacedKey tamedByKey = new NamespacedKey(plugin, "tamedBy");
                NamespacedKey selfDestructionKey = new NamespacedKey(plugin, "selfDestruction");
                String tamedByValue = dataContainer.getOrDefault(tamedByKey, PersistentDataType.STRING, "");

                LOGGER.info("Creeper " + entityName + " was tamed by: " + tamedByValue);
                if (!tamedByValue.isEmpty()) {
                    event.setCancelled(true);
                }
            }
        }

        /*
        @EventHandler
        public void onEntityTarget(EntityTargetEvent event) {
            Entity entity = event.getEntity();
            String entityName = entity.getName();
            Entity target = event.getTarget();

            if (target == null) {
                 LOGGER.info("Entity " + event.getEntity().getName() + " has no target in location " + event.getEntity().getLocation());
            } else {
                 LOGGER.info("Entity " + event.getTarget().getName() + " was targeted by " + event.getEntity().getName() + " in location " + event.getEntity().getLocation());
            }
        }
        */

        @EventHandler
        public void onCreeperIgnite(CreeperIgniteEvent event) {
            Entity entity = event.getEntity();
            String entityName = entity.getName();
            LOGGER.info("Creeper is ignited");
        }

        @EventHandler
        public void onCreeperPower(CreeperPowerEvent event) {
            Entity entity = event.getEntity();
            String entityName = entity.getName();
            LOGGER.info("Creeper is powered!");
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity entity = (LivingEntity) event.getEntity();
                String entityName = entity.getName();
                // LOGGER.info("Entity " + entityName + " is damaged by " + event.getCause() + " of " + event.getDamage());

                if (entity.getHealth() <= 0) {
                    entity.setHealth(Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue());
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onEntityExplode(EntityExplodeEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof Creeper) {
                Creeper creeper = (Creeper) entity;
                MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);
                PersistentDataContainer dataContainer = creeper.getPersistentDataContainer();
                assert plugin != null;
                NamespacedKey tamedByKey = new NamespacedKey(plugin, "tamedBy");
                String tamedByValue = dataContainer.get(tamedByKey, PersistentDataType.STRING);

                if (tamedByValue == null) {
                    LOGGER.info("Creeper is not tamed by anyone");
                } else {
                    LOGGER.info("Exploded creeper was tamed by " + tamedByValue);
                    UUID steamUUID = UUID.fromString(tamedByValue);
                    Player player = Bukkit.getPlayer(steamUUID);

                    if (player == null) {
                        LOGGER.info("Failed to find a player by UUID. Is he offline?");
                    } else if (creeper.getTarget() != player) {
                        player.sendMessage(Component.text(ChatColor.RED + getMessage(player, "creeper_is_not_targeting")));
                    } else if (creeper.getLocation().distance(player.getLocation()) > 10) {
                        player.sendMessage(Component.text(ChatColor.RED + getMessage(player, "creeper_is_too_far_away_the_player")));
                    } else {
                        player.sendMessage(Component.text(ChatColor.GREEN + getMessage(player, "creeper_is_tamed_by_you_cancelling_explosion")));
                        event.setCancelled(true);
                        event.setYield(0);
                        creeper.setIgnited(false);
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
            LOGGER.info(event.getPlayer().getName() + " is interacted with hand " + event.getHand().name() + " to " + event.getRightClicked().getName());

            //noinspection ConstantConditions
            if (event.getRightClicked() instanceof Creeper && event.getPlayer() != null) {
                Creeper creeper = (Creeper) event.getRightClicked();
                Player interactedPlayer = event.getPlayer();
                UUID interactedSteamUUID = interactedPlayer.getUniqueId();

                MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);
                PersistentDataContainer dataContainer = creeper.getPersistentDataContainer();
                assert plugin != null;
                NamespacedKey tamedByKey = new NamespacedKey(plugin, "tamedBy");
                NamespacedKey selfDestructionKey = new NamespacedKey(plugin, "selfDestruction");
                String tamedByValue = dataContainer.get(tamedByKey, PersistentDataType.STRING);

                boolean isAboutToSelfDestruct = !dataContainer.getOrDefault(selfDestructionKey, PersistentDataType.STRING, "").isEmpty();

                if (tamedByValue == null) {
                    LOGGER.info("Interacted creeper was NOT tamed by anyone");
                    interactedPlayer.sendMessage(Component.text(ChatColor.RED + getMessage(interactedPlayer, "creeper_is_not_tamed_by_anyone")));
                } else if (isAboutToSelfDestruct) {
                    LOGGER.info("Interacted creeper is about to destruct");
                    interactedPlayer.sendMessage(Component.text(ChatColor.RED + getMessage(interactedPlayer, "creeper_is_already_about_to_destruct")));
                } else {
                    LOGGER.info("Interacted creeper was tamed by " + tamedByValue);
                    UUID steamUUID = UUID.fromString(tamedByValue);
                    Player player = Bukkit.getPlayer(steamUUID);

                    if (player == null) {
                        LOGGER.info("Interacted creeper was tamed unknown player");
                        interactedPlayer.sendMessage(Component.text(ChatColor.RED + getMessage(interactedPlayer, "failed_to_find_a_tamed_player_by_uuid")));
                    } else if (creeper.getLocation().distance(player.getLocation()) > 10) {
                        LOGGER.info("Interacted creeper was tamed by a player which is too far");
                        interactedPlayer.sendMessage(Component.text(ChatColor.RED + getMessage(interactedPlayer, "creeper_is_too_far_away_the_player")));
                    } else if (!interactedSteamUUID.toString().equals(steamUUID.toString())) {
                        LOGGER.info("Interacted creeper was tamed by UUID: '" + steamUUID + "', which is not the same as interacting UUID: '" + interactedSteamUUID + "'");
                        interactedPlayer.sendMessage(Component.text(ChatColor.RED + getMessage(interactedPlayer, "interacting_player_is_not_the_same")));
                    } else {
                        LOGGER.info("Interacted creeper was tamed by interacting player, setting explosion timeout!");
                        player.sendMessage(Component.text(ChatColor.RED + getMessage(player, "creeper_is_about_to_explode")));
                        dataContainer.set(selfDestructionKey, PersistentDataType.STRING, "1");
                        Location loc = creeper.getLocation();
                        World world = loc.getWorld();

                        world.setStorm(true);
                        world.setThundering(true);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                LOGGER.info("Explosion!!!");
                                if (creeper.isValid()) { // Check if creeper is still valid
                                    world.createExplosion(
                                            loc,
                                            10f,
                                            true,
                                            true,
                                            player
                                    ); // Explode the creeper
                                    creeper.remove(); // Remove the creeper

                                    world.strikeLightning(loc);

                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            world.setStorm(false);
                                            world.setThundering(false);
                                        }
                                    }.runTaskLater(plugin, 20 * 10);
                                }
                            }
                        }.runTaskLater(plugin, 20 * 10); // 20 ticks per second, 10 seconds delay
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerDropItem(PlayerDropItemEvent event) {
            Player player = event.getPlayer();
            ItemStack item = event.getItemDrop().getItemStack();
            if (item.getType() == Material.CAKE) {
                Location pancakeLoc = event.getItemDrop().getLocation();
                for (Entity entity : pancakeLoc.getWorld().getNearbyEntities(pancakeLoc, 5, 5, 5)) {
                    if (entity instanceof Player && entity != player) {
                        wishHappyBirthday((Player) entity, player);
                    }
                }
            }
        }

        private void tameMob(EntityDeathEvent event) {
            Entity entity = event.getEntity();
            Player player = event.getEntity().getKiller();
            assert player != null;
            UUID steamUUID = player.getUniqueId();

            if (entity.getType() == EntityType.CREEPER) {
                ((Creeper) entity).setPowered(true);
                ((Creeper) entity).setIgnited(false);

                event.setCancelled(true); // Prevent the creeper from dying
            } else if (entity.getType() == EntityType.ZOMBIE) {
                Location initialLocation = event.getEntity().getLocation();
                // initialLocation.getWorld().strikeLightning(initialLocation);
                entity.remove();
                entity = initialLocation.getWorld().spawnEntity(initialLocation, EntityType.GIANT);
                entity.setGravity(true);
                entity.customName(Component.text(ChatColor.RED + "Zombie Giant"));
                entity.setCustomNameVisible(true);
                ((Giant) entity).setAI(true);
                ((Giant) entity).setCollidable(true);
                ((Giant) entity).setAware(true);
                ((Giant) entity).setCanPickupItems(true);
                ((Giant) entity).setRemoveWhenFarAway(false);


                Entity finalEntity = entity;
                MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);
                assert plugin != null;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (finalEntity.isValid() && player.isValid()) {
                            Vector velocity = player.getLocation().subtract(finalEntity.getLocation()).toVector().normalize().multiply(0.1);

                            finalEntity.setVelocity(velocity);
                            // Make the giant face the direction of the player
                            finalEntity.getLocation().setDirection(velocity);

                            Vector direction = finalEntity.getVelocity();
                            double x = direction.getX();
                            double z = direction.getZ();

                            float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
                            float pitch = (float) Math.toDegrees(Math.asin(direction.getY()));

                            finalEntity.setRotation(yaw, pitch);

                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (finalEntity.isValid() && player.isValid()) {
                            // Get the player's and giant's locations
                            Location playerLoc = player.getLocation();
                            Location giantLoc = finalEntity.getLocation();

                            // Calculate the vector from the giant to the player
                            Vector direction = playerLoc.subtract(giantLoc).toVector();

                            // Calculate the angle between the player and the giant
                            double angle = Math.atan2(direction.getZ(), direction.getX());

                            // Calculate the X and Z components of the velocity
                            double x = Math.cos(angle);
                            double z = Math.sin(angle);

                            // Calculate the Y component of the velocity
                            double y = direction.getY() / direction.length();

                            // Create the velocity vector
                            Vector velocity = new Vector(x, y, z);
                            velocity.normalize();

                            // Launch the fireball
                            Fireball fireball = ((Giant) finalEntity).launchProjectile(LargeFireball.class, velocity);
                            fireball.setIsIncendiary(true);
                            fireball.setYield(0);

                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 60L, 60L);
            }

            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 8f, 1f);

            MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);
            PersistentDataContainer dataContainer = entity.getPersistentDataContainer();
            assert plugin != null;
            NamespacedKey tamedByKey = new NamespacedKey(plugin, "tamedBy");
            String tamedByValue = steamUUID.toString();
            dataContainer.set(tamedByKey, PersistentDataType.STRING, tamedByValue);

            LOGGER.info("Setting " + entity.getName() + " as tamed by " + tamedByValue);
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

            LOGGER.info(player.getName() + "Killed " + event.getEntity().getName() + " — incrementing MobKill score");

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
            // player.sendMessage(Component.text(ChatColor.GREEN + getMessage(player, "you_killed_mobs", newScore)));

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

    public void wishHappyBirthday(Player to, Player from) {
        String title = "З днем народження!";
        String subtitle = from.getName() + " дарує вам тортик";
        int fadeIn = 10; // duration of fade in (in ticks)
        int stay = 20 * 10; // duration of stay (in ticks), 3 seconds
        int fadeOut = 20; // duration of fade out (in ticks)

        to.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        to.setHealth(to.getMaxHealth());
        to.setFoodLevel(9999);
        playHappyBirthdaySong(to);
    }

    public void playHappyBirthdaySong(Player player) {
        DelayedNote[] happyBirthdayNotes = {
                new DelayedNote(new Note(0, Note.Tone.D, false), NoteDelay.THREE), // ha
                new DelayedNote(new Note(0, Note.Tone.D, false), NoteDelay.ONE), // ppy
                new DelayedNote(new Note(0, Note.Tone.E, false), NoteDelay.FOUR), // birth
                new DelayedNote(new Note(0, Note.Tone.D, false), NoteDelay.FOUR), // day
                new DelayedNote(new Note(1, Note.Tone.G, false), NoteDelay.FOUR), // to
                new DelayedNote(new Note(1, Note.Tone.F, true), NoteDelay.EIGHT), // you

                new DelayedNote(new Note(0, Note.Tone.D, false), NoteDelay.THREE), // ha
                new DelayedNote(new Note(0, Note.Tone.D, false), NoteDelay.ONE), // ppy
                new DelayedNote(new Note(0, Note.Tone.E, false), NoteDelay.FOUR), // birth
                new DelayedNote(new Note(0, Note.Tone.D, false), NoteDelay.FOUR), // day
                new DelayedNote(new Note(1, Note.Tone.A, false), NoteDelay.FOUR), // to
                new DelayedNote(new Note(1, Note.Tone.G, false), NoteDelay.EIGHT), // you

                new DelayedNote(new Note(0, Note.Tone.D, false), NoteDelay.THREE), // ha
                new DelayedNote(new Note(0, Note.Tone.D, false), NoteDelay.ONE), // ppy
                new DelayedNote(new Note(1, Note.Tone.D, false), NoteDelay.FOUR), // birth
                new DelayedNote(new Note(1, Note.Tone.B, false), NoteDelay.FOUR), // day
                new DelayedNote(new Note(1, Note.Tone.G, false), NoteDelay.FOUR), // dear
                new DelayedNote(new Note(1, Note.Tone.F, true), NoteDelay.FOUR), // pla
                new DelayedNote(new Note(0, Note.Tone.E, false), NoteDelay.FOUR), // yer

                new DelayedNote(new Note(1, Note.Tone.C, false), NoteDelay.THREE), // ha
                new DelayedNote(new Note(1, Note.Tone.C, false), NoteDelay.ONE), // ppy
                new DelayedNote(new Note(1, Note.Tone.B, false), NoteDelay.FOUR), // birth
                new DelayedNote(new Note(1, Note.Tone.G, false), NoteDelay.FOUR), // day
                new DelayedNote(new Note(1, Note.Tone.A, false), NoteDelay.FOUR), // to
                new DelayedNote(new Note(1, Note.Tone.G, false), NoteDelay.FIVE), // you
        };

        playSong(player, happyBirthdayNotes);
    }

    public void givePlayerExperience(Player player) {
        // Get player's current location
        Location loc = player.getLocation();

        for (int i = 0; i < 30; i++) {
            Random random = new Random();

            // Generate random X and Z coordinates within 5 blocks from player
            double x = loc.getX() + random.nextInt(11) - 7;
            double z = loc.getZ() + random.nextInt(11) - 7;

            // Set Y coordinate to player's Y minus 5 blocks
            double y = loc.getY() + 7;

            // Create new location with random X, Y, and Z coordinates
            Location spawnLoc = new Location(loc.getWorld(), x, y, z);

            // Spawn experience orb at the new location
            spawnLoc.getWorld().spawn(spawnLoc, ExperienceOrb.class).setExperience(100);
        }

        player.setLevel(player.getLevel() + 100);
    }

    public void spawnParticles(Player player) {
        player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation(), 100);
    }

    public void playSong(Player player, DelayedNote[] song) {
        int currentTick = 0;
        MobKillsPlugin plugin = (MobKillsPlugin) Bukkit.getPluginManager().getPlugin(MOB_KILLS);

        if (plugin == null) {
            return;
        }

        for (DelayedNote delayedNote : song) {
            final Note note = delayedNote.getNote();
            final NoteDelay delay = delayedNote.getDelay();
            final int useCount = note.getId();

            final float pitch = (float) Math.pow(2.0, (useCount - 12) / 12.0);
            final float volume = 1f;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.RECORDS, volume, pitch);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.RECORDS, volume, pitch);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.RECORDS, volume, pitch);
            }, currentTick);

            currentTick += delay.getTicks() + 1;
        }
    }

    public String getMessage(Player player, String key, Object... placeholders) {
        UUID playerId = player.getUniqueId();
        YamlConfiguration langConfig = playerLangConfigs.get(playerId);

        String message = langConfig.getString(key);
        if (message == null) {
            LOGGER.info("getMessage: message for key " + key + " is null, returning empty string");
            return "";
        }

        // Replace placeholders
        if (placeholders != null && placeholders.length > 0) {
            message = String.format(message, placeholders);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}