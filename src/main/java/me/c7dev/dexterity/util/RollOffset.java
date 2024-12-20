package me.c7dev.dexterity.util;

import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/**
 * Used to calculate the roll degrees, roll offset vector, or transformation quaternion for a DexBlock
 */
public class RollOffset {
	
	private Vector offset;
	private float roll_deg;
	private Quaternionf q;
	
	/**
	 * Constructs the roll offset vector and quaternion
	 * 
	 * @param deg Roll in degrees
	 * @param scale Scale of block
	 */
	public RollOffset(float deg, Vector scale) {
		roll_deg = deg;
		float frad = (float) Math.toRadians(deg);
		q = new Quaternionf(new AxisAngle4f(frad, 0f, 0f, 1f));
//		offset = new Vector(0.5 - (ROOT2INV*Math.cos(frad + PI4)), 0.5 - (ROOT2INV*Math.sin(frad+PI4)), 0);
		
		scale = scale.clone().multiply(0.5);
		double sin = Math.sin(frad), cos = Math.cos(frad);
		offset = new Vector((scale.getX()*(1-cos)) + (scale.getY()*sin), (scale.getY()*(1-cos)) - (scale.getX()*sin), 0);
	}
	
	/**
	 * Constructs the roll offset vector and roll degrees
	 * Quaternion must only have rotation in z axis
	 * 
	 * @param r Left rotation quaternion
	 * @param scale Scale of block
	 */
	public RollOffset(Quaternionf r, Vector scale) {
		q = r;
		double frad = -Math.acos(-r.w) * 2;
		roll_deg = (float) Math.toDegrees(frad);
		
//		offset = new Vector(0.5 - (ROOT2INV*Math.cos(frad + PI4)), 0.5 - (ROOT2INV*Math.sin(frad+PI4)), 0);
		scale = scale.clone().multiply(0.5);
		double sin = Math.sin(frad), cos = Math.cos(frad);
		offset = new Vector((scale.getX()*(1-cos)) + (scale.getY()*sin), (scale.getY()*(1-cos)) - (scale.getX()*sin), 0);
	}
	
	public Vector getOffset() {
		return offset;
	}
	
	public float getRoll() {
		return roll_deg;
	}
	
	public Quaternionf getQuaternion() {
		return q;
	}

}
