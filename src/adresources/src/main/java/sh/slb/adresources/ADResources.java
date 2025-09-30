package sh.slb.adresources;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

@Mod(ADResources.MODID)
public class ADResources {
    public static final String MODID = "adresources";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS  = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public ADResources() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register special Semaphore blocks (with levels)
        registerSemaphoreBlock("bedrock1", 2);
        registerSemaphoreBlock("bedrock2", 3);
        registerSemaphoreBlock("bedrock3", 4);
        registerSemaphoreBlock("bedrock4", 5);
        registerSemaphoreBlock("bedrock5", 6);

        // Register normal ore blocks
        registerBasicBlock("ore0");
		registerBasicBlock("ore1");
		registerBasicBlock("ore2");
		registerBasicBlock("ore3");
		registerBasicBlock("ore4");
		registerBasicBlock("ore5");
		registerBasicBlock("ore6");
		registerBasicBlock("ore7");
		registerBasicBlock("ore8");
		registerBasicBlock("ore9");		


        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    /** Registers a plain block + its BlockItem */
    private static RegistryObject<Block> registerBasicBlock(String name) {
        RegistryObject<Block> block = BLOCKS.register(name, 
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE))
        );
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** Registers a SemaphoreBlock with a level + its BlockItem */
    private static RegistryObject<Block> registerSemaphoreBlock(String name, int level) {
        RegistryObject<Block> block = BLOCKS.register(name, 
            () -> new SemaphoreBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(50F, 36000F), // why not?
                level
            )
        );
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
