package com.example.examplemod.client;

import com.example.examplemod.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

// 불사조 궁 장판 지속음 (모닥불 타닥타닥, sounds/skill/phoenix_zone.ogg — 이음매 없이 반복되는 10초).
// 장판 가운데서 들리고(16칸까지), 처음 0.5초에 걸쳐 커지고 끝나기 전 0.75초에 걸쳐 작아짐.
// 장판이 중간에 사라지면(시전자 접속 종료·/clearstatus) 0.5초에 걸쳐 꺼짐.
public class PhoenixZoneSound extends AbstractTickableSoundInstance {

    private static final float MAX_VOLUME = 1.0F;
    private final int zoneId;

    public PhoenixZoneSound(int zoneId, double x, double y, double z) {
        super(ModSounds.PHOENIX_ZONE.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.zoneId = zoneId;
        this.x = x;
        this.y = y + 0.5;
        this.z = z;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
    }

    // 처음엔 거의 무음으로 시작해서 키우므로, 크기 0 근처여도 재생을 시작해야 함
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        float level = ClientPhoenixZones.soundLevel(zoneId);
        if (level < 0F) {
            volume -= MAX_VOLUME / 10F;
            if (volume <= 0.01F) {
                stop();
            }
            return;
        }
        volume = Math.max(0.01F, MAX_VOLUME * level);
    }
}
