package net.samagames.hub.common.managers;

import net.minecraft.server.v1_8_R3.*;
import net.samagames.hub.Hub;
import net.samagames.hub.cosmetics.gadgets.displayers.MoutMout2000Displayer;
import net.samagames.hub.cosmetics.pets.nms.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class EntityManager extends AbstractManager
{
    public EntityManager(Hub hub)
    {
        super(hub);

        this.registerCustomEntities();
    }

    public void registerCustomEntities()
    {
        Hub.getInstance().log(this, Level.INFO, "Registering custom entities...");

        this.registerEntity("MoutMout2000", 91, EntitySheep.class, MoutMout2000Displayer.MoutMout2000Sheep.class);

        this.registerEntity("Chicken", 93, EntityChicken.class, CosmeticChicken.class);
        this.registerEntity("Cow", 92, EntityCow.class, CosmeticCow.class);
        this.registerEntity("EntityHorse", 100, EntityHorse.class, CosmeticHorse.class);
        this.registerEntity("VillagerGolem", 99, EntityIronGolem.class, CosmeticIronGolem.class);
        this.registerEntity("LavaSlime", 62, EntityMagmaCube.class, CosmeticMagmaCube.class);
        this.registerEntity("Pig", 90, EntityPig.class, CosmeticPig.class);
        this.registerEntity("Rabbit", 101, EntityRabbit.class, CosmeticRabbit.class);
        this.registerEntity("Sheep", 91, EntitySheep.class, CosmeticSheep.class);
        this.registerEntity("Slime", 55, EntitySlime.class, CosmeticSlime.class);
        this.registerEntity("Wolf", 95, EntityWolf.class, CosmeticWolf.class);
        this.registerEntity("Enderman", 58, EntityEnderman.class, CosmeticEnderman.class);

        Hub.getInstance().log(this, Level.INFO, "Registered custom entites with success!");
    }

    public void registerEntity(String name, int id, Class<? extends EntityInsentient> nmsClass, Class<? extends EntityInsentient> customClass)
    {
        BiomeBase[] biomes;

        try
        {
            biomes = (BiomeBase[]) getPrivateStatic(BiomeBase.class, "biomes");
            this.registerEntityInEntityEnum(customClass, name, id);
        }
        catch (Exception e)
        {
            Hub.getInstance().log(this, Level.SEVERE, "Can't register custom entity '" + customClass.getName() + "'!");
            e.printStackTrace();

            return;
        }

        for (BiomeBase biomeBase : biomes)
        {
            if (biomeBase == null)
                break;

            for (String field : new String[]{"at", "au", "av", "aw"})
            {
                try
                {
                    Field list = BiomeBase.class.getDeclaredField(field);
                    list.setAccessible(true);
                    List<BiomeBase.BiomeMeta> mobList = (List<BiomeBase.BiomeMeta>) list.get(biomeBase);

                    mobList.stream().filter(meta -> nmsClass.equals(meta.b)).forEach(meta -> meta.b = customClass);
                }
                catch (Exception e)
                {
                    Hub.getInstance().log(this, Level.SEVERE, "Can't register custom entity '" + customClass.getName() + "'!");
                    e.printStackTrace();
                }
            }
        }

        Hub.getInstance().log(this, Level.INFO, "Registered custom entity '" + customClass.getName() + "'");
    }

    private void registerEntityInEntityEnum(Class paramClass, String paramString, int paramInt) throws Exception
    {
        ((Map) this.getPrivateStatic(EntityTypes.class, "c")).put(paramString, paramClass);
        ((Map) this.getPrivateStatic(EntityTypes.class, "d")).put(paramClass, paramString);
        ((Map) this.getPrivateStatic(EntityTypes.class, "e")).put(paramInt, paramClass);
        ((Map) this.getPrivateStatic(EntityTypes.class, "f")).put(paramClass, paramInt);
        ((Map) this.getPrivateStatic(EntityTypes.class, "g")).put(paramString, paramInt);
    }

    private Object getPrivateStatic(Class clazz, String f) throws Exception
    {
        Field field = clazz.getDeclaredField(f);
        field.setAccessible(true);

        return field.get(null);
    }

    @Override
    public String getName() { return "EntityManager"; }
}
