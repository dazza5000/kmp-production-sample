//
//  FeedsList.swift
//  iosApp
//
//  Created by Ekaterina.Petrova on 11.11.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import SwiftUI
import RssReader

struct FeedsList: ConnectedView {
    
    struct Props {
        let defaultFeeds: [RssFeed]
        let userFeeds: [RssFeed]
        let onAdd: (String) -> ()
        let onRemove: (String) -> ()
    }
    
    func map(state: FeedUiState, store: ObservableFeedStore) -> Props {
        return Props(defaultFeeds: state.feeds.filter { $0.isDefault },
                     userFeeds: state.feeds.filter { !$0.isDefault },
                     onAdd: { url in
                        store.addFeed(url: url)
                     }, onRemove: { url in
                        store.deleteFeed(url: url)
                     })
    }
    
    @SwiftUI.State var showsAlert: Bool = false
    
    func body(props: Props) -> some View {
        List {
            ForEach(props.defaultFeeds) { FeedRow(feed: $0) }
            ForEach(props.userFeeds) { FeedRow(feed: $0) }
                .onDelete( perform: { set in
                    set.map { props.userFeeds[$0] }.forEach { props.onRemove($0.sourceUrl) }
                })
        }
        .alert(isPresented: $showsAlert, TextAlert(title: "Title") {
            if let url = $0 {
                props.onAdd(url)
            }
        })
        .navigationTitle("Feeds list")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarItems(trailing: Button(action: {
            showsAlert = true
        }) {
            Image(systemName: "plus.circle").imageScale(.large)
        })
    }
}

extension RssFeed: Identifiable { }
