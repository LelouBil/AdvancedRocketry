package zmaster587.advancedRocketry.api.body.station;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.api.body.GenericProperties;
import zmaster587.advancedRocketry.util.StationLandingLocation;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.util.*;

public class StationProperties extends GenericProperties implements Cloneable {

	public boolean created;
	public final ResourceLocation location;

	//Position of the station
	public int altitude = 100;
	public int[] rotation = new int[3];
	public ResourceLocation orbit = Constants.INVALID_PLANET;
	public boolean anchored = false;

	//Positions within the station
	public HashedBlockPosition spawn;
	public List<StationLandingLocation> pads;
	public HashMap<HashedBlockPosition, String> gantries;

	//Transformable properties exposed to the user
	public double gravitation;
	public Direction direction;
	public int[] omega = new int[3];
	public int targetAltitude = 100;
	public double targetGravitation;
	public int[] targetOmega = new int[3];

	public StationProperties(ResourceLocation location, int pressure) {
		super(location.getPath(), pressure, 0);
		this.location = location;
	}

	@Override
	public double getGravitation() {
		return gravitation;
	}

	public void writeToNbt(CompoundNBT nbt) {
		nbt.putBoolean("created", created);

		nbt.putInt("altitude", altitude);
		nbt.putIntArray("rotation", rotation);
        nbt.putString("orbit", orbit.toString());
		nbt.putBoolean("anchored", anchored);

		nbt.putIntArray("spawn", new int[]{ spawn.x, spawn.y, spawn.z} );
		ListNBT list = new ListNBT();
		for(StationLandingLocation pos : this.pads) {
			CompoundNBT tag = new CompoundNBT();
			tag.putBoolean("occupied", pos.getOccupied());
			tag.putBoolean("automatic", pos.getAllowedForAutoLand());
			tag.putIntArray("pos", new int[] {pos.getPos().x, pos.getPos().z});
			tag.putString("name", pos.getName());
			list.add(tag);
		}
		nbt.put("pads", list);
		list = new ListNBT();
		for(Map.Entry<HashedBlockPosition, String> obj : this.gantries.entrySet()) {
			CompoundNBT tag = new CompoundNBT();
			HashedBlockPosition pos = obj.getKey();
			String str = obj.getValue();
			tag.putIntArray("pos", new int[] {pos.x, pos.y, pos.z});
			tag.putString("id", str);
			list.add(tag);
		}
		nbt.put("gantries", list);

		nbt.putDouble("gravitation", gravitation);
		if(direction != null) nbt.putInt("direction", direction.ordinal());
		nbt.putIntArray("omega", omega);
		nbt.putInt("targetAltitude", targetAltitude);
		nbt.putDouble("targetGravitation", targetGravitation);
		nbt.putIntArray("targetOmega", targetOmega);
	}

	public void readFromNbt(CompoundNBT nbt) {
		created = nbt.getBoolean("created");

		altitude = nbt.getInt("altitude");
		rotation = nbt.getIntArray("rotation");
		orbit = new ResourceLocation(nbt.getString("orbit"));
		anchored = nbt.getBoolean("anchored");

		int[] position = nbt.getIntArray("spawn");
		spawn = new HashedBlockPosition(position[0], position[1], position[2]);
		ListNBT list = nbt.getList("pads", net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
		pads.clear();
		for(int i = 0; i < list.size(); i++) {
			CompoundNBT tag = list.getCompound(i);
			int[] posInt = tag.getIntArray("pos");
			HashedBlockPosition pos = new HashedBlockPosition(posInt[0], 0, posInt[1]);
			StationLandingLocation loc = new StationLandingLocation(pos, tag.getString("name"));
			pads.add(loc);
			loc.setOccupied(tag.getBoolean("occupied"));
			loc.setAllowedForAutoLand(!tag.contains("occupied") || tag.getBoolean("occupied"));
		}
		list = nbt.getList("gantries", net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
		gantries.clear();
		for(int i = 0; i < list.size(); i++) {
			CompoundNBT tag = list.getCompound(i);
			int[] posInt = tag.getIntArray("pos");
			HashedBlockPosition pos = new HashedBlockPosition(posInt[0], posInt[1], posInt[2]);
			String str = tag.getString("id");
			gantries.put(pos, str);
		}

		gravitation = nbt.getDouble("gravitation");
		direction = Direction.byIndex(nbt.getInt("direction"));
		omega = nbt.getIntArray("omega");
		targetAltitude = nbt.getInt("targetAltitude");
		targetGravitation = nbt.getDouble("targetGravitation");
		targetOmega = nbt.getIntArray("targetOmega");
	}

	@Override
	public StationProperties clone() {
		try {
			return (StationProperties) super.clone();
		} catch(CloneNotSupportedException e) {
			return null;
		}
	}

}
