package me.c7dev.tensegrity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.BlockDisplayFace;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexTransformation;
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
				
		//DexterityDisplay disp = plugin.getClickedDisplay(e.getPlayer());
		//if (disp != null) Bukkit.getPluginManager().callEvent(new PlayerClickBlockDisplayEvent(e.getPlayer(), disp));
		
		if (e.getPlayer().hasPermission("dexterity.command")) {	
			
			ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
			BlockDisplayFace clicked = plugin.getAPI().getLookingAt(e.getPlayer());
			DexSession session = plugin.getEditSession(e.getPlayer().getUniqueId());
			DexterityDisplay clicked_display = null;
			
			if (clicked != null) {
				clicked_display = plugin.getMappedDisplay(clicked.getBlockDisplay().getUniqueId()).getDexterityDisplay();
			}
			
			if (hand.getType() == Material.BLAZE_ROD && hand.getItemMeta().getDisplayName().equals(plugin.getConfigString("wand-title", "§fDexterity Wand"))) {
				if (session == null) session = new DexSession(e.getPlayer(), plugin);
				e.setCancelled(true);
				
				if (clicked_display != null) {
					session.setSelected(clicked_display);
					return;
				}
				
				if (e.getAction() == Action.LEFT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), true); //pos1
				else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), false); //pos2
			} else if (hand.getType() != Material.AIR && hand.getType().isSolid()) {
				if (clicked_display == null || session == null) return;
				e.setCancelled(true);
				
				final DexSession sessionf = session;
				DexBlock db = plugin.getMappedDisplay(clicked.getBlockDisplay().getUniqueId());
				if (db == null) {
					Bukkit.broadcastMessage("db null");
					return; //clicked display not null
				}
				
				if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
					BlockData bdata = Bukkit.createBlockData(hand.getType());
					
					double scalar = Math.abs(DexUtils.vector(clicked.getBlockDisplay().getTransformation().getScale()).dot(clicked.getBlockFace().getDirection()));
					Vector delta = clicked.getBlockFace().getDirection().multiply(scalar);
					
					BlockDisplay b = e.getPlayer().getWorld().spawn(clicked.getDisplayCenterLocation().add(delta), BlockDisplay.class, a -> {
						a.setBlock(bdata);
						DexTransformation trans = db.getTransformation();
						if (sessionf.getEditingScale() != null) trans.setScale(sessionf.getEditingScale());
						a.setTransformation(trans.build());
					});
					clicked_display.getBlocks().add(new DexBlock(b, clicked_display));
					e.getPlayer().playSound(e.getPlayer().getLocation(), bdata.getSoundGroup().getPlaceSound(), 1f, 1f);
				} else {
					e.getPlayer().playSound(e.getPlayer().getLocation(), db.getEntity().getBlock().getSoundGroup().getBreakSound(), 1f, 1f);
					db.remove();
				}
			}
		}
	}

}
