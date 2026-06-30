import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // Compose 가 시스템 인셋(상태바/홈바)을 직접 처리한다.
                .ignoresSafeArea(.all)
                .preferredColorScheme(.light)
        }
    }
}
