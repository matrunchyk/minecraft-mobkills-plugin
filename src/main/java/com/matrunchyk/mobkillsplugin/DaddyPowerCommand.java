package com.matrunchyk.mobkillsplugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DaddyPowerCommand implements CommandExecutor {
    private static final int SKELETONS_REQUIRED = 10;

    private final MobKillsPlugin plugin;

    public DaddyPowerCommand(MobKillsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (!player.hasPermission("mobkills.daddypower")) {
                player.sendMessage(Component.text(ChatColor.RED + "You do not have permission to use this command."));
                return true;
            }

            if (plugin.getSkeletonCount(player) < SKELETONS_REQUIRED) {
                player.sendMessage(Component.text(ChatColor.RED + "Ви повинні перемогти ще " + (SKELETONS_REQUIRED - plugin.getSkeletonCount(player)) + " скететів, щоб активувати Daddy Power!"));
                return true;
            }

            plugin.resetSkeletonCount(player);

            // Remove all items and armor from player
            clearInventory(player);

            // Add items to player's inventory
            addEnchantedItems(player);

            // Kill monsters 20 blocks around
            killMonsters(player);

            // Give experience
            plugin.givePlayerExperience(player);

            // Apply player effects
            applyPlayerEffects(player);

            // Play sound
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS, 1, 1);
            // player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1, 1);
            // player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.BLOCKS, 1, 1);
            // player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1, 1);

            // Spawn particles
            plugin.spawnParticles(player);

            // Allow player to fly
            allowFlying(player);

            // Congratulate the player
            showNotification(player);

            return true;
        }

        sender.sendMessage(Component.text(ChatColor.RED + "This command can only be executed by a player."));
        return true;
    }

    private void allowFlying(Player player) {
        // Allow the player to fly
        player.setAllowFlight(true);
        int flightDuration = 15; // Minutes

        // Create a BukkitRunnable that warns the player about fly mode expiration after ${flightDuration - 1} minutes
        BukkitRunnable warnDisableFlightTask = new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 1, 1);

                if (player.isFlying()) {
                    player.sendMessage(ChatColor.YELLOW + "Ви скоро впадете!");
                }
            }
        };

        // Create a BukkitRunnable that disables flight after ${flightDuration} minutes
        BukkitRunnable disableFlightTask = new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1, 1);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(ChatColor.RED + "Ви більше не можете літати");
            }
        };

        // Schedule the BukkitRunnable to run in flightDuration minutes (20 ticks per second)
        warnDisableFlightTask.runTaskLater(plugin, 20 * 60 * (flightDuration - 1));
        disableFlightTask.runTaskLater(plugin, 20 * 60 * flightDuration);
    }

    private static void showNotification(Player player) {
        // player.sendMessage(Component.text(ChatColor.GREEN + "Ви отримали Daddy Power!"));
        String title = "Ви отримали Daddy Power!";
        String subtitle = "Тепер у вас нова зброя, броня, ефекти та ви можете літати!";
        int fadeIn = 10; // duration of fade in (in ticks)
        int stay = 20 * 15; // duration of stay (in ticks), 3 seconds
        int fadeOut = 20; // duration of fade out (in ticks)

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    private void applyPlayerEffects(Player player) {
        // Speed I effect for 1 hour (ticks * seconds * minutes)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60 * 60 * 20, 0, false, false));

        // Haste II effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 60 * 60 * 20, 1, false, false));

        // Strength II effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 60 * 60 * 20, 1, false, false));

        // Instant Health II effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 60 * 60 * 20, 1, false, false));

        // Jump Boost II effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 60 * 60 * 20, 0, false, false));

        // Regeneration II effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60 * 60 * 20, 1, false, false));

        // Resistance III effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60 * 60 * 20, 2, false, false));

        // Fire Resistance IV effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60 * 60 * 20, 3, false, false));

        // Water Breathing III effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60 * 60 * 20, 2, false, false));

        // Night Vision II effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60 * 60 * 20, 1, false, false));

        // Health Boost II effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 60 * 60 * 20, 1, false, false));

        // Slow Falling II effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60 * 60 * 20, 1, false, false));

        // Hero of the Village X effect for 1 hour
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 60 * 60 * 20, 9, false, false));
    }

    private void killMonsters(Player player) {
        List<EntityType> mobsToKill = Arrays.asList(
                EntityType.BLAZE,
                EntityType.CREEPER,
                EntityType.ENDERMITE,
                EntityType.EVOKER,
                EntityType.GHAST,
                EntityType.HOGLIN,
                EntityType.HUSK,
                EntityType.ILLUSIONER,
                EntityType.MAGMA_CUBE,
                EntityType.PHANTOM,
                EntityType.PIGLIN_BRUTE,
                EntityType.PILLAGER,
                EntityType.RAVAGER,
                EntityType.SILVERFISH,
                EntityType.SKELETON,
                EntityType.SLIME,
                EntityType.VEX,
                EntityType.VINDICATOR,
                EntityType.WITCH,
                EntityType.WITHER,
                EntityType.WITHER_SKELETON,
                EntityType.ZOGLIN,
                EntityType.ZOMBIE
        );

        // Loop through all entities within 15 blocks of the player
        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (mobsToKill.contains(entity.getType())) {
                ((Damageable) entity).damage(9999, player);
                entity.setLastDamageCause(new EntityDamageByEntityEvent(player, entity, EntityDamageEvent.DamageCause.MAGIC, 1000));
                // Add any additional effects you want here
                // entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_DEATH, 1, 1);
                // Set the maximum loot drop for the entity
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).setMaximumNoDamageTicks(0);
                }
            }
        }
    }

    public static void clearInventory(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        // Regular inventory
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item != null) {
                Material type = item.getType();
                if (type == Material.DIAMOND_SWORD || type == Material.GOLDEN_SWORD || type == Material.IRON_SWORD || type == Material.STONE_SWORD || type == Material.WOODEN_SWORD ||
                        type == Material.DIAMOND_PICKAXE || type == Material.GOLDEN_PICKAXE || type == Material.IRON_PICKAXE || type == Material.STONE_PICKAXE || type == Material.WOODEN_PICKAXE ||
                        type == Material.DIAMOND_AXE || type == Material.GOLDEN_AXE || type == Material.IRON_AXE || type == Material.STONE_AXE || type == Material.WOODEN_AXE ||
                        type == Material.DIAMOND_SHOVEL || type == Material.GOLDEN_SHOVEL || type == Material.IRON_SHOVEL || type == Material.STONE_SHOVEL || type == Material.WOODEN_SHOVEL ||
                        type == Material.DIAMOND_BOOTS || type == Material.GOLDEN_BOOTS || type == Material.IRON_BOOTS || type == Material.LEATHER_BOOTS || type == Material.CHAINMAIL_BOOTS ||
                        type == Material.DIAMOND_LEGGINGS || type == Material.GOLDEN_LEGGINGS || type == Material.IRON_LEGGINGS || type == Material.LEATHER_LEGGINGS || type == Material.CHAINMAIL_LEGGINGS ||
                        type == Material.DIAMOND_CHESTPLATE || type == Material.GOLDEN_CHESTPLATE || type == Material.IRON_CHESTPLATE || type == Material.LEATHER_CHESTPLATE || type == Material.CHAINMAIL_CHESTPLATE ||
                        type == Material.DIAMOND_HELMET || type == Material.GOLDEN_HELMET || type == Material.IRON_HELMET || type == Material.LEATHER_HELMET || type == Material.CHAINMAIL_HELMET ||
                        type == Material.BOW) {
                    inventory.setItem(i, null);
                }
            }
        }

        // Remove all items from armor slots
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);

        player.updateInventory();

        inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                int freeSlot = inventory.firstEmpty();
                if (freeSlot != -1) {
                    inventory.setItem(freeSlot, item);
                    inventory.setItem(i, null);
                } else {
                    inventory.setItem(i, null);
                }
            }
        }
    }

    public ItemStack createEnchantedItem(Material material, String name, Object... enchantments) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        for (int i = 0; i < enchantments.length; i += 2) {
            Enchantment enchantment = (Enchantment) enchantments[i];
            int level = (int) enchantments[i + 1];
            meta.addEnchant(enchantment, level, true);
        }
        item.setItemMeta(meta);
        return item;
    }


    private ItemStack[] createDaddyPowerWeapon(Player player) {
        String name = player.getName() + " Супер ";
        return new ItemStack[]{
                createEnchantedItem(Material.NETHERITE_SWORD, name + "Меч",
                        Enchantment.FIRE_ASPECT, 2,
                        Enchantment.KNOCKBACK, 2,
                        Enchantment.LOOT_BONUS_BLOCKS, 3,
                        Enchantment.SWEEPING_EDGE, 3,
                        Enchantment.DAMAGE_ARTHROPODS, 5,
                        Enchantment.MENDING, 1,
                        Enchantment.DURABILITY, 3),
                createEnchantedItem(Material.NETHERITE_PICKAXE, name + "Кирка",
                        Enchantment.DIG_SPEED, 5,
                        Enchantment.LOOT_BONUS_BLOCKS, 3,
                        Enchantment.SILK_TOUCH, 1,
                        Enchantment.MENDING, 1,
                        Enchantment.DURABILITY, 3),
                createEnchantedItem(Material.NETHERITE_AXE, name + "Сокира",
                        Enchantment.DIG_SPEED, 5,
                        Enchantment.SILK_TOUCH, 1,
                        Enchantment.MENDING, 1,
                        Enchantment.DAMAGE_ALL, 5,
                        Enchantment.DURABILITY, 3),
                createEnchantedItem(Material.NETHERITE_SHOVEL, name + "Лопата",
                        Enchantment.DIG_SPEED, 5,
                        Enchantment.SILK_TOUCH, 1,
                        Enchantment.MENDING, 1,
                        Enchantment.DURABILITY, 3),
                createEnchantedItem(Material.BOW, name + "Лук",
                        Enchantment.ARROW_FIRE, 1,
                        Enchantment.MENDING, 1,
                        Enchantment.ARROW_DAMAGE, 5,
                        Enchantment.ARROW_KNOCKBACK, 2,
                        Enchantment.DURABILITY, 3),
        };
    }

    private ItemStack[] createDaddyPowerArmor(Player player) {
        String name = player.getName() + " Супер ";
        return new ItemStack[]{
                createEnchantedItem(Material.NETHERITE_HELMET, name + "Шолом",
                        Enchantment.WATER_WORKER, 1,
                        Enchantment.PROTECTION_ENVIRONMENTAL, 4,
                        Enchantment.OXYGEN, 3,
                        Enchantment.MENDING, 1,
                        Enchantment.THORNS, 3,
                        Enchantment.DURABILITY, 3),
                createEnchantedItem(Material.NETHERITE_LEGGINGS, name + "Легінси",
                        Enchantment.PROTECTION_ENVIRONMENTAL, 4,
                        Enchantment.MENDING, 1,
                        Enchantment.THORNS, 3,
                        Enchantment.DURABILITY, 3),
                createEnchantedItem(Material.NETHERITE_CHESTPLATE, name + "Нагрудник",
                        Enchantment.PROTECTION_ENVIRONMENTAL, 4,
                        Enchantment.MENDING, 1,
                        Enchantment.THORNS, 3,
                        Enchantment.DURABILITY, 3),
                createEnchantedItem(Material.NETHERITE_BOOTS, name + "Кросівки",
                        Enchantment.FROST_WALKER, 2,
                        Enchantment.PROTECTION_FALL, 4,
                        Enchantment.MENDING, 1,
                        Enchantment.THORNS, 3,
                        Enchantment.DURABILITY, 3),
        };
    }

    private void addEnchantedItems(Player player) {
        Inventory inventory = player.getInventory();
        EntityEquipment equipment = player.getEquipment();
        ItemStack[] weapon = createDaddyPowerWeapon(player);
        ItemStack[] armor = createDaddyPowerArmor(player);

        int hotbarStartIndex = 0;
        int hotbarSize = 9;

        // Add enchanted hotbar items
        for (int i = hotbarStartIndex; i < hotbarStartIndex + hotbarSize; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null) {
                inventory.addItem(itemStack);
            }
            inventory.setItem(i, i - hotbarStartIndex < weapon.length ? weapon[i - hotbarStartIndex] : null);
        }

        equipment.setHelmet(armor[0]);
        equipment.setChestplate(armor[1]);
        equipment.setLeggings(armor[2]);
        equipment.setBoots(armor[3]);
    }
}
