package minecrafttransportsimulator.sound;

import minecrafttransportsimulator.baseclasses.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Class that holds sound information.  One class is created for each sound that's playing
 * in the {@link InterfaceSound}.  This class holds data such as the current
 * source the sound is playing from, whether the sound is supposed to be looping or not, etc.  
 * Setting {@link #stopSound} will stop this sound immediately, while {@link #streaming} tells
 * the audio system that this sound needs to be read in via chunks rather than all at once.
 *
 * @author don_bruce
 */
public class SoundInstance{
	public final AEntityB_Existing entity;
	public final String soundName;
	public final boolean looping;
	public final Radio radio;
	
	//Runtime variables.
	public int sourceIndex;
	public float volume = 1.0F;
	public float pitch = 1.0F;
	public boolean stopSound = false;
	
	public SoundInstance(AEntityB_Existing entity, String soundName){
		this.entity = entity;
		this.soundName = soundName;
		this.looping = false;
		this.radio = null;
	}
	
	public SoundInstance(AEntityB_Existing entity, String soundName, boolean looping){
		this.entity = entity;
		this.soundName = soundName;
		this.looping = looping;
		this.radio = null;
	}

	public SoundInstance(AEntityB_Existing entity, String soundName, boolean looping, Radio radio){
		this.entity = entity;
		this.soundName = soundName;
		this.looping = looping;
		this.radio = radio;
	}
	
	/**
	 *  Flags the sound as stopped.  This will cause the audio system to stop
	 *  playing it the next update call.
	 */
	public void stop(){
		this.stopSound = true;
	}
	
	/**
	 *  Returns true if the sound should be dampened.
	 *  Used if we are in an enclosed vehicle and in first-person mode.
	 *  If the sound is streaming, and the vehicle is the provider, it is
	 *  assumed the sound is the vehicle radio, so it should NOT be dampened.
	 */
	public boolean shouldBeDampened(){
		//FIXME move this into main entity sound update volume code.
		AEntityBase entityRiding = InterfaceClient.getClientPlayer().getEntityRiding();
		return entityRiding instanceof EntityVehicleF_Physics && !((EntityVehicleF_Physics) entityRiding).definition.motorized.openTop && InterfaceClient.inFirstPerson() && (radio == null || !entityRiding.equals(entity));
	}
}
