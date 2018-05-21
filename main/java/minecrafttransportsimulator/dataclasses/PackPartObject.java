package minecrafttransportsimulator.dataclasses;

public class PackPartObject{
	public PartGeneralConfig general;
    public PartEngineConfig engine;
    public PartGroundDeviceConfig groundDevice;
    public PartPropellerConfig propeller;

    public class PartGeneralConfig{
    	public String type;
    	public String[] materials;
    }
    
    public class PartEngineConfig{
    	public boolean isAutomatic;
    	public byte starterPower;
    	public byte starterDuration;
    	public int maxRPM;
    	public float fuelConsumption;
    	public float[] gearRatios;
    	public String[] repairMaterials;
    }
    
    public class PartGroundDeviceConfig{
    	public boolean canBeFlat;
    	public boolean canFloat;
        public boolean rotatesOnShaft;
        public boolean isLongPart;
        public float diameter;
        public float thickness;
        public float motiveFriction;
        public float lateralFriction;
        
        //These parameters are extra and depend on what is chosen in the booleans above.
        public float extraCollisionBoxOffset;
        public float flatDiameter;
    }
    
    public class PartPropellerConfig{
    	public byte numberBlades;
    	public byte pitch;
    	public int diameter;
    	public int startingHealth;
    }
}