package com.github.jetbrains.rssreader.datasource.storage.entity

import androidx.room3.Embedded
import androidx.room3.Relation
import com.github.jetbrains.rssreader.domain.RssFeed

data class FeedWithItems(
    @Embedded val feed: FeedEntity,
    @Relation(
        parentColumns = ["url"],
        entityColumns = ["feedUrl"]
    )
    val items: List<ItemEntity>
) {
    fun toDomain(): RssFeed = feed.toDomain(items)

    companion object {
        fun fromDomain(feed: RssFeed): Pair<FeedEntity, List<ItemEntity>> {
            val feedEntity = FeedEntity(
                url = feed.sourceUrl,
                title = feed.channel?.title ?: "",
                description = feed.channel?.description ?: "",
                link = feed.channel?.link ?: "",
                copyright = feed.channel?.copyright,
                imageUrl = feed.channel?.image?.url,
                isDefault = feed.isDefault
            )
            val itemEntities = (feed.channel?.item ?: emptyList()).map { item ->
                ItemEntity(
                    feedUrl = feed.sourceUrl,
                    guid = item.guid,
                    title = item.title,
                    link = item.link,
                    pubDate = item.pubDate,
                    description = item.description,
                    contentEncoded = item.contentEncoded,
                    mediaContentUrl = item.mediaContent?.url
                )
            }
            return Pair(feedEntity, itemEntities)
        }
    }
}
