package me.c7dev.dexterity.util;

public class RotationPlan {
	
	public double x_deg = 0, y_deg = 0, z_deg = 0, yaw_deg = 0, pitch_deg = 0, roll_deg = 0;
	public boolean set_x = false, set_y = false, set_z = false, set_yaw = false, set_pitch = false, set_roll = false, reset = false, async = true;
	
	public RotationPlan() {
		
	}
}
