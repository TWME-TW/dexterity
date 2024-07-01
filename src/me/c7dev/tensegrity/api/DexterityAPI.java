package me.c7dev.tensegrity.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import me.c7dev.tensegrity.DexSession;
import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.ClickedBlockDisplay;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;

public class DexterityAPI {
	
	Dexterity plugin;
	private BlockFace[] faces = {
			BlockFace.UP,
			BlockFace.DOWN,
			BlockFace.SOUTH,
			BlockFace.NORTH,
			BlockFace.EAST,
			BlockFace.WEST
	};
	
	HashMap<UUID, Integer> pidMap = new HashMap<>();
	List<UUID> markerPoints = new ArrayList<>();
	private int pid_ = Integer.MIN_VALUE + 1; //min val reserved for getOrDefault
	
	private int getNewPID() {
		int p = pid_;
		if (pid_ == Integer.MAX_VALUE) pid_ = Integer.MIN_VALUE + 1;
		else pid_++;
		return p;
	}
	
	public DexterityAPI(Dexterity plugin) {
		this.plugin = plugin;
	}

	public static DexterityAPI getInstance() {
		return Dexterity.getPlugin(Dexterity.class).getAPI();
	}
	
	public Set<String> getDisplayLabels() {
		return plugin.getDisplayLabels();
	}
	
	public Collection<DexterityDisplay> getDisplays() {
		return plugin.getDisplays();
	}
	
	public DexterityDisplay getDisplay(String label) {
		return plugin.getDisplay(label);
	}
	
	public DexSession getEditSession(UUID u) {
		return plugin.getEditSession(u);
	}
	
	public String getAuthor() {
		final String message_for_pirates = "Go ahead and make the software FREE for the benefit of Minecraft servers. However, you'd better not claim it as your own or load it with viruses, or I'll find you >:3" +
				"\n Leave a visible spigotMC link to the original work at the top of your page. Also take a shower you smell like rum.";
		return ("ytrew").replace('y', 'C').replace('w', 'v').replace('t', '7').replace('r', 'd');
	}
	
	public DexterityDisplay createDisplay(Location l1, Location l2) { //l1 and l2 bounding box, all blocks inside converted
		if (!l1.getWorld().getName().equals(l2.getWorld().getName())) return null;
		
		int xmin = Math.min(l1.getBlockX(), l2.getBlockX()), xmax = Math.max(l1.getBlockX(), l2.getBlockX());
		int ymin = Math.min(l1.getBlockY(), l2.getBlockY()), ymax = Math.max(l1.getBlockY(), l2.getBlockY());
		int zmin = Math.min(l1.getBlockZ(), l2.getBlockZ()), zmax = Math.max(l1.getBlockZ(), l2.getBlockZ());
		
		if (Math.abs(xmax-xmin) * Math.abs(ymax-ymin) * Math.abs(zmax-zmin) > plugin.getMaxVolume()) {
			Bukkit.getLogger().warning("Failed to create a display because it exceeds the maximum volume in config!");
			return null;
		}
		
		Location center = new Location(l1.getWorld(), Math.min(l1.getX(), l2.getX()) + Math.abs((l1.getX()-l2.getX())/2),
				Math.min(l1.getY(), l2.getY()) + Math.abs(((l1.getY() - l2.getY()) / 2)),
				Math.min(l1.getZ(), l2.getZ()) + Math.abs((l1.getZ() - l2.getZ()) / 2));
		center.add(0.5, 0.5, 0.5);
		
		DexterityDisplay d = new DexterityDisplay(plugin);

		for (int x = xmin; x <= xmax; x++) {
			for (int y = ymin; y <= ymax; y++) {
				for (int z = zmin; z <= zmax; z++) {
					Block b = new Location(l1.getWorld(), x, y, z).getBlock();
					if (b.getType() != Material.BARRIER && b.getType() != Material.AIR) {
						DexBlock db = new DexBlock(b, d);
						d.getBlocks().add(db);
						//db.setBrightness(b2.getLightFromBlocks(), b2.getLightFromSky());
					}
				}
			}
		}
		
		d.recalculateCenter();
		
		//plugin.registerDisplay(d.getLabel(), d);
		
		//plugin.saveDisplays();
		
		return d;
	}
	
