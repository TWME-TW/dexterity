package me.c7dev.tensegrity.util;

import org.joml.Quaternionf;

public class OrientationKey {
	
	private Quaternionf q;
	private double yaw, pitch;
	
	public static final double epsilon = 0.000001;
	
	public OrientationKey(double yaw, double pitch, Quaternionf q) {
		this.q = q;
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	public Quaternionf getQuaternion() {
		return q;
	}
	public double getYaw() {
		return yaw;
	}
	public double getPitch() {
		return pitch;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OrientationKey)) return false;
		OrientationKey k = (OrientationKey) obj;
		return Math.abs(k.getYaw() - yaw) < epsilon && Math.abs(k.getPitch() - pitch) < epsilon
				&& Math.abs(k.getQuaternion().w - q.w) < epsilon && Math.abs(k.getQuaternion().z - q.z) < epsilon
				&& Math.abs(k.getQuaternion().x - q.x) < epsilon && Math.abs(k.getQuaternion().y - q.y) < epsilon; 
	}
	
	public int hashCode() {
		int hash = 7;
		
		hash = 31 * hash + (int) (Double.doubleToLongBits(yaw) ^ (Double.doubleToLongBits(yaw) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(pitch) ^ (Double.doubleToLongBits(pitch) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(q.x) ^ (Double.doubleToLongBits(q.x) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(q.y) ^ (Double.doubleToLongBits(q.y) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(q.z) ^ (Double.doubleToLongBits(q.z) >>> 32));
		hash = 31 * hash + (int) (Double.doubleToLongBits(q.w) ^ (Double.doubleToLongBits(q.w) >>> 32));
		
		return hash;
		
	}
	
	

}
