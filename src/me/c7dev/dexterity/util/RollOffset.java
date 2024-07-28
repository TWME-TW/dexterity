package me.c7dev.dexterity.util;

import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class RollOffset {
	
	private Vector offset;
	private float roll_deg;
	private Quaternionf q;
	
	private static final double ROOT2INV = (Math.sqrt(2)/2), PI4 = Math.PI/4;
	
	public RollOffset(float deg) {
		roll_deg = deg;
		float frad = (float) Math.toRadians(deg);
		q = new Quaternionf(new AxisAngle4f(frad, 0f, 0f, 1f));
		offset = new Vector(0.5 - (ROOT2INV*Math.cos(frad + PI4)), 0.5 - (ROOT2INV*Math.sin(frad+PI4)), 0);
	}
	
	public RollOffset(Quaternionf r) {
		q = r;
		double frad = -Math.acos(-r.w) * 2;
		roll_deg = (float) Math.toDegrees(frad);
		offset = new Vector(0.5 - (ROOT2INV*Math.cos(frad + PI4)), 0.5 - (ROOT2INV*Math.sin(frad+PI4)), 0);
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
