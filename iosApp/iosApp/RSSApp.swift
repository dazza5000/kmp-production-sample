//
//  App.swift
//  iosApp
//
//  Created by Ekaterina.Petrova on 13.11.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import RssReader
import UIKit

#if DEBUG
struct DebugUiFlags {
    static let useComposeUIKey = "useComposeUI"
    static var useComposeUI: Bool {
        UserDefaults.standard.bool(forKey: useComposeUIKey)
    }
}

struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
#endif

@main
struct RSSApp: App {
    let rss: RssReader
    let store: ObservableFeedStore

#if DEBUG
    @AppStorage(DebugUiFlags.useComposeUIKey) private var useComposeUI: Bool = false
#endif
    
    init() {
        KoinHelperKt.doInitKoin()
        let helper = KoinHelper()
        rss = helper.rssReader
        store = ObservableFeedStore(viewModel: helper.feedViewModel)
    }
  
    var body: some Scene {
        WindowGroup {
#if DEBUG
            if useComposeUI {
                ZStack(alignment: .bottomTrailing) {
                    ComposeRootView()
                        .ignoresSafeArea()
                    Button(action: {
                        useComposeUI = false
                    }) {
                        Text("SwiftUI")
                            .font(.caption.bold())
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.black.opacity(0.75))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .shadow(radius: 4)
                    }
                    .padding()
                }
            } else {
                ZStack(alignment: .bottomTrailing) {
                    RootView().environmentObject(store)
                    Button(action: {
                        useComposeUI = true
                    }) {
                        Text("Use Compose UI")
                            .font(.caption.bold())
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.black.opacity(0.75))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .shadow(radius: 4)
                    }
                    .padding()
                }
            }
#else
            RootView().environmentObject(store)
#endif
        }
    }
}

class ObservableFeedStore: ObservableObject {
    @Published public var state: FeedUiState = FeedUiState(isLoading: false, feeds: [], selectedFeed: nil)
    
    let viewModel: FeedViewModel
    var stateWatcher: Closeable?

    init(viewModel: FeedViewModel) {
        self.viewModel = viewModel
        stateWatcher = IosViewModelUtilsKt.watchState(self.viewModel).watch { [weak self] state in
            self?.state = state
        }
    }
    
    public func refresh(forceLoad: Bool = false) {
        viewModel.refresh(forceLoad: forceLoad)
    }

    public func addFeed(url: String) {
        viewModel.addFeed(url: url)
    }

    public func deleteFeed(url: String) {
        viewModel.deleteFeed(url: url)
    }

    public func selectFeed(feed: RssFeed?) {
        viewModel.selectFeed(feed: feed)
    }
    
    deinit {
        stateWatcher?.close()
    }
}

public protocol ConnectedView: View {
    associatedtype Props
    associatedtype V: View
    
    func map(state: FeedUiState, store: ObservableFeedStore) -> Props
    func body(props: Props) -> V
}

public extension ConnectedView {
    func render(state: FeedUiState, store: ObservableFeedStore) -> V {
        let props = map(state: state, store: store)
        return body(props: props)
    }
    
    var body: StoreConnector<V> {
        return StoreConnector(content: render)
    }
}

public struct StoreConnector<V: View>: View {
    @EnvironmentObject var store: ObservableFeedStore
    let content: (FeedUiState, ObservableFeedStore) -> V
    
    public var body: V {
        return content(store.state, store)
    }
}
