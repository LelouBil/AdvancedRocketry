package zmaster587.advancedRocketry.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.api.AdvancedRocketryAPI;
import zmaster587.advancedRocketry.stations.SpaceStation;
import zmaster587.libVulpes.network.BasePacket;

import java.util.logging.Logger;

public class PacketSpaceStationInfo extends BasePacket {
	SpaceStation station;
	ResourceLocation location;
	CompoundNBT nbt;
	
	public PacketSpaceStationInfo() {}

	public PacketSpaceStationInfo(ResourceLocation location, SpaceStation station) {
		this.station = station;
		this.location = location;
	}

	@Override
	public void write(PacketBuffer out) {
		CompoundNBT nbt = new CompoundNBT();
		out.writeResourceLocation(location);

		try {
			station.writeToNbt(nbt);
		} catch(NullPointerException e) {
			out.writeBoolean(true);
			Logger.getLogger("advancedrocketry").warning("Station " + location + " has thrown an exception trying to write NBT!");
		}
	}

	@Override
	public void readClient(PacketBuffer in) {
		PacketBuffer packetBuffer = new PacketBuffer(in);

		location = in.readResourceLocation();
		nbt = packetBuffer.readCompoundTag();
	}

	@Override
	public void read(PacketBuffer in) {
		//Should never be read on the server!
	}

	@Override
	public void executeClient(PlayerEntity thePlayer) {
		this.station = AdvancedRocketryAPI.spaceObjectManager.getSpaceStation(location);

		//Station needs to be created
		if (station == null) {
			station = new SpaceStation(location);
			station.readFromNbt(nbt);
			AdvancedRocketryAPI.spaceObjectManager.registerSpaceStationClient(station, new ResourceLocation(nbt.getString("orbit")), location);
		} else {
			station.readFromNbt(nbt);
		}
	}

	@Override
	public void executeServer(ServerPlayerEntity player) {}

}
