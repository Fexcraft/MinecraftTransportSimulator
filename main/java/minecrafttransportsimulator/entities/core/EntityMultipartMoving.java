package minecrafttransportsimulator.entities.core;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.minecrafthelpers.AABBHelper;
import minecrafttransportsimulator.minecrafthelpers.BlockHelper;
import minecrafttransportsimulator.minecrafthelpers.ItemStackHelper;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
/**General moving entity class.  This provides a basic set of variables and functions for moving entities.
 * Simple things like texture and display names are included, as well as standards for removal of this
 * entity based on names and damage.  This is the most basic class used for custom multipart entities.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartMoving extends EntityMultipartParent{
	public boolean openTop;
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public byte displayTextMaxLength;
	public double velocity;
	public double health;
	public String name="";
	public String ownerName=MTS.MODID;
	public String displayText="";
	
	/**
	 * Array containing part data for spawnable parts.
	 * This is populated after instantiation and may not be ready right after spawning.
	 * Data comes from {@link PackParserSystem} when {@link #writeToNBT(NBTTagCompound)} is called.
	 * Means there is a slight lag client-side for the population of this field, so do NOT
	 * assume it is populated on the client!
	 */
	protected List<PartData> partData;
			
	public EntityMultipartMoving(World world){
		super(world);
	}
	
	public EntityMultipartMoving(World world, float posX, float posY, float posZ, float playerRotation, String name){
		super(world, posX, posY, posZ, playerRotation);
		this.name = name;
		//This only gets done at the beginning when the entity is first spawned.
		this.displayText = PackParserSystem.getStringProperty(name, "defaultDisplayText");
		//Make sure all data for the PackParser in the NBT methods is inited now that we have a name.
		NBTTagCompound tempTag = new NBTTagCompound();
		this.writeEntityToNBT(tempTag);
		this.readFromNBT(tempTag);
	}
	
	/**
	 * Gets a list of collision boxes used for player and entity collisions.
	 * Used once during spawning, where these boxes are turned into {@link #EntityCore}s
	 */
	public List<Float[]> getCollisionBoxes(){
		List<Float[]> boxList = new ArrayList<Float[]>();
		for(byte i=0; i<=99; ++i){
			if(PackParserSystem.doesPropertyExist(this.name, "collisionBox" + i)){
				Float[] data = PackParserSystem.getFloatArrayProperty(this.name, "collisionBox" + i);
				boxList.add(new Float[]{data[0], data[1], data[2], data[3], data[4]});
			}
		}
		return boxList;
	}
		
	/**
	 * Gets the strength of an explosion when this entity is destroyed.
	 * Is not used if explosions are disabled in the config.
	 */
	protected abstract float getExplosionStrength();
	
	@Override
	public boolean performRightClickAction(MTSEntity clicked, EntityPlayer player){
		if(!worldObj.isRemote){
			if(player.ridingEntity instanceof EntitySeat){
				if(this.equals(((EntitySeat) player.ridingEntity).parent)){
					//No in-use changes for sneaky sneaks!
					return false;
				}
			}else if(PlayerHelper.getHeldStack(player) != null){
				if(ItemStackHelper.getItemFromStack(PlayerHelper.getHeldStack(player)).equals(Items.name_tag)){
					this.displayText = PlayerHelper.getHeldStack(player).getDisplayName().length() > this.displayTextMaxLength ? PlayerHelper.getHeldStack(player).getDisplayName().substring(0, this.displayTextMaxLength - 1) : PlayerHelper.getHeldStack(player).getDisplayName();
					this.sendDataToClient();
					return true;
				}else if(PlayerHelper.isPlayerHoldingWrench(player)){
					return false;
				}else{
					ItemStack heldStack = PlayerHelper.getHeldStack(player);
					Item heldItem = ItemStackHelper.getItemFromStack(heldStack);
					EntityMultipartChild childClicked = (EntityMultipartChild) clicked;	
					PartData dataToSpawn = null;
					float closestPosition = 9999;
					
					//Look though the part data to find the class that goes with the held item.
					for(PartData data : partData){
						for(String partName : data.validNames){
							if(heldItem.getUnlocalizedName().equals(partName)){
								//The held item can spawn a part.
								//Now find the closest spot to put it.
								float distance = (float) Math.hypot(childClicked.offsetX - data.offsetX, childClicked.offsetZ - data.offsetZ);
								if(distance < closestPosition){
									//Make sure a part doesn't exist already.
									boolean childPresent = false;
									for(EntityMultipartChild child : this.getChildren()){
										if(child.offsetX == data.offsetX && child.offsetY == data.offsetY && child.offsetZ == data.offsetZ){
											childPresent = true;
											break;
										}
									}
									if(!childPresent){
										closestPosition = distance;
										dataToSpawn = data;
									}
								}
							}
						}
					}
					
					if(dataToSpawn != null){
						//We have a part, now time to spawn it.
						try{
							Constructor<? extends EntityMultipartChild> construct = MTSRegistry.partClasses.get(heldItem.getUnlocalizedName()).getConstructor(World.class, EntityMultipartParent.class, String.class, float.class, float.class, float.class, int.class);
							EntityMultipartChild newChild = construct.newInstance(worldObj, this, this.UUID, dataToSpawn.offsetX, dataToSpawn.offsetY, dataToSpawn.offsetZ, ItemStackHelper.getItemDamage(heldStack));
							newChild.setNBTFromStack(heldStack);
							newChild.setTurnsWithSteer(dataToSpawn.turnsWithSteer);
							newChild.setController(dataToSpawn.isController);
							this.addChild(newChild.UUID, newChild, true);
							if(!PlayerHelper.isPlayerCreative(player)){
								PlayerHelper.removeItemFromHand(player, 1);
							}
							return true;
						}catch(Exception e){
							System.err.println("ERROR SPAWING PART!");
							e.printStackTrace();
						}
					}
				}
			}
		}else if(PlayerHelper.isPlayerHoldingWrench(player)){
			MTS.proxy.openGUI(this, player);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				EntityPlayer attackingPlayer = (EntityPlayer) source.getEntity();
				if(attackingPlayer.isSneaking()){
					if(attackingPlayer.capabilities.isCreativeMode || attackingPlayer.getDisplayName().endsWith(this.ownerName)){
						this.setDead();
						return true;
					}
				}
			}
			if(!this.equals(source.getEntity())){
				if(!this.isDead){
					health -= damage;
					if(health <= 0){
						this.explodeAtPosition(this.posX, this.posY, this.posZ);
					}
				}
			}
		}
		return true;
	}

	@Override
	public void setDead(){
		if(!worldObj.isRemote){
			for(EntityMultipartChild child : this.getChildren()){
				ItemStack stack = child.getItemStack();
				if(stack != null){
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, stack));
				}
			}
		}
		super.setDead();
	}
	
	protected void getChildCollisions(EntityMultipartChild child, AxisAlignedBB box, List<AxisAlignedBB> boxList){
		//Need to contract the box because sometimes the slight error in math causes issues.
		Map<AxisAlignedBB, Integer[]> collisionMap = AABBHelper.getCollidingBlockBoxes(worldObj, box.contract(0.01F, 0.01F,  0.01F), child.collidesWithLiquids());
		boxList.clear();
		if(!collisionMap.isEmpty()){
			for(Entry<AxisAlignedBB, Integer[]> entry : collisionMap.entrySet()){
				float hardness = BlockHelper.getBlockHardness(worldObj, entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]);
				if(hardness  <= 0.2F && hardness >= 0){
					BlockHelper.setBlockToAir(worldObj, entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]);
            		motionX *= 0.95;
            		motionY *= 0.95;
            		motionZ *= 0.95;
				}else{
					boxList.add(entry.getKey());
				}
			}
		}
	}
	
	public void explodeAtPosition(double x, double y, double z){
		this.setDead();
		if(ConfigSystem.getBooleanConfig("Explosions")){
			worldObj.newExplosion(this, x, y, z, this.getExplosionStrength(), true, true);
		}
	}
	
	/**
	 * Calculates the weight of the inventory passed in.  Used for physics calculations.
	 * @param inventory
	 */
	public static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				weight += 1.2F*stack.stackSize/stack.getMaxStackSize()*(ConfigSystem.getStringConfig("HeavyItems").contains(ItemStackHelper.getItemFromStack(stack).getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
		
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.name=tagCompound.getString("name");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayText=tagCompound.getString("displayText");
		
		this.openTop = PackParserSystem.getBooleanProperty(name, "openTop");
		this.displayTextMaxLength = PackParserSystem.getIntegerProperty(name, "displayTextMaxLength").byteValue();
		
		partData = new ArrayList<PartData>();
		for(byte i=0; i<=99; ++i){
			if(PackParserSystem.doesPropertyExist(name, "part" + i)){
				String data = PackParserSystem.getStringProperty(name, "part" + i);
				float posX = Float.valueOf(data.substring(data.indexOf('=') + 1, data.indexOf(',') - 1));
				data = data.substring(data.indexOf(',') + 1);
				float posY = Float.valueOf(data.substring(0, data.indexOf(',') - 1));
				data = data.substring(data.indexOf(',') + 1);
				float posZ = Float.valueOf(data.substring(0, data.indexOf(',') - 1));
				data = data.substring(data.indexOf(',') + 1);
				boolean turnsWithSteer = Boolean.valueOf(data.substring(0, data.indexOf(',') - 1));
				data = data.substring(data.indexOf(',') + 1);
				boolean isController = Boolean.valueOf(data.substring(0, data.indexOf(',') - 1));
				data = data.substring(data.indexOf(',') + 1);
				List<String> validNames = new ArrayList<String>();
				while(data.indexOf(',') != -1){
					validNames.add(data.substring(0, data.indexOf(',') - 1));
					data = data.substring(data.indexOf(',') + 1);
				}
				validNames.add(data.substring(0, data.indexOf(';') - 1));
				partData.add(new PartData(posX, posY, posZ, turnsWithSteer, isController, validNames.toArray(new String[0])));
			}
		}
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("brakeOn", this.brakeOn);
		tagCompound.setBoolean("parkingBrakeOn", this.parkingBrakeOn);
		tagCompound.setString("name", this.name);
		tagCompound.setString("ownerName", this.ownerName);
		tagCompound.setString("displayText", this.displayText);
	}
	
	/**This class contains data for parts that can be attached to or are attached to this parent.
	 * These are added by the data parsed by the {@link PackParserSystem}.
	 * Data is used during item clicks to determine the part to spawn.
	 * 
	 *@author don_bruce
	 */
	protected class PartData{
		public final float offsetX;
		public final float offsetY;
		public final float offsetZ;
		public final boolean turnsWithSteer;
		public final boolean isController;
		public final String[] validNames;
		
		public PartData(float offsetX, float offsetY, float offsetZ, boolean turnsWithSteer, boolean isController, String... validNames){
			this.turnsWithSteer = turnsWithSteer;
			this.isController = isController;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.validNames = validNames;
		}
	}
}
