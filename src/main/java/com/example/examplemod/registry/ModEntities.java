package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.CloneEntity;
import com.example.examplemod.entity.LaserBeamEntity;
import com.example.examplemod.entity.SkillProjectileEntity;
import com.example.examplemod.entity.TrainingDummyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ExampleMod.MODID);

    public static final RegistryObject<EntityType<SkillProjectileEntity>> SKILL_PROJECTILE =
            ENTITY_TYPES.register("skill_projectile",
                    () -> EntityType.Builder.<SkillProjectileEntity>of(SkillProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("skill_projectile"));

    // 분신 몹. 플레이어와 같은 히트박스(0.6 x 1.8). CREATURE = 살아있는 길찾기 몹에 맞는 분류.
    public static final RegistryObject<EntityType<CloneEntity>> CLONE =
            ENTITY_TYPES.register("clone",
                    () -> EntityType.Builder.<CloneEntity>of(CloneEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build("clone"));

    // 훈련용 좀비(더미). 좀비와 같은 히트박스. MONSTER 분류.
    public static final RegistryObject<EntityType<TrainingDummyEntity>> TRAINING_DUMMY =
            ENTITY_TYPES.register("training_dummy",
                    () -> EntityType.Builder.<TrainingDummyEntity>of(TrainingDummyEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("training_dummy"));

    // 레이저 빔 닻 엔티티 (보이는 빔은 렌더러가 그림)
    public static final RegistryObject<EntityType<LaserBeamEntity>> LASER_BEAM =
            ENTITY_TYPES.register("laser_beam",
                    () -> EntityType.Builder.<LaserBeamEntity>of(LaserBeamEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("laser_beam"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    // MOD 버스에서 발생(EntityAttributeCreationEvent implements IModBusEvent).
    // ExampleMod 생성자에서 modEventBus.addListener(ModEntities::onAttributeCreation)로 연결한다.
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(CLONE.get(), CloneEntity.createAttributes().build());
        event.put(TRAINING_DUMMY.get(), TrainingDummyEntity.createAttributes().build());
    }
}
