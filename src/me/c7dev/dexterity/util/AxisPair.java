package me.c7dev.dexterity.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import me.c7dev.dexterity.api.DexterityAPI;

/**
 * Used to calculate the new yaw, pitch, and roll of a DexBlock in a rotation
 */
public class AxisPair {
	
	private Vector3d dir1, dir2;
	
	public static double PI2 = Math.PI/2;
	
	public void highlight(DexBlock db, double seconds) {
		db.getDexterityDisplay().getPlugin().api().markerPoint(db.getLocation().add(DexUtils.vector(dir1)), Color.LIME, seconds);
		db.getDexterityDisplay().getPlugin().api().markerPoint(db.getLocation().add(DexUtils.vector(dir2)), Color.ORANGE, seconds);
		db.getDexterityDisplay().getPlugin().api().markerPoint(db.getLocation(), Color.BLACK, seconds);
	}
	
	public void highlight(Location loc, DexterityAPI api, double seconds) {
		api.markerPoint(loc.clone().add(DexUtils.vector(dir1)), Color.LIME, seconds);
		api.markerPoint(loc.clone().add(DexUtils.vector(dir2)), Color.ORANGE, seconds);
		api.markerPoint(loc, Color.BLACK, seconds);
	}
	
	public AxisPair() {
		dir1 = new Vector3d(0, 0, 1);
		dir2 = new Vector3d(1, 0, 0);
	}
	
	public AxisPair(DexBlock db) {
		this(db.getEntity().getLocation().getYaw(), -db.getEntity().getLocation().getPitch(), -db.getRoll());
	}
	
	public AxisPair(double yaw, double pitch, double roll) {
		this();
		Quaterniond q = new Quaterniond();
		q.rotateZ(Math.toRadians(roll));
		q.rotateX(Math.toRadians(pitch));
		q.rotateY(Math.toRadians(yaw));
		q.transformInverse(dir1);
		q.transformInverse(dir2);
	}
	
	public AxisPair(Vector x, Vector z) {
		if (!DexUtils.isOrthonormal(x, z)) throw new IllegalArgumentException("Axes must be orthonormal!");
		dir1 = DexUtils.vectord(z);
		dir2 = DexUtils.vectord(x);
	}

	
	public void transform(Quaterniond q1) {
		q1.transform(dir1);
		q1.transform(dir2);
	}
	
	/**
	 * Get the vector used to calculate yaw and pitch
	 * @return
	 */
	public Vector getAxis1() {
		return DexUtils.vector(dir1);
	}
	
	/**
	 * Get the vector used to calculate roll
	 * @return
	 */
	public Vector getAxis2() {
		return DexUtils.vector(dir2);
	}
	
	
	/**
	 * Returns a vector holding the pitch (x), yaw (y), and roll (z) in degrees
	 * @return
	 */
	public Vector getPitchYawRoll() {
		
		double yaw_rad = -Math.atan2(dir1.x, dir1.z);
		double pitch_rad = -Math.asin(dir1.y);

		Vector3d dir2_proj = new Vector3d(Math.cos(yaw_rad), 0, Math.sin(yaw_rad));
		double roll_rad = Math.acos(dir2.dot(dir2_proj));
		if (Double.isNaN(roll_rad) || !Double.isFinite(roll_rad)) roll_rad = 0;

		if (dir2.y < 0) roll_rad = -roll_rad;
		if (Double.isNaN(yaw_rad)) yaw_rad = 0;
		if (Double.isNaN(pitch_rad)) pitch_rad = PI2;
		
		return new Vector((float) Math.toDegrees(pitch_rad), (float) Math.toDegrees(yaw_rad), (float) Math.toDegrees(roll_rad));
	}

}
