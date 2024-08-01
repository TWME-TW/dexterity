package me.c7dev.dexterity.util;

public class RotationPlan {
	
	public double x_deg = 0, y_deg = 0, z_deg = 0, yaw_deg = 0, pitch_deg = 0, roll_deg = 0;
	public boolean set_x = false, set_y = false, set_z = false, set_yaw = false, set_pitch = false, set_roll = false, reset = false, async = true;
	
	public RotationPlan() {
		
	}
	
	public RotationPlan clone() {
		RotationPlan r = new RotationPlan();
		r.x_deg = x_deg;
		r.y_deg = y_deg;
		r.z_deg = z_deg;
		r.yaw_deg = yaw_deg;
		r.pitch_deg = pitch_deg;
		r.roll_deg = roll_deg;
		r.set_x = set_x;
		r.set_y = set_y;
		r.set_z = set_z;
		r.set_y = set_yaw;
		r.set_pitch = set_pitch;
		r.set_roll = set_roll;
		r.reset = reset;
		r.async = async;
		return r;
	}
}
