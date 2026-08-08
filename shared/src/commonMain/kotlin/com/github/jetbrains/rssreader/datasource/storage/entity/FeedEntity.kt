package com.github.jetbrains.rssreader.datasource.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeds")
data class FeedEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val description: String?,
    val link: String?,
    val copyright: String?,
    val imageUrl: String?,
    val isDefault: Boolean
)
