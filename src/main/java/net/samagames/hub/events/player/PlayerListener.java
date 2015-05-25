package net.samagames.hub.events.player;

import net.samagames.api.SamaGamesAPI;
import net.samagames.hub.Hub;
import net.samagames.hub.cosmetics.jukebox.JukeboxPlaylist;
import net.samagames.hub.games.AbstractGame;
import net.samagames.hub.games.sign.GameSign;
import net.samagames.hub.gui.profile.GuiClickMe;
import net.samagames.tools.InventoryUtils;
import net.samagames.tools.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.Date;
import java.util.UUID;

public class PlayerListener implements Listener
{
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if(event.getPlayer().getLocation().subtract(0.0D, 1.0D, 0.0D).getBlock().getType() == Material.SLIME_BLOCK)
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100, 10));
    }

    @EventHandler
    public void onFallIntoVoid(EntityDamageEvent event)
    {
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID)
            if (event.getEntity() instanceof Player)
                event.getEntity().teleport(Hub.getInstance().getPlayerManager().getLobbySpawn());
    }

    @EventHandler
    public void onClick(InventoryInteractEvent event)
    {
        if (!event.getWhoClicked().isOp())
            event.setCancelled(true);
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncPlayerChatEvent event)
    {
        if (!Hub.getInstance().getChatManager().canChat())
        {
            if (!SamaGamesAPI.get().getPermissionsManager().hasPermission(event.getPlayer(), "hub.bypassmute"))
            {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Le chat est désactivé.");
            }
            else
            {
                event.getPlayer().sendMessage(ChatColor.GOLD + "Attention : chat désactivé");
            }
        }
        else if (Hub.getInstance().getChatManager().getActualSlowDuration() > 0 && !SamaGamesAPI.get().getPermissionsManager().hasPermission(event.getPlayer(), "hub.bypassmute"))
        {
            if (!Hub.getInstance().getChatManager().hasPlayerTalked(event.getPlayer()))
            {
                Hub.getInstance().getChatManager().actualizePlayerLastMessage(event.getPlayer());
            }
            else
            {
                Date lastMessage = Hub.getInstance().getChatManager().getLastPlayerMessageDate(event.getPlayer());
                Date actualMessage = new Date(lastMessage.getTime() + (Hub.getInstance().getChatManager().getActualSlowDuration() * 1000));
                Date current = new Date();

                if (actualMessage.after(current))
                {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "Le chat est actuellement ralenti.");

                    double whenNext = Math.floor((actualMessage.getTime() - current.getTime()) / 1000);
                    event.getPlayer().sendMessage(ChatColor.GOLD + "Prochain message autorisé dans : " + (int) whenNext + " secondes");
                }
                else
                {
                    Hub.getInstance().getChatManager().actualizePlayerLastMessage(event.getPlayer());
                }
            }
        }
        else if (StringUtils.containsIgnoreCase(event.getMessage(), "Minechat") || StringUtils.containsIgnoreCase(event.getMessage(), "minecraft connect"))
        {
            event.getPlayer().sendMessage(ChatColor.GOLD + "La publicité d'application de chat Minecraft est censurée.");
            return;
        }

        if (!event.isCancelled())
        {
            JukeboxPlaylist current = Hub.getInstance().getCosmeticManager().getJukeboxManager().getCurrentSong();

            if (current != null && current.getPlayedBy().equals(event.getPlayer().getName()))
                event.setFormat(ChatColor.DARK_AQUA + "[" + ChatColor.AQUA + "DJ" + ChatColor.DARK_AQUA + "]" + event.getFormat());
        }

        if(!Hub.getInstance().getNPCManager().canTalk(event.getPlayer()))
            event.setCancelled(true);

        if(!event.isCancelled())
            for (Player player : Bukkit.getOnlinePlayers())
                if (!Hub.getInstance().getNPCManager().canTalk(player) || Hub.getInstance().getChatManager().hasChatDisabled(player))
                   event.getRecipients().remove(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        this.onPlayerLeave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event)
    {
        this.onPlayerLeave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();

        player.setGameMode(GameMode.ADVENTURE);
        player.setWalkSpeed(0.3F);
        player.setFlySpeed(0.2F);
        InventoryUtils.cleanPlayer(player);
        Hub.getInstance().getPlayerManager().getStaticInventory().setInventoryToPlayer(player);

        Bukkit.getScheduler().runTaskAsynchronously(Hub.getInstance(), () ->
        {
            Hub.getInstance().getCosmeticManager().handleLogin(player);
            Hub.getInstance().getPlayerManager().handleLogin(player);
            Hub.getInstance().getScoreboardManager().addScoreboardReceiver(player);
            Hub.getInstance().getHologramManager().addReceiver(player);

            player.teleport(new Location(Bukkit.getWorlds().get(0), -19, 51, 89));

            if (SamaGamesAPI.get().getPermissionsManager().hasPermission(player, "hub.fly"))
                Bukkit.getScheduler().runTask(Hub.getInstance(), () -> player.setAllowFlight(true));

            if (SamaGamesAPI.get().getPermissionsManager().hasPermission(player, "hub.announce"))
                Bukkit.broadcastMessage(PlayerUtils.getFullyFormattedPlayerName(player) + ChatColor.YELLOW + " a rejoint le hub !");
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(final PlayerTeleportEvent event)
    {
        final Player player = event.getPlayer();

        if (player.getVehicle() != null)
        {
            /**
             * TODO: Create this event for the pets (Depends of CosmeticsManager)
             *
            if (Hub.getInstance().getCosmeticsManager().getPetsHandler().hadPet(player))
            {
                Hub.getInstance().getCosmeticsManager().getPetsHandler().removePet(player);
                Bukkit.getScheduler().runTaskAsynchronously(Hub.getInstance(), () -> SamaGamesAPI.get().getPlayerManager().getPlayerData(player.getUniqueId()).remove("selectedpet"));
            }
            else
            {
                ((CraftEntity) vehicle).getHandle().getWorld().removeEntity(((CraftEntity) vehicle).getHandle());
            }
            **/
        }
    }


    @EventHandler
    public void onEntityDimount(EntityDismountEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            final Player player = (Player) event.getEntity();

            /**
             * TODO: Create this event for the pets (Depends of CosmeticsManager)
             *
            if (Hub.getInstance().getCosmeticsManager().getPetsHandler().hadPet(player))
            {
                Hub.getInstance().getCosmeticsManager().getPetsHandler().removePet(player);
                Bukkit.getScheduler().runTaskAsynchronously(Hub.getInstance(), () -> SamaGamesAPI.get().getPlayerManager().getPlayerData(player.getUniqueId()).remove("selectedpet"));
            }
            **/
        }
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event)
    {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        if (item == null)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(Hub.getInstance(), () ->
        {
            Hub.getInstance().getPlayerManager().getStaticInventory().doInteraction(player, item);
        });
    }

    @EventHandler
    public void onPlayerDamaged(final EntityDamageByEntityEvent event)
    {
        event.setCancelled(true);

        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player)
        {
            final Player player = (Player) event.getDamager();

            Bukkit.getScheduler().runTaskAsynchronously(Hub.getInstance(), () ->
            {
                if(SamaGamesAPI.get().getSettingsManager().isEnabled(player.getUniqueId(), "clickme-punch", true))
                {
                    Player target = (Player) event.getEntity();

                    if (!SamaGamesAPI.get().getSettingsManager().isEnabled(target.getUniqueId(), "clickme", false))
                        return;

                    Hub.getInstance().getGuiManager().openGui(player, new GuiClickMe(target));
                }
            });

        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) { event.setCancelled(true); }

    @EventHandler
    public void onPlayerInteractEntityEvent(final PlayerInteractEntityEvent event)
    {
        if(event.getRightClicked().getType() == EntityType.VILLAGER)
        {
            if(event.getRightClicked().hasMetadata("npc-id"))
            {
                event.setCancelled(true);

                Bukkit.getScheduler().runTaskAsynchronously(Hub.getInstance(), () ->
                {
                    if(Hub.getInstance().getNPCManager().hasNPC(UUID.fromString(event.getRightClicked().getMetadata("npc-id").get(0).asString())))
                        if(Hub.getInstance().getNPCManager().canTalk(event.getPlayer()))
                            Hub.getInstance().getNPCManager().getNPCByID(UUID.fromString(event.getRightClicked().getMetadata("npc-id").get(0).asString())).getAction().execute(event.getPlayer());
                });
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(final PlayerInteractEvent event)
    {
        if(event.getClickedBlock() != null)
        {
            Material material = event.getClickedBlock().getType();

            if (material == Material.SIGN || material == Material.SIGN_POST || material == Material.WALL_SIGN)
            {
                Sign sign = (Sign) event.getClickedBlock().getState();

                if (sign.hasMetadata("game") && sign.hasMetadata("map"))
                {
                    AbstractGame game = Hub.getInstance().getGameManager().getGameByIdentifier(sign.getMetadata("game").get(0).asString());
                    GameSign gameSign = game.getGameSignByMap(sign.getMetadata("map").get(0).asString());

                    if(SamaGamesAPI.get().getPermissionsManager().hasPermission(event.getPlayer(), "hub.debug.sign"))
                    {
                        if(event.getPlayer().isSneaking())
                        {
                            gameSign.developperClick(event.getPlayer());
                            return;
                        }
                    }

                    gameSign.click(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerGameModeChangeEvent(PlayerGameModeChangeEvent event)
    {
        if (SamaGamesAPI.get().getPermissionsManager().hasPermission(event.getPlayer(), "hub.fly"))
            Bukkit.getScheduler().runTask(Hub.getInstance(), () -> event.getPlayer().setAllowFlight(true));
    }

    private void onPlayerLeave(final Player player)
    {
        Bukkit.getScheduler().runTaskAsynchronously(Hub.getInstance(), () ->
        {
            Hub.getInstance().getCosmeticManager().handleLogout(player);
            Hub.getInstance().getPlayerManager().handleLogout(player);
            Hub.getInstance().getChatManager().enableChatFor(player);
            Hub.getInstance().getNPCManager().talkFinished(player);
            Hub.getInstance().getScoreboardManager().removeScoreboardReceiver(player);
            Hub.getInstance().getHologramManager().removeReceiver(player);
        });
    }
}
