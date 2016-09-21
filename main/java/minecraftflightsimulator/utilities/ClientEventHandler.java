package minecraftflightsimulator.utilities;

import java.awt.Color;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityFlyable;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.packets.general.GUIPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.config.Property;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**This class handles rendering/camera edits that need to happen when riding planes.
 * It also calls GUI's up when the player presses the P key.
 * 
 * @author don_bruce
 */
@SideOnly(Side.CLIENT)
public class ClientEventHandler{
	private static Minecraft minecraft = Minecraft.getMinecraft();	
	public static ClientEventHandler instance = new ClientEventHandler();
	
	@SubscribeEvent
	public void on(TickEvent.ClientTickEvent event){
		if(minecraft.theWorld != null){
			if(event.phase.equals(Phase.END)){
				/*Minecraft skips updating children who were spawned before their parents.
				 *This forces them to update, but causes them to lag their rendering until
				 * the next tick.  It's one of the main reasons why all the child rendering
				 * is shoved into custom code.
				 */
				for(Object entity : minecraft.theWorld.getLoadedEntityList()){
					if(entity instanceof EntityParent){
						((EntityParent) entity).moveChildren();
					}
				}
				if(minecraft.thePlayer.ridingEntity == null){
					RenderHelper.changeCameraRoll(0);
					RenderHelper.changeCameraZoom(0);
				}
				if(MFS.firstRun){
					minecraft.thePlayer.openGui(MFS.instance, -1, null, 1, 1, 1);
					MFS.firstRun = false;
					MFS.config.getCategory(MFS.config.CATEGORY_GENERAL).put("FirstRun", new Property("FirstRun", String.valueOf(MFS.firstRun), Property.Type.BOOLEAN));
					MFS.config.save();
				}
			}
		}
	}
	
	/*INS180
	@SubscribeEvent
	public void on(TickEvent.RenderTickEvent event){
		if(event.phase.equals(Phase.START) && minecraft.theWorld != null){
			for(Object obj : minecraft.theWorld.loadedEntityList){
				if(obj instanceof EntityParent){
					((EntityParent) obj).rendered = false;
				}
			}
		}
	}	
	
	@SubscribeEvent
	public void on(RenderWorldLastEvent event){
        Entity renderEntity = minecraft.getRenderViewEntity();
        RenderManager manager = minecraft.getRenderManager();
		for(Object obj : minecraft.theWorld.loadedEntityList){
			if(obj instanceof EntityParent){
				if(!((EntityParent) obj).rendered){
					GlStateManager.depthFunc(515);
					minecraft.entityRenderer.enableLightmap();
					net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
					((EntityParent) obj).rendered = true;
	                manager.renderEntityStatic((Entity) obj, event.partialTicks, false);
	                for(EntityChild child : ((EntityParent) obj).getChildren()){
	                	Entity rider = child.getRider();
	                	if(child.getRider() != null && !(minecraft.thePlayer.equals(child.getRider()) && minecraft.gameSettings.thirdPersonView == 0)){
	                		manager.renderEntityStatic(child.getRider(), event.partialTicks, false);
	                	}
	                }
	                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
	                minecraft.entityRenderer.disableLightmap();
				}
			}
		}
	}
	INS180*/
	
	@SubscribeEvent
	public void on(RenderPlayerEvent.Pre event){
		if(event.entityPlayer.ridingEntity instanceof EntitySeat){
			EntityParent parent = ((EntitySeat) event.entityPlayer.ridingEntity).parent;
			if(parent!=null){
				GL11.glPushMatrix();
				/*INS180
				GL11.glTranslated(0, event.entityPlayer.getEyeHeight(), 0);
				INS180*/
				GL11.glRotated(parent.rotationPitch, Math.cos(parent.rotationYaw  * 0.017453292F), 0, Math.sin(parent.rotationYaw * 0.017453292F));
				GL11.glRotated(parent.rotationRoll, -Math.sin(parent.rotationYaw  * 0.017453292F), 0, Math.cos(parent.rotationYaw * 0.017453292F));
				/*INS180
				GL11.glTranslated(0, -event.entityPlayer.getEyeHeight(), 0);
				INS180*/
			}
		}
	}
	
	@SubscribeEvent
	public void on(RenderPlayerEvent.Post event){
		if(event.entityPlayer.ridingEntity instanceof EntitySeat){
			if(((EntitySeat) event.entityPlayer.ridingEntity).parent!=null){
				GL11.glPopMatrix();
			}
		}
	}
	
	/*INS180
	@SubscribeEvent
	public void on(CameraSetup event){
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView==0){
			if(event.entity.ridingEntity instanceof EntitySeat){
				if(((EntitySeat) event.entity.ridingEntity).parent!=null){
					event.roll = ((EntitySeat) event.entity.ridingEntity).parent.rotationRoll;
				}
			}
		}
	}
	INS180*/
	
	@SubscribeEvent
	public void on(RenderGameOverlayEvent.Pre event){
		if(minecraft.thePlayer.ridingEntity instanceof EntitySeat){
			if(event.type.equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
				event.setCanceled(true);
			}else if(event.type.equals(RenderGameOverlayEvent.ElementType.CHAT)){
				EntitySeat seat = ((EntitySeat) minecraft.thePlayer.ridingEntity);
				if(seat.parent instanceof EntityFlyable && seat.controller && (minecraft.gameSettings.thirdPersonView==0 || RenderHelper.hudMode == 1)){
					InstrumentHelper.updateAircraftEngineProperties((EntityFlyable) seat.parent);
					((EntityFlyable) seat.parent).drawHUD(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
				}
			}
		}else if(event.type.equals(RenderGameOverlayEvent.ElementType.CHAT)){
			if(minecraft.theWorld.getTotalWorldTime()%10 < 8){
				for(Object entity : minecraft.theWorld.loadedEntityList){
					if(entity instanceof EntityFlyable && ((Entity) entity).getDistanceToEntity(minecraft.thePlayer) < 5){
						RenderHelper.bindTexture(new ResourceLocation("mfs", "textures/items/mc1720.png"));
						RenderHelper.renderSquare(0, 16, event.resolution.getScaledHeight() - 2, event.resolution.getScaledHeight() - 18, 0, 0, false);
						RenderHelper.drawString(Keyboard.getKeyName(ControlHelper.configKey.getKeyCode()), 6, event.resolution.getScaledHeight() - 12, Color.white);
						return;
					}
				}
			}
		}
	}
	
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event){
    	if(ControlHelper.configKey.isPressed()){
    		EntityClientPlayerMP player = minecraft.thePlayer;
    		if(player.ridingEntity instanceof EntitySeat){
    			player.openGui(MFS.instance, -1, null, 0, 0, 0);
    		}else{
    			for(Object entity : player.worldObj.loadedEntityList){
    				if(entity instanceof EntityVehicle){
    					EntityVehicle vehicle = (EntityVehicle) entity;
    					if(vehicle.getDistanceToEntity(player) < 5){
    						MFS.MFSNet.sendToServer(new GUIPacket(vehicle.getEntityId()));
    						return;
    					}
    				}
    			}
    			
    		}
    	}
    }
}