	private Vector[][] getBasisVecs(Matrix3d mat) {
		Vector a = new Vector(mat.m00, mat.m01, mat.m02).normalize(), 
				b = new Vector(mat.m10, mat.m11, mat.m12).normalize(), 
				c = new Vector(mat.m20, mat.m21, mat.m22).normalize();
//		Vector[][] basis_vecs = {
//				{new Vector(1, 0, 0), new Vector(0, 0, 1)},
//				{new Vector(1, 0, 0), new Vector(0, 0, 1)},
//				{new Vector(-1, 0, 0), new Vector(0, 1, 0)},
//				{new Vector(1, 0, 0), new Vector(0, 1, 0)},
//				{new Vector(0, 0, -1), new Vector(0, 1, 0)},
//				{new Vector(0, 0, 1), new Vector(0, 1, 0)}
//		};
		Vector[][] basis_vecs = {{a, c}, {a, c}, {a, b}, {a.clone().multiply(-1), b}, {c.clone().multiply(-1), b}, {c, b}};
		return basis_vecs;
	}
	
	private Vector getNormal(BlockFace f, Vector up, Vector east, Vector south) {
		switch(f) {
		case UP: return up.clone().normalize();
		case DOWN: return up.clone().multiply(-1).normalize();
		case SOUTH: return south.clone().normalize();
		case NORTH: return south.clone().multiply(-1).normalize();
		case EAST: return east.clone().normalize();
		case WEST: return east.clone().multiply(-1).normalize();
		default: return new Vector(0, 0, 0);
		}
	}
	
