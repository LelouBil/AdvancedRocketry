package zmaster587.advancedRocketry.entity;

import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryEntities;
import zmaster587.advancedRocketry.util.AudioRegistry;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.network.PacketSpawnEntity;

public class EntityLaserNode extends Entity {

	//Used to make sure the emitter is still loaded and so we can send blocks back to the emitter
	//Also we don't want the chunk loading with the laser being there without an emitter it will cause a crash
	//private TileSpaceLaser creator;

	public EntityLaserNode(EntityType<?> type,  World par1World) {
		super(AdvancedRocketryEntities.ENTITY_LASER_NODE, par1World);
		ignoreFrustumCheck = true;
		noClip = true;
	}
	
	// intentionally not saved, flag to determine if the entity controlling the laser is somehow disconnected
	boolean isValid = false;

	public EntityLaserNode(World world, double x, double y, double z) {
		this(AdvancedRocketryEntities.ENTITY_LASER_NODE, world);
		this.setPosition(x, y, z);
	}
	
	public void markValid()
	{
		isValid = true;
	}

	@Override
	protected void registerData() {
	}
	
	@Override
	public boolean isInvulnerable() { return true; }
	
	@Override
	public void remove() {
		super.remove();
		this.cleanUp();
	}

	//TODO: make safe
	/**Removes all the lightblocks created by the laser
	 * For the love of all things good... do NOT call this twice
	 */
	public void cleanUp() {
		if(!this.world.isRemote)
		{
			for(int h = 0; h < world.getHeight(); h++) {
				for(int i = 0; i < 9; i++) {
					int x = (int)this.getPosX() + (i % 3) - 1;
					int z = (int)getPosZ() + (i / 3) - 1;
					BlockPos pos = new BlockPos(x, h, z);
					if(world.getBlockState(pos).getBlock() == AdvancedRocketryBlocks.blockLightSource)
						world.setBlockState(pos, Blocks.AIR.getDefaultState());
				}
			}
		}
	}

	
	@Override
	public void tick() {

		if(!world.isRemote && !isValid) {
			this.remove();

			return;
		}	

		super.tick();
		
		if(this.world.isRemote)	{
			if(Minecraft.getInstance().gameSettings.particles != ParticleStatus.MINIMAL){
				final double spread = 3;
				final double initialSpeed = .5;
				for(int i = 0; i < (Minecraft.getInstance().gameSettings.particles == ParticleStatus.ALL ? 20 : 5); i++)
					AdvancedRocketry.proxy.spawnParticle("fireworksSpark",world, this.getPosX() + (this.rand.nextDouble()*spread) - (spread/2), this.getPosY(), this.getPosZ() + (this.rand.nextDouble()*spread) - (spread/2), initialSpeed * this.rand.nextDouble() - (initialSpeed/2), initialSpeed * this.rand.nextDouble() * 20 + initialSpeed, initialSpeed * this.rand.nextDouble() - (initialSpeed/2));


				//this.worldObj.spawnParticle("tilecrack_" + this.worldObj.getBlockId((int)this.posX, (int)this.posY - 1, (int)this.posZ) + "_" + 0, this.posX + (this.rand.nextDouble()*spread) - (spread/2), this.posY + 5, this.posZ + (this.rand.nextDouble()*spread) - (spread/2), initialSpeed * this.rand.nextDouble(), initialSpeed * this.rand.nextDouble() * 20 + initialSpeed, initialSpeed * this.rand.nextDouble() - (initialSpeed/2));
				AdvancedRocketry.proxy.spawnParticle("hugeexplosion", world, this.getPosX() + (this.rand.nextDouble()*spread) - (spread/2), this.getPosY(), this.getPosZ() + (this.rand.nextDouble()*spread) - (spread/2), initialSpeed * this.rand.nextDouble(), initialSpeed * this.rand.nextDouble() * 4 + initialSpeed, initialSpeed * this.rand.nextDouble() - (initialSpeed/2));
			}
			//TODO: use sound setting
			LibVulpes.proxy.playSound(world, getPositionUnderneath(), AudioRegistry.laserDrill, SoundCategory.NEUTRAL, 1.0f, (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);
			
		}
	}

	
	@Override
	/**
	 * Checks if the entity is in range to render by using the past in distance and comparing it to its average edge
	 * length * 64 * renderDistanceWeight Args: distance
	 */
	@OnlyIn(value=Dist.CLIENT)
	public boolean isInRangeToRenderDist(double par1)
	{
		//double d1 = this.boundingBox.getAverageEdgeLength();
		//d1 *= 4096.0D * this.renderDistanceWeight;
		return par1 < 16777216D;
	}


	//Dont need to write anything
	@Override
	protected void readAdditional(CompoundNBT nbttagcompound) {
	}

	@Override
	protected void writeAdditional(CompoundNBT nbttagcompound) {
	}
	
	@Override
	public IPacket<?> createSpawnPacket() {
		return new PacketSpawnEntity(this);
	}
}
