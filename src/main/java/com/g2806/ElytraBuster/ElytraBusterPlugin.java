package com.g2806.ElytraBuster;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class ElytraBusterPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<TNTPrimed, UUID> tntOwners = new HashMap<>();
    private static final String SPECIAL_TNT_KEY = "elytrabuster_tnt";
    private static final String LEVEL_KEY = "elytrabuster_level";
    private static final String CROSSBOW_FIREWORK_KEY = "crossbow_firework";
    private final Map<Location, Integer> specialTNTLocations = new HashMap<>();
    private final Map<Location, DispenseData> dispenserTNTLocations = new HashMap<>();
    private final Logger logger = getLogger();
    private NamespacedKey[] recipeKeys;

    private static class DispenseData {
        final int level;
        final long timestamp;

        DispenseData(int level, long timestamp) {
            this.level = level;
            this.timestamp = timestamp;
        }
    }

    @Override
    public void onEnable() {
        // Initialize recipe keys
        recipeKeys = new NamespacedKey[] {
                new NamespacedKey(this, "elytrabuster_tnt_1"),
                new NamespacedKey(this, "elytrabuster_tnt_2"),
                new NamespacedKey(this, "elytrabuster_tnt_3"),
                new NamespacedKey(this, "elytrabuster_tnt_4")
        };

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("elytrabustertnt").setExecutor(this);
        registerCraftingRecipes();
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                Iterator<Map.Entry<Location, DispenseData>> dispenserIterator = dispenserTNTLocations.entrySet().iterator();
                while (dispenserIterator.hasNext()) {
                    Map.Entry<Location, DispenseData> entry = dispenserIterator.next();
                    if (currentTime - entry.getValue().timestamp > 250) {
                        dispenserIterator.remove();
                    }
                }
            }
        }.runTaskTimer(this, 0L, 5L);
        logger.info("ElytraBuster enabled!");
    }

    @Override
    public void onDisable() {
        logger.info("ElytraBuster disabled!");
    }

    private void registerCraftingRecipes() {
        // Level 1 Recipe
        ItemStack level1 = createSpecialTNT(1, 1);
        ShapedRecipe recipe1 = new ShapedRecipe(recipeKeys[0], level1);
        recipe1.shape(" P ", "GTG", " G ");
        recipe1.setIngredient('T', Material.TNT);
        recipe1.setIngredient('P', Material.ENDER_PEARL);
        recipe1.setIngredient('G', Material.GUNPOWDER);
        getServer().addRecipe(recipe1);

        // Level 2 Recipe
        ItemStack level2 = createSpecialTNT(1, 2);
        ShapedRecipe recipe2 = new ShapedRecipe(recipeKeys[1], level2);
        recipe2.shape("APA", "GTG", " G ");
        recipe2.setIngredient('T', Material.TNT);
        recipe2.setIngredient('P', Material.ENDER_PEARL);
        recipe2.setIngredient('G', Material.GUNPOWDER);
        recipe2.setIngredient('A', Material.DIAMOND);
        getServer().addRecipe(recipe2);

        // Level 3 Recipe
        ItemStack level3 = createSpecialTNT(1, 3);
        ShapedRecipe recipe3 = new ShapedRecipe(recipeKeys[2], level3);
        recipe3.shape("EPE", "GTG", " G ");
        recipe3.setIngredient('T', Material.TNT);
        recipe3.setIngredient('P', Material.ENDER_PEARL);
        recipe3.setIngredient('G', Material.GUNPOWDER);
        recipe3.setIngredient('E', Material.EMERALD);
        getServer().addRecipe(recipe3);

        // Level 4 Recipe
        ItemStack level4 = createSpecialTNT(1, 4);
        ShapedRecipe recipe4 = new ShapedRecipe(recipeKeys[3], level4);
        recipe4.shape("NPA", "GTG", "EGE");
        recipe4.setIngredient('T', Material.TNT);
        recipe4.setIngredient('P', Material.ENDER_PEARL);
        recipe4.setIngredient('G', Material.GUNPOWDER);
        recipe4.setIngredient('A', Material.AMETHYST_SHARD);
        recipe4.setIngredient('E', Material.EMERALD);
        recipe4.setIngredient('N', Material.ANCIENT_DEBRIS);
        getServer().addRecipe(recipe4);

        logger.info("Registered ElytraBuster TNT recipes: Level 1, 2, 3, 4");
    }

    private ItemStack createSpecialTNT(int amount, int level) {
        ItemStack specialTNT = new ItemStack(Material.TNT, amount);
        ItemMeta meta = specialTNT.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cElytraBuster TNT (Level " + (level == 4 ? "4 - Explosive" : level) + ")");
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(new NamespacedKey(this, SPECIAL_TNT_KEY), PersistentDataType.STRING, "special");
            data.set(new NamespacedKey(this, LEVEL_KEY), PersistentDataType.INTEGER, level);
            specialTNT.setItemMeta(meta);
        }
        return specialTNT;
    }

    private int getTNTLevel(ItemStack item) {
        if (item == null || item.getType() != Material.TNT || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(new NamespacedKey(this, SPECIAL_TNT_KEY), PersistentDataType.STRING)) {
            Integer level = data.get(new NamespacedKey(this, LEVEL_KEY), PersistentDataType.INTEGER);
            return level != null ? level : 0;
        }
        return 0;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("elytrabuster.tnt")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /elytrabustertnt <level> (1-4)");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
            if (level < 1 || level > 4) {
                player.sendMessage("§cLevel must be between 1 and 4.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cLevel must be a number between 1 and 4.");
            return true;
        }

        player.getInventory().addItem(createSpecialTNT(1, level));
        player.sendMessage("§aYou have received an ElytraBuster TNT (Level " + (level == 4 ? "4 - Explosive" : level) + ")!");
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (NamespacedKey key : recipeKeys) {
            player.discoverRecipe(key);
        }
        logger.info("Recipes discovered for player: " + player.getName());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        int level = getTNTLevel(item);
        if (level > 0) {
            specialTNTLocations.put(event.getBlock().getLocation(), level);
        }
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        int level = getTNTLevel(item);
        if (level > 0) {
            Location dispenserLoc = event.getBlock().getLocation();
            dispenserTNTLocations.put(dispenserLoc, new DispenseData(level, System.currentTimeMillis()));
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Firework firework && event.getEntity().getShooter() instanceof Player player) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (mainHand.getType() == Material.CROSSBOW || offHand.getType() == Material.CROSSBOW) {
                PersistentDataContainer data = firework.getPersistentDataContainer();
                data.set(new NamespacedKey(this, CROSSBOW_FIREWORK_KEY), PersistentDataType.STRING, "crossbow");
                logger.info("Crossbow-launched firework tagged at " + firework.getLocation() + " by " + player.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent) {
            Entity damager = damageEvent.getDamager();
            if (damager instanceof TNTPrimed tnt) {
                PersistentDataContainer data = tnt.getPersistentDataContainer();
                if (data.has(new NamespacedKey(this, SPECIAL_TNT_KEY), PersistentDataType.STRING)) {
                    Integer level = data.get(new NamespacedKey(this, LEVEL_KEY), PersistentDataType.INTEGER);
                    if (level != null && level >= 1 && level <= 4) {
                        event.setDeathMessage(player.getName() + " was intercepted by a Level " +
                                (level == 4 ? "4 (Explosive)" : level) + " ElytraBuster TNT!");
                        logger.info("Custom death message set for " + player.getName() + " (Level " + level + ") caused by TNT at " + tnt.getLocation());
                    }
                }
            }
        }
        logger.info("Player " + player.getName() + " died, damage cause: " +
                (player.getLastDamageCause() != null ? player.getLastDamageCause().getCause() : "unknown"));
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        PersistentDataContainer data = tnt.getPersistentDataContainer();
        Location tntLocation = tnt.getLocation();
        int tempLevel = 0;

        if (specialTNTLocations.containsKey(tntLocation.getBlock().getLocation())) {
            tempLevel = specialTNTLocations.get(tntLocation.getBlock().getLocation());
            specialTNTLocations.remove(tntLocation.getBlock().getLocation());
        } else {
            for (Map.Entry<Location, DispenseData> entry : dispenserTNTLocations.entrySet()) {
                Location dispenserLoc = entry.getKey();
                if (tntLocation.distanceSquared(dispenserLoc) <= 4) {
                    tempLevel = entry.getValue().level;
                    break;
                }
            }
        }

        if (tempLevel == 0) return;

        final int tntLevel = tempLevel;

        data.set(new NamespacedKey(this, SPECIAL_TNT_KEY), PersistentDataType.STRING, "special");
        data.set(new NamespacedKey(this, LEVEL_KEY), PersistentDataType.INTEGER, tntLevel);

        tnt.setFuseTicks(15 * 20);
        tnt.setVelocity(new Vector(0, 2, 0));

        new BukkitRunnable() {
            int ticksLived = 0;
            final int maxTicks = 15 * 20;
            boolean hasAscended = false;
            double initialY;
            Entity target = null;
            Location fireworkLastLocation = null;
            boolean warningSent = false;
            final double speed = tntLevel == 1 ? 0.7 : tntLevel == 2 ? 0.9 : 1.5;
            final double verticalAdjust = tntLevel == 1 ? 0.25 : tntLevel == 2 ? 0.35 : 0.6;
            final float explosionPower = tntLevel == 1 ? 2.0f : tntLevel == 2 ? 3.0f : tntLevel == 3 ? 3.5f : 4.0f;
            final boolean explodeOnContact = tntLevel == 4;

            @Override
            public void run() {
                if (!tnt.isValid() || tnt.isDead()) {
                    tntOwners.remove(tnt);
                    cancel();
                    return;
                }

                Location tntLocation = tnt.getLocation();
                Vector velocity = tnt.getVelocity();

                if (ticksLived == 0) {
                    initialY = tntLocation.getY();
                }

                // Check for block collision before movement
                if (isCollidingWithBlock(tnt, tntLocation, velocity)) {
                    tnt.remove();
                    tnt.getWorld().createExplosion(tntLocation.getX(), tntLocation.getY(), tntLocation.getZ(), explosionPower, false, true, tnt);
                    logger.info("ElytraBuster TNT (Level " + tntLevel + ") collided with block and exploded at " + tntLocation);
                    tntOwners.remove(tnt);
                    cancel();
                    return;
                }

                // Ascent phase
                if (!hasAscended) {
                    if (tntLocation.getY() < initialY + 20) {
                        tnt.setVelocity(new Vector(0, 0.5, 0));
                    } else {
                        hasAscended = true;
                        tnt.setVelocity(new Vector(0, 0, 0));
                    }
                }

                // Floating and chasing phase
                if (hasAscended) {
                    tnt.setGravity(false);
                    tnt.setVelocity(tnt.getVelocity().setY(0));

                    // If chasing a despawned firework's last location
                    if (fireworkLastLocation != null) {
                        if (tntLocation.distanceSquared(fireworkLastLocation) <= 1) {
                            tnt.remove();
                            tnt.getWorld().createExplosion(tntLocation.getX(), tntLocation.getY(), tntLocation.getZ(), explosionPower, false, true, tnt);
                            logger.info("ElytraBuster TNT (Level " + tntLevel + ") exploded at firework's last location " + fireworkLastLocation);
                            tntOwners.remove(tnt);
                            cancel();
                            return;
                        }
                        Vector direction = fireworkLastLocation.toVector().subtract(tntLocation.toVector()).normalize();
                        tnt.setVelocity(direction.multiply(speed).setY(0));
                        if (tntLocation.getY() < fireworkLastLocation.getY() - 1) {
                            tnt.setVelocity(tnt.getVelocity().add(new Vector(0, verticalAdjust, 0)));
                        } else if (tntLocation.getY() > fireworkLastLocation.getY() + 1) {
                            tnt.setVelocity(tnt.getVelocity().add(new Vector(0, -verticalAdjust, 0)));
                        }
                    } else {
                        Firework nearestFirework = findNearestFirework(tnt);
                        if (nearestFirework != null && nearestFirework.isValid()) {
                            target = nearestFirework;
                            fireworkLastLocation = null;
                        } else if (target instanceof Firework && (!target.isValid() || target.isDead())) {
                            if (target != null) {
                                fireworkLastLocation = target.getLocation();
                                logger.info("Firework despawned, TNT targeting last location " + fireworkLastLocation);
                            }
                            target = null;
                        }

                        if (target == null && fireworkLastLocation == null) {
                            target = findNearestElytraPlayer(tnt);
                            if (target != null) {
                                tntOwners.put(tnt, target instanceof Player ? ((Player) target).getUniqueId() : null);
                                warningSent = false;
                            } else {
                                tntOwners.remove(tnt);
                                tnt.setVelocity(new Vector(0, 0, 0));
                            }
                        }

                        if (target != null && target.isValid()) {
                            if (target instanceof Player player && !warningSent) {
                                player.sendTitle("§cA TNT is following you!", tntLevel == 4 ? "§4Explodes on contact!" : "", 10, 70, 20);
                                warningSent = true;
                            }

                            if (explodeOnContact && tntLocation.distanceSquared(target.getLocation()) <= 1) {
                                tnt.remove();
                                tnt.getWorld().createExplosion(tntLocation.getX(), tntLocation.getY(), tntLocation.getZ(), explosionPower, false, true, tnt);
                                logger.info("ElytraBuster TNT (Level " + tntLevel + ") contact explosion at " + tntLocation);
                                tntOwners.remove(tnt);
                                cancel();
                                return;
                            }

                            Location targetLocation = target.getLocation();
                            Vector direction = targetLocation.toVector().subtract(tntLocation.toVector()).normalize();
                            tnt.setVelocity(direction.multiply(speed).setY(0));

                            if (tntLocation.getY() < targetLocation.getY() - 1) {
                                tnt.setVelocity(tnt.getVelocity().add(new Vector(0, verticalAdjust, 0)));
                            } else if (tntLocation.getY() > targetLocation.getY() + 1) {
                                tnt.setVelocity(tnt.getVelocity().add(new Vector(0, -verticalAdjust, 0)));
                            }
                        }
                    }
                }

                // Play musical note sound every 4 ticks
                if (ticksLived % 4 == 0) {
                    float progress = (float) ticksLived / maxTicks;
                    float notePitch = 0.7f + (0.8f * progress);
                    tnt.getWorld().playSound(tntLocation, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, notePitch);
                }

                ticksLived++;
            }

            private boolean isCollidingWithBlock(TNTPrimed tnt, Location location, Vector velocity) {
                Material centerBlock = location.getBlock().getType();
                if (centerBlock.isSolid()) {
                    logger.info("Collision detected with solid block " + centerBlock + " at " + location);
                    return true;
                }

                Vector normalizedVelocity = velocity.clone().normalize();
                RayTraceResult result = tnt.getWorld().rayTraceBlocks(
                        location,
                        normalizedVelocity,
                        velocity.length() + 0.5,
                        org.bukkit.FluidCollisionMode.NEVER,
                        true
                );
                if (result != null && result.getHitBlock() != null && result.getHitBlock().getType().isSolid()) {
                    logger.info("Raycast collision detected with solid block " + result.getHitBlock().getType() + " at " + result.getHitPosition());
                    return true;
                }

                return false;
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private Player findNearestElytraPlayer(TNTPrimed tnt) {
        double closestDistance = Double.MAX_VALUE;
        Player closestPlayer = null;

        for (Entity entity : tnt.getWorld().getNearbyEntities(tnt.getLocation(), 50, 50, 50)) {
            if (entity instanceof Player player && player.isGliding()) {
                double distance = player.getLocation().distanceSquared(tnt.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
        }
        return closestPlayer;
    }

    private Firework findNearestFirework(TNTPrimed tnt) {
        double closestDistance = Double.MAX_VALUE;
        Firework closestFirework = null;
        UUID ownerUUID = tntOwners.get(tnt);

        for (Entity entity : tnt.getWorld().getNearbyEntities(tnt.getLocation(), 50, 50, 50)) {
            if (entity instanceof Firework firework) {
                PersistentDataContainer data = firework.getPersistentDataContainer();
                if (data.has(new NamespacedKey(this, CROSSBOW_FIREWORK_KEY), PersistentDataType.STRING) &&
                        ownerUUID != null && firework.getWorld().getPlayers().stream()
                        .anyMatch(p -> p.getUniqueId().equals(ownerUUID))) {
                    double distance = firework.getLocation().distanceSquared(tnt.getLocation());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestFirework = firework;
                    }
                }
            }
        }
        return closestFirework;
    }
}