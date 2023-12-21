package me.c7dev.tensegrity.displays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.animation.Animation;
import me.c7dev.tensegrity.displays.animation.RotationAnimation;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DoubleHolder;
import me.c7dev.tensegrity.util.Plane;

public class DexterityDisplay {
	
	private Dexterity plugin;
	private Location center;
	private Plane rot_plane = Plane.XZ;
	private int id;
	private String label;
	private double scale = 1;
	
	private List<DexBlock> blocks = new ArrayList<>();
	private List<Animation> animations = new ArrayList<>();
		
	public DexterityDisplay(Dexterity plugin, Location center, String label) {
		this.plugin = plugin;
		this.center = center;
		this.id = plugin.getNextDisplayId();
		
		if (label == null) {
			int i =1;
			for (; i < plugin.getDisplays().size()+1; i++) {
				if (!plugin.getLabelMap().containsKey("display-" + i)) break;
			}
			this.label = "display-" + i;
		} else this.label = label;
		plugin.getLabelMap().put(this.label, id);
	}
	
	public int getID() {
		return id;
	}
	
	public String getLabel() {
		return label;
	}
	public boolean setLabel(String s) {
		if (plugin.getLabelMap().containsKey(s)) return false;
		
		plugin.getLabelMap().put(s, id);
		label = s;
		BlockDisplay bd = blocks.get(0).getEntity();
		bd.setRotation(35f, 35f);
		return true;
	}
	
	public List<DexBlock> getBlocks(){
		return blocks;
	}

	public List<Animation> getAnimations(){
		return animations;
	}
	
	public double getScale() {
		return scale;
	}

	public void setEntities(List<DexBlock> entities_){
		this.blocks = entities_;
		plugin.getDisplays().add(this);
	}
	
	public void remove() {
		for (DexBlock b : blocks) {
			b.getEntity().remove();
		}
		plugin.getDisplays().remove(this);
		plugin.saveDisplays();
	}
	
	public Dexterity getPlugin() {
		return plugin;
	}
	
	public Location getCenter() {
		return center.clone();
	}
	
	public void setCenter(Location loc) {
		center = loc;
		for (DexBlock b : blocks) {
			b.recalculateRadius(center);
		}
	}
	
	public void teleport(Location loc) {
		Vector diff = new Vector(loc.getX() - center.getX(), loc.getY() - center.getY(), loc.getZ() - center.getZ());
		//else diff = new Vector(loc.getX() - center.getX(), loc.getY() - center.getY(), loc.getZ() - center.getZ());
		center.add(diff);
		for (DexBlock b : blocks) {
			b.move(diff);
		}
	}
	
	public Plane getRotationPlane() {
		return rot_plane;
	}
	
	public void setRotationPlane(Plane plane) {
		//if (this.rot_plane == plane) return;
		this.rot_plane = plane;
		//for (DexBlock b : blocks) {
		//	b.recalculateRadius(center);
		//}
	}
	
	public void setScale(double s) {
		
	}
	@Deprecated
	public void forceSetScale(double s) { //for init only
		this.scale = s;
	}
	
	public void rotate(double degrees, Plane plane) {
		double radians_m = Math.toRadians(degrees % 360);
		if (radians_m < 0) radians_m += 2*Math.PI;
		final double radians = radians_m;
		
		RotationAnimation animation = null;
		for (Animation a : animations) {
			if (a instanceof RotationAnimation) {
				animation = (RotationAnimation) a;
				break;
			}
		}
		if (animation != null) {
			if (animation.isPaused()) animation = null;
			else animation.setPaused(true);
		}
		
		final RotationAnimation a = animation;
		final long start = System.currentTimeMillis();
		
		new BukkitRunnable() {
			double sin = 2*Math.sin(radians/2), root2 = Math.sqrt(2)/2;
			
			@Override
			public void run() {
				if (plane == Plane.XY && degrees < 0) sin *= -1;
				final HashMap<UUID,DoubleHolder> vx_map = new HashMap<>(), vy_map = new HashMap<>();
				for (DexBlock block : blocks) {
					//if (rot_plane != plane) block.recalculateRadius(center, plane);
					
					double angle = block.calculateAngle(center, plane);
					if (Double.isNaN(angle)) angle = 0;
					//Bukkit.broadcastMessage("angle=" + angle + ", r=" + block.getRadius());
					vx_map.put(block.getEntity().getUniqueId(), new DoubleHolder(-block.getRadius(plane)*sin*Math.sin(angle + radians/2) - 
							0*root2*sin*Math.sin(Math.toRadians(block.getRotationXZ()) + radians/2)));
					vy_map.put(block.getEntity().getUniqueId(), new DoubleHolder( block.getRadius(plane)*sin*Math.cos(angle + radians/2) +
							0*root2*sin*Math.cos(Math.toRadians(block.getRotationXZ()) + radians/2)));
				}
				//Bukkit.broadcastMessage("took " + (System.currentTimeMillis() - start) + "ms to calculate");
				
				new BukkitRunnable() {
					@Override
					public void run() {
						for (DexBlock block : blocks) {
							//block.addRotation((float) -degrees, 0f);
							if (!vx_map.containsKey(block.getEntity().getUniqueId())) continue;
							double vx = vx_map.get(block.getEntity().getUniqueId()).getValue();
							double vy = vy_map.get(block.getEntity().getUniqueId()).getValue();
							
							//Bukkit.broadcastMessage("dx = " + vx + ", dy=" + vy + ", r=" + block.getRadius() + " t=" + block.getEntity().getBlock().getMaterial().toString());
							
							switch(plane) {
							case XY:
								block.move(vx, -vy, 0);
								break;
							case ZY:
								block.move(0, vx, vy);
								break;
							default:
								block.move(vx, 0, vy);
								break;
							}
						}
						
						if (a != null) {
							a.recalculateAngleSteps();
							a.setPaused(false);
						}
						
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}

}
