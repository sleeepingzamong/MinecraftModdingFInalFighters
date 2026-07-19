package com.example.examplemod.client;

import java.util.UUID;

import com.example.examplemod.entity.CloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 분신 렌더러. 바닐라 플레이어 모델(WIDE)로 그리고, 소환자 UUID로 그 플레이어의 스킨을 찾아 입힌다.
 *
 * MobRenderer를 쓰는 이유:
 *  - PlayerRenderer는 AbstractClientPlayer 전용이라 CapeLayer 등이 캐스팅 크래시(CCE)를 낸다.
 *  - HumanoidMobRenderer도 쓸 수 있으나(레이어가 캐스팅하지 않음) 분신은 손에 든 것/엘리트라가
 *    없으므로 ItemInHandLayer/ElytraLayer는 불필요. 가장 단순한 MobRenderer로 충분하다.
 *
 * ModelLayers.PLAYER는 바닐라가 항상 굽는 전역 레이어라 어떤 렌더러에서도 bakeLayer 가능하다.
 * 커스텀 모델 레이어 등록(RegisterLayerDefinitions)은 필요 없다.
 */
@OnlyIn(Dist.CLIENT)
public class CloneRenderer extends MobRenderer<CloneEntity, PlayerModel<CloneEntity>> {

    public CloneRenderer(EntityRendererProvider.Context context) {
        // false = 넓은 팔(WIDE). 0.5F = 플레이어와 같은 그림자 크기.
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        // 손에 든 아이템(카타나)을 그리는 레이어
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity entity) {
        UUID owner = entity.getOwnerUUID();
        if (owner == null) {
            // 소환자 정보가 없으면 엔티티 UUID 기준 기본 스킨(스티브/알렉스).
            return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
        }
        // 바닐라 AbstractClientPlayer.getSkinTextureLocation()과 동일한 null-safe 체인.
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return DefaultPlayerSkin.getDefaultSkin(owner);
        }
        PlayerInfo info = connection.getPlayerInfo(owner); // @Nullable: 탭리스트 맵 조회
        // getSkinLocation()은 내부적으로 non-null(로딩 중엔 기본 스킨으로 폴백), 미싱 텍스처 프레임 없음.
        return info == null ? DefaultPlayerSkin.getDefaultSkin(owner) : info.getSkinLocation();
    }
}
