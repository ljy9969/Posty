import SwiftUI
import Shared

/** Kotlin(Compose Multiplatform)의 MainViewController 를 SwiftUI 에 끼워 넣는다. */
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