	public ClickedBlockDisplay getLookingAt(Player p) {
		List<Entity> near = p.getNearbyEntities(6d, 6d, 6d);
		Vector dir = p.getLocation().getDirection();
		Vector eye_loc = p.getEyeLocation().toVector();
		double mindist = Double.MAX_VALUE;
		ClickedBlockDisplay nearest = null;
				
		HashMap<Vector, Vector[][]> basis = new HashMap<>();
		HashMap<Vector, Matrix3d> rot_matrices = new HashMap<>();
				
		Vector[][] basis_vecs_norot = getBasisVecs(new Matrix3d(1, 0, 0, 0, 1, 0, 0, 0, 1));
									
		for (Entity entity : near) {
			if (!(entity instanceof BlockDisplay) || markerPoints.contains(entity.getUniqueId())) continue;
			BlockDisplay e = (BlockDisplay) entity;
			Vector scale_raw = DexUtils.vector(e.getTransformation().getScale());
			if (scale_raw.getX() < 0 || scale_raw.getY() < 0 || scale_raw.getZ() < 0) continue; //TODO figure out
			scale_raw.multiply(0.5);
			//Location loc = e.getLocation().add(scale);
			Location loc = e.getLocation();
			
			Vector scale = DexUtils.hadimard(DexUtils.getBlockDimensions(e.getBlock()), scale_raw);
			
			//loc.add(scale).subtract(scale_raw);
			loc.setY(loc.getY() + scale.getY() - scale_raw.getY());
						
			
			//loc.add(scale.getX()-0.5, scale.getY()-0.5, scale.getZ()-0.5);
			//if (transl != null) loc.add(transl.x(), transl.y(), transl.z());

			//if (!e.isGlowing()) markerPoint(loc, Color.AQUA, 4);
			
			Vector diff = loc.toVector().subtract(eye_loc).normalize();
			double dot1 = diff.dot(dir);
			if (dot1 < (scale.lengthSquared() <= 1.2 ? 0.1 : -0.4)) continue;
			Vector locv = loc.toVector();
			
			
			boolean rotated = e.getLocation().getYaw() != 0 || e.getLocation().getPitch() != 0;
			Vector up_dir, south_dir, east_dir;
			Vector[][] basis_vecs;
			if (rotated) {
				Vector key = new Vector(e.getLocation().getYaw(), e.getLocation().getPitch(), 0f);
				Matrix3d rotmat = rot_matrices.get(key);
				if (rotmat == null) {
					rotmat = DexUtils.rotMatDeg(e.getLocation().getPitch(), e.getLocation().getYaw(), 0);
					rot_matrices.put(key, rotmat);
					basis_vecs = getBasisVecs(rotmat);
					basis.put(key, basis_vecs);		
				} else basis_vecs = basis.get(key);
				east_dir = new Vector(rotmat.m00, rotmat.m01, rotmat.m02).multiply(scale.getX());
				up_dir = new Vector(rotmat.m10, rotmat.m11, rotmat.m12).multiply(scale.getY());
				south_dir = new Vector(rotmat.m20, rotmat.m21, rotmat.m22).multiply(scale.getZ());
				
			} else {
				east_dir = new Vector(scale.getX(), 0, 0);
				up_dir = new Vector(0, scale.getY(), 0);
				south_dir = new Vector(0, 0, scale.getZ());
				basis_vecs = basis_vecs_norot;
			}
			
									
			//block face centers
			Vector up = locv.clone().add(up_dir), down = locv.clone().add(up_dir.clone().multiply(-1));
			Vector south = locv.clone().add(south_dir), north = locv.clone().add(south_dir.clone().multiply(-1));
			Vector east = locv.clone().add(east_dir), west = locv.clone().add(east_dir.clone().multiply(-1));
			
			Vector[] locs = {up, down, south, north, east, west};
						
			for (int i = 0; i < locs.length; i++) {
												
				Vector basis1 = basis_vecs[i][0];
				Vector basis2 = basis_vecs[i][1];
				
				// Solve `(FaceCenter) + a(basis1) + b(basis2) = c(dir) + (EyeLoc)` to find intersection of block face plane
				Vector L = eye_loc.clone().subtract(locs[i]);
				Matrix3f matrix = new Matrix3f(
						(float) basis1.getX(), (float) basis1.getY(), (float) basis1.getZ(),
						(float) basis2.getX(), (float) basis2.getY(), (float) basis2.getZ(),
						(float) dir.getX(), (float) dir.getY(), (float) dir.getZ()
						);
				matrix.invert();
				Vector3f cf = new Vector3f();
				Vector c = DexUtils.vector(matrix.transform(DexUtils.vector(L), cf));
				double dist = -c.getZ();
				if (dist < 0) continue; //behind head
				
				switch(i) { //check within block face
				case 0:
				case 1:
					if (Math.abs(c.getX()) > scale.getX()) continue;
					if (Math.abs(c.getY()) > scale.getZ()) continue;
					break;
				case 2:
				case 3:
					if (Math.abs(c.getX()) > scale.getX()) continue;
					if (Math.abs(c.getY()) > scale.getY()) continue;
					break;
				default:
					if (Math.abs(c.getX()) > scale.getZ()) continue;
					if (Math.abs(c.getY()) > scale.getY()) continue;
				}
								
				Vector raw_offset = basis1.clone().multiply(c.getX())
						.add(basis2.clone().multiply(c.getY()));
				Vector blockoffset = locs[i].clone().add(raw_offset);
				
				//markerPoint(DexUtils.location(loc.getWorld(), blockoffset), Color.WHITE, 5);
				
				if (dist < mindist) {
					mindist = dist;
					nearest = new ClickedBlockDisplay(e, faces[i], raw_offset, DexUtils.location(loc.getWorld(), blockoffset), loc, getNormal(faces[i], up_dir, east_dir, south_dir));
				}
			}
			
		}
		
		return nearest;
	}
	
