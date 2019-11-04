package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.packs.components.PackComponentPart;
import minecrafttransportsimulator.packs.objects.PackObjectVehicle.PackPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PartGroundDevicePontoon extends APartGroundDevice{
	public PartGroundDevicePontoon(EntityVehicleE_Powered vehicle, PackComponentPart packComponent, PackPart vehicleDefinition, NBTTagCompound dataTag){
		super(vehicle, packComponent, vehicleDefinition, dataTag);
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound();
	}
	
	@Override
	public float getWidth(){
		return this.packComponent.pack.pontoon.width;
	}
	
	@Override
	public float getHeight(){
		return this.getWidth();
	}
	
	@Override
	public boolean isPartCollidingWithBlocks(Vec3d collisionOffset){
		if(super.isPartCollidingWithBlocks(collisionOffset)){
			return true;
    	}else{
    		VehicleAxisAlignedBB collisionBox = this.getAABBWithOffset(collisionOffset);
			int minX = (int) Math.floor(collisionBox.minX);
	    	int maxX = (int) Math.floor(collisionBox.maxX + 1.0D);
	    	int minY = (int) Math.floor(collisionBox.minY);
	    	int maxY = (int) Math.floor(collisionBox.maxY + 1.0D);
	    	int minZ = (int) Math.floor(collisionBox.minZ);
	    	int maxZ = (int) Math.floor(collisionBox.maxZ + 1.0D);
	    	
	    	for(int i = minX; i < maxX; ++i){
	    		for(int j = minY; j < maxY; ++j){
	    			for(int k = minZ; k < maxZ; ++k){
	    				BlockPos checkPos = new BlockPos(i, j, k);
	    				if(vehicle.world.isBlockLoaded(checkPos)){
		    				if(vehicle.world.getBlockState(checkPos).getMaterial().isLiquid()){
		    					return true;
		    				}
	    				}
	    			}
	    		}
	    	}
	    	return false;
    	}
    }
	
	@Override
	public float getMotiveFriction(){
		return 0;
	}
	
	@Override
	public float getLateralFriction(){
		return this.packComponent.pack.pontoon.lateralFriction;
	}
	
	@Override
	public float getLongPartOffset(){
		return packComponent.pack.pontoon.extraCollisionBoxOffset;
	}
	
	@Override
	public boolean canBeDrivenByEngine(){
		return false;
	}
}
