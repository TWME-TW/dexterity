package me.c7dev.dexterity.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import me.c7dev.dexterity.api.events.DisplayRotationEvent;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.transaction.RotationTransaction;
import me.c7dev.dexterity.util.AxisPair;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.QueuedRotation;
import me.c7dev.dexterity.util.RotationPlan;

public class DexRotation {
	
	private HashMap<Vector, Vector> dirs = new HashMap<>();
	private HashMap<Vector, AxisPair> axispairs = new HashMap<>();
	private DexterityDisplay d;
	private Vector3d x = new Vector3d(1, 0, 0), y = new Vector3d(0, 1, 0), z = new Vector3d(0, 0, 1);
	private QueuedRotation last = null;
	private double base_x = 0, base_y = 0, base_z = 0, base_pitch = 0, base_roll = 0, base_yaw = 0;
	private List<BlockDisplay> points = new ArrayList<>();
	private RotationTransaction t = null;
	private LinkedList<QueuedRotation> queue = new LinkedList<>();
	private boolean processing = false;
	
	public static final double cutoff = 0.000001;
	
	public DexRotation(DexterityDisplay d) {
		if (d == null) throw new IllegalArgumentException("Arguments cannot be null");
		this.d = d;
		refreshAxis();
	}
	
	public DexRotation(DexterityDisplay d, Vector x, Vector y, Vector z) {
		if (d == null || x == null || y == null || z == null) throw new IllegalArgumentException("Arguments cannot be null");
		if (!DexUtils.isOrthonormal(x, y, z)) throw new IllegalArgumentException("Axes are not orthnomormal!");
		this.d = d;
		this.x = DexUtils.vectord(x);
		this.y = DexUtils.vectord(y);
		this.z = DexUtils.vectord(z);
	}
	
