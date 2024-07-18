package me.c7dev.tensegrity.displays.animation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.DexterityDisplay;

public class RideAnimation extends Animation {
	
	public enum LookMode {
		NONE,
		YAW_ONLY,
		YAW_PITCH,
	}
	
	private Location start_loc;
	private double speed = 2.0/20, seat_y_offset = -1.34;
	private boolean x_enabled = true, y_enabled = true, z_enabled = true, teleport_when_done = false;
	private LookMode look_mode = LookMode.YAW_PITCH;
	private Snowball mount;
	private Player p;
	private Vector seat_offset = new Vector(0, seat_y_offset, 0);

	public RideAnimation(DexterityDisplay display, Dexterity plugin) {
		super(display, plugin, 1);
		
		spawnMount();
		
		setFrameRate(1);
		
		start_loc = display.getCenter();
		
		if (!x_enabled && !y_enabled && !z_enabled) return;
						
		super.setRunnable(new BukkitRunnable() {
			@Override
			public void run() {
				if (p == null || isPaused()) return;
				if (!p.isOnline() || mount.getPassengers().size() == 0) {
					if (mount.isDead()) getDisplay().teleport(new Vector(0, 0.5, 0));
					stop();
					return;
				}
				
				Vector dir = p.getLocation().getDirection();
				if (!x_enabled) dir.setX(0);
				if (!y_enabled) dir.setY(0);
				if (!z_enabled) dir.setZ(0);
				dir.normalize();
				
				Block crash = mount.getLocation().add(dir.clone().multiply(1.5*speed)).getBlock();
				if (crash.getType() != Material.AIR && crash.getType().isSolid()) {
					mount.setVelocity(new Vector(0, 0, 0));
					return;
				}
				
				dir.multiply(speed);
				mount.setVelocity(dir);
				
				display.teleport(dir);
				if (look_mode == LookMode.YAW_ONLY) display.rotate(p.getLocation().getYaw(), 0, 0, 0, true, false, false, false);
				else if (look_mode == LookMode.YAW_PITCH) display.setRotation(p.getLocation().getYaw(), p.getLocation().getPitch(), 0);
				
			}
		});
	}
	
	private void spawnMount() {
		mount = getDisplay().getCenter().getWorld().spawn(getDisplay().getCenter().add(seat_offset), Snowball.class, a -> {
			//a.addPassenger(p);
			//a.setVisible(false);
			//a.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false));
			a.setSilent(true);
			a.setGravity(false);
		});
	}
	
	public Player getMountedPlayer() {
		return p;
	}
	
	public boolean mount(Player player) {
		if (p != null) return false;
		if (mount == null || mount.isDead()) spawnMount();
		p = player;
		mount.addPassenger(player);
		return true;
	}
	public void dismount() {
		if (p == null) return;
		if (mount != null) mount.removePassenger(p);
	}
	
	public void setXEnabled(boolean b) {
		x_enabled = b;
	}
	public void setYEnabled(boolean b) {
		y_enabled = b;
	}
	public void setZEnabled(boolean b) {
		z_enabled = b;
	}
	public void setSpeed(double blocks_per_second) {
		speed = blocks_per_second / 20;
	}
	public double getSpeed() {
		return 20*speed;
	}
	public void setTeleportBackOnDismount(boolean b) {
		teleport_when_done = b;
	}
	
	public void setLookingMode(LookMode lm) {
		look_mode = lm;
	}
	
	public void setSeatOffset(Vector v) {
		v.setY(v.getY() + seat_y_offset);
		Vector diff = v.clone().subtract(seat_offset);
		if (mount != null) mount.teleport(mount.getLocation().add(diff));
		seat_offset = v;
	}
	public Vector getSeatOffset() {
		return seat_offset;
	}
	
	@Override
	public void stop() {
		super.kill();
		p = null;
		if (mount != null) mount.remove();
		if (teleport_when_done) getDisplay().teleport(start_loc);
	}

}
