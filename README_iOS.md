# Posty — iOS (Compose Multiplatform)

Posty가 **Compose Multiplatform**로 전환되어 Android와 iOS가 **하나의 UI/로직 코드**를 공유합니다.

## 모듈 구조

```
Posty/
├─ shared/        KMP + Compose Multiplatform 라이브러리 (UI·뷰모델·도메인 전부)
│   ├─ commonMain  BoardScreen·DeckLineBoard·StickyNoteCard·CompletedScreen·EditTaskScreen,
│   │              PostyViewModel·TaskRepository·Task, 테마, 날짜 포맷, PostyApp/PostyRoot
│   ├─ androidMain AndroidPostyStore(DataStore), randomUuid/currentTimeMillis(actual),
│   │              setPostyContent(Activity 진입), PlatformBackHandler(actual)
│   └─ iosMain     IosPostyStore(NSUserDefaults), actual들, MainViewController()
├─ app/           Android 앱(com.android.application): MainActivity·알림·위젯·리소스 → :shared 의존
├─ iosApp/        iOS 앱(SwiftUI 호스트) — shared 프레임워크의 MainViewController() 표시
│   ├─ project.yml      XcodeGen 정의(.xcodeproj 를 손으로 관리하지 않음)
│   └─ iosApp/          iOSApp.swift, ContentView.swift
└─ .github/workflows/ios.yml   macOS 러너에서 iOS 빌드 검증
```

핵심 버전: Kotlin 2.2.20 · Compose Multiplatform 1.11.1 · AGP 9.2.1 · Gradle 9.4.1.

## Android 빌드 (Windows/any)

```
./gradlew :app:assembleDebug      # 기존과 동일하게 동작 (검증 완료)
```

## iOS 빌드 (반드시 macOS + Xcode 필요)

Windows에서는 iOS 네이티브 컴파일이 불가합니다. macOS에서:

```
brew install xcodegen
cd iosApp && xcodegen generate      # iosApp.xcodeproj 생성
open iosApp.xcodeproj                # Xcode 에서 실행(시뮬레이터/기기)
```

Xcode 빌드 시 `project.yml`의 pre-build 스크립트가 자동으로
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` 를 호출해 Kotlin(Compose) 프레임워크를 컴파일·임베드합니다.

순수 프레임워크 컴파일만 확인하려면:
```
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## CI

`.github/workflows/ios.yml` — **저장소 루트 기준**으로 동작합니다.
- 이 저장소의 루트가 곧 Posty 프로젝트(루트에 `gradlew`)라면 그대로 동작합니다.
- Posty가 하위 폴더라면 워크플로 파일을 실제 repo 루트의 `.github/workflows/` 로 옮기고
  `working-directory` 를 (예) `Posty`, `Posty/iosApp` 로 맞추세요.

CI는 ① iOS 프레임워크 링크 → ② XcodeGen 으로 프로젝트 생성 → ③ 시뮬레이터용 앱 빌드를 수행합니다.

## Windows에서 검증한 것 / iOS에서 검증할 것

- ✅ Android 전체 빌드·기기 설치·실행
- ✅ commonMain(공용 UI/로직) 컴파일
- ✅ iosMain 메타데이터 컴파일(타입·바인딩 해석: NSUserDefaults·NSUUID·NSDate·ComposeUIViewController)
- ⏳ iOS 네이티브 링크·실제 실행 → **CI(macOS) 또는 Mac/Xcode 에서 확인 필요**

## 아직 남은 iOS 후속 작업 (Android에는 있으나 iOS 미구현)

1. **마감 알림** — 현재 iOS는 무동작(`TaskSideEffects.None`). `UNUserNotificationCenter` 로
   `IosReminders : TaskSideEffects` 를 만들어 `MainViewController` 에서 주입하면 됩니다.
   (iOS 빌드가 CI에서 초록불 된 뒤 추가 권장 — 네이티브 API라 Mac에서 반복 검증 필요)
2. **홈 위젯** — Android는 App Widget. iOS는 WidgetKit(Swift) 별도 구현 필요.
3. **앱 아이콘 / 런치스크린** — iosApp에 에셋 추가.
4. **iOS 화면 내 뒤로가기** — 현재 무동작. 필요 시 가장자리 스와이프 제스처로 보완.
