package com.github.jetbrains.rssreader.datasource.storage.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.github.jetbrains.rssreader.domain.Item
import com.github.jetbrains.rssreader.domain.MediaContent

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["url"],
            childColumns = ["feedUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("feedUrl")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedUrl: String,
    val guid: String,
    val title: String?,
    val link: String?,
    val pubDate: String?,
    val description: String?,
    val contentEncoded: String?,
    val mediaContentUrl: String?
) {
    fun toDomain() = Item(
        title = title,
        pubDate = pubDate,
        link = link,
        guid = guid,
        description = description,
        contentEncoded = contentEncoded,
        mediaContent = mediaContentUrl?.let { MediaContent(url = it) }
    )
}
