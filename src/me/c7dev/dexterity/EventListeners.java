package me.c7dev.dexterity;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.api.events.PlayerClickBlockDisplayEvent;
import me.c7dev.dexterity.api.events.TransactionCompletionEvent;
import me.c7dev.dexterity.api.events.TransactionEvent;
import me.c7dev.dexterity.api.events.TransactionRedoEvent;
import me.c7dev.dexterity.api.events.TransactionUndoEvent;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.displays.animation.Animation;
import me.c7dev.dexterity.displays.animation.RideableAnimation;
import me.c7dev.dexterity.transaction.RemoveTransaction;
import me.c7dev.dexterity.util.ClickedBlock;
import me.c7dev.dexterity.util.ClickedBlockDisplay;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexTransformation;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.InteractionCommand;

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
		
		if (e.getPlayer().hasPermission("dexterity.click") || e.getPlayer().hasPermission("dexterity.build")) {	
			
			if (clickDelay(e.getPlayer().getUniqueId())) return;
			boolean right_click = e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK;
			
			//calculate if player clicked a block display
			ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
			ClickedBlockDisplay clicked = (DexUtils.isAllowedMaterial(hand.getType()) || !right_click || hand.getType() == Material.AIR) ? plugin.api().getLookingAt(e.getPlayer()) : null;
			
			boolean clicked_block;
			if (clicked == null) clicked_block = right_click;
			else {
				ClickedBlock clicked_block_data = plugin.api().getPhysicalBlockLookingAtRaw(e.getPlayer(), 0.1, clicked.getDistance());
				clicked_block = clicked_block_data != null && clicked_block_data.getDistance() < clicked.getDistance();
			}
			DexSession session = plugin.getEditSession(e.getPlayer().getUniqueId());
			DexterityDisplay clicked_display = null;
			DexBlock clicked_db = null;
			boolean holding_wand = hand.getType() == Material.WOODEN_AXE || (hand.getType() == Material.BLAZE_ROD && hand.getItemMeta().getDisplayName().equals(plugin.getConfigString("wand-title", "§fDexterity Wand")));

			if (clicked != null) {
				if (clicked.getBlockDisplay().getMetadata("dex-ignore").size() > 0) return;
				
				clicked_db = plugin.getMappedDisplay(clicked.getBlockDisplay().getUniqueId());
				if (clicked_db != null) clicked_display = clicked_db.getDexterityDisplay();
			}

			//normal player or saved display click
			if (clicked_display != null && clicked_display.isSaved() && (!holding_wand || !e.getPlayer().hasPermission("dexterity.build"))) {
				if (clicked == null || clicked_block) return;
				//click a display as normal player or with nothing in hand
				RideableAnimation ride = (RideableAnimation) clicked_display.getAnimation(RideableAnimation.class);
				
				if (ride != null && ride.getMountedPlayer() == null) {
					ride.mount(e.getPlayer());
					Animation anim = (Animation) ride;
					anim.start();
				}
				e.setCancelled(true);

				InteractionCommand[] cmds = clicked_display.getCommands();
				if (cmds.length == 0) {
					if ((e.getPlayer().hasPermission("dexterity.buid") || e.getPlayer().hasPermission("dexterity.command.cmd"))
							&& clicked_display.hasOwner(e.getPlayer())) {
						session.clickMsg();
					}
				} else for (InteractionCommand cmd : cmds) cmd.exec(e.getPlayer(), right_click);

			} else if (e.getPlayer().hasPermission("dexterity.build")) {
				//wand click
				if (holding_wand) {
					e.setCancelled(true);

					//select display with wand
					if (!clicked_block && clicked_display != null && clicked_display.getLabel() != null) {
						session.setSelected(clicked_display, true);
						return;
					}

					boolean msg = hand.getType() != Material.WOODEN_AXE || e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR;
					if (clicked != null && !clicked_block) { //click block with wand (set pos1 or pos2)
						boolean is_l1 = !right_click;
						Vector scale = DexUtils.hadimard(DexUtils.vector(clicked.getBlockDisplay().getTransformation().getScale()), DexUtils.getBlockDimensions(clicked.getBlockDisplay().getBlock()));
						session.setContinuousLocation(clicked.getDisplayCenterLocation(), is_l1, scale, msg);
					} else if (e.getClickedBlock() != null) {
						if (e.getAction() == Action.LEFT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), true, msg); //pos1
						else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), false, msg); //pos2
					}
				} 
				
				//break or place block display
				else {
					if (clicked == null || clicked_block) return;
					e.setCancelled(true);
					
					if (clicked_display != null && !clicked_display.hasOwner(e.getPlayer())) return;
					
					//send event
					PlayerClickBlockDisplayEvent click_event = new PlayerClickBlockDisplayEvent(e.getPlayer(), clicked, e.getAction(), clicked_display);
					Bukkit.getPluginManager().callEvent(click_event);
					if (click_event.isCancelled()) return;

					//place a block display
					if (right_click) {
						if (hand.getType() != Material.AIR) {

							BlockData bdata;
							switch(hand.getType()) {
							case NETHER_STAR:
								bdata = Bukkit.createBlockData(Material.NETHER_PORTAL);
								break;
							case FLINT_AND_STEEL:
								bdata = Bukkit.createBlockData(Material.FIRE);
								break;
							default:
								if (hand.getType() == clicked.getBlockDisplay().getBlock().getMaterial()) bdata = clicked.getBlockDisplay().getBlock();
								else {
									try {
										bdata = Bukkit.createBlockData(hand.getType());
									} catch (Exception ex) {
										return;
									}
								}
							}

							Vector placingDimensions = DexUtils.getBlockDimensions(bdata);

							Vector blockscale = DexUtils.vector(clicked.getBlockDisplay().getTransformation().getScale());
							Vector blockdimensions = DexUtils.getBlockDimensions(clicked.getBlockDisplay().getBlock());

							//calculate dimensions of clicked block display
							Vector avgPlaceDimensions;
							if (clicked.getBlockFace() == BlockFace.DOWN) {
								avgPlaceDimensions = blockdimensions.clone().multiply(0.5).add(placingDimensions).add(new Vector(-0.5, -0.5, -0.5));
							}
							else {
								placingDimensions.setY(1); //account for block's y axis asymmetry
								avgPlaceDimensions = blockdimensions.clone().add(placingDimensions).multiply(0.5);
							}

							Vector offset = DexUtils.hadimard(blockscale, avgPlaceDimensions);

							Vector dir = clicked.getNormal();
							Vector delta = dir.clone().multiply(DexUtils.faceToDirectionAbs(clicked.getBlockFace(), offset));

							DexTransformation trans = (clicked_db == null ? new DexTransformation(clicked.getBlockDisplay().getTransformation()) : clicked_db.getTransformation());

							Location fromLoc = clicked.getDisplayCenterLocation();
							if (clicked.getBlockFace() != BlockFace.UP && clicked.getBlockFace() != BlockFace.DOWN) fromLoc.add(clicked.getUpDir().multiply((blockscale.getY()/2)*(1 - blockdimensions.getY())));


							BlockDisplay b = plugin.spawn(fromLoc.clone().add(delta), BlockDisplay.class, a -> {
								a.setBlock(bdata);
								trans.setScale(blockscale);
								if (clicked.getRollOffset() == null) trans.setDisplacement(blockscale.clone().multiply(-0.5));
								else trans.setDisplacement(blockscale.clone().multiply(-0.5).add(clicked.getRollOffset().getOffset()));
								a.setTransformation(trans.build());
							});

							e.getPlayer().playSound(b.getLocation(), bdata.getSoundGroup().getPlaceSound(), 1f, 1f);

							if (clicked_display != null) {
								DexBlock new_db = new DexBlock(b, clicked_display, clicked_db.getRoll());
								new_db.getTransformation().setDisplacement(new_db.getTransformation().getDisplacement().subtract(clicked_db.getTransformation().getRollOffset()));
								new_db.getTransformation().setRollOffset(clicked_db.getTransformation().getRollOffset().clone());
								clicked_display.addBlock(new_db);
								if (session != null) session.pushBlock(new_db, true);
							}
						}

					} else { //break a block display
						e.getPlayer().playSound(clicked.getBlockDisplay().getLocation(), clicked.getBlockDisplay().getBlock().getSoundGroup().getBreakSound(), 1f, 1f);

						if (clicked_db == null) clicked.getBlockDisplay().remove();
						else {
							if (session != null) session.pushBlock(clicked_db, false);
							clicked_db.remove();
						}
					}
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
		if (!e.getPlayer().isSneaking()) loc = DexUtils.blockLoc(loc); //block location
		else loc.add(-0.5, 0, -0.5); //precise location
		
		loc.add(session.getFollowingOffset());
		
		Location center = session.getSelected().getCenter();
		if (loc.getX() == center.getX() && loc.getY() == center.getY() && loc.getZ() == center.getZ()) return;
		
		double cutoff = 0.01; //follow player
		if (Math.abs(e.getTo().getX() - e.getFrom().getX()) > cutoff || Math.abs(e.getTo().getY() - e.getFrom().getY()) > cutoff || Math.abs(e.getTo().getZ() - e.getFrom().getZ()) > cutoff) {
			session.getSelected().teleport(loc);
		}
	}
	
	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (!e.getPlayer().hasPermission("worldedit.selection.pos") || !e.getPlayer().hasPermission("dexterity.command")) return;
		if (e.getMessage().equalsIgnoreCase("//pos1") || e.getMessage().equalsIgnoreCase("//pos2")) {
			DexSession s = plugin.api().getSession(e.getPlayer());
			if (s != null) {
				s.setLocation(e.getPlayer().getLocation(), e.getMessage().equalsIgnoreCase("//pos1"), false);
			}
		}
	}
	
	@EventHandler
	public void onPhysics(BlockPhysicsEvent e) {
		Location loc = e.getBlock().getLocation();
		for (Entry<UUID, DexSession> entry : plugin.editSessionIter()) {
			DexSession session = entry.getValue();
			if (session.isCancellingPhysics() && loc.getWorld().getName().equals(session.getLocation1().getWorld().getName())) {
				if (loc.getX() >= Math.min(session.getLocation1().getX(), session.getLocation2().getX())
						&& loc.getX() <= Math.max(session.getLocation1().getX(), session.getLocation2().getX())
						&& loc.getY() >= Math.min(session.getLocation1().getY(), session.getLocation2().getY())
						&& loc.getY() <= Math.max(session.getLocation1().getY(), session.getLocation2().getY())
						&& loc.getZ() >= Math.min(session.getLocation1().getZ(), session.getLocation2().getZ())
						&& loc.getZ() <= Math.max(session.getLocation1().getZ(), session.getLocation2().getZ())) {
					e.setCancelled(true);
					return;
				}
			}
		}
	}
	
	private void updateAxes(TransactionEvent e) {
		if (e.getSession().isShowingAxes()) {
			if (e.getTransaction() instanceof RemoveTransaction) e.getSession().setShowingAxes(null);
			else e.getSession().updateAxisDisplays();
		}
	}
	
	@EventHandler
	public void onTransactionPush(TransactionCompletionEvent e) {
		updateAxes(e);
	}
	
	@EventHandler
	public void onTransactionUndo(TransactionUndoEvent e) {
		updateAxes(e);
	}
	
	@EventHandler
	public void onTransactionRedo(TransactionRedoEvent e) {
		updateAxes(e);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		UUID u = e.getPlayer().getUniqueId();
		DexSession session = plugin.getEditSession(u);
		if (session != null) {
			session.cancelEdit();
			new BukkitRunnable() {
				@Override
				public void run() {
					Player p = Bukkit.getPlayer(u);
					if (p == null || !p.isOnline()) plugin.deleteEditSession(u);
				}
			}.runTaskLater(plugin, 600l); //TODO make this configurable
		}
	}
}
