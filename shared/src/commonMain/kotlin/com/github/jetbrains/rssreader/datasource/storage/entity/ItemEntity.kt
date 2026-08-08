package com.github.jetbrains.rssreader.datasource.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index(value = ["feedUrl"])]
)
data class ItemEntity(
    @PrimaryKey val guid: String,
    val feedUrl: String,
    val title: String?,
    val pubDate: String?,
    val link: String?,
    val description: String?,
    val contentEncoded: String?,
    val mediaUrl: String?
)
