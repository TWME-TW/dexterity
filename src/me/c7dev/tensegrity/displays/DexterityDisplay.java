package me.c7dev.tensegrity.displays;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.api.DexRotation;
import me.c7dev.tensegrity.displays.animation.Animation;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.RotationPlan;
import me.c7dev.tensegrity.util.DexUtils;

public class DexterityDisplay {
	
	private Dexterity plugin;
	private Location center;
	private String label;
	private Vector scale;
	private DexterityDisplay parent;
	private boolean started_animations = false, zero_pitch = false;
	private UUID uuid = UUID.randomUUID(), editing_lock;
	private DexRotation rot = null;
	
	private List<DexBlock> blocks = new ArrayList<>();
	private List<Animation> animations = new ArrayList<>();
	private List<DexterityDisplay> subdisplays = new ArrayList<>();
	
	public DexterityDisplay(Dexterity plugin) {
		this(plugin, null, new Vector(1, 1, 1));
	}
	
	//TODO make block transactions update scale and rotation
		
	public DexterityDisplay(Dexterity plugin, Location center, Vector scale) {
		this.plugin = plugin;
		this.scale = scale == null ? new Vector(1, 1, 1) : scale;
		if (center == null) recalculateCenter();
		else this.center = center;
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
	
	public double getYaw() {
		return rot == null ? 0 : rot.getYaw();
	}
	public double getPitch() {
		return rot == null ? 0 : rot.getPitch();
	}
	public double getRoll() {
		return rot == null ? 0 : rot.getRoll();
	}
	
	public void recalculateCenter() {
		Vector cvec = new Vector(0, 0, 0);
		World w;
		int n = 0;
		zero_pitch = true;
		
		boolean set_dir = false;
		
		if (blocks.size() == 0) {
			w = plugin.getDefaultWorld();
			n = 1;
		}
		else {
			w = blocks.get(0).getEntity().getLocation().getWorld();
			n = blocks.size();
			double scalex = -1, scaley = -1, scalez = -1;
			for (DexBlock db : blocks) {
				cvec.add(db.getLocation().toVector());
				
				Vector scale = db.getTransformation().getScale();
				if (scale.getX() > scalex) scalex = scale.getX();
				if (scale.getY() > scaley) scaley = scale.getY();
				if (scale.getZ() > scalez) scalez = scale.getZ();
				
				if (zero_pitch && db.getEntity().getLocation().getPitch() != 0) zero_pitch = false;
//				if (!set_dir) { //TODO
//					base_yaw = db.getEntity().getLocation().getYaw();
//					base_pitch = db.getEntity().getLocation().getPitch();
//				} else {
//					if (db.getEntity().getLocation().getYaw() != base_yaw) base_yaw = 0;
//					if (db.getEntity().getLocation().getPitch() != base_pitch) base_pitch = 0;
//				}
			}
			scale = new Vector(scalex, scaley, scalez);
		}
		center = DexUtils.location(w, cvec.multiply(1.0/n));
	}
	
	public void setDefaultLabel() {
		int i =1;
		while (plugin.getDisplayLabels().contains("display-" + i)) i++;
		setLabel("display-" + i);
	}
	
	@Deprecated
	public void forceSetLabel(String s) {
		if (s == null || plugin.getDisplayLabels().contains(s)) return;
		label = s;
	}
	
	public boolean setLabel(String s) {
		if (s == null || plugin.getDisplayLabels().contains(s)) return false;
		if (label != null) plugin.unregisterDisplay(this);
		plugin.registerDisplay(s, this);
		label = s;
		for (DexBlock db : blocks) {
			if (db.getDexterityDisplay() == null || !db.getDexterityDisplay().isListed()) db.setDexterityDisplay(this);
		}
		plugin.saveDisplays();
		return true;
	}
		
	public boolean isListed() {
		return label != null;
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

	public void setEntities(List<DexBlock> entities_, boolean recalc_center){
		this.blocks = entities_;
		plugin.unregisterDisplay(this);
		if (recalc_center) recalculateCenter();
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
	
	public boolean canHardMerge() {
		return !hasStartedAnimations();
	}
	
	public boolean hasStartedAnimations() {
		return started_animations;
	}
	
	public boolean isZeroPitch() { //if yaw-only rotation optimization is appropriate
		return zero_pitch;
	}
	
	@Deprecated
	public void setZeroPitch(boolean b) {
		zero_pitch = b;
	}
	
	public boolean hardMerge(DexterityDisplay subdisplay) {
		if (!subdisplay.getCenter().getWorld().getName().equals(center.getWorld().getName()) ||
			!subdisplay.canHardMerge() || !canHardMerge()) return false;
		plugin.unregisterDisplay(subdisplay);
		for (DexBlock b : subdisplay.getBlocks()) {
			b.setDexterityDisplay(this);
			blocks.add(b);
			if (zero_pitch && b.getEntity().getLocation().getPitch() != 0) zero_pitch = false;
		}
		for (DexterityDisplay subdisp : subdisplay.getSubdisplays()) {
			subdisp.merge(this, null);
		}
		return true;
	}
	
	public DexterityDisplay merge(DexterityDisplay newparent, String new_group) {
		if (newparent == this || newparent.getLabel().equals(label) || subdisplays.contains(newparent) || parent != null) return null;
		if (rootDisplay(this).containsSubdisplay(newparent)) return null;
		if (!newparent.getCenter().getWorld().getName().equals(center.getWorld().getName())) return null;
		if (new_group != null && plugin.getDisplayLabels().contains(new_group)) return null;
		if (label == null || newparent.getLabel() == null) return null;
		
		plugin.unregisterDisplay(this, true);
		stopAnimations(true);
		newparent.stopAnimations(true);
		Vector c2v = center.toVector().add(newparent.getCenter().toVector()).multiply(0.5); //midpoint
		Location c2 = new Location(center.getWorld(), c2v.getX(), c2v.getY(), c2v.getZ());
		newparent.setCenter(c2);
		
		DexterityDisplay r;
		if (new_group == null) {
			newparent.getSubdisplays().add(this);
			setParent(newparent);
			r = newparent;
		} else {
			plugin.unregisterDisplay(newparent, true);
			DexterityDisplay p = new DexterityDisplay(plugin);
			p.setLabel(new_group);
			
			setParent(p);
			newparent.setParent(p);
			p.getSubdisplays().add(this);
			p.getSubdisplays().add(newparent);
			r = p;
		}
		
		plugin.saveDisplays();
		return r;
	}
	
	public UUID getEditingLock() {
		return editing_lock;
	}
	
	public void setEditingLock(UUID u) {
		editing_lock = u;
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
		if (restore) {
			for (DexBlock b : blocks) {
				Location loc = DexUtils.blockLoc(b.getEntity().getLocation());
				loc.getBlock().setBlockData(b.getEntity().getBlock());
				b.getEntity().remove();
			}
		} else for (DexBlock b : blocks) b.getEntity().remove();
		
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
	}
	
	public void startAnimations() {
		if (hasStartedAnimations()) return;
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
	
	public void scale(float s) {
		scale(new Vector(s, s, s));
	}
	
	public void setScale(float s) {
		setScale(new Vector(s, s, s));
	}
	
	public void setScale(Vector s) {
		if (s.getX() == 0 || s.getY() == 0 || s.getZ() == 0) return;
		scale(new Vector(s.getX() / scale.getX(), s.getY() / scale.getY(), s.getZ() / scale.getZ()));
	}
		
	public void scale(Vector v) {
		if (v.getX() == 0 || v.getY() == 0 || v.getZ() == 0) throw new IllegalArgumentException("Scale cannot be zero!");
		Vector sd = v.clone().add(new Vector(-1, -1, -1));
		for (DexBlock db : blocks) {
			
//			Vector disp = db.getEntity().getLocation().toVector().subtract(center.toVector());
			Vector disp = db.getLocation().toVector().subtract(center.toVector());
			Vector diff = DexUtils.hadimard(disp, sd);
			Vector block_scale = DexUtils.hadimard(v, db.getTransformation().getScale());
			Vector roll_offset = DexUtils.hadimard(v, db.getTransformation().getRollOffset());
			
			db.move(diff);
			
			db.getTransformation()
					.setDisplacement(block_scale.clone().multiply(-0.5))
					.setScale(block_scale)
					.setRollOffset(roll_offset);
			db.updateTransformation();
		}
		scale = DexUtils.hadimard(scale, v);
		for (DexterityDisplay sub : subdisplays) sub.setScale(v);
	}
	
	@Deprecated
	public void resetScale(Vector v) {
		scale = v.clone();
	}
	
	public void align() { //TODO add -from_center
		DexBlock block = null;
		double minx = 0, miny = 0, minz = 0;
		for (DexBlock b : blocks) {
			Location loc = b.getLocation().add(b.getTransformation().getDisplacement());
			if (block == null || (loc.getX() <= minx && loc.getY() <= miny && loc.getZ() <= minz)) {
				block = b;
				minx = loc.getX();
				miny = loc.getY();
				minz = loc.getZ();
			}
		}
		if (block != null) {
			Location loc = block.getLocation().add(block.getTransformation().getDisplacement());
			Vector disp = loc.clone().subtract(DexUtils.blockLoc(loc.clone())).toVector();
			teleport(center.clone().subtract(disp));
		}
	}
	
//	public void rotateQ(double x, double y, double z) {
//		for (DexBlock b : blocks) {
//			b.setTransformation(b.getTransformation().setDisplacement(new Vector(0, 0, 0)));
//		}
//		Vector centerv = center.toVector().add(scale.clone().multiply(0.5));
//		plugin.getAPI().markerPoint(DexUtils.location(center.getWorld(), centerv), Color.LIME, 8);
//		double gamma = Math.toRadians(x), beta = Math.toRadians(y), alpha = Math.toRadians(z);
//		
//		Matrix3d rotmat = new Matrix3d(
//				Math.cos(alpha)*Math.cos(beta), (Math.cos(alpha)*Math.sin(beta)*Math.sin(gamma)) - (Math.sin(alpha)*Math.cos(gamma)), (Math.cos(alpha)*Math.sin(beta)*Math.cos(gamma)) + (Math.sin(alpha)*Math.sin(beta)),
//				Math.sin(alpha)*Math.cos(beta), (Math.sin(alpha)*Math.sin(beta)*Math.sin(gamma)) + (Math.cos(alpha)*Math.cos(gamma)), (Math.sin(alpha)*Math.sin(beta)*Math.cos(gamma)) - (Math.cos(alpha)*Math.sin(gamma)),
//				-Math.sin(beta), Math.cos(beta)*Math.sin(gamma), Math.cos(beta)*Math.cos(gamma)
//				).transpose();
//		
//		for (DexBlock b : blocks) {
//			Vector3f oldOffset = DexUtils.vector(b.getEntity().getLocation().toVector().subtract(centerv));
//			Vector3f newOffset = new Vector3f();
//			rotmat.transform(oldOffset, newOffset);
//			Location loc = b.getLocation().add(DexUtils.vector(newOffset.sub(oldOffset)));
////			plugin.getAPI().markerPoint(b.getEntity().getLocation(), Color.RED, 8);
////			plugin.getAPI().markerPoint(loc, Color.ORANGE, 8);
//			b.teleport(loc);
//			//b.setRotation(Math.sin(Math.toRadians(pitch)), Math.sin(Math.toRadians(yaw)), Math.sin(Math.toRadians(roll)));
//		}
//	}
	
	public void rotate(float y_deg, float pitch_deg, float roll_deg) {
		RotationPlan plan = new RotationPlan();
		plan.y_deg = y_deg;
		plan.pitch_deg = pitch_deg;
		plan.roll_deg = roll_deg;
		rotate(plan);
	}
	
	public void setRotation(float y_deg, float pitch_deg, float roll_deg) {
		RotationPlan plan = new RotationPlan();
		plan.y_deg = y_deg;
		plan.pitch_deg = pitch_deg;
		plan.roll_deg = roll_deg;
		plan.set_y = true;
		plan.set_pitch = true;
		plan.set_roll = true;
		rotate(plan);
	}
	
	public void setBaseRotation(float y, float pitch, float roll) {
		if (rot == null) rot = new DexRotation(this, y, pitch, roll);
		else rot.setAxes(y, pitch, roll);
	}
	
	public DexRotation getRotationManager() {
		return rot;
	}
	
	public DexRotation getRotationManager(boolean create_new) {
		if (rot == null && create_new) rot = new DexRotation(this);
		return rot;
	}
	
	public void rotate(RotationPlan plan) {
		if (rot == null) rot = new DexRotation(this);
		rot.rotate(plan);
	}

}
