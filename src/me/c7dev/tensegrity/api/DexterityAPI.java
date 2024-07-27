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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import me.c7dev.tensegrity.DexSession;
import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.transaction.ConvertTransaction;
import me.c7dev.tensegrity.util.ClickedBlock;
import me.c7dev.tensegrity.util.ClickedBlockDisplay;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;
import me.c7dev.tensegrity.util.OrientationKey;
import me.c7dev.tensegrity.util.RollOffset;
import me.c7dev.tensegrity.util.SavedBlockState;

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
		return Dexterity.getPlugin(Dexterity.class).api();
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
	
	public DexSession getSession(Player p) {
		return plugin.getEditSession(p.getUniqueId());
	}
	
	public DexSession getSession(UUID u) {
		return plugin.getEditSession(u);
	}
	
	public DexterityDisplay convertBlocks(Location l1, Location l2) {
		return convertBlocks(l1, l2, null);
	}
	public DexterityDisplay convertBlocks(Location l1, Location l2, ConvertTransaction t) { //l1 and l2 bounding box, all blocks inside converted
		if (!l1.getWorld().getName().equals(l2.getWorld().getName())) return null;
		
		int xmin = Math.min(l1.getBlockX(), l2.getBlockX()), xmax = Math.max(l1.getBlockX(), l2.getBlockX());
		int ymin = Math.min(l1.getBlockY(), l2.getBlockY()), ymax = Math.max(l1.getBlockY(), l2.getBlockY());
		int zmin = Math.min(l1.getBlockZ(), l2.getBlockZ()), zmax = Math.max(l1.getBlockZ(), l2.getBlockZ());
		
		if ((xmax-xmin) * (ymax-ymin) * (zmax-zmin) > plugin.getMaxVolume()) {
			Bukkit.getLogger().warning("Failed to create a display because it exceeds the maximum volume in config!");
			return null;
		}
		
		DexterityDisplay d = new DexterityDisplay(plugin);

		for (int x = xmin; x <= xmax; x++) {
			for (int y = ymin; y <= ymax; y++) {
				for (int z = zmin; z <= zmax; z++) {
					Block b = new Location(l1.getWorld(), x, y, z).getBlock();
					if (b.getType() != Material.BARRIER && b.getType() != Material.AIR) {
						SavedBlockState saved = null;
						if (t != null) saved = new SavedBlockState(b);
						
						DexBlock db = new DexBlock(b, d);
						d.getBlocks().add(db);
						if (t != null) {
							t.addBlock(saved, db);
						}
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
		Vector[][] basis_vecs = {{a, c}, {a, c}, {a, b}, {a.clone().multiply(-1), b}, {c.clone().multiply(-1), b}, {c, b}};
		return basis_vecs;
	}
	
	private Vector[][] getBasisVecs(Vector a, Vector b, Vector c){
		Vector[][] basis_vecs = {{a, c}, {a, c}, {a, b}, {a.clone().multiply(-1), b}, {c.clone().multiply(-1), b}, {c, b}};
		return basis_vecs;
	}
	
	//get the block display that the player is looking at
	//avg 0.011371 milliseconds per block on a potato pc :D
	public ClickedBlockDisplay getLookingAt(Player p) {
		if (p == null) throw new IllegalArgumentException("Player cannot be null!");
		List<Entity> near = p.getNearbyEntities(4.5, 4.5, 4.5);
		Vector dir = p.getLocation().getDirection();
		Vector eye_loc = p.getEyeLocation().toVector();
		double mindist = Double.MAX_VALUE;
		ClickedBlockDisplay nearest = null;
				
		HashMap<Float, RollOffset> roll_offsets = new HashMap<>();
		HashMap<OrientationKey, Vector[]> axes = new HashMap<>();
				
		Vector[][] basis_vecs_norot = getBasisVecs(new Matrix3d(1, 0, 0, 0, 1, 0, 0, 0, 1));
		Vector east_unit = new Vector(1, 0, 0), up_unit = new Vector(0, 1, 0), south_unit = new Vector(0, 0, 1);
									
		for (Entity entity : near) {
			if (!(entity instanceof BlockDisplay) || markerPoints.contains(entity.getUniqueId())) continue;
			BlockDisplay e = (BlockDisplay) entity;
			Vector scale_raw = DexUtils.vector(e.getTransformation().getScale());
			if (scale_raw.getX() < 0 || scale_raw.getY() < 0 || scale_raw.getZ() < 0) continue; //TODO figure out displacement to center
			scale_raw.multiply(0.5);
			//Location loc = e.getLocation().add(scale);
			Vector scale = DexUtils.hadimard(DexUtils.getBlockDimensions(e.getBlock()), scale_raw);
			
			//check if the player is looking in the general direction of the block, accounting for scale
			Vector diff = e.getLocation().toVector().subtract(eye_loc).normalize();
			double dot1 = diff.dot(dir);
			if (dot1 < (scale.lengthSquared() <= 1.2 ? 0 : -0.4)) continue;
			
			Vector up_dir, south_dir, east_dir;
			Vector[][] basis_vecs;
			DexBlock db = plugin.getMappedDisplay(e.getUniqueId());
			
			RollOffset ro = null;
			Location loc;// = e.getLocation().add(displacement).add(scale_raw);
			//calculate roll and its offset
			if (db == null) {
				float key = e.getTransformation().getLeftRotation().w;
				Vector displacement = DexUtils.vector(e.getTransformation().getTranslation());
				if (key != 0) {
					ro = roll_offsets.get(key); //does not account for pitch and yaw built into the rotation quaternion, assumed that blocks managed by other plugins are not built on
					if (ro == null) {
						ro = new RollOffset(e.getTransformation().getLeftRotation());
						roll_offsets.put(key, ro);
					}
					displacement.subtract(ro.getOffset());
				}
				loc = e.getLocation().add(displacement).add(scale_raw);
			} else {
				loc = db.getLocation();
//				plugin.getAPI().markerPoint(db.getLocation(), Color.AQUA, 4); //TODO center not working for scaled blocks
			}
			
			if (e.getLocation().getYaw() != 0 || e.getLocation().getPitch() != 0 || e.getTransformation().getLeftRotation().w != 0) { //if rotated, we need to transform the displacement vecs and basis vectors accordingly
				
				OrientationKey key = new OrientationKey(e.getLocation().getYaw(), e.getLocation().getPitch(), 0, e.getTransformation().getLeftRotation());
				Vector[] res = axes.get(key);
				if (res == null) {
					Vector3f east_dir_d = new Vector3f(1, 0, 0), up_dir_d = new Vector3f(0, 1, 0), south_dir_d = new Vector3f(0, 0, 1);
					Quaternionf q = DexUtils.cloneQ(e.getTransformation().getLeftRotation());
					q.z = -q.z;
					q.rotateX((float) -Math.toRadians(e.getLocation().getPitch()));
					q.rotateY((float) Math.toRadians(e.getLocation().getYaw()));

					q.transformInverse(east_dir_d);
					q.transformInverse(up_dir_d);
					q.transformInverse(south_dir_d);

					east_dir = DexUtils.vector(east_dir_d);
					up_dir = DexUtils.vector(up_dir_d);
					south_dir = DexUtils.vector(south_dir_d);
					basis_vecs = getBasisVecs(east_dir, up_dir, south_dir);
					
					Vector[] res2 = {east_dir, up_dir, south_dir};
					axes.put(key, res2);
					
				} else {
					east_dir = res[0];
					up_dir = res[1];
					south_dir = res[2];
					basis_vecs = getBasisVecs(east_dir, up_dir, south_dir);
				}
			} else {
				east_dir = east_unit;
				up_dir = up_unit;
				south_dir = south_unit;
				basis_vecs = basis_vecs_norot;
			}

			//calculate location of visual display accounting for axis asymmetry
			loc.add(up_dir.clone().multiply(scale.getY() - scale_raw.getY()));
			Vector locv = loc.toVector();

			//block face centers
			Vector up = locv.clone().add(up_dir.clone().multiply(scale.getY())), 
					down = locv.clone().add(up_dir.clone().multiply(-scale.getY())),
					south = locv.clone().add(south_dir.clone().multiply(scale.getZ())), 
					north = locv.clone().add(south_dir.clone().multiply(-scale.getZ())),
					east = locv.clone().add(east_dir.clone().multiply(scale.getX())), 
					west = locv.clone().add(east_dir.clone().multiply(-scale.getX()));

			Vector[] locs = {up, down, south, north, east, west};
						
//			plugin.api().markerPoint(loc, Color.AQUA, 4);
			
			for (int i = 0; i < locs.length; i++) {
				
//				plugin.api().markerPoint(DexUtils.location(loc.getWorld(), locs[i]), Color.LIME, 4);
				
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
				double dist = -c.getZ(); //distance from player's eye to precise location on surface in units of blocks (magic :3)
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
				Vector blockoffset = locs[i].clone().add(raw_offset); //surface location
				
				if (dist < mindist) {
					mindist = dist;
					nearest = new ClickedBlockDisplay(e, faces[i], raw_offset, DexUtils.location(loc.getWorld(), blockoffset), 
							loc, up_dir, east_dir, south_dir, dist);
					if (ro != null) nearest.setRollOffset(ro);
				}
			}	
		}
				
		return nearest;
	}
	
	public ClickedBlock getPhysicalBlockLookingAt(Player p) {
		return getPhysicalBlockLookingAtRaw(p, 0.01, 5); // same as getBlockLookingAt(p, 100);
	}
	
	public ClickedBlock getPhysicalBlockLookingAt(Player p, double percent_precision) {
		return getPhysicalBlockLookingAtRaw(p, Math.abs(1/percent_precision), 5);
	}
	
	public ClickedBlock getPhysicalBlockLookingAtRaw(Player p, double step_multiplier, double max_dist) {
		Vector step = p.getLocation().getDirection().multiply(step_multiplier);
		
		Location loc = p.getEyeLocation();
		int i = 0, max = (int) (max_dist / step_multiplier);
		Block b = null;
		boolean found = false;
		while (i < max) {
			loc.add(step);
			i++;
			b = loc.getBlock();
			if (b.getType() == Material.AIR) continue;
			
			Vector size = DexUtils.getBlockDimensions(b.getBlockData());
			size.setX(size.getX()/2);
			size.setZ(size.getZ()/2);
			Vector locv = b.getLocation().add(0.5, 0, 0.5).toVector(); //TODO add 0.5 to y for upper slabs
			Vector l1 = locv.clone().subtract(size.clone().setY(0)), l2 = locv.clone().add(size);
			
			if (loc.getX() >= l1.getX() && loc.getX() <= l2.getX() &&
					loc.getY() >= l1.getY() && loc.getY() <= l2.getY() &&
					loc.getZ() >= l1.getZ() && loc.getZ() <= l2.getZ()) {
				found = true;
				break;
			}
		}
		
		if (!found || b == null || b.getType() == Material.AIR) return null;
		return new ClickedBlock(b, i*step_multiplier);
	}
	
	public BlockDisplay markerPoint(Location loc, Color glow, int seconds) {
		float size = 0.04f;
		Location loc_ = loc.clone();
		loc_.setPitch(0);
		loc_.setYaw(0);
		BlockDisplay disp = loc.getWorld().spawn(loc_, BlockDisplay.class, a -> {
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
	
	public String getAuthor() {
		String a = "%%__USER__%%, %%__RESOURCE__%%, %%__NONCE__%%";
		final String message_for_pirates = "Make it FREE. You'd better not claim it as your own or load it with viruses, or I'll find you >:3" +
				"\n Leave a visible spigotMC link to the original work at the top of your page. Also take a shower you smell like rum.";
		return ("ytrew").replace('y', 'C').replace('w', 'v').replace('t', '7').replace('r', 'd');
	}
	
	public void tempHighlight(DexterityDisplay d, int ticks) {
		tempHighlight(d, ticks, Color.SILVER);
	}
	
	public void tempHighlight(DexterityDisplay d, int ticks, Color c) {
		List<BlockDisplay> blocks = new ArrayList<>();
		for (DexBlock db : d.getBlocks()) blocks.add(db.getEntity());
		tempHighlight(blocks, ticks, c);
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
	
	public boolean isInProcess(UUID u) {
		return pidMap.containsKey(u);
	}
	
	public List<BlockDisplay> getBlockDisplaysInRegion(Location l1r, Location l2r) {
		return getBlockDisplaysInRegionContinuous(DexUtils.blockLoc(l1r.clone()), DexUtils.blockLoc(l2r.clone()), new Vector(0, 0, 0), new Vector(1, 1, 1));
	}
	
	public List<BlockDisplay> getBlockDisplaysInRegionContinuous(Location l1r, Location l2r, Vector l1o, Vector l2o) {
		List<BlockDisplay> blocks = new ArrayList<>();
		
		Location l1 = l1r.clone(), l2 = l2r.clone();
						
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
		
//		if (scale_offset == null) l2.add(new Vector(1, 1, 1));
		l1.subtract(l1o);
		l2.add(l2o);
				
//		markerPoint(l1, Color.LIME, 4);
//		markerPoint(l2, Color.GREEN, 4);
		
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
