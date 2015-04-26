package net.samagames.hub.gui;

import net.samagames.hub.Hub;
import net.samagames.hub.common.managers.AbstractManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager extends AbstractManager
{
    protected ConcurrentHashMap<UUID, AbstractGui> currentGUIs;

    public GuiManager(Hub hub)
    {
        super(hub);
        this.currentGUIs = new ConcurrentHashMap<>();
    }

    public void openGui(Player player, AbstractGui gui)
    {
        if (this.currentGUIs.containsKey(player.getUniqueId()))
            this.closeGui(player);

        this.currentGUIs.put(player.getUniqueId(), gui);
        gui.display(player);
    }

    public void closeGui(Player player)
    {
        player.closeInventory();
        this.removeClosedGui(player);
    }

    public void removeClosedGui(Player player)
    {
        if (this.currentGUIs.containsKey(player.getUniqueId()))
        {
            this.getPlayerGui(player).onClose(player);
            this.currentGUIs.remove(player.getUniqueId());
        }
    }

    public AbstractGui getPlayerGui(HumanEntity player)
    {
        if (this.currentGUIs.containsKey(player.getUniqueId()))
            return this.currentGUIs.get(player.getUniqueId());

        return null;
    }

    public ConcurrentHashMap<UUID, AbstractGui> getPlayersGui()
    {
        return this.currentGUIs;
    }

    @Override
    public String getName() { return "GuiManager"; }
}