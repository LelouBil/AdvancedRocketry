package zmaster587.advancedRocketry.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.api.body.station.IStation;
import zmaster587.advancedRocketry.event.PlanetEventHandler;
import zmaster587.advancedRocketry.api.body.StationManager;
import zmaster587.advancedRocketry.stations.SpaceStation;
import zmaster587.libVulpes.network.BasePacket;

public class PacketStationUpdate extends BasePacket {
	IStation spaceObject;
	ResourceLocation stationNumber;
	Type type;
	
	ResourceLocation destOrbitingBody;
	int fuel;
	double rx,ry,rz,drx,dry,drz;
	float orbitalDistance;
	CompoundNBT nbt;

	public enum Type {
		DEST_ORBIT_UPDATE,
		ORBIT_UPDATE,
		SIGNAL_WHITE_BURST,
		FUEL_UPDATE,
		ROTANGLE_UPDATE,
		ALTITUDE_UPDATE
	}

	public PacketStationUpdate() {}

	public PacketStationUpdate(IStation dimProperties, Type type) {
		this.spaceObject = dimProperties;
		this.stationNumber = dimProperties.getId();
		this.type = type;
	}

	@Override
	public void write(PacketBuffer out) {
		out.writeResourceLocation(stationNumber);
		out.writeInt(type.ordinal());

		switch(type) {
		case DEST_ORBIT_UPDATE:
			out.writeResourceLocation(spaceObject.getDestOrbitingBody());
			break;
		case ORBIT_UPDATE:
			out.writeResourceLocation(spaceObject.getOrbitingPlanetId());
			break;
		case FUEL_UPDATE:
			if(spaceObject instanceof SpaceStation)
				out.writeInt(((SpaceStation)spaceObject).getFuelAmount());
			break;
		case ROTANGLE_UPDATE:
			out.writeDouble(spaceObject.getRotation(Direction.EAST));
			out.writeDouble(spaceObject.getRotation(Direction.UP));
			out.writeDouble(spaceObject.getRotation(Direction.NORTH));
			out.writeDouble(spaceObject.getDeltaRotation(Direction.EAST));
			out.writeDouble(spaceObject.getDeltaRotation(Direction.UP));
			out.writeDouble(spaceObject.getDeltaRotation(Direction.NORTH));
			break;
		case ALTITUDE_UPDATE:
			out.writeFloat(spaceObject.getOrbitalDistance());
			break;
		default:
		}
	}

	@Override
	public void readClient(PacketBuffer in) {
		stationNumber = in.readResourceLocation();
		type = Type.values()[in.readInt()];


		switch(type) {
		case DEST_ORBIT_UPDATE:
		case ORBIT_UPDATE:
			destOrbitingBody = in.readResourceLocation();
		break;
		case FUEL_UPDATE:
		    fuel = in.readInt();
			break;
		case ROTANGLE_UPDATE:
			rx = in.readDouble();
			ry = in.readDouble();
			rz = in.readDouble();
			drx = in.readDouble();
			dry = in.readDouble();
			drz = in.readDouble();
			break;
		case SIGNAL_WHITE_BURST:
			break;
		case ALTITUDE_UPDATE:
			orbitalDistance = in.readFloat();
			break;
		}
	}

	@Override
	public void read(PacketBuffer in) {
		//Should never be read on the server!
	}

	@Override
	public void executeClient(PlayerEntity thePlayer) {
		spaceObject = StationManager.getSpaceManager().getSpaceStation(stationNumber);
		
		switch(type) {
		case DEST_ORBIT_UPDATE:
			spaceObject.setDestOrbitingBody(destOrbitingBody );
			break;
		case ORBIT_UPDATE:
			spaceObject.setOrbitingBody(destOrbitingBody);
			break;
		case FUEL_UPDATE:
			if(spaceObject instanceof SpaceStation)
				((SpaceStation)spaceObject).setFuelAmount(fuel);
			break;
		case ROTANGLE_UPDATE:
			spaceObject.setRotation(rx, Direction.EAST);
			spaceObject.setRotation(ry, Direction.UP);
			spaceObject.setRotation(rz, Direction.NORTH);
			spaceObject.setDeltaRotation(drx, Direction.EAST);
			spaceObject.setDeltaRotation(dry, Direction.UP);
			spaceObject.setDeltaRotation(drz, Direction.NORTH);
			break;
		case SIGNAL_WHITE_BURST:
			PlanetEventHandler.runBurst(Minecraft.getInstance().world.getGameTime() + 20, 20);
			break;
		case ALTITUDE_UPDATE:
			spaceObject.setOrbitalDistance(orbitalDistance);
			break;
		}	
	}

	@Override
	public void executeServer(ServerPlayerEntity player) {}
}
