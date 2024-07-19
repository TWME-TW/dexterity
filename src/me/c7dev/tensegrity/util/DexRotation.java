package me.c7dev.tensegrity.util;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
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
	private HashMap<Vector, AxisPair> axispairs = new HashMap<>();
	private Location center;
	private DexterityDisplay d;
	private Vector3d x = new Vector3d(1, 0, 0), y = new Vector3d(0, 1, 0), z = new Vector3d(0, 0, 1);
	private Quaterniond q1 = new Quaterniond(0, 0, 0, 1);
	private double base_x = 0, base_y = 0, base_z = 0, base_pitch = 0, base_roll = 0, base_yaw = 0;
	
	public static final double cutoff = 0.000001;
	
	public DexRotation(DexterityDisplay d) {
		this.d = d;
		refreshAxis();
	}
	
	public DexRotation(DexterityDisplay d, Vector x, Vector y, Vector z) {
		this.d = d;
		
		//confirm orthonormality
		if (x.dot(y) < cutoff && y.dot(z) < cutoff && z.dot(x) < cutoff && x.length() - 1 < cutoff && y.length() < cutoff && z.length() < cutoff) {
			this.x = DexUtils.vectord(x);
			this.y = DexUtils.vectord(y);
			this.z = DexUtils.vectord(z);
		} else refreshAxis();
	}

	public void refreshAxis() {
		
		double min_yaw = Double.MAX_VALUE, yaw_val = 0, min_pitch = Double.MAX_VALUE, pitch_val = 0, min_roll = Double.MAX_VALUE, roll_val = 0;
		for (DexBlock db : d.getBlocks()) {
			double yaw = Math.abs(db.getLocation().getYaw()), pitch = Math.abs(db.getLocation().getPitch()), roll = Math.abs(db.getRoll());
			if (yaw < min_yaw) {
				min_yaw = yaw;
				yaw_val = db.getLocation().getYaw();
			}
			if (pitch < min_pitch) {
				min_pitch = pitch;
				pitch_val = db.getLocation().getPitch();
			}
			if (roll < min_roll) {
				min_roll = roll;
				roll_val = db.getRoll();
			}
		}
		
		base_yaw = yaw_val;
		base_pitch = pitch_val;
		base_roll = roll_val;
		
		Bukkit.broadcastMessage("yaw = " + yaw_val + ", pitch = " + pitch_val + ", roll = " + roll_val);
		
		Quaterniond s = new Quaterniond();
		s.rotateZ(-Math.toRadians(base_roll));
		s.rotateX(-Math.toRadians(base_pitch));
		s.rotateY(Math.toRadians(base_yaw));
		
		x = new Vector3d(1, 0, 0);
		y = new Vector3d(0, 1, 0);
		z = new Vector3d(0, 0, 1);
		s.transformInverse(x);
		s.transformInverse(y);
		s.transformInverse(z);
		
	}
	
	public double getX() {
		return base_x;
	}
	public double getY() {
		return base_y;
	}
	public double getZ() {
		return base_z;
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
		RotationPlan p = new RotationPlan();
		p.yaw_deg = yaw_deg;
		p.pitch_deg = pitch_deg;
		p.roll_deg = roll_deg;
		rotate(p);
	}
	
	public void rotate(RotationPlan plan) {
		
		double del_x = plan.set_x ? plan.x_deg - base_x : plan.x_deg,
				del_y = plan.set_y ? plan.y_deg - base_y : plan.y_deg,
				del_z = plan.set_z ? plan.z_deg - base_z : plan.z_deg,
				del_yaw = plan.set_yaw ? plan.yaw_deg - base_yaw : plan.yaw_deg,
				del_pitch = plan.set_pitch ? plan.pitch_deg - base_pitch : plan.pitch_deg,
				del_roll = plan.set_roll ? plan.roll_deg - base_roll : plan.roll_deg;
		
		if (del_x == 0 && del_y == 0 && del_z == 0 && del_yaw == 0 && del_pitch == 0 && del_roll == 0 && !plan.reset) return;
				
		Quaterniond q = new Quaterniond(0, 0, 0, 1);
//		q_noroll = null;
		if (plan.reset) q = resetQuaternion(q);
		if (del_z != 0) q = zQuaternion(Math.toRadians(del_z), q);
		if (del_roll != 0) q = rollQuaternion(Math.toRadians(del_roll), q);
		if (del_x != 0) q = xQuaternion(Math.toRadians(del_x), q);
		if (del_pitch != 0) q = pitchQuaternion(Math.toRadians(del_pitch), q);
		if (del_yaw != 0) q = yawQuaternion(Math.toRadians(del_yaw), q);
		if (del_y != 0) q = yQuaternion(Math.toRadians(del_y), q);
		
		q1 = new Quaterniond();
		q.invert(q1);
				
		if (plan.async) rotateAsync(q1, d.getCenter());
		else rotate(q1, d.getCenter());
		
//		d.getPlugin().getAPI().markerPoint(d.getCenter().add(DexUtils.vector(x)), Color.RED, 10);
//		d.getPlugin().getAPI().markerPoint(d.getCenter().add(DexUtils.vector(y)), Color.LIME, 10);
//		d.getPlugin().getAPI().markerPoint(d.getCenter().add(DexUtils.vector(z)), Color.BLUE, 10);
//		d.getPlugin().getAPI().markerPoint(d.getCenter(), Color.BLACK, 6);
		
		base_y = (base_y + del_y) % 360;
		base_x = (base_x + del_x) % 360;
		base_z = (base_z + del_z) % 360;
		base_yaw = (base_yaw + del_yaw) % 360;
		base_pitch = (base_pitch + del_pitch) % 360;
		base_roll = (base_roll + del_roll) % 360;
	}
	
	private Quaterniond yQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_y = new Quaterniond(0, sintheta, 0, costheta);
		q_y.transformInverse(x);
		q_y.transformInverse(y);
		q_y.transformInverse(z);
		return src.mul(q_y);
	}
	private Quaterniond yawQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_yaw = new Quaterniond(sintheta*y.x, sintheta*y.y, sintheta*y.z, costheta);
		q_yaw.transformInverse(x);
		q_yaw.transformInverse(y);
		q_yaw.transformInverse(z);
		return src.mul(q_yaw);
	}
	private Quaterniond xQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_x = new Quaterniond(sintheta, 0, 0, costheta);
		q_x.transformInverse(x);
		q_x.transformInverse(y);
		q_x.transformInverse(z);
		return src.mul(q_x);
	}
	private Quaterniond pitchQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_pitch = new Quaterniond(sintheta*x.x, sintheta*x.y, sintheta*x.z, costheta);
		q_pitch.transformInverse(x);
		q_pitch.transformInverse(y);
		q_pitch.transformInverse(z);
		return src.mul(q_pitch);
	}
	private Quaterniond zQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_z = new Quaterniond(0, 0, sintheta, costheta);
		q_z.transformInverse(x);
		q_z.transformInverse(y);
		q_z.transformInverse(z);
		return src.mul(q_z);
	}
	private Quaterniond rollQuaternion(double rads, Quaterniond src) {
		double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads/2);
		Quaterniond q_roll = new Quaterniond(sintheta*z.x, sintheta*z.y, sintheta*z.z, costheta);
		q_roll.transformInverse(x);
		q_roll.transformInverse(y);
		q_roll.transformInverse(z);
		return src.mul(q_roll);
	}
	private Quaterniond resetQuaternion(Quaterniond src) {
		Vector3d cross = new Vector3d();
		y.cross(new Vector3d(0, 1, 0), cross);
		Quaterniond q_res = new Quaterniond(cross.x, cross.y, cross.z, y.y);
		q_res.transformInverse(x);
		q_res.transformInverse(y);
		q_res.transformInverse(z);
		return src.mul(q_res);
	}
	
	public void again() {
		rotate(q1, d.getCenter());
	}
	
	public void rotate(Quaterniond q1) {
		rotate(q1, d.getCenter());
	}
	
	//avg 0.00048400 ms per block :D
	public void rotate(Quaterniond q1, Location center) {
		if (d == null) return;
		
		dirs.clear();
		
		Vector centerv = center.toVector();
		for (DexBlock db : d.getBlocks()) {
			Vector key = new Vector(db.getEntity().getLocation().getPitch(), db.getEntity().getLocation().getYaw(), db.getRoll());
			Vector dir = dirs.get(key);
			if (dir == null) {
				AxisPair a = axispairs.get(key);
				if (a == null) a = new AxisPair(db);
				
				a.transform(q1);
				dir = a.getPitchYawRoll();
				dirs.put(key, dir);
				axispairs.put(dir, a);
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
	
	public void againAsync() {
		rotateAsync(q1, d.getCenter());
	}
	
	public void rotateAsync(Quaterniond q1) {
		rotateAsync(q1, d.getCenter());
	}
	
	public void rotateAsync(Quaterniond q1, Location center) {
		if (d == null) return;
		
		dirs.clear();
		
		Vector centerv = center.toVector();
		new BukkitRunnable() {
			@Override
			public void run() {
				
				HashMap<UUID, Vector> offsets = new HashMap<>(), rots = new HashMap<>();
				
				for (DexBlock db : d.getBlocks()) { //mapping
					Vector key = new Vector(db.getEntity().getLocation().getPitch(), db.getEntity().getLocation().getYaw(), db.getRoll());
					Vector dir = dirs.get(key);
					if (dir == null) {
						AxisPair a = axispairs.get(key);
						if (a == null) a = new AxisPair(db);
						
						a.transform(q1);
						dir = a.getPitchYawRoll();
						dirs.put(key, dir);
						axispairs.put(dir, a);
					}
					
					Vector r = db.getLocation().toVector().subtract(centerv);
					Vector3d r_trans = DexUtils.vectord(r);
					q1.transform(r_trans);
					Vector offset = DexUtils.vector(r_trans).subtract(r);
					
					offsets.put(db.getEntity().getUniqueId(), offset);
					rots.put(db.getEntity().getUniqueId(), dir);
					
				}
				
				new BukkitRunnable() {
					@Override
					public void run() {
						for (DexBlock db : d.getBlocks()) {
							
							Vector offset = offsets.get(db.getEntity().getUniqueId()), dir = rots.get(db.getEntity().getUniqueId());
							if (offset == null || dir == null) continue;
							
							db.move(offset);
							db.getEntity().setRotation((float) dir.getY(), (float) dir.getX());
							db.setRoll((float) dir.getZ());
						}
					}
				}.runTask(d.getPlugin());
				
			}
		}.runTaskAsynchronously(d.getPlugin());
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
