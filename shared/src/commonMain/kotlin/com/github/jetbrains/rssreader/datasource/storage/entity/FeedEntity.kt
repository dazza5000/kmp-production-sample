package com.github.jetbrains.rssreader.datasource.storage.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.github.jetbrains.rssreader.domain.Channel
import com.github.jetbrains.rssreader.domain.Image
import com.github.jetbrains.rssreader.domain.RssFeed

@Entity(tableName = "feeds")
data class FeedEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String,
    val link: String,
    val copyright: String?,
    val imageUrl: String?,
    val isDefault: Boolean = false
) {
    fun toDomain(items: List<ItemEntity>) = RssFeed(
        version = "2.0",
        sourceUrl = url,
        channel = Channel(
            title = title,
            description = description,
            link = link,
            copyright = copyright,
            item = items.map { it.toDomain() },
            image = imageUrl?.let { Image(url = it, title = null, link = null, width = null, height = null) }
        ),
        isDefault = isDefault
    )
}
