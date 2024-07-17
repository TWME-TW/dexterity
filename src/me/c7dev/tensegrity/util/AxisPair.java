package me.c7dev.tensegrity.util;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class AxisPair {
	
	private Vector3d dir1, dir2;
	private DexBlock remove = null; //TODO remove
	
	public AxisPair() {
		this(new Vector3d(0, 0, 1), new Vector3d(1, 0, 0));
	}
	
	public AxisPair(Vector a, Vector b) {
		this(DexUtils.vectord(a), DexUtils.vectord(b));
	}
	
	public AxisPair(DexBlock db) {
		this();
		Quaterniond q = new Quaterniond();
		q.rotateZ(-Math.toRadians(db.getRoll()));
		q.rotateX(-Math.toRadians(db.getEntity().getLocation().getPitch()));
		q.rotateY(Math.toRadians(db.getEntity().getLocation().getYaw()));
		q.transformInverse(dir1);
		q.transformInverse(dir2);
		
		remove = db;
//		Bukkit.broadcastMessage("prev dir1 = " + DexUtils.vectorString(DexUtils.vector(dir1), 4));
	}
	
	public AxisPair(Vector3d a, Vector3d b) {
		dir1 = new Vector3d(a.x, a.y, a.z); //yaw and pitch
		dir2 = new Vector3d(b.x, b.y, b.z); //roll
	}
	
	public void transform(Quaterniond q1) {
		q1.transform(dir1);
		q1.transform(dir2);
		if (remove != null) {
			remove.getDexterityDisplay().getPlugin().getAPI().markerPoint(remove.getLocation(), Color.BLACK, 10);
			remove.getDexterityDisplay().getPlugin().getAPI().markerPoint(remove.getLocation().add(DexUtils.vector(dir1)), Color.LIME, 10);
			remove.getDexterityDisplay().getPlugin().getAPI().markerPoint(remove.getLocation().add(DexUtils.vector(dir2)), Color.YELLOW, 10);
		}
	}
	
	
	public Vector getPitchYawRoll() {
		Vector3d xz = new Vector3d(dir1.x, dir1.y, dir1.z);
		xz.y = 0; //projection onto xz plane
		xz.normalize();
		Bukkit.broadcastMessage("dir2 = " + DexUtils.vectorString(DexUtils.vector(dir2), 4));
				
		double yaw_rad = Math.acos(xz.z); //dot with [0, 0, 1] and get angle
		if (xz.x > 0) yaw_rad = -yaw_rad;
		
		double pitch_rad = Math.acos(xz.dot(dir1));
		if (dir1.y > 0) pitch_rad = -pitch_rad;

		Vector3d xz2 = new Vector3d(dir2.x, dir2.y, dir2.z);
		xz2.y = 0;
		xz2.normalize();
		double roll_rad = Math.acos(xz2.dot(dir2));
		if (dir2.y < 0) roll_rad = -roll_rad;
		
		if (!Double.isFinite(yaw_rad)) yaw_rad = 0;
		if (!Double.isFinite(pitch_rad)) pitch_rad = 0;
		if (!Double.isFinite(roll_rad)) roll_rad = 0;
		return new Vector(Math.toDegrees(pitch_rad), Math.toDegrees(yaw_rad), Math.toDegrees(roll_rad));
	}

}
