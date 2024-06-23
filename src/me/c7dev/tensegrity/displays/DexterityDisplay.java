package me.c7dev.tensegrity.displays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.animation.Animation;
import me.c7dev.tensegrity.displays.animation.RotationAnimation;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;
import me.c7dev.tensegrity.util.DoubleHolder;
import me.c7dev.tensegrity.util.Plane;

public class DexterityDisplay {
	
	private Dexterity plugin;
	private Location center;
	private Plane rot_plane = Plane.XZ;
	private String label;
	private Vector scale;
	private DexterityDisplay parent;
	private boolean started_animations = false;
	private boolean isListed = false;
	private UUID uuid = UUID.randomUUID();
	
	private List<DexBlock> blocks = new ArrayList<>();
	private List<Animation> animations = new ArrayList<>();
	private List<DexterityDisplay> subdisplays = new ArrayList<>();
	
	public DexterityDisplay(Dexterity plugin, Location center) {
		this(plugin, center, new Vector(1, 1, 1));
	}
		
	public DexterityDisplay(Dexterity plugin, Location center, Vector scale) {
		this.plugin = plugin;
		this.center = center;
		this.scale = scale;
	}
	
	public UUID getUniqueId() {
		return uuid;
	}
	
	public boolean equals(DexterityDisplay d) {
		return uuid.equals(d.getUniqueId());
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setDefaultLabel() {
		int i =1;
		while (plugin.getDisplayLabels().contains("display-" + i)) i++;
		setLabel("display-" + i);
	}
	public boolean setLabel(String s) {
		if (plugin.getDisplayLabels().contains(s)) return false;
		isListed = true;
		label = s;
		plugin.registerDisplay(s, this);
		plugin.saveDisplays();
		return true;
	}
	
	public boolean isListed() {
		return isListed;
	}
	
	public List<DexBlock> getBlocks(){
		return blocks;
	}

	public List<Animation> getAnimations(){
		return animations;
	}
	
	public Vector getScale() {
		return scale;
	}

	public void setEntities(List<DexBlock> entities_){
		this.blocks = entities_;
		plugin.unregisterDisplay(this);
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
		
		plugin.unregisterDisplay(this);
		stopAnimations(true);
		m.stopAnimations(true);
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
			plugin.unregisterDisplay(m);
			DexterityDisplay p = new DexterityDisplay(plugin, c2);
			p.setLabel(new_group);
			
			setParent(p);
			m.setParent(p);
			p.getSubdisplays().add(this);
			p.getSubdisplays().add(m);
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
		
		plugin.unregisterDisplay(this);
		
		for (DexterityDisplay subdisplay : subdisplays.toArray(new DexterityDisplay[subdisplays.size()])) subdisplay.remove(restore);
	}
	
	public int getGroupSize() {
		if (label == null) return 1;
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
	
	public void startAnimations() {
		if (started_animations) return;
		started_animations = true;
		for (Animation a : animations) {
			a.start();
		}
	}
	
	public void stopAnimations(boolean force) {
		started_animations = false;
		for (Animation a : animations) {
			if (force) a.kill();
			else a.stop();
		}
	}
	
	public boolean hasStartedAnimations() {
		return started_animations;
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
	
	public void setGlow(Color c, boolean propegate) {
		if (c == null) {
			for (DexBlock b : blocks) b.getEntity().setGlowing(false);
		} else {
			for (DexBlock b : blocks) {
				b.getEntity().setGlowColorOverride(c);
				b.getEntity().setGlowing(true);
			}
		}
		if (propegate) {
			for (DexterityDisplay d : subdisplays) d.setGlow(c, true);
		}
	}
	
	public void setScale(float s) {
		setScale(new Vector(s, s, s));
	}
		
	public void setScale(Vector s) {
		Vector v = new Vector(s.getX() / scale.getX(), s.getY() / scale.getY(), s.getZ() / scale.getZ());
		Vector sd = v.clone().add(new Vector(-1, -1, -1));
		Vector3f trans_disp = DexUtils.vector(s.clone().multiply(-0.5)); //DexUtils.vector(v.clone().add(scale.clone().multiply(-0.5))).mul(0.5f);
		for (DexBlock db : blocks) {
			Vector disp = db.getEntity().getLocation().toVector().subtract(center.toVector());
//			displacement.setX((displacement.getX() * (x - 1)) + (x >= 0 ? -0.5 : Math.abs(x) - 0.5));
//			displacement.setY((displacement.getY() * (y - 1)) + (y >= 0 ? -0.5 : Math.abs(y) - 0.5));
//			displacement.setZ((displacement.getZ() * (z - 1)) + (z >= 0 ? -0.5 : Math.abs(z) - 0.5));
			
			//Vector diff = new Vector(disp.getX()*(sd.getX()-1), disp.getY()*(sd.getY()-1), disp.getZ()*(sd.getZ()-1));
			Vector diff = DexUtils.hadimard(disp, sd);
						
			db.move(diff);
			
			db.getTransformation()
					.setDisplacement(trans_disp)
					.setScale(DexUtils.vector(s));
			db.updateTransformation();
		}
		scale = s;
		for (DexterityDisplay sub : subdisplays) sub.setScale(v);
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
		
		/*final RotationAnimation a = animation;
		
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
		}*/
		
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
					vx_map.put(block.getEntity().getUniqueId(), new DoubleHolder(-block.getRadius(plane)*sin *Math.sin(angle + radians/2) - 
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
								block.getTransformation().setDisplacement(block.getTransformation().getDisplacement()
										.add(new Vector3f((float) vx, (float) -vy, 0f)));
								block.getEntity().setRotation((float) degrees + 90, 0);
								block.updateTransformation();
								//block.move(vx, -vy, 0);
								break;
							case ZY:
								//block.move(0, vx, vy);
								block.getTransformation().setDisplacement(block.getTransformation().getDisplacement()
										.add(new Vector3f((float) vx, 0f, (float) vy)));
								block.getEntity().setRotation((float) degrees + 90, 0);
								block.updateTransformation();
								break;
							default:
								//block.getTransformation().setDisplacement(new Vector3f((float) vx, 0, (float) vy).add(Dexterity.DEFAULT_DISP));
								block.getEntity().setRotation((float) degrees, 0);
								//block.updateTransformation();
								block.move(vx, 0, vy);
								break;
							}
						}
						
						/*if (a != null) {
							a.recalculateAngleSteps();
							a.setPaused(false);
						}*/
						
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}

}
