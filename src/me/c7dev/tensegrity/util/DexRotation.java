package me.c7dev.tensegrity.util;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3f;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class DexRotation {
	
	private HashMap<Double, Matrix3d> rotmats_d = new HashMap<>();
	private HashMap<Vector, Matrix3d> rotmats_v = new HashMap<>();
	private HashMap<Vector, Vector> dirs = new HashMap<>();
	private Location center;
	private DexterityDisplay d;
	private Vector3d x = new Vector3d(1, 0, 0), y = new Vector3d(0, 1, 0), z = new Vector3d(0, 0, 1);
	private Quaterniond q1 = new Quaterniond(0, 0, 0, 1);
	private double base_yaw = 0, base_pitch = 0, base_roll = 0, base_rel_yaw = 0;
	
	public static final double cutoff = 0.0001;
	
	public DexRotation(DexterityDisplay d) {
		this.d = d;
	}
	
	public DexRotation(DexterityDisplay d, Vector x, Vector y, Vector z) {
		this(d);
		this.x = DexUtils.vectord(x);
		this.y = DexUtils.vectord(y);
		this.z = DexUtils.vectord(z);
	}
	
	public double getYaw() {
		return base_yaw;
	}
	public double getPitch() {
		return base_pitch;
	}
	public double getRoll() {
		return base_roll;
	}
	
	public void rotate(float yaw_deg, float pitch_deg, float roll_deg) {
		rotate(yaw_deg, pitch_deg, roll_deg, false, false, false);
	}
	
	public void rotate(float yaw_deg, float pitch_deg, float roll_deg, boolean set_yaw, boolean set_pitch, boolean set_roll) {
							
		double del_yaw = set_yaw ? yaw_deg - base_yaw : yaw_deg,
				del_pitch = set_pitch ? pitch_deg - base_pitch : pitch_deg,
				del_roll = set_roll ? roll_deg - base_roll : roll_deg;
		
		if (del_yaw == 0 && del_pitch == 0 && del_roll == 0) return;
				
		Quaterniond q = new Quaterniond(0, 0, 0, 1);
		if (del_yaw != 0) q = yawQuaternion(Math.toRadians(del_yaw), q);
		if (del_pitch != 0) q = pitchQuaternion(Math.toRadians(del_pitch), q);
		if (del_roll != 0) q = rollQuaternion(Math.toRadians(del_roll), q);
		
		q1 = new Quaterniond();
		q.invert(q1);
		
		executeRot(q1);
		
//		d.getPlugin().getAPI().markerPoint(d.getCenter().add(DexUtils.vector(x)), Color.RED, 10);
//		d.getPlugin().getAPI().markerPoint(d.getCenter().add(DexUtils.vector(y)), Color.LIME, 10);
//		d.getPlugin().getAPI().markerPoint(d.getCenter().add(DexUtils.vector(z)), Color.BLUE, 10);
//		d.getPlugin().getAPI().markerPoint(d.getCenter(), Color.BLACK, 6);
		
		base_yaw += del_yaw;
		base_pitch += del_pitch;
		base_roll += del_roll;
	}
	
	private Quaterniond yawQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_yaw = new Quaterniond(0, sintheta, 0, costheta);
		q_yaw.transformInverse(x);
		q_yaw.transformInverse(z);
		return src.mul(q_yaw);
	}
	private Quaterniond relYawQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_yaw = new Quaterniond(sintheta*y.x, sintheta*y.y, sintheta*y.z, costheta);
		q_yaw.transformInverse(x);
		q_yaw.transformInverse(z);
		return src.mul(q_yaw);
	}
	private Quaterniond pitchQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_pitch = new Quaterniond(sintheta*x.x, sintheta*x.y, sintheta*x.z, costheta);
		q_pitch.transformInverse(z);
		q_pitch.transformInverse(y);
		return src.mul(q_pitch);
	}
	private Quaterniond rollQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_roll = new Quaterniond(sintheta*z.x, sintheta*z.y, sintheta*z.z, costheta);
		q_roll.transformInverse(x);
		q_roll.transformInverse(y);
		return src.mul(q_roll);
	}
	
	private void executeRot(Quaterniond q1) {
//		Quaterniond q1 = new Quaterniond();
//		q.invert(q1);
		
		dirs.clear();
		
		Vector centerv = d.getCenter().toVector();
		for (DexBlock db : d.getBlocks()) {
			Vector key = new Vector(db.getEntity().getLocation().getPitch(), db.getEntity().getLocation().getYaw(), db.getRoll());
			Vector dir = dirs.get(key);
			if (dir == null) {
				AxisPair a = new AxisPair(db); //TODO make another map <dir, AxisPair> to get this for animations
				a.transform(q1);
				dir = a.getPitchYawRoll();
				dirs.put(key, dir);
			}
			
			Vector r = db.getLocation().toVector().subtract(centerv);
			Vector3d r_trans = DexUtils.vectord(r);
			q1.transform(r_trans);
			
			Vector offset = DexUtils.vector(r_trans).subtract(r);
			db.move(offset);
			db.getEntity().setRotation((float) dir.getY(), (float) dir.getX());
			db.setRoll((float) dir.getZ());
		}
	}
	
	private void rotateAll(float yaw_deg, float pitch_deg, float roll_deg, boolean set_yaw, boolean set_pitch, boolean set_roll) {
		Bukkit.broadcastMessage("rotate ALL");
		
		if (d.isZeroPitch() && (pitch_deg != 0 || roll_deg != 0)) d.setZeroPitch(false);
		float base_yaw = (float) d.getYaw(),  base_pitch = (float) d.getPitch(), base_roll = (float) d.getRoll();
		
		final Vector centerv = center.toVector();
		double yaw = Math.toRadians(yaw_deg), roll = Math.toRadians(roll_deg), pitch = Math.toRadians(pitch_deg);
		float basePitch = (float) Math.toRadians(base_pitch), baseRoll = (float) Math.toRadians(base_roll);
		
		Bukkit.broadcastMessage("pitch = " + Math.toDegrees(pitch) + ", yaw = "  +Math.toDegrees(yaw) + ", roll=" + Math.toDegrees(roll));
		Matrix3d rotmat = DexUtils.rotMat(pitch, yaw, roll);
		for (DexBlock b : d.getBlocks()) {
			float oldPitchDeg = b.getEntity().getLocation().getPitch(), oldYawDeg = b.getEntity().getLocation().getYaw(), oldRollDeg = b.getRoll();
			float oldPitch = (float) Math.toRadians(oldPitchDeg), oldYaw = (float) Math.toRadians(oldYawDeg);
			
			//double pitch = Math.toRadians(pitch_deg - (set_pitch ? oldPitchDeg : 0)); //-oldPitch
			double yaw2 = Math.toRadians(yaw_deg - (set_yaw ? oldYawDeg : 0));
			double oldRoll = Math.toRadians(oldRollDeg);
						
			//double deltaYaw = yaw + (set_yaw ? -oldYaw : oldYaw);
			//double deltaYaw = set_yaw ? yaw + oldYaw - baseYaw : yaw + oldYaw,
			double deltaPitch = set_pitch ? pitch + oldPitch - basePitch : pitch + oldPitch,
					deltaRoll = set_roll ? roll + oldRoll - baseRoll : roll + oldRoll;

//			if (pitch == 0 && roll == 0 && Math.abs(deltaYaw - oldYaw) < cutoff) return;
			
//			Matrix3d rotmat;
			Vector key = new Vector(oldPitchDeg, oldYawDeg, 0);
//			if (rotmats_v.containsKey(key)) {
//				rotmat = rotmats_v.get(key);
//			} else {
////				Matrix3d undoRollMat = new Matrix3d(
////						Math.cos(oldRoll), -Math.sin(oldRoll), 0,
////						Math.sin(oldRoll), Math.cos(oldRoll), 0,
////						0, 0, 1);
//				Matrix3d undoYawMat = new Matrix3d(
//						Math.cos(oldYaw), 0f, -Math.sin(oldYaw),
//						0f, 1f, 0f,
//						Math.sin(oldYaw), 0f, Math.cos(oldYaw));
//				Matrix3d undoPitchMat = new Matrix3d(
//						1, 0, 0,
//						0, Math.cos(oldPitch), Math.sin(oldPitch),
//						0, -Math.sin(oldPitch), Math.cos(oldPitch));
//				Matrix3d applyrot = DexUtils.rotMat(deltaPitch, yaw2, deltaRoll);
//
//				//rotmat = applyrot.mul(undoYawMat).mul(undoRollMat);
//				rotmat = applyrot.mul(undoPitchMat).mul(undoYawMat);
//				rotmats_v.put(key, rotmat);
//				
//			}
			
			Vector3f oldOffset = DexUtils.vector(b.getEntity().getLocation().toVector().subtract(centerv));
			Vector3f newOffset = new Vector3f();
			rotmat.transform(oldOffset, newOffset);

			Location loc = DexUtils.location(center.getWorld(), centerv.clone().add(DexUtils.vector(newOffset)));
			loc.setYaw(set_yaw ? oldYawDeg - base_yaw + yaw_deg : oldYawDeg + yaw_deg);
			loc.setPitch(pitch_deg + (set_pitch ? 0 : oldPitchDeg));
			b.teleport(loc);
			b.setRoll(set_roll ? roll_deg : b.getRoll() + roll_deg);
		}
		
		if (pitch_deg == 0 && set_pitch && roll_deg == 0 && set_roll && rotmats_v.size() == 1) {
			d.setZeroPitch(true);
		}
//		d.setBaseRotation(set_yaw ? yaw_deg : yaw_deg + base_yaw, set_pitch ? pitch_deg : pitch_deg + base_pitch, set_roll ? roll_deg : roll_deg + base_roll);
	}
	
	private void rotateYawPitch(float yaw_deg, float pitch_deg, boolean set_yaw, boolean set_pitch) {
		
//		if (d.getBlocks().size() == 0) return;
//		
//		if (Math.abs(yaw_deg) < cutoff && !set_yaw && Math.abs(pitch_deg) < cutoff && !set_pitch) return;
		
		Bukkit.broadcastMessage("rotate YAW, PITCH");
		if (pitch_deg == 0 && d.isZeroPitch()) {
			rotateYaw(yaw_deg, set_yaw);
			return;
		}
		if (d.isZeroPitch() && pitch_deg != 0) d.setZeroPitch(false);
		float base_yaw = (float) d.getYaw(), base_pitch = (float) d.getPitch(), base_roll = (float) d.getRoll();
				
		final Vector centerv = center.toVector();
		double yaw = Math.toRadians(yaw_deg);
//		float baseYaw = (float) Math.toRadians(base_yaw);
		for (DexBlock b : d.getBlocks()) {
			float oldPitchDeg = b.getEntity().getLocation().getPitch(), oldYawDeg = b.getEntity().getLocation().getYaw();
			float oldYaw = (float) Math.toRadians(oldYawDeg);
			double pitch = Math.toRadians(pitch_deg - (set_pitch ? oldPitchDeg : 0)); //-oldPitch
			
			double deltaYaw = yaw + (set_yaw ? -oldYaw : oldYaw);
//			double deltaYaw = set_yaw ? yaw + oldYaw - baseYaw : yaw + oldYaw;

			if (pitch == 0 && Math.abs(deltaYaw - oldYaw) < cutoff) continue;
			
			Matrix3d rotmat;
			Vector key = new Vector(oldPitchDeg, oldYawDeg, 0);
			if (rotmats_v.containsKey(key)) {
				rotmat = rotmats_v.get(key);
			} else {
				Matrix3d undoYawMat = new Matrix3d(
						Math.cos(oldYaw), 0f, -Math.sin(oldYaw),
						0f, 1f, 0f,
						Math.sin(oldYaw), 0f, Math.cos(oldYaw));
				Matrix3d applyrot = DexUtils.rotMat(pitch, deltaYaw, 0);

				rotmat = applyrot.mul(undoYawMat);
				rotmats_v.put(key, rotmat);
				
			}

			Vector3f oldOffset = DexUtils.vector(b.getEntity().getLocation().toVector().subtract(centerv));
			Vector3f newOffset = new Vector3f();
			rotmat.transform(oldOffset, newOffset);

			Location loc = DexUtils.location(center.getWorld(), centerv.clone().add(DexUtils.vector(newOffset)));
			loc.setYaw(set_yaw ? oldYawDeg - base_yaw + yaw_deg : oldYawDeg + yaw_deg);
			loc.setPitch(pitch_deg + (set_pitch ? 0 : oldPitchDeg));
			b.teleport(loc);
		}
		
		if (pitch_deg == 0 && set_pitch && rotmats_v.size() == 1) {
			d.setZeroPitch(true);
		}
//		d.setBaseRotation(set_yaw ? yaw_deg : yaw_deg + base_yaw, set_pitch ? pitch_deg : pitch_deg + base_pitch, base_roll);
	}
	
	//faster function for the case where pitch == 0
	public void rotateYaw(float yaw_deg, boolean set) {
		Bukkit.broadcastMessage("using yaw shortcut");
		if (!d.isZeroPitch() || (!set && Math.abs(yaw_deg) <= cutoff)) return;
		float base_yaw = (float) d.getYaw(),  base_pitch = (float) d.getPitch(), base_roll = (float) d.getRoll();
		
		final Vector centerv = center.toVector();
		double yaw = Math.toRadians(yaw_deg);
		for (DexBlock b : d.getBlocks()) {
			float oldYawDeg = b.getEntity().getLocation().getYaw();

			double deltaYaw = (set ? yaw - Math.toRadians(oldYawDeg) : yaw);
			if (Math.abs(deltaYaw) < cutoff) continue;
			Matrix3d rotmat;
			if (rotmats_d.containsKey(deltaYaw)) rotmat = rotmats_d.get(deltaYaw);
			else {
				rotmat = new Matrix3d(
						Math.cos(deltaYaw), 0f, Math.sin(deltaYaw),
						0f, 1f, 0f,
						-Math.sin(deltaYaw), 0f, Math.cos(deltaYaw));
				rotmats_d.put(deltaYaw, rotmat);
			}
			Vector3f oldOffset = DexUtils.vector(b.getEntity().getLocation().toVector().subtract(centerv));
			Vector3f newOffset = new Vector3f();
			rotmat.transform(oldOffset, newOffset);

			Location loc = DexUtils.location(center.getWorld(), centerv.clone().add(DexUtils.vector(newOffset)));
			loc.setYaw(set ? yaw_deg : yaw_deg + oldYawDeg);
			loc.setPitch(b.getEntity().getLocation().getPitch());
			b.teleport(loc);
		}
//		d.setBaseRotation(set ? yaw_deg : base_yaw + yaw_deg, base_pitch, base_roll);
	}

}
