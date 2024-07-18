package me.c7dev.tensegrity.util;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class AxisPair {
	
	private Vector3d dir1, dir2, dir2_proj;
	
	public static double PI2 = Math.PI/2;
	
	public AxisPair() {
		this(new Vector3d(0, 0, 1), new Vector3d(1, 0, 0), new Vector3d(1, 0, 0));
	}
	
	public AxisPair(DexBlock db) {
		this();
		Bukkit.broadcastMessage("new axis pair from db");
		Quaterniond q = new Quaterniond();
		q.rotateZ(-Math.toRadians(db.getRoll()));
		q.rotateX(-Math.toRadians(db.getEntity().getLocation().getPitch()));
		double yaw = Math.toRadians(db.getEntity().getLocation().getYaw());
		q.rotateY(yaw);
		q.transformInverse(dir1);
		q.transformInverse(dir2);
		
		Quaterniond q2 = new Quaterniond();
		q2.rotateY(Math.toRadians(yaw));
		q2.transformInverse(dir2_proj); //dir2_proj should never be rotated by roll, rotating by pitch should do nothing
	}
	
	public AxisPair(Vector3d a, Vector3d b, Vector3d b_proj) {
		dir1 = new Vector3d(a.x, a.y, a.z); //yaw and pitch
		dir2 = new Vector3d(b.x, b.y, b.z); //roll
		dir2_proj = new Vector3d(b_proj.x, b_proj.y, b_proj.z);
	}
	
	public void transform(Quaterniond q1, Quaterniond no_roll) {
		q1.transform(dir1);
		q1.transform(dir2);
		
		if (no_roll != null) no_roll.transformInverse(dir2_proj);
		
		if (Double.isNaN(dir1.x) || Double.isNaN(dir1.y) || Double.isNaN(dir1.z) || Double.isNaN(dir2.x) || Double.isNaN(dir2.y) || Double.isNaN(dir2.z)) {
			Bukkit.broadcastMessage("is nan"); //TODO remove
			dir1 = new Vector3d(0, 0, 1);
			dir2 = new Vector3d(1, 0, 0);
		}
	}
	
	
	public Vector getPitchYawRoll(DexBlock db) { //TODO remove param
//		Bukkit.broadcastMessage("dir2 = " + DexUtils.vectorString(DexUtils.vector(dir2), 4));
				
//		double yaw_rad = Math.acos(xz.z); //dot with [0, 0, 1] and get angle
//		if (xz.x > 0) yaw_rad = -yaw_rad; //atan2(x, z)
		
//		double pitch_rad = Math.acos(xz.dot(dir1));
//		if (dir1.y > 0) pitch_rad = -pitch_rad;
		
		double yaw_rad = -Math.atan2(dir1.x, dir1.z);
		double pitch_rad = -Math.asin(dir1.y);

		Vector3d xz2 = new Vector3d(dir2.x, dir2.y, dir2.z);
		xz2.y = 0;
		xz2.normalize();
//		Vector3d xz2 = dir2_proj;
		double roll_rad;
		if (xz2.x == 0 && xz2.y == 0 && xz2.z == 0) {
			roll_rad = dir2.y > 0 ? -90 : 90;
		} else {
			roll_rad = Math.acos(xz2.dot(dir2));
			
			Vector3d xz = new Vector3d(dir1.x, dir1.y, dir1.z);
			xz.y = 0; //projection onto xz plane
			xz.normalize();
			double cross_y = xz.z * xz2.x - xz.x * xz2.z;
			
//			Bukkit.broadcastMessage("cross product = " + DexUtils.vectorString(DexUtils.vector(cross), 4) + ", cross_y = " + cross_y);
			if (cross_y < 0) roll_rad = Math.PI - roll_rad;
			if (dir2.y < 0) roll_rad = -roll_rad;
//			roll_rad = roll_rad % PI2;
		}
		
		if (db != null) {
			db.getDexterityDisplay().getPlugin().getAPI().markerPoint(db.getLocation(), Color.BLACK, 10);
			db.getDexterityDisplay().getPlugin().getAPI().markerPoint(db.getLocation().add(DexUtils.vector(dir1)), Color.LIME, 10);
			db.getDexterityDisplay().getPlugin().getAPI().markerPoint(db.getLocation().add(DexUtils.vector(xz2)), Color.RED, 10);
			db.getDexterityDisplay().getPlugin().getAPI().markerPoint(db.getLocation().add(DexUtils.vector(dir2)), Color.ORANGE, 10);

		}
		
		if (!Double.isFinite(yaw_rad) || Double.isNaN(yaw_rad)) yaw_rad = 0;
		if (!Double.isFinite(pitch_rad) || Double.isNaN(pitch_rad)) pitch_rad = 0;
//		if (!Double.isFinite(roll_rad)) roll_rad = dir2.y > 0 ? -90 : 90;
		return new Vector((float) Math.toDegrees(pitch_rad), (float) Math.toDegrees(yaw_rad), (float) Math.toDegrees(roll_rad));
	}

}
