package me.c7dev.tensegrity.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class DexBlock {
	
	private double radius_xz, radius_xy, radius_zy;
	private float rot_xz, rot_xy;
	private int angle_step = 0;
	private BlockDisplay entity;
	//private boolean armor_stand;
	
	//public static final Vector AS_OFFSET = new Vector(0.5, -0.5, 0.5);
	
	public DexBlock(Block display, DexterityDisplay d) {
		this.entity = display.getLocation().getWorld().spawn(display.getLocation(), BlockDisplay.class, (spawned) -> {
			spawned.setBlock(display.getBlockData());
			d.getPlugin().getBlockUUIDMap().put(spawned.getUniqueId(), d.getID());
		});
		recalculateRadius(d.getCenter());
	}
	public DexBlock(BlockDisplay bd, DexterityDisplay d) {
		this.entity = bd;
		recalculateRadius(d.getCenter());
		d.getPlugin().getBlockUUIDMap().put(bd.getUniqueId(), d.getID());
	}
	
	public BlockDisplay getEntity() {
		return this.entity;
	}
	
	public void teleport(Location loc) {
		entity.teleport(loc);
	}
	public void move(Vector v) {
		entity.teleport(entity.getLocation().add(v));
	}
	public void move(double x, double y, double z) {
		entity.teleport(entity.getLocation().add(x, y, z));
	}
	public void setBrightness(int blockLight, int skyLight) {
		//entity.setBrightness(new Brightness(blockLight, skyLight));
	}
	
	public double getRadius(Plane plane) {
		switch(plane) {
		case XY: return radius_xy;
		case ZY: return radius_zy;
		default: return radius_xz;
		}
	}
	public int getAngleStep() {
		return this.angle_step;
	}
	public void setAngleStep(int i) {
		this.angle_step = i;
	}
	
	public void addRotation(float xz, float xy) {
		rot_xz += xz; rot_xy += xy;
		entity.setRotation(rot_xz, rot_xy);
	}
	public void setRotation(float xz, float xy) {
		rot_xz = xz; rot_xy = xy;
		entity.setRotation(rot_xz, rot_xy);
	}
	public float getRotationXZ() {
		return rot_xz;
	}
	public float getRotationXY() {
		return rot_xy;
	}
	
	public void scaleRadius(double s) {
		radius_xz *= s;
		radius_xy *= s;
		radius_zy *= s;
	}
	
	public Location getLocation() {
		//if (armor_stand) return entity.getLocation().add(AS_OFFSET);
		return entity.getLocation().add(0.5, 0.5, 0.5);
	}
	
	public double calculateAngle(Location center, Plane plane) {
		if (getRadius(plane) == 0) return Double.NaN; //coaxial block
		Vector anglev, cv = center.toVector(); //both lie on the plane of rotation for the block
		
		Location loc = this.getLocation();
		if (plane == Plane.XZ) {
			anglev = new Vector(1, 0, 0);
			cv.setY(loc.getY());
		}
		else {
			anglev = new Vector(0, 1, 0);
			if (plane == Plane.ZY) cv.setX(loc.getX());
			else cv.setZ(loc.getZ()); //XY
		}
		
		
		Vector displacement = loc.toVector().subtract(cv);
		int m = 1;
		if (plane == Plane.XY) {
			if (displacement.getX() < 0) m = -1; 
		} else if (displacement.getZ() < 0) m = -1;
		
		return m*displacement.angle(anglev);
	}
	
	public void recalculateRadius(Location center) { //cylindrical coords TODO
				
			Location c = center.clone();
			c.setZ(entity.getLocation().getZ());
			radius_xy = entity.getLocation().add(0.5, 0.5, 0).distance(c);
		
			c = center.clone();
			c.setX(entity.getLocation().getX());
			radius_zy = entity.getLocation().add(0, 0.5, 0.5).distance(c);
			
			c = center.clone();
			c.setY(entity.getLocation().getY());
			radius_xz = entity.getLocation().add(0.5, 0, 0.5).distance(c);
	}

}
