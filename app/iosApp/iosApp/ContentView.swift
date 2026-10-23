import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        let serverURL = Bundle.main.object(forInfoDictionaryKey: "FiveLinksServerURL") as? String ?? ""
        #if DEBUG
        let requiresSecureOnline = false
        #else
        let requiresSecureOnline = true
        #endif
        return MainViewControllerKt.MainViewController(
            onlineServerUrl: serverURL,
            requireSecureOnline: requiresSecureOnline
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
