package me.c7dev.tensegrity;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.api.events.PlayerClickBlockDisplayEvent;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.BlockDisplayFace;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexTransformation;
import me.c7dev.tensegrity.util.DexUtils;

public class EventListeners implements Listener {
	
	private Dexterity plugin;
	private HashMap<UUID, Long> click_delay = new HashMap<>();
	
	public EventListeners(Dexterity plugin) {
		this.plugin = plugin;
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
			
			if (clickDelay(e.getPlayer().getUniqueId())) return;
			
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
				if (session == null) session = new DexSession(e.getPlayer(), plugin);
				e.setCancelled(true);
				
				if (clicked_display != null && clicked_display.getLabel() != null) {
					session.setSelected(clicked_display, true);
					return;
				}
				if (clicked != null) {
					if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) session.setLocation(DexUtils.blockLoc(clicked.getDisplayCenterLocation()), true);
					else session.setLocation(DexUtils.blockLoc(clicked.getDisplayCenterLocation()), false);
					if (session.getLocation1() == null || session.getLocation2() == null) plugin.getAPI().tempHighlight(clicked.getBlockDisplay(), 15);
				} else if (e.getClickedBlock() != null) {
					if (e.getAction() == Action.LEFT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), true); //pos1
					else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), false); //pos2
				}
			} else {
				if (clicked == null) return;
				e.setCancelled(true);
				
				PlayerClickBlockDisplayEvent click_event = new PlayerClickBlockDisplayEvent(e.getPlayer(), clicked, e.getAction(), clicked_display);
				Bukkit.getPluginManager().callEvent(click_event);
				if (click_event.isCancelled()) return;
								
				if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
					if (hand.getType() != Material.AIR && hand.getType().isSolid()) {
						BlockData bdata = Bukkit.createBlockData(hand.getType());

						//TODO pick up from here, loc needs to be in center of new block display
						Vector blockscale = DexUtils.vector(clicked.getBlockDisplay().getTransformation().getScale());
						//Vector3f blockscale3f = (session == null || session.getEditingScale() == null) ? DexUtils.vector(blockscale) : session.getEditingScale();
						Vector placingScale = blockscale;
						
						Vector dimensions = DexUtils.hadimard(blockscale, DexUtils.getBlockDimensions(clicked.getBlockDisplay().getBlock()));
						//Vector placedDimensions = DexUtils.hadimard(blockscale, DexUtils.getBlockDimensions(bdata));
						
						Vector dir = clicked.getBlockFace().getDirection();
						//Vector delta = DexUtils.hadimard(dimensions.clone().add(placingScale).multiply(0.5), dir);
						Vector delta = DexUtils.hadimard(dimensions.clone().setY(0.5 * (dimensions.getY() + placingScale.getY())), dir);
						
						DexTransformation trans = (clicked_db == null ? new DexTransformation(clicked.getBlockDisplay().getTransformation()) : clicked_db.getTransformation());

						//if no rotation to consider
						Location fromLoc = clicked.getBlockDisplay().getLocation();
						if (Math.abs(dir.getX()) == 1) fromLoc.setX(clicked.getDisplayCenterLocation().getX());
						if (Math.abs(dir.getY()) == 1) fromLoc.setY(clicked.getDisplayCenterLocation().getY());
						if (Math.abs(dir.getZ()) == 1) fromLoc.setZ(clicked.getDisplayCenterLocation().getZ());
						//Location fromLoc = clicked.getDisplayCenterLocation();
												
						BlockDisplay b = e.getPlayer().getWorld().spawn(fromLoc.add(delta), BlockDisplay.class, a -> {
							a.setBlock(bdata);
							trans.setScale(DexUtils.vector(placingScale));
							trans.setDisplacement(DexUtils.vector(placingScale.clone().multiply(-0.5)));
							a.setTransformation(trans.build());
						});
//						plugin.getAPI().markerPoint(clicked.getDisplayCenterLocation(), Color.RED, 6);
//						plugin.getAPI().markerPoint(clicked.getBlockDisplay().getLocation(), Color.ORANGE, 6);
//						plugin.getAPI().markerPoint(b.getLocation(), Color.LIME, 6);
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
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		DexSession session = plugin.getEditSession(e.getPlayer().getUniqueId());
		if (session == null || !session.isFollowing() || session.getSelected() == null) return;
		if (!session.getSelected().getCenter().getWorld().getName().equals(e.getPlayer().getWorld().getName())) {
			session.cancelEdit();
			session.setSelected(null, false);
			return;
		}
		
		Location loc = e.getPlayer().getLocation();
		if (!e.getPlayer().isSneaking()) loc = DexUtils.blockLoc(loc);
		else loc.add(-0.5, 0, -0.5);
		
		loc.add(session.getFollowingOffset());
		
		Location center = session.getSelected().getCenter();
		if (loc.getX() == center.getX() && loc.getY() == center.getY() && loc.getZ() == center.getZ()) return;
		
		double cutoff = 0.001;
		if (Math.abs(e.getTo().getX() - e.getFrom().getX()) > cutoff || Math.abs(e.getTo().getY() - e.getFrom().getY()) > cutoff || Math.abs(e.getTo().getZ() - e.getFrom().getZ()) > cutoff) {
			session.getSelected().teleport(loc);
		}
	}

}