	public DexRotation(DexterityDisplay d, double yaw, double pitch, double roll) {
		if (d == null) throw new IllegalArgumentException("Arguments cannot be null");
		this.d = d;
		base_yaw = yaw;
		base_pitch = pitch;
		base_roll = roll;
		Quaterniond s = new Quaterniond(0, 0, 0, 1);
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

	public void refreshAxis() {
		
		clearCached();
		
		//alternate way to check the block closest to 0
//		double min_yaw = Double.MAX_VALUE, yaw_val = 0, min_pitch = Double.MAX_VALUE, pitch_val = 0, min_roll = Double.MAX_VALUE, roll_val = 0;
//		for (DexBlock db : d.getBlocks()) {
//			double yaw = Math.abs(db.getEntity().getLocation().getYaw()), pitch = Math.abs(db.getEntity().getLocation().getPitch()), roll = Math.abs(db.getRoll());
//			if (yaw < min_yaw && pitch < min_pitch && roll < min_roll) {
//				min_yaw = yaw;
//				yaw_val = db.getEntity().getLocation().getYaw();
//				min_pitch = pitch;
//				pitch_val = -db.getEntity().getLocation().getPitch();
//				min_roll = roll;
//				roll_val = -db.getRoll();
//			}
//		}
		
		//Finds the mode of all three axes, rather than the closest to zero
		double yaw_mode = 0, pitch_mode = 0, roll_mode = 0;
		int count = 0;
		for (DexBlock db : d.getBlocks()) {
			double yaw = db.getEntity().getLocation().getYaw(), pitch = db.getEntity().getLocation().getPitch(), roll = db.getRoll();
			
			if (yaw == yaw_mode && pitch == pitch_mode && roll == roll_mode) count++;
			else {
				count--;
				if (count < 0) {
					yaw_mode = yaw;
					pitch_mode = pitch;
					roll_mode = roll;
					count = 0;
				}
			}
		}
		
		base_yaw = yaw_mode;
		base_pitch = pitch_mode;
		base_roll = roll_mode;
		
		Quaterniond s = new Quaterniond(0, 0, 0, 1);
		s.rotateZ(Math.toRadians(-base_roll));
		s.rotateX(Math.toRadians(-base_pitch));
		s.rotateY(Math.toRadians(base_yaw));
		
		x = new Vector3d(1, 0, 0);
		y = new Vector3d(0, 1, 0);
		z = new Vector3d(0, 0, 1);
		s.transformInverse(x);
		s.transformInverse(y);
		s.transformInverse(z);
		
	}
	
	public Vector getXAxis() {
		return DexUtils.vector(x);
	}
	public Vector getYAxis() {
		return DexUtils.vector(y);
	}
	public Vector getZAxis() {
		return DexUtils.vector(z);
	}
	
	public void setTransaction(RotationTransaction t2) { //async callback
		t = t2;
	}
	
	public void clearCached() {
		dirs.clear();
		axispairs.clear();
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
	
	public void setAxes(Vector x, Vector y, Vector z) {
		if (x == null || y == null || z == null) throw new IllegalArgumentException("Axes cannot be null!");
		if (!DexUtils.isOrthonormal(x, y, z)) throw new IllegalArgumentException("Axes are not orthonormal!");
		this.x = new Vector3d(x.getX(), x.getY(), x.getZ());
		this.y = new Vector3d(y.getX(), y.getY(), y.getZ());
		this.z = new Vector3d(z.getX(), z.getY(), z.getZ());
		clearCached();
	}
	
	public void setAxes(float yaw, float pitch, float roll) {
		Quaterniond s = new Quaterniond(0, 0, 0, 1);
		s.rotateZ(-Math.toRadians(yaw));
		s.rotateX(-Math.toRadians(pitch));
		s.rotateY(Math.toRadians(roll));
		
		base_yaw = yaw;
		base_pitch = pitch;
		base_roll = roll;
		
		x = new Vector3d(1, 0, 0);
		y = new Vector3d(0, 1, 0);
		z = new Vector3d(0, 0, 1);
		s.transformInverse(x);
		s.transformInverse(y);
		s.transformInverse(z);		
	}
	
	public void rotate(float y_deg) {
		rotate(y_deg, 0, 0);
	}
	
	public Quaterniond rotate(float y_deg, float pitch_deg, float roll_deg) {
		RotationPlan p = new RotationPlan();
		p.y_deg = y_deg;
		p.pitch_deg = pitch_deg;
		p.roll_deg = roll_deg;
		return rotate(p);
	}
	
	public Quaterniond rotate(RotationPlan plan) {
		
		double del_x, del_y, del_z, del_yaw, del_pitch, del_roll;
		if (plan.reset) {
			del_x = -plan.x_deg; del_y = -plan.y_deg; del_z = -plan.z_deg;
			del_pitch = plan.pitch_deg; del_yaw = plan.yaw_deg; del_roll = plan.roll_deg;
			base_y = 0; base_x = 0; base_z = 0;
			base_yaw = 0; base_pitch = 0; base_roll = 0;
		} else {
			del_x = plan.set_x ? -plan.x_deg - base_x : -plan.x_deg; //right hand rule
			del_y = plan.set_y ? -plan.y_deg - base_y : -plan.y_deg;
			del_z = plan.set_z ? -plan.z_deg - base_z : -plan.z_deg;
			del_yaw = plan.set_yaw ? plan.yaw_deg - base_yaw : plan.yaw_deg;
			del_pitch = plan.set_pitch ? plan.pitch_deg - base_pitch : plan.pitch_deg;
			del_roll = plan.set_roll ? plan.roll_deg - base_roll : plan.roll_deg;
			if (del_x == 0 && del_y == 0 && del_z == 0 && del_yaw == 0 && del_pitch == 0 && del_roll == 0) return null;
		}
						
		Quaterniond q = new Quaterniond(0, 0, 0, 1);
		if (plan.reset) q = resetQuaternion();
		if (del_z != 0) q = zQuaternion(Math.toRadians(del_z), q);
		if (del_roll != 0) q = rollQuaternion(Math.toRadians(del_roll), q);
		if (del_x != 0) q = xQuaternion(Math.toRadians(del_x), q);
		if (del_pitch != 0) q = pitchQuaternion(Math.toRadians(del_pitch), q);
		if (del_yaw != 0) q = yawQuaternion(Math.toRadians(del_yaw), q);
		if (del_y != 0) q = yQuaternion(Math.toRadians(del_y), q);
		
		Quaterniond q1 = new Quaterniond();
		q.invert(q1);
		
		rotate(q1, plan.async);
		
		base_y = (base_y + del_y) % 360;
		base_x = (base_x + del_x) % 360;
		base_z = (base_z + del_z) % 360;
		base_yaw = (base_yaw + del_yaw) % 360;
		base_pitch = (base_pitch + del_pitch) % 360;
		base_roll = (base_roll + del_roll) % 360;
		
		return DexUtils.cloneQ(q1);
	}
	
	public QueuedRotation prepareRotation(RotationPlan plan, RotationTransaction transaction) {
		double del_x, del_y, del_z, del_yaw, del_pitch, del_roll;
		if (plan.reset) {
			del_x = -plan.x_deg; del_y = -plan.y_deg; del_z = -plan.z_deg;
			del_pitch = plan.pitch_deg; del_yaw = plan.yaw_deg; del_roll = plan.roll_deg;
			base_y = 0; base_x = 0; base_z = 0;
			base_yaw = 0; base_pitch = 0; base_roll = 0;
		} else {
			del_x = plan.set_x ? -plan.x_deg - base_x : -plan.x_deg; //right hand rule
			del_y = plan.set_y ? -plan.y_deg - base_y : -plan.y_deg;
			del_z = plan.set_z ? -plan.z_deg - base_z : -plan.z_deg;
			del_yaw = plan.set_yaw ? plan.yaw_deg - base_yaw : plan.yaw_deg;
			del_pitch = plan.set_pitch ? plan.pitch_deg - base_pitch : plan.pitch_deg;
			del_roll = plan.set_roll ? plan.roll_deg - base_roll : plan.roll_deg;
			if (del_x == 0 && del_y == 0 && del_z == 0 && del_yaw == 0 && del_pitch == 0 && del_roll == 0) return null;
		}
						
		Quaterniond q = new Quaterniond(0, 0, 0, 1);
		if (plan.reset) q = resetQuaternion();
		if (del_z != 0) q = zQuaternion(Math.toRadians(del_z), q);
		if (del_roll != 0) q = rollQuaternion(Math.toRadians(del_roll), q);
		if (del_x != 0) q = xQuaternion(Math.toRadians(del_x), q);
		if (del_pitch != 0) q = pitchQuaternion(Math.toRadians(del_pitch), q);
		if (del_yaw != 0) q = yawQuaternion(Math.toRadians(del_yaw), q);
		if (del_y != 0) q = yQuaternion(Math.toRadians(del_y), q);
		
		Quaterniond q1 = new Quaterniond();
		q.invert(q1);
		
		return new QueuedRotation(q1, plan.async, transaction);
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
	private Quaterniond resetQuaternion() {
		Vector3d cross = new Vector3d(), cross2 = new Vector3d(),
				x_tgt = new Vector3d(1, 0, 0), y_tgt = new Vector3d(0, 1, 0);
		y_tgt.cross(y, cross);
		Quaterniond q_res_y = new Quaterniond(cross.x, cross.y, cross.z, 1 + y.y);
		q_res_y.transformInverse(x);
		q_res_y.transformInverse(y);
		q_res_y.transformInverse(z);
		
		x_tgt.cross(x, cross2);
		Quaterniond q_res_xz = new Quaterniond(cross2.x, cross2.y, cross2.z, 1 + x.x);
		q_res_xz.transformInverse(x);
		q_res_xz.transformInverse(y);
		q_res_xz.transformInverse(z);
		
		Quaterniond r = new Quaterniond(0, 0, 0, 1);
		return r.mul(q_res_y).mul(q_res_xz);
	}
	
	public void again() {
		rotate(last);
	}
	
	public void rotate(Quaterniond q1) {
		rotate(q1, true);
	}
	
	public void rotate(Quaterniond q1, boolean async) {
		if (q1 == null) throw new IllegalArgumentException("Quaternion cannot be null!");
		rotate(new QueuedRotation(q1, async, t));
	}
	
	public void rotate(QueuedRotation rotation) {
		if (rotation == null) throw new IllegalArgumentException("Rotation cannot be null!");
		queue.addLast(rotation);
		if (!processing) dequeue();
	}
	
	private void dequeue() {
		if (queue.size() == 0) {
			processing = false;
			t = null;
			return;
		}
		QueuedRotation r = queue.getFirst();
		queue.removeFirst();
		if (r.isAsync()) executeRotationAsync(r);
		else executeRotation(r);
	}
	
	public QueuedRotation getPreviousRotation() {
		return last;
	}
	
	//avg 0.00048400 ms per block :3
	private void executeRotation(QueuedRotation rot) {
		Quaterniond q1 = rot.getQuaternion();
		RotationTransaction trans = rot.getTransaction();
		if (d == null) throw new IllegalArgumentException("Quaternion cannot be null!");
		
		DisplayRotationEvent event = new DisplayRotationEvent(d, q1);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		
		if (!rot.equals(last)) dirs.clear(); //TODO only do this when different rotation
		
		Vector centerv = d.getCenter().toVector();
		processing = true;
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
		
		if (trans != null) trans.commit();
		
		last = rot;
		dequeue();
	}
	
	private void executeRotationAsync(QueuedRotation rot) {
		Quaterniond q1 = rot.getQuaternion();
		RotationTransaction trans = rot.getTransaction();
		if (q1 == null) throw new IllegalArgumentException("Quaternion cannot be null!");
		
		DisplayRotationEvent event = new DisplayRotationEvent(d, q1);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		
		if (!rot.equals(last)) dirs.clear();
		
		processing = true;
		Vector centerv = d.getCenter().toVector();
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
						
						if (trans != null) trans.commit();
						
						last = rot;
						dequeue();
						
					}
				}.runTask(d.getPlugin());
				
			}
		}.runTaskAsynchronously(d.getPlugin());
	}
	
	public void highlightAxes(int seconds) {
		for (BlockDisplay b : points) {
			b.remove();
		}
		points.clear();
		
		points.add(d.getPlugin().api().markerPoint(d.getCenter().add(DexUtils.vector(x)), Color.RED, seconds));
		points.add(d.getPlugin().api().markerPoint(d.getCenter().add(DexUtils.vector(y)), Color.LIME, seconds));
		points.add(d.getPlugin().api().markerPoint(d.getCenter().add(DexUtils.vector(z)), Color.BLUE, seconds));
		points.add(d.getPlugin().api().markerPoint(d.getCenter(), Color.SILVER, seconds));
	}

}
