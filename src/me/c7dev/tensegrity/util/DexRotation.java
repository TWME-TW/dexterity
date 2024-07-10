package me.c7dev.tensegrity.util;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Vector3f;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class DexRotation {
	
	private HashMap<Double, Matrix3d> rotmats_d = new HashMap<>();
	private HashMap<Vector, Matrix3d> rotmats_v = new HashMap<>();
	private List<DexBlock> blocks;
	private Location center;
	private DexterityDisplay d;
	
	public static final double cutoff = 0.0001;
	
	public DexRotation(DexterityDisplay d) {
		blocks = d.getBlocks();
		this.d = d;
		center = d.getCenter();
	}
	
	public void rotate(float yaw_deg, float pitch_deg, float roll_deg) {
		rotate(yaw_deg, pitch_deg, roll_deg, false, false, false);
	}
	
	public void rotate(float yaw_deg, float pitch_deg, float roll_deg, boolean set_yaw, boolean set_pitch, boolean set_roll) {
		if (blocks.size() == 0) return;
	
		float base_yaw = d.getYaw(),  base_pitch = d.getPitch(), base_roll = d.getRoll();

		boolean affect_pitch = (set_pitch && Math.abs(pitch_deg - base_pitch) > cutoff) || (!set_pitch && Math.abs(pitch_deg) > cutoff);
		boolean affect_yaw = (set_yaw && Math.abs(yaw_deg - base_yaw) > cutoff) || (!set_yaw && Math.abs(yaw_deg) > cutoff);
		boolean affect_roll = (set_roll && Math.abs(roll_deg - base_roll) > cutoff) || (!set_roll && Math.abs(roll_deg) > cutoff);
	
		Bukkit.broadcastMessage("yaw = " + affect_yaw + ", pitch = " + affect_pitch + ", roll = " + affect_roll);
		
		if (affect_pitch || affect_roll || !d.isZeroPitch()) {
			if (affect_roll) rotateAll(yaw_deg, pitch_deg, roll_deg, set_yaw, set_pitch, set_roll);
			else rotateYawPitch(yaw_deg, pitch_deg, set_yaw, set_pitch);
		}
		else if (affect_yaw) rotateYaw(yaw_deg, set_yaw);
		else Bukkit.broadcastMessage("NONE");
	}
	
	private void rotateAll(float yaw_deg, float pitch_deg, float roll_deg, boolean set_yaw, boolean set_pitch, boolean set_roll) {
		Bukkit.broadcastMessage("rotate ALL");
		
		if (d.isZeroPitch() && (pitch_deg != 0 || roll_deg != 0)) d.setZeroPitch(false);
		float base_yaw = d.getYaw(),  base_pitch = d.getPitch(), base_roll = d.getRoll();
		
		final Vector centerv = center.toVector();
		double yaw = Math.toRadians(yaw_deg);
		float baseYaw = (float) Math.toRadians(base_yaw);
		for (DexBlock b : blocks) {
			float oldPitchDeg = b.getEntity().getLocation().getPitch(), oldYawDeg = b.getEntity().getLocation().getYaw(), oldRollDeg = b.getRoll();
			float oldYaw = (float) Math.toRadians(oldYawDeg);
			double pitch = Math.toRadians(pitch_deg - (set_pitch ? oldPitchDeg : 0)); //-oldPitch
			double roll = Math.toRadians(roll_deg - (set_roll ? oldRollDeg : 0));
			
			//double deltaYaw = yaw + (set_yaw ? -oldYaw : oldYaw);
			double deltaYaw = set_yaw ? yaw + oldYaw - baseYaw : yaw + oldYaw;

//			if (pitch == 0 && roll == 0 && Math.abs(deltaYaw - oldYaw) < cutoff) return;
			
			Matrix3d rotmat;
			Vector key = new Vector(oldPitchDeg, oldYawDeg, 0);
			if (rotmats_v.containsKey(key)) {
				rotmat = rotmats_v.get(key);
			} else {
				Matrix3d undoYawMat = new Matrix3d(
						Math.cos(oldYaw), 0f, -Math.sin(oldYaw),
						0f, 1f, 0f,
						Math.sin(oldYaw), 0f, Math.cos(oldYaw));
				Matrix3d applyrot = DexUtils.rotMat(pitch, deltaYaw, roll);
				Bukkit.broadcastMessage("roll = " + Math.toDegrees(roll));

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
			b.setRoll(set_roll ? roll_deg : b.getRoll() + roll_deg);
		}
		
		if (pitch_deg == 0 && set_pitch && roll_deg == 0 && set_roll && rotmats_v.size() == 1) {
			d.setZeroPitch(true);
		}
		d.setBaseRotation(set_yaw ? yaw_deg : yaw_deg + base_yaw, set_pitch ? pitch_deg : pitch_deg + base_pitch, set_roll ? roll_deg : roll_deg + base_roll);
	}
	
	private void rotateYawPitch(float yaw_deg, float pitch_deg, boolean set_yaw, boolean set_pitch) {
		
//		if (blocks.size() == 0) return;
//		
//		if (Math.abs(yaw_deg) < cutoff && !set_yaw && Math.abs(pitch_deg) < cutoff && !set_pitch) return;
		
		Bukkit.broadcastMessage("rotate YAW, PITCH");
		if (pitch_deg == 0 && d.isZeroPitch()) {
			rotateYaw(yaw_deg, set_yaw);
			return;
		}
		if (d.isZeroPitch() && pitch_deg != 0) d.setZeroPitch(false);
		float base_yaw = d.getYaw(),  base_pitch = d.getPitch(), base_roll = d.getRoll();
				
		final Vector centerv = center.toVector();
		double yaw = Math.toRadians(yaw_deg);
//		float baseYaw = (float) Math.toRadians(base_yaw);
		for (DexBlock b : blocks) {
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
		d.setBaseRotation(set_yaw ? yaw_deg : yaw_deg + base_yaw, set_pitch ? pitch_deg : pitch_deg + base_pitch, base_roll);
	}
	
	//faster function for the case where pitch == 0
	public void rotateYaw(float yaw_deg, boolean set) {
		Bukkit.broadcastMessage("using yaw shortcut");
		if (!d.isZeroPitch() || (!set && Math.abs(yaw_deg) <= cutoff)) return;
		float base_yaw = d.getYaw(),  base_pitch = d.getPitch(), base_roll = d.getRoll();
		
		final Vector centerv = center.toVector();
		double yaw = Math.toRadians(yaw_deg);
		for (DexBlock b : blocks) {
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
		d.setBaseRotation(set ? yaw_deg : base_yaw + yaw_deg, base_pitch, base_roll);
	}

}
