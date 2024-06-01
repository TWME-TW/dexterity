package me.c7dev.tensegrity.displays;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.animation.Animation;
import me.c7dev.tensegrity.displays.animation.RotationAnimation;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;
import me.c7dev.tensegrity.util.Plane;

public class DexterityDisplay {
	
	private Dexterity plugin;
	private Location center;
	private Plane rot_plane = Plane.XZ;
	private String label;
	private double scale_x = 1, scale_y = 1, scale_z = 1;
	private DexterityDisplay parent;
	
	private List<DexBlock> blocks = new ArrayList<>();
	private List<Animation> animations = new ArrayList<>();
	private List<DexterityDisplay> subdisplays = new ArrayList<>();
		
	public DexterityDisplay(Dexterity plugin, Location center, String label) {
		this.plugin = plugin;
		this.center = center;
		
		if (label == null) {
			int i =1;
			while (plugin.getDisplayLabels().contains("display-" + i)) i++;
			this.label = "display-" + i;
		} else this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	public boolean setLabel(String s) {
		if (plugin.getDisplayLabels().contains(s)) return false;
		label = s;
		return true;
	}
	
	public List<DexBlock> getBlocks(){
		return blocks;
	}

	public List<Animation> getAnimations(){
		return animations;
	}
	
	public double getScale() {
		return Math.pow(scale_x*scale_y*scale_z, 1/3.0); // M0
	}
	public double getScaleX() {
		return scale_x;
	}
	public double getScaleZ() {
		return scale_z;
	}
	public double getScaleY() {
		return scale_y;
	}

	public void setEntities(List<DexBlock> entities_){
		this.blocks = entities_;
		plugin.getDisplays().add(this);
	}
	
	public List<DexterityDisplay> getSubdisplays() {
		return subdisplays;
	}
	
	public DexterityDisplay getParent() {
		return parent;
	}
	
	public void setParent(DexterityDisplay p) {
		if (p == this) {
			Bukkit.getLogger().severe("Tried to set parent to self");
			return;
		}
		parent = p;
	}
	
	public DexterityDisplay getRootDisplay() {
		return rootDisplay(this);
	}
	
	public DexterityDisplay rootDisplay(DexterityDisplay d) {
		if (d.getParent() == null) return this;
		else return rootDisplay(d.getParent());
	}
	
	public boolean containsSubdisplay(DexterityDisplay d) {
		if (subdisplays.contains(d)) return true;
		for (DexterityDisplay child : subdisplays) {
			if (child.containsSubdisplay(d)) return true;
		}
		return false;
	}
	
	public DexterityDisplay merge(DexterityDisplay m, String new_group) { //TODO serialize
		if (m == this || m.getLabel().equals(label) || subdisplays.contains(m) || parent != null) return null;
		if (rootDisplay(this).containsSubdisplay(m)) return null;
		if (!m.getCenter().getWorld().getName().equals(center.getWorld().getName())) return null;
		if (new_group != null && plugin.getDisplayLabels().contains(new_group)) return null;
		
		plugin.getDisplays().remove(this);
		stopAnimations();
		m.stopAnimations();
		Vector c2v = center.toVector().add(m.getCenter().toVector()).multiply(0.5); //midpoint
		Location c2 = new Location(center.getWorld(), c2v.getX(), c2v.getY(), c2v.getZ());
		center = c2;
		m.setCenter(c2);
		
		DexterityDisplay r;
		if (new_group == null) {
			m.getSubdisplays().add(this);
			setParent(m);
			r = m;
		} else {
			plugin.getDisplays().remove(m);
			DexterityDisplay p = new DexterityDisplay(plugin, c2, new_group);
			
			setParent(p);
			m.setParent(p);
			p.getSubdisplays().add(this);
			p.getSubdisplays().add(m);
			plugin.registerDisplay(new_group, p);
			r = p;
		}
		
		plugin.saveDisplays();
		return r;
	}
	
	public void unmerge() {
		if (parent == null) return;
		parent.getSubdisplays().remove(this);
		parent = null;
		plugin.registerDisplay(label, this);
		plugin.saveDisplays();
	}
	
	public void remove(boolean restore) {
		if (parent != null) parent.getSubdisplays().remove(this);
		removeHelper(restore);
		plugin.saveDisplays();
	}
	
	private void removeHelper(boolean restore) {
		for (DexBlock b : blocks) {
			if (restore) {
				Location loc = DexUtils.blockLoc(b.getEntity().getLocation());
				loc.getBlock().setBlockData(b.getEntity().getBlock());
			}
			b.getEntity().remove();
		}
		
		plugin.getDisplays().remove(this);
		plugin.getDisplayLabels().remove(label);
		
		for (DexterityDisplay subdisplay : subdisplays) subdisplay.remove(restore);
	}
	
	public int getGroupSize() {
		return getGroupSize(this);
	}
	public int getGroupSize(DexterityDisplay s) {
		int i = 1;
		for (DexterityDisplay sub : s.getSubdisplays()) i += getGroupSize(sub);
		return i;
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
	
	public void stopAnimations() {
		for (Animation a : animations) {
			a.stop();
		}
	}
	
	public void teleport(Location loc) {
		Vector diff = new Vector(loc.getX() - center.getX(), loc.getY() - center.getY(), loc.getZ() - center.getZ());
		//else diff = new Vector(loc.getX() - center.getX(), loc.getY() - center.getY(), loc.getZ() - center.getZ());
		teleport(diff);
	}
	
	public void teleport(Vector diff) {
		center.add(diff);
		for (DexBlock b : blocks) {
			b.move(diff);
		}
		for (DexterityDisplay subd : subdisplays) subd.teleport(diff);
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
	
	public void setScale(float s) {
		setScale(s, s, s);
	}
		
	public void setScale(float x, float y, float z) {
		for (DexBlock db : blocks) {
			Vector displacement = db.getEntity().getLocation().toVector().subtract(center.toVector());
			displacement.setX((displacement.getX() * (x - 1)) + (x >= 0 ? -0.5 : Math.abs(x) - 0.5));
			displacement.setY((displacement.getY() * (y - 1)) + (y >= 0 ? -0.5 : Math.abs(y) - 0.5));
			displacement.setZ((displacement.getZ() * (z - 1)) + (z >= 0 ? -0.5 : Math.abs(z) - 0.5));
			Vector3f dispv = new Vector3f((float) displacement.getX(), (float) displacement.getY(), (float) displacement.getZ());
			db.getTransformation()
					.setDisplacement(dispv)
					.setScale(x, y, z);
			db.updateTransformation();
		}
		scale_x = x; scale_y = y; scale_z = z;
		for (DexterityDisplay sub : subdisplays) sub.setScale(x, y, z);
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
		
		double sin = Math.sin(radians/2);
		double cos = Math.cos(radians/2);
		double x = sin*plane.getNormal().getX();
		double y = sin*plane.getNormal().getY();
		double z = sin*plane.getNormal().getZ();
		double w = cos;
		
		double mag = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2) + Math.pow(w, 2));
		x = x/mag; y = y/mag; z = z/mag; w = w/mag;
		
		Quaternionf left = new Quaternionf(-x, -y, -z, w);
		Quaternionf right = new Quaternionf(x, y, z, w);
		
		boolean printed = false;
		for (DexBlock db : blocks) {
			Transformation tr1 = db.getEntity().getTransformation();
			if (!printed) {
				printed = true;
				Bukkit.broadcastMessage(tr1.getLeftRotation().toString());
				Bukkit.broadcastMessage(tr1.getRightRotation().toString());
			}
			
			Transformation tr2 = new Transformation(
					tr1.getTranslation(),
					left,
					tr1.getScale(),
					right
					);
			db.getEntity().setTransformation(tr2);
		}
		
		/*new BukkitRunnable() {
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
		}.runTaskAsynchronously(plugin);*/
	}

}
