package xyz.amudev.leadAnyMob;

import io.papermc.paper.entity.Leashable; // Native Paper interface for 1.21.11
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class EventHandler implements Listener {
    private final LeadAnyMob plugin;
    private final Set<EntityType> leashableInVanilla = new HashSet<>();

    public EventHandler(LeadAnyMob plugin) {
        this.plugin = plugin;
        setupVanillaList();
    }

    private void setupVanillaList() {
        leashableInVanilla.add(EntityType.ALLAY);
        leashableInVanilla.add(EntityType.ARMADILLO);
        leashableInVanilla.add(EntityType.AXOLOTL);
        leashableInVanilla.add(EntityType.BEE);
        leashableInVanilla.add(EntityType.CAMEL);
        leashableInVanilla.add(EntityType.CAT);
        leashableInVanilla.add(EntityType.CHICKEN);
        leashableInVanilla.add(EntityType.COW);
        leashableInVanilla.add(EntityType.DOLPHIN);
        leashableInVanilla.add(EntityType.DONKEY);
        leashableInVanilla.add(EntityType.FOX);
        leashableInVanilla.add(EntityType.FROG);
        leashableInVanilla.add(EntityType.GLOW_SQUID);
        leashableInVanilla.add(EntityType.GOAT);
        leashableInVanilla.add(EntityType.HOGLIN);
        leashableInVanilla.add(EntityType.HORSE);
        leashableInVanilla.add(EntityType.IRON_GOLEM);
        leashableInVanilla.add(EntityType.COPPER_GOLEM);
        leashableInVanilla.add(EntityType.LLAMA);
        leashableInVanilla.add(EntityType.TRADER_LLAMA);
        leashableInVanilla.add(EntityType.MOOSHROOM);
        leashableInVanilla.add(EntityType.MULE);
        leashableInVanilla.add(EntityType.OCELOT);
        leashableInVanilla.add(EntityType.PARROT);
        leashableInVanilla.add(EntityType.PIG);
        leashableInVanilla.add(EntityType.POLAR_BEAR);
        leashableInVanilla.add(EntityType.RABBIT);
        leashableInVanilla.add(EntityType.SHEEP);
        leashableInVanilla.add(EntityType.SKELETON_HORSE);
        leashableInVanilla.add(EntityType.SNIFFER);
        leashableInVanilla.add(EntityType.SNOW_GOLEM);
        leashableInVanilla.add(EntityType.SQUID);
        leashableInVanilla.add(EntityType.STRIDER);
        leashableInVanilla.add(EntityType.WOLF);
        leashableInVanilla.add(EntityType.ZOGLIN);
        leashableInVanilla.add(EntityType.ZOMBIE_HORSE);
        leashableInVanilla.add(EntityType.CAMEL_HUSK);
        leashableInVanilla.add(EntityType.NAUTILUS);
        leashableInVanilla.add(EntityType.ZOMBIE_NAUTILUS);

        // Add all Boat and Chest Boat variants dynamically to avoid missing new wood types
        for (EntityType type : EntityType.values()) {
            if (type.name().contains("BOAT")) {
                leashableInVanilla.add(type);
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        // 1. Permission and Context Checks
        if (!player.hasPermission("leadanymob.use")) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.LEAD) return;

        if (event.getRightClicked() instanceof LivingEntity entity) {
            // 2. Filter for non-vanilla leashable mobs
            if (!leashableInVanilla.contains(entity.getType()) && !entity.isLeashed()) {

                // 3. Consume Lead if not in Creative
                if (player.getGameMode() != GameMode.CREATIVE) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                }

                // 4. Set Leash (delayed by 1 tick to prevent instant-snapping)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    entity.setLeashHolder(player);
                }, 1L);
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onFenceAttach(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        // Use Tag.FENCES to support all wood types including Pale Oak
        if (!Tag.FENCES.isTagged(event.getClickedBlock().getType())) return;

        Player player = event.getPlayer();

        // Search for any entity the player is currently leading
        for (Entity nearby : player.getNearbyEntities(10, 10, 10)) {
            if (nearby instanceof LivingEntity living && living.isLeashed() && player.equals(living.getLeashHolder())) {

                // If it's one of our "custom" leashed mobs, handle the fence transfer manually
                if (!leashableInVanilla.contains(living.getType())) {

                    // Create the LeashHitch (the wooden knot) at the clicked block
                    LeashHitch hitch = nearby.getWorld().spawn(event.getClickedBlock().getLocation(), LeashHitch.class);
                    living.setLeashHolder(hitch);
                    event.setCancelled(true);
                }
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onLeadSnap(EntityUnleashEvent event) {
        Entity entity = event.getEntity();

        // 1. Safety check to make sure the entity is valid
        if (!entity.isValid() || entity.isDead()) return;

        // Using the native 1.21.11 Leashable interface to support both custom mobs and modern vanilla leashable entities
        if (entity instanceof Leashable leashed) {
            Entity holder = leashed.getLeashHolder();
            if (holder == null || !holder.isValid() || holder.isDead()) return;

            // 2. Safety check: if the player is offline, allow the unleash to occur
            if (holder instanceof Player player && !player.isOnline()) return;

            EntityUnleashEvent.UnleashReason reason = event.getReason();
            boolean isGlidingPlayer = holder instanceof Player player && player.isGliding();

            // 3. Allow manual player unleashes (right clicking the mob or fence) unless they are gliding
            if (reason == EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH) {
                if (!isGlidingPlayer) {
                    return; // Let the player unleash it normally
                }
            }

            // 4. Intercept distance limits, firework boosts (often classified under UNKNOWN/DISTANCE),
            // or when the leash holder is in Elytra flight
            if (reason == EntityUnleashEvent.UnleashReason.DISTANCE ||
                    reason == EntityUnleashEvent.UnleashReason.UNKNOWN ||
                    isGlidingPlayer) {

                // Cancel the event so the lead is never actually broken and no item is dropped
                event.setCancelled(true);

                // Instantly teleport the entity closer to the holder to prevent it from lagging behind
                entity.teleport(holder.getLocation().add(0, 1, 0));
            }
        }
    }
}