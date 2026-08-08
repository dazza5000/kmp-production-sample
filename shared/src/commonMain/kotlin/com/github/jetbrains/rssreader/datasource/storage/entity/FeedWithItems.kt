package com.github.jetbrains.rssreader.datasource.storage.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.github.jetbrains.rssreader.domain.Channel
import com.github.jetbrains.rssreader.domain.Image
import com.github.jetbrains.rssreader.domain.Item
import com.github.jetbrains.rssreader.domain.MediaContent
import com.github.jetbrains.rssreader.domain.RssFeed

data class FeedWithItems(
    @Embedded val feed: FeedEntity,
    @Relation(
        parentColumn = "url",
        entityColumn = "feedUrl"
    )
    val items: List<ItemEntity>
)

fun FeedWithItems.toDomain(): RssFeed {
    val domainItems = items.map { item ->
        Item(
            title = item.title,
            pubDate = item.pubDate,
            link = item.link,
            guid = item.guid,
            description = item.description,
            contentEncoded = item.contentEncoded,
            mediaContent = item.mediaUrl?.let { MediaContent(url = it) }
        )
    }
    val channel = Channel(
        title = feed.title,
        description = feed.description,
        link = feed.link,
        copyright = feed.copyright,
        item = domainItems,
        image = feed.imageUrl?.let { Image(url = it, title = feed.title, link = feed.link, width = null, height = null) }
    )
    return RssFeed(
        version = "2.0",
        sourceUrl = feed.url,
        isDefault = feed.isDefault,
        channel = channel
    )
}

fun RssFeed.toEntities(): Pair<FeedEntity, List<ItemEntity>> {
    val feedEntity = FeedEntity(
        url = sourceUrl,
        title = channel?.title,
        description = channel?.description,
        link = channel?.link,
        copyright = channel?.copyright,
        imageUrl = channel?.image?.url,
        isDefault = isDefault
    )
    val itemEntities = (channel?.item ?: emptyList()).map { item ->
        ItemEntity(
            guid = item.guid,
            feedUrl = sourceUrl,
            title = item.title,
            pubDate = item.pubDate,
            link = item.link,
            description = item.description,
            contentEncoded = item.contentEncoded,
            mediaUrl = item.mediaContent?.url
        )
    }
    return Pair(feedEntity, itemEntities)
}
