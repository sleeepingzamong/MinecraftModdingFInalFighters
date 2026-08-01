package com.example.examplemod.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;

// 팔다리 관절(무릎·팔꿈치) 굽힘 주입 모디파이어 — json(gecko)이 bend 값을 못 실어서 코드로 얹는다.
// 부위별 {틱, 각도} 키프레임 트랙을 받아 애니메이션 진행 시각에 맞춰 선형 보간한다.
// setTracks()로 실행 중 트랙 교체 가능 (걷기/뛰기 등 모션 전환 시 관절 리듬도 같이 바뀌게).
public class LimbBendModifier extends AbstractModifier {

    private volatile float[][] rightLeg;
    private volatile float[][] leftLeg;
    private volatile float[][] rightArm;
    private volatile float[][] leftArm;

    public LimbBendModifier() {
        this(null, null, null, null);
    }

    public LimbBendModifier(float[][] rightLeg, float[][] leftLeg, float[][] rightArm, float[][] leftArm) {
        this.rightLeg = rightLeg;
        this.leftLeg = leftLeg;
        this.rightArm = rightArm;
        this.leftArm = leftArm;
    }

    // 다리만 고정 각도로 굽히는 간편 생성 (회전 베기·궁 대기용)
    public static LimbBendModifier legs(float rightDegrees, float leftDegrees) {
        return new LimbBendModifier(constant(rightDegrees), constant(leftDegrees), null, null);
    }

    public static LimbBendModifier legs(float[][] rightKeys, float[][] leftKeys) {
        return new LimbBendModifier(rightKeys, leftKeys, null, null);
    }

    private static float[][] constant(float degrees) {
        return new float[][]{{0F, degrees}};
    }

    public void setTracks(float[][] rightLeg, float[][] leftLeg, float[][] rightArm, float[][] leftArm) {
        this.rightLeg = rightLeg;
        this.leftLeg = leftLeg;
        this.rightArm = rightArm;
        this.leftArm = leftArm;
    }

    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value0) {
        Vec3f value = super.get3DTransform(modelName, type, tickDelta, value0);
        if (type != TransformType.BEND) {
            return value;
        }

        // 다리와 팔은 굽힘 축 방향이 서로 반대 — 트랙의 양수 = 자연스러운 접힘이 되게 부위별 부호 적용
        float sign;
        float[][] keys = switch (modelName) {
            case "rightLeg" -> { sign = 1F; yield rightLeg; }
            case "leftLeg" -> { sign = 1F; yield leftLeg; }
            case "rightArm" -> { sign = -1F; yield rightArm; }
            case "leftArm" -> { sign = -1F; yield leftArm; }
            default -> { sign = 0F; yield null; }
        };
        if (keys == null) {
            return value;
        }

        float degrees = sample(keys, currentTick(tickDelta));
        return new Vec3f(
                value.getX(),
                value.getY() + sign * (float) Math.toRadians(degrees),
                value.getZ());
    }

    // 아래에 깔린 애니메이션의 현재 재생 시각 (틱) — 루프 애니메이션이면 루프 안에서 순환
    private float currentTick(float delta) {
        IAnimation anim = this.getAnim();
        if (anim instanceof KeyframeAnimationPlayer player) {
            return player.getTick() + delta;
        }
        return 0F;
    }

    // 키프레임 선형 보간
    private static float sample(float[][] keys, float tick) {
        if (tick <= keys[0][0]) {
            return keys[0][1];
        }
        for (int i = 1; i < keys.length; i++) {
            if (tick <= keys[i][0]) {
                float t0 = keys[i - 1][0], v0 = keys[i - 1][1];
                float t1 = keys[i][0], v1 = keys[i][1];
                float progress = (tick - t0) / (t1 - t0);
                return v0 + (v1 - v0) * progress;
            }
        }
        return keys[keys.length - 1][1];
    }
}
