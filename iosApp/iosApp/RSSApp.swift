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

@main
struct RSSApp: App {
    let rss: RssReader
    let store: ObservableFeedStore
    
    init() {
        KoinHelperKt.doInitKoin()
        let helper = KoinHelper()
        rss = helper.rssReader
        store = ObservableFeedStore(viewModel: helper.feedViewModel)
    }
  
    var body: some Scene {
        WindowGroup {
            RootView().environmentObject(store)
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
