package zmaster587.advancedRocketry.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.api.body.station.IStation;
import zmaster587.advancedRocketry.api.body.planet.PlanetProperties;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStation;
import zmaster587.libVulpes.network.BasePacket;

import java.util.logging.Logger;

public class PacketSpaceStationInfo extends BasePacket {
	SpaceStation spaceObject;
	ResourceLocation stationNumber;
	int direction;
	String clazzId;
	int fuelAmt;
	CompoundNBT nbt;
	boolean hasWarpCores;
	
	public PacketSpaceStationInfo() {}

	public PacketSpaceStationInfo(ResourceLocation stationNumber, IStation spaceObject) {
		this.spaceObject = (SpaceStation)spaceObject;
		this.stationNumber = stationNumber;
	}

	@Override
	public void write(PacketBuffer out) {
		CompoundNBT nbt = new CompoundNBT();
		out.writeResourceLocation(stationNumber);
		boolean flag = false; //TODO //dimProperties == null;
		
		if(!flag) {
			
			//Try to send the nbt data of the dimension to the client, if it fails(probably due to non existent Biome ids) then remove the dimension
			try {
				spaceObject.writeToNbt(nbt);
				//spaceObject.getProperties().writeToNBT(nbt);
				PacketBuffer packetBuffer = new PacketBuffer(out);
				out.writeBoolean(false);
				packetBuffer.writeString(SpaceObjectManager.getSpaceManager().getIdentifierFromClass(spaceObject.getClass()));
				packetBuffer.writeCompoundTag(nbt);
				
				packetBuffer.writeInt(spaceObject.getFuelAmount());
				packetBuffer.writeBoolean(spaceObject.hasWarpCores);
				
				out.writeInt(spaceObject.getForwardDirection().ordinal());
				
			} catch(NullPointerException e) {
				out.writeBoolean(true);
				Logger.getLogger("advancedRocketry").warning("Dimension " + stationNumber + " has thrown an exception trying to write NBT!");
			}

		}
		else
			out.writeBoolean(flag);

	}

	@Override
	public void readClient(PacketBuffer in) {
		PacketBuffer packetBuffer = new PacketBuffer(in);

		stationNumber = in.readResourceLocation();

		clazzId = packetBuffer.readString(127);
		nbt = packetBuffer.readCompoundTag();
		fuelAmt = packetBuffer.readInt();

		hasWarpCores = in.readBoolean();

		direction = in.readInt();

	}

	@Override
	public void read(PacketBuffer in) {
		//Should never be read on the server!
	}

	@Override
	public void executeClient(PlayerEntity thePlayer) {
		IStation spaceObject = SpaceObjectManager.getSpaceManager().getSpaceStation(stationNumber);
		this.spaceObject = (SpaceStation) spaceObject;

		//Station needs to be created
		if (spaceObject == null) {
			IStation object = SpaceObjectManager.getSpaceManager().getNewSpaceObjectFromIdentifier(clazzId);
			object.readFromNbt(nbt);
			object.setProperties(PlanetProperties.createFromNBT(stationNumber, nbt));
			((SpaceStation) object).setForwardDirection(Direction.values()[direction]);

			SpaceObjectManager.getSpaceManager().registerSpaceObjectClient(object, object.getOrbitingPlanetId(), stationNumber);
			((SpaceStation) object).setFuelAmount(fuelAmt);
			((SpaceStation) object).hasWarpCores = hasWarpCores;
		} else {
			spaceObject.readFromNbt(nbt);
			//iObject.setProperties(DimensionProperties.createFromNBT(stationNumber, nbt));
			((SpaceStation) spaceObject).setForwardDirection(Direction.values()[direction]);
			((SpaceStation) spaceObject).setFuelAmount(fuelAmt);
			((SpaceStation) spaceObject).hasWarpCores = hasWarpCores;
		}
	}

	@Override
	public void executeServer(ServerPlayerEntity player) {}

}
