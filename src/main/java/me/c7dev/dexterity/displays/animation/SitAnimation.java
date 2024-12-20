package me.c7dev.dexterity.displays.animation;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.api.events.DisplayTranslationEvent;
import me.c7dev.dexterity.displays.DexterityDisplay;

public class SitAnimation extends Animation implements RideableAnimation, Listener {
	
	private final double seat_y_offset = -1.5;
	private ArmorStand mount;
	private Player p;
	private Vector seat_offset = new Vector(0, seat_y_offset, 0);
	private boolean freeze_dismount_event = false;

	public SitAnimation(DexterityDisplay display) {
		super(display, 1);
		Bukkit.getPluginManager().registerEvents(this, display.getPlugin());
	}
	
	private void spawnMount() {
		mount = getDisplay().getPlugin().spawn(getDisplay().getCenter().add(seat_offset), ArmorStand.class, a -> {
			a.setSilent(true);
			a.setGravity(false);
			a.setVisible(false);
		});
	}
	
	public void refreshMountedPlayer() {
		if (mount == null) {
			p = null;
			return;
		}
		if (mount.getPassengers().size() == 0) p = null;
	}
	
	public Player getMountedPlayer() {
		refreshMountedPlayer();
		return p;
	}
	
	public boolean mount(Player player) {
		if (p != null) return false;
		if (mount != null) mount.remove();
		spawnMount();
		p = player;
		mount.addPassenger(player);
		return true;
	}
	
	public void dismount() {
		if (p == null) return;
		if (mount != null) {
			mount.removePassenger(p);
			mount.remove();
		}
	}
	
	public void setSeatOffset(Vector v) {
		v = v.clone();
		v.setY(v.getY() + seat_y_offset);
		Vector diff = v.clone().subtract(seat_offset);
		if (mount != null) mount.teleport(mount.getLocation().add(diff));
		seat_offset = v;
	}
	
	public Vector getSeatOffset() {
		return seat_offset.clone().subtract(new Vector(0, seat_y_offset, 0));
	}
	
	@EventHandler
	public void onDismountEvent(EntityDismountEvent e) {
		if (p == null || mount == null || freeze_dismount_event || !e.getEntity().getUniqueId().equals(p.getUniqueId())) return;
		dismount();
	}
	
	@EventHandler
	public void onDisplayMove(DisplayTranslationEvent e) {
		if (!e.getDisplay().equals(super.getDisplay())) return;
		refreshMountedPlayer();
		if (mount == null) return;
		freeze_dismount_event = true;
		if (p != null) mount.removePassenger(p);
		mount.teleport(e.getTo().add(seat_offset));
		if (p != null) mount.addPassenger(p);
		freeze_dismount_event = false;
	}
	
	@Override
	public void stop() {
		super.kill();
		p = null;
		if (mount != null) mount.remove();
		HandlerList.unregisterAll(this);
	}

}
