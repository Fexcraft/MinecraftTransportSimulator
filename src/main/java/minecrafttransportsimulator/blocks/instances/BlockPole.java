package minecrafttransportsimulator.blocks.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPole;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemWrench;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;

/**Pole block class.  This class allows for dynamic collision boxes and dynamic
 * placement of components on poles via the Tile Entity.
 *
 * @author don_bruce
 */
public class BlockPole extends ABlockBase implements IBlockTileEntity<TileEntityPole>{
	private final Map<Axis, BoundingBox> axisBounds = new HashMap<Axis, BoundingBox>();
	
	public BlockPole(){
		super(10.0F, 5.0F);
		double connectorRadius = 0.125D;
		double axialRadius = (0.5D - connectorRadius)/2D;
		double axialCenterPoint = 0.5D - axialRadius;
		axisBounds.put(Axis.NONE, new BoundingBox(new Point3d(0, 0, 0), connectorRadius, connectorRadius, connectorRadius));
		axisBounds.put(Axis.UP, new BoundingBox(new Point3d(0, axialCenterPoint, 0), connectorRadius, axialRadius, connectorRadius));
		axisBounds.put(Axis.DOWN, new BoundingBox(new Point3d(0, -axialCenterPoint, 0), connectorRadius, axialRadius, connectorRadius));
		axisBounds.put(Axis.NORTH, new BoundingBox(new Point3d(0, 0, -axialCenterPoint), connectorRadius, connectorRadius, axialRadius));
		axisBounds.put(Axis.SOUTH, new BoundingBox(new Point3d(0, 0, axialCenterPoint), connectorRadius, connectorRadius, axialRadius));
		axisBounds.put(Axis.EAST, new BoundingBox(new Point3d(axialCenterPoint, 0, 0), axialRadius, connectorRadius, connectorRadius));
		axisBounds.put(Axis.WEST, new BoundingBox(new Point3d(-axialCenterPoint, 0, 0), axialRadius, connectorRadius, connectorRadius));
	}
	
	@Override
	public void onPlaced(IWrapperWorld world, Point3i location, IWrapperPlayer player){
		//If there's no NBT data, this is a new pole and needs to have its initial component added.
		IWrapperNBT data = player.getHeldStack().getData();
		if(data.getString("packID").isEmpty()){
			TileEntityPole pole = (TileEntityPole) world.getTileEntity(location);
			pole.components.put(Axis.NONE, TileEntityPole.createComponent(((ItemPoleComponent) player.getHeldItem())));
		}
	}
	
	@Override
	public boolean onClicked(IWrapperWorld world, Point3i location, Axis axis, IWrapperPlayer player){
		//Fire a packet to interact with this pole.  Will either add, remove, or allow editing of the pole.
		//Only fire packet if player is holding a pole component that's not an actual pole, a wrench,
		//or is clicking a sign with text.
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(location);
		if(pole != null){
			IWrapperItemStack heldStack = player.getHeldStack();
			AItemBase heldItem = heldStack.getItem();
			boolean isPlayerHoldingWrench = heldItem instanceof ItemWrench;
			boolean isPlayerClickingEditableSign = pole.components.get(axis) instanceof TileEntityPole_Sign && pole.components.get(axis).definition.general.textObjects != null;
			boolean isPlayerHoldingComponent = heldItem instanceof ItemPoleComponent && !(heldItem instanceof ItemPole);
			if(world.isClient()){
				if(isPlayerHoldingWrench){
					MasterLoader.networkInterface.sendToServer(new PacketTileEntityPoleChange(pole, axis, null, null, true));
				}else if(isPlayerClickingEditableSign){
					MasterLoader.networkInterface.sendToServer(new PacketTileEntityPoleChange(pole, axis, null, null, false));
				}else if(isPlayerHoldingComponent){
					List<String> textLines = null;
					ItemPoleComponent component = (ItemPoleComponent) heldItem;
					if(component.definition.general.textObjects != null){
						textLines = heldStack.getData().getStrings("textLines", component.definition.general.textObjects.size());
					}
					MasterLoader.networkInterface.sendToServer(new PacketTileEntityPoleChange(pole, axis, component, textLines, false));	
				}else{
					return false;
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void addCollisionBoxes(IWrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
		//For every connection or component we have, return a collision box.
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(location);
		if(pole != null){
			for(Axis axis : Axis.values()){
				if(world.getBlock(axis.getOffsetPoint(location)) instanceof BlockPole || world.isBlockSolid(axis.getOffsetPoint(location)) || pole.components.containsKey(axis)){
					collidingBoxes.add(axisBounds.get(axis));
				}
			}
		}else{
			super.addCollisionBoxes(world, location, collidingBoxes);
		}
	}
	
	@Override
	public TileEntityPole createTileEntity(IWrapperWorld world, Point3i position, IWrapperNBT data){
		return new TileEntityPole(world, position, data);
	}

	@Override
	public Class<TileEntityPole> getTileEntityClass(){
		return TileEntityPole.class;
	}
}
