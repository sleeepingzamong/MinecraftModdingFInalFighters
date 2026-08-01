# 플레이어 모션 제작 가이드 (FinalFighters)

> 2026-07-25 기준 검증 완료. 구조: **블록벤치(제작) → json 파일 → playerAnimator(재생, 모드에 이미 내장)**

## 0. 전체 흐름 한눈에

```
블록벤치에서 모션 제작
   ↓ json 내보내기
src\main\resources\assets\examplemod\player_animation\모션이름.json
   ↓ 게임에서 F3+T (또는 재시작)
코드에서 재생 조건 연결 (PlayerAnimations.java 패턴)
```

## 1. 준비 (한 번만)

1. **Blockbench 설치** — https://www.blockbench.net
2. **플러그인 설치** — Blockbench → File → Plugins → `GeckoLib Models & Animations` 검색 → 설치
   - ⚠️ 구버전 "GeckoLib Animation Utils"는 Blockbench 5부터 작동 안 함
3. **플레이어 템플릿 다운로드** — 브라우저에 붙여넣기:
   ```
   https://raw.githubusercontent.com/KosmX/emotes/dev/blender/model_item.bbmodel
   ```
   - KosmX(playerAnimator 제작자) 공식 저장소, 14.9KB
   - ⚠️ 과거의 `model.bbmodel` 링크는 삭제됨 — 위 파일명이 현재 버전
   - 저장 위치 추천: `C:\modding\blockbench\`

## 2. 블록벤치에서 제작

1. `model_item.bbmodel` 열기 (형식 변환을 물어보면 수락)
2. 우측 상단 **Animate 탭** 클릭
3. 좌측 상단 **Add Animation** → 설정:
   - **이름**: 영문 소문자 (예: `hammer_spin`) — 파일명과 똑같이 맞출 것
   - **Loop**: 유지되는 자세면 ON / 한 번 재생하는 모션이면 OFF
4. 애니메이션 만들기:
   - 왼쪽 목록에서 본(뼈) 선택 → 타임라인 시점 클릭 → 본을 회전/이동하면 키프레임 생성
   - 스페이스바 = 미리보기 재생
   - 키프레임 우클릭 → 보간(Linear/Smooth) 선택 — Smooth가 자연스러움
5. **주의사항**:
   - 본 이름(head, torso, right_arm, left_arm, right_leg, left_leg)은 절대 바꾸지 말 것
   - 상반신 위주로 만들 것 (다리까지 고정하면 이동 시 발이 미끄러져 보임)
   - 공격 모션은 0.2~0.4초가 마인크래프트 감성에 맞음

## 3. 내보내기 → 게임 적용

1. 애니메이션 json 내보내기 (Ctrl+S 또는 Export)
2. 저장 위치: `C:\modding\src\main\resources\assets\examplemod\player_animation\`
3. 파일명 = 애니메이션 이름 = 영문 소문자
4. 게임에서 **F3+T** → 안 먹히면 게임 재시작
5. 형식 참고: 기존 `charging.json`이 완성 예시 (format_version 1.8.0, Bedrock/GeckoLib 형식)

## 4. 코드 연결 (클로드에게 맡기기)

json을 넣은 뒤 클로드에게 "OO 모션 만들었어, XX에 연결해줘"라고 요청.
재생 패턴은 `client\PlayerAnimations.java` 참고 (차징 자세가 완성 예제):
- 애니메이션 키: `examplemod:파일명`
- 레이어 등록 → 조건 만족 시 `layer.setAnimation(new KeyframeAnimationPlayer(anim))` / 해제 시 `null`

## 5. 현재 목표: 해머 X스킬 모션 (`hammer_spin`)

- **회전(빙글빙글)은 만들지 말 것** — 코드(`HammerSpinRenderEvents`)가 자동으로 돌림. 모션에 넣으면 이중 회전됨
- 만들 것 = **도는 동안 유지할 자세 1개** (팔 뻗기, 허리 숙임, 스탠스 등)
- Loop: ON / 이름: `hammer_spin`
- 완성되면 클로드가: 기존 강제 자세(웅크림, 팔 하드코딩) 제거 + 회전 중인 모든 플레이어에게 모션 적용
