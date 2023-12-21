package me.c7dev.tensegrity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import me.c7dev.tensegrity.api.events.PlayerClickBlockDisplayEvent;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexUtils;
import net.md_5.bungee.api.ChatColor;

public class EventListeners implements Listener {
	
	ChatColor cc;
	Dexterity plugin;
	
	public EventListeners(Dexterity plugin) {
		this.plugin = plugin;
		cc = plugin.getChatColor();
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
		
	@EventHandler
	public void onBlockClick(PlayerInteractEvent e) {
		
		BlockDisplay clicked = DexUtils.getClickedBlockDisplay(e.getPlayer());
		DexterityDisplay disp = null;
		if (clicked != null) {
			if (plugin.getBlockUUIDMap().containsKey(clicked.getUniqueId())) {
				disp = plugin.getDisplay(plugin.getBlockUUIDMap().get(clicked.getUniqueId()));
			}
			Bukkit.getPluginManager().callEvent(new PlayerClickBlockDisplayEvent(e.getPlayer(), clicked, disp));
		}
		
		ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
		if (e.getPlayer().hasPermission("dexterity.command")) {			
			
			
			if (hand.getType() == Material.BLAZE_ROD && hand.getItemMeta().getDisplayName().equals(plugin.getConfigString("wand-title", "§fDexterity Wand"))) {
				DexSession session = plugin.getEditSession(e.getPlayer().getUniqueId());
				if (session == null) session = new DexSession(e.getPlayer(), plugin);
				e.setCancelled(true);

				if (clicked != null) {
					if (disp != null) {
						session.setSelected(disp);
						Location center = disp.getCenter();
						String label = disp.getLabel() == null ? "at " + plugin.getChatColor2() + DexUtils.locationString(center, 0) : plugin.getChatColor2() + disp.getLabel();
						e.getPlayer().sendMessage(cc + "Selected the display " + label);
					}
					return;
				}
				
				if (e.getAction() == Action.LEFT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), false, 0); //pos1
				else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), false, 1); //pos2
			}
		}
	}

}
