package me.c7dev.tensegrity.util;

import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class AxisPair {
	
	private Vector3d dir1, dir2;
	
	public static double PI2 = Math.PI/2;
	
	public AxisPair() {
		dir1 = new Vector3d(0, 0, 1);
		dir2 = new Vector3d(1, 0, 0);
	}
	
	public AxisPair(DexBlock db) {
		this();
		Quaterniond q = new Quaterniond();
		q.rotateZ(-Math.toRadians(db.getRoll()));
		q.rotateX(-Math.toRadians(db.getEntity().getLocation().getPitch()));
		q.rotateY(Math.toRadians(db.getEntity().getLocation().getYaw()));
		q.transformInverse(dir1);
		q.transformInverse(dir2);
	}

	
	public void transform(Quaterniond q1) {
		q1.transform(dir1);
		q1.transform(dir2);
	}
	
	
	public Vector getPitchYawRoll() {
		
//		if (res_set) return res.clone();
		
		double yaw_rad = -Math.atan2(dir1.x, dir1.z); //res.y
		double pitch_rad = -Math.asin(dir1.y);

//		Vector3d xz2 = new Vector3d(dir2.x, dir2.y, dir2.z);
//		xz2.y = 0;
//		xz2.normalize();
//		if (xz2.x == 0 && xz2.y == 0 && xz2.z == 0) {
//			roll_rad = dir2.y > 0 ? -90 : 90;
		Vector3d dir2_proj = new Vector3d(Math.cos(yaw_rad), 0, Math.sin(yaw_rad));
		double roll_rad = Math.acos(dir2.dot(dir2_proj));
		if (Double.isNaN(roll_rad) || !Double.isFinite(roll_rad)) roll_rad = 0;

		//			double cross_y = dir1.z * dir2_proj.x - dir1.x * dir2_proj.z;

		if (dir2.y < 0) roll_rad = -roll_rad;
		
//		if (db != null) {
//			db.getDexterityDisplay().getPlugin().getAPI().markerPoint(db.getLocation(), Color.BLACK, 10);
//			db.getDexterityDisplay().getPlugin().getAPI().markerPoint(db.getLocation().add(DexUtils.vector(dir1)), Color.LIME, 10);
//			db.getDexterityDisplay().getPlugin().getAPI().markerPoint(db.getLocation().add(DexUtils.vector(dir2_proj)), Color.RED, 10);
//			db.getDexterityDisplay().getPlugin().getAPI().markerPoint(db.getLocation().add(DexUtils.vector(dir2)), Color.ORANGE, 10);
//		}
		
		if (Double.isNaN(yaw_rad)) yaw_rad = 0;
		if (Double.isNaN(pitch_rad)) pitch_rad = PI2;
//		if (!Double.isFinite(roll_rad)) roll_rad = dir2.y > 0 ? -90 : 90;
		return new Vector((float) Math.toDegrees(pitch_rad), (float) Math.toDegrees(yaw_rad), (float) Math.toDegrees(roll_rad));
//		res_set = true;
//		return res.clone();
	}

}
