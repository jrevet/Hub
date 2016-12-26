package net.samagames.hub.gui.profile;

import net.samagames.api.SamaGamesAPI;
import net.samagames.api.stats.IPlayerStats;
import net.samagames.hub.Hub;
import net.samagames.hub.gui.AbstractGui;
import net.samagames.tools.chat.fanciful.FancyMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *                )\._.,--....,'``.
 * .b--.        /;   _.. \   _\  (`._ ,.
 * `=,-,-'~~~   `----(,_..'--(,_..'`-.;.'
 *
 * Created by Jérémy L. (BlueSlime) on 26/12/2016
 */
class GuiStatistics extends AbstractGui
{
    private static final int[] BASE_SLTOS = {10, 11, 12, 13, 14, 15, 16};
    private int lines = 0;
    private int slot = 0;

    GuiStatistics(Hub hub)
    {
        super(hub);
    }

    @Override
    public void display(Player player)
    {
        this.inventory = this.hub.getServer().createInventory(null, 45, "Statistiques");

        this.hub.getServer().getScheduler().runTaskAsynchronously(this.hub, () ->
        {
            this.update(player);
            this.hub.getServer().getScheduler().runTask(this.hub, () -> player.openInventory(this.inventory));
        });
    }

    @Override
    public void update(Player player)
    {
        IPlayerStats playerStats = SamaGamesAPI.get().getStatsManager().getPlayerStats(player.getUniqueId());

        this.setGameStatisticsSlotData("Hub", this.hub.getGameManager().getGameByIdentifier("hub").getIcon(), Arrays.asList(
                Pair.of("Woots reçus", playerStats.getJukeBoxStatistics()::getWoots),
                Pair.of("Woots donnés", playerStats.getJukeBoxStatistics()::getWootsGiven),
                Pair.of("Mehs reçus", playerStats.getJukeBoxStatistics()::getMehs)
        ));

        this.setGameStatisticsSlotData("UHC", this.hub.getGameManager().getGameByIdentifier("uhczone").getIcon(), Arrays.asList(
                Pair.of("Woots reçus", playerStats.getJukeBoxStatistics()::getWoots),
                Pair.of("Woots donnés", playerStats.getJukeBoxStatistics()::getWootsGiven),
                Pair.of("Mehs reçus", playerStats.getJukeBoxStatistics()::getMehs)
        ));

        this.setSlotData(ChatColor.YELLOW + "Voir votre profil en ligne", new ItemStack(Material.NETHER_STAR, 1), this.inventory.getSize() - 4, null, "website");
        this.setSlotData(getBackIcon(), this.inventory.getSize() - 4, "back");
    }

    @Override
    public void onClick(Player player, ItemStack stack, String action, ClickType clickType)
    {
        switch (action)
        {
            case "website":
                new FancyMessage(ChatColor.YELLOW + "Cliquez sur ").then("[Accéder]").color(ChatColor.GOLD).style(ChatColor.BOLD).link("https://www.samagames.net/stats/" + player.getName() + ".html").then(" pour accéder à vos statistiques en ligne.").color(ChatColor.YELLOW).send(player);
                break;

            case "back":
                this.hub.getGuiManager().openGui(player, new GuiProfile(this.hub));
                break;
        }
    }

    private void setGameStatisticsSlotData(String game, ItemStack icon, List<Pair<String, Callable<Integer>>> statistics)
    {
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + game);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Statistique de " + game);
        lore.add("");

        for (Pair<String, Callable<Integer>> statistic : statistics)
        {
            try
            {
                lore.add(ChatColor.GRAY + "- " + statistic.getLeft() + " : " + ChatColor.WHITE + statistic.getRight().call());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Le détail de vos statistiques est");
        lore.add(ChatColor.GRAY + "disponible sur votre profil en ligne.");
        lore.add(ChatColor.GRAY + "Cliquez sur l'étoile pour y accéder.");

        meta.setLore(lore);
        icon.setItemMeta(meta);

        this.setSlotData(icon, (BASE_SLTOS[this.slot] + (this.lines * 9)), "none");

        this.slot++;

        if (this.slot == 7)
        {
            this.slot = 0;
            this.lines++;
        }
    }
}