package me.c7dev.dexterity.util;

import org.joml.Quaternionf;

public class OrientationKey {
	
	private Quaternionf q;
	private double x, y, z;
	
	public static final double epsilon = 0.000001;
	
	public OrientationKey(double yaw, double pitch, double roll, Quaternionf q) {
		this.q = q;
		this.x = yaw;
		this.y = pitch;
		this.z = roll;
	}
	
	public Quaternionf getQuaternion() {
		return q;
	}
	public double getYaw() {
		return x;
	}
	public double getPitch() {
		return y;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OrientationKey)) return false;
		OrientationKey k = (OrientationKey) obj;
		return Math.abs(k.getYaw() - x) < epsilon && Math.abs(k.getPitch() - y) < epsilon
				&& Math.abs(k.getQuaternion().w - q.w) < epsilon && Math.abs(k.getQuaternion().z - q.z) < epsilon
				&& Math.abs(k.getQuaternion().x - q.x) < epsilon && Math.abs(k.getQuaternion().y - q.y) < epsilon; 
	}
	
	public int hashCode() {
		int hash = 7;
		
		hash = 31 * hash + (int) (Double.doubleToLongBits(x) ^ (Double.doubleToLongBits(x) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(y) ^ (Double.doubleToLongBits(y) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(z) ^ (Double.doubleToLongBits(z) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(q.x) ^ (Double.doubleToLongBits(q.x) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(q.y) ^ (Double.doubleToLongBits(q.y) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(q.z) ^ (Double.doubleToLongBits(q.z) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(q.w) ^ (Double.doubleToLongBits(q.w) >>> 32));
		
		return hash;
		
	}
	
	

}