	public BlockDisplay markerPoint(Location loc, Color glow, int seconds) {
		float size = 0.04f;
		BlockDisplay disp = loc.getWorld().spawn(loc, BlockDisplay.class, a -> {
			a.setBlock(Bukkit.createBlockData(Material.WHITE_CONCRETE));
			if (glow != null) {
				a.setGlowColorOverride(glow);
				a.setGlowing(true);
			}
			Transformation t = a.getTransformation();
			Transformation t2 = new Transformation(new Vector3f(-size/2, -size/2, -size/2), t.getLeftRotation(), 
					new Vector3f(size, size, size), t.getRightRotation());
			a.setTransformation(t2);
			markerPoints.add(a.getUniqueId());
		});
		if (seconds > 0) {
			new BukkitRunnable() {
				public void run() {
					markerPoints.add(disp.getUniqueId());
					disp.remove();
				}
			}.runTaskLater(plugin, seconds*20l);
		}
		return disp;
	}
	
	public void tempHighlight(DexterityDisplay d, int ticks) {
		List<BlockDisplay> blocks = new ArrayList<>();
		for (DexBlock db : d.getBlocks()) blocks.add(db.getEntity());
		tempHighlight(blocks, ticks, Color.SILVER);
	}
	public void tempHighlight(BlockDisplay block, int ticks, Color c) {
		List<BlockDisplay> blocks = new ArrayList<>();
		blocks.add(block);
		tempHighlight(blocks, ticks, c);
	}
	
	private void tempHighlight(List<BlockDisplay> blocks, int ticks, Color c) {
		List<UUID> unhighlight = new ArrayList<>();
		int pid = getNewPID();
		for (BlockDisplay block : blocks) {
			if (!block.isGlowing() || pidMap.containsKey(block.getUniqueId())) {
				unhighlight.add(block.getUniqueId());
				block.setGlowColorOverride(c);
				block.setGlowing(true);
				pidMap.put(block.getUniqueId(), pid);
			}
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				for (UUID u : unhighlight) {
					if (pidMap.getOrDefault(u, Integer.MIN_VALUE) != pid) continue;
					pidMap.remove(u);
					Entity e = Bukkit.getEntity(u);
					if (e != null) e.setGlowing(false);
				}
			}
		}.runTaskLater(plugin, ticks);
	}
	
	public List<BlockDisplay> getBlockDisplaysInRegion(Location l1r, Location l2r) {
		List<BlockDisplay> blocks = new ArrayList<>();
		
		Location l1 = DexUtils.blockLoc(l1r.clone()), l2 = DexUtils.blockLoc(l2r.clone());
				
		if (l1.getX() > l2.getX()) {
			double xt = l1.getX();
			l1.setX(l2.getX());
			l2.setX(xt);
		}
		if (l1.getY() > l2.getY()) {
			double yt = l1.getY();
			l1.setY(l2.getY());
			l2.setY(yt);
		}
		if (l1.getZ() > l2.getZ()) {
			double zt = l1.getZ();
			l1.setZ(l2.getZ());
			l2.setZ(zt);
		}
		
		l2.add(1, 1, 1);
				
//		markerPoint(l1, Color.LIME, 10);
//		markerPoint(l2, Color.GREEN, 10);
		
		int xchunks = (int) Math.ceil((l2.getX() - l1.getX()) / 16) + 1;
		int zchunks = (int) Math.ceil((l2.getZ() - l1.getZ()) / 16) + 1;
		Location sel = l1.clone();
		for (int x = 0; x < xchunks; x++) {
			Location xsel = sel.clone();
			for (int z = 0; z < zchunks; z++) {
				Chunk chunk = xsel.getChunk();
				if (!chunk.isLoaded()) continue;
				for (Entity entity : chunk.getEntities()) {
					if (entity instanceof BlockDisplay && !markerPoints.contains(entity.getUniqueId())) {
						BlockDisplay bd = (BlockDisplay) entity;
						if (entity.getLocation().getX() >= l1.getX() && entity.getLocation().getX() <= l2.getX() 
								&& entity.getLocation().getY() >= l1.getY() && entity.getLocation().getY() <= l2.getY()
								&& entity.getLocation().getZ() >= l1.getZ() && entity.getLocation().getZ() <= l2.getZ()) {
		
							blocks.add(bd);
						}
					}
				}
				xsel.add(0, 0, 16);
			}
			sel.add(16, 0, 0);
		}
						
		return blocks;
	}
	
}
