package me.c7dev.tensegrity.util;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class AxisPair {
	
	private Vector3d dir1, dir2;
	
	public static double PI2 = Math.PI/2;
	
	public void highlight(DexBlock db, int seconds) {
		db.getDexterityDisplay().getPlugin().api().markerPoint(db.getLocation().add(DexUtils.vector(dir1)), Color.LIME, seconds);
		db.getDexterityDisplay().getPlugin().api().markerPoint(db.getLocation().add(DexUtils.vector(dir2)), Color.ORANGE, seconds);
		db.getDexterityDisplay().getPlugin().api().markerPoint(db.getLocation(), Color.BLACK, seconds);
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
