package me.c7dev.tensegrity;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.BlockDisplayFace;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexTransformation;
import me.c7dev.tensegrity.util.DexUtils;
import net.md_5.bungee.api.ChatColor;

public class EventListeners implements Listener {
	
	private ChatColor cc;
	private Dexterity plugin;
	private HashMap<UUID, Long> click_delay = new HashMap<>();
	
	public EventListeners(Dexterity plugin) {
		this.plugin = plugin;
		cc = plugin.getChatColor();
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public boolean clickDelay(UUID u) {
		int delay = 100;
		if (System.currentTimeMillis() - click_delay.getOrDefault(u, 0l) < delay) return true;
		final long newdelay = System.currentTimeMillis() + delay;
		click_delay.put(u, newdelay);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (click_delay.getOrDefault(u, 0l) == newdelay) click_delay.remove(u);
			}
		}.runTaskLater(plugin, (int) (delay*0.02));
		return false;
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
			DexBlock clicked_db = null;
			
			if (clicked != null) {
				clicked_db = plugin.getMappedDisplay(clicked.getBlockDisplay().getUniqueId());
				if (clicked_db != null) clicked_display = clicked_db.getDexterityDisplay();
			}
						
			if (hand.getType() == Material.BLAZE_ROD && hand.getItemMeta().getDisplayName().equals(plugin.getConfigString("wand-title", "§fDexterity Wand"))) {
				if (clickDelay(e.getPlayer().getUniqueId())) return;
				if (session == null) session = new DexSession(e.getPlayer(), plugin);
				e.setCancelled(true);
				
				if (clicked_display != null) {
					session.setSelected(clicked_display, true);
					return;
				}
								
				if (e.getAction() == Action.LEFT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), true); //pos1
				else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), false); //pos2
			} else {
				if (clicked == null) return;
				e.setCancelled(true);
								
				if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
					if (hand.getType() != Material.AIR && hand.getType().isSolid()) {
						BlockData bdata = Bukkit.createBlockData(hand.getType());

						//TODO pick up from here, loc needs to be in center of new block display
						Vector blockscale = DexUtils.vector(clicked.getBlockDisplay().getTransformation().getScale());
						Vector scale = DexUtils.hadimard(blockscale, DexUtils.getBlockSize(clicked.getBlockDisplay().getBlock()));
						Vector3f scale3f = (session == null || session.getEditingScale() == null) ? DexUtils.vector(blockscale) : session.getEditingScale();

						double scalar = Math.abs(scale.dot(clicked.getBlockFace().getDirection()));
						Vector delta = clicked.getBlockFace().getDirection().multiply(scalar);
						DexTransformation trans = (clicked_db == null ? new DexTransformation(clicked.getBlockDisplay().getTransformation()) : clicked_db.getTransformation());

						BlockDisplay b = e.getPlayer().getWorld().spawn(clicked.getDisplayCenterLocation().add(delta), BlockDisplay.class, a -> {
							a.setBlock(bdata);
							trans.setScale(scale3f);
							trans.setDisplacement(DexUtils.vector(scale.clone().multiply(-0.5)));
							a.setTransformation(trans.build());
						});
						e.getPlayer().playSound(b.getLocation(), bdata.getSoundGroup().getPlaceSound(), 1f, 1f);

						if (clicked_display != null) clicked_display.getBlocks().add(new DexBlock(b, clicked_display));
					}
				} else {
					e.getPlayer().playSound(clicked.getBlockDisplay().getLocation(), clicked.getBlockDisplay().getBlock().getSoundGroup().getBreakSound(), 1f, 1f);
					if (clicked_db == null) clicked.getBlockDisplay().remove();
					else clicked_db.remove();
				}
			}
		}
	}

}
