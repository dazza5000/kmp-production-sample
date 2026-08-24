package com.github.jetbrains.rssreader.domain

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalXmlUtilApi::class)
class RssFeedParsingTest {

    private val xml = XML {
        defaultPolicy {
            unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
        }
    }

    @Test
    fun testRssFeedParsingFromXml() {
        val feed = xml.decodeFromString(RssFeed.serializer(), RSS_FIXTURE_XML)

        // feed version
        assertEquals("2.0", feed.version)

        // channel assertions
        val channel = feed.channel
        assertNotNull(channel)
        assertEquals("Kotlin Blog", channel.title)
        assertEquals(
            "The official Kotlin blog covering language updates, Multiplatform, and libraries.",
            channel.description
        )
        assertEquals("https://blog.jetbrains.com/kotlin/", channel.link)

        // items count
        val items = channel.item
        assertEquals(3, items.size)

        // Item 1: all standard fields, description, no contentEncoded, no mediaContent
        val item1 = items[0]
        assertEquals("Kotlin Multiplatform in Production", item1.title)
        assertEquals("Mon, 10 Aug 2026 08:00:00 +0000", item1.pubDate)
        assertEquals("https://blog.jetbrains.com/kotlin/2026/08/kmp-in-production/", item1.link)
        assertEquals("kmp-prod-1001", item1.guid)
        assertEquals(
            "A deep dive into cross-platform architecture with Kotlin Multiplatform.",
            item1.description
        )
        assertNull(item1.contentEncoded)
        assertNull(item1.mediaContent)
        assertNull(item1.getImageUrl())

        // Item 2: content:encoded with HTML image, no description
        val item2 = items[1]
        assertEquals("State Management in KMP", item2.title)
        assertEquals("Tue, 11 Aug 2026 09:30:00 +0000", item2.pubDate)
        assertEquals("https://blog.jetbrains.com/kotlin/2026/08/state-management/", item2.link)
        assertEquals("kmp-state-1002", item2.guid)
        assertNull(item2.description)
        assertNotNull(item2.contentEncoded)
        assertNull(item2.mediaContent)
        assertEquals("https://example.com/images/kmp-banner.png", item2.getImageUrl())

        // Item 3: media:enclosure, minimal fields
        val item3 = items[2]
        assertEquals("Compose Multiplatform Highlights", item3.title)
        assertEquals("Wed, 12 Aug 2026 12:00:00 +0000", item3.pubDate)
        assertEquals("https://blog.jetbrains.com/kotlin/2026/08/cmp-highlights/", item3.link)
        assertEquals("cmp-highlights-1003", item3.guid)
        assertNull(item3.contentEncoded)
        assertNotNull(item3.mediaContent)
        assertEquals("https://example.com/images/cmp-enclosure.jpg", item3.mediaContent.url)
        assertEquals("https://example.com/images/cmp-enclosure.jpg", item3.getImageUrl())
    }

    companion object {
        const val RSS_FIXTURE_XML = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:media="http://search.yahoo.com/mrss/">
    <channel>
        <title>Kotlin Blog</title>
        <description>The official Kotlin blog covering language updates, Multiplatform, and libraries.</description>
        <link>https://blog.jetbrains.com/kotlin/</link>
        <generator>CustomGenerator 1.0</generator>
        <unknownChannelElement>ExtraData</unknownChannelElement>

        <item>
            <title>Kotlin Multiplatform in Production</title>
            <pubDate>Mon, 10 Aug 2026 08:00:00 +0000</pubDate>
            <link>https://blog.jetbrains.com/kotlin/2026/08/kmp-in-production/</link>
            <guid>kmp-prod-1001</guid>
            <description>A deep dive into cross-platform architecture with Kotlin Multiplatform.</description>
            <unknownItemElement>SomeExtraValue</unknownItemElement>
        </item>

        <item>
            <title>State Management in KMP</title>
            <pubDate>Tue, 11 Aug 2026 09:30:00 +0000</pubDate>
            <link>https://blog.jetbrains.com/kotlin/2026/08/state-management/</link>
            <guid>kmp-state-1002</guid>
            <content:encoded><![CDATA[<p>Here is an update on state management.</p><img src="https://example.com/images/kmp-banner.png" alt="Banner"/><p>More details follow...</p>]]></content:encoded>
        </item>

        <item>
            <title>Compose Multiplatform Highlights</title>
            <pubDate>Wed, 12 Aug 2026 12:00:00 +0000</pubDate>
            <link>https://blog.jetbrains.com/kotlin/2026/08/cmp-highlights/</link>
            <guid>cmp-highlights-1003</guid>
            <media:enclosure url="https://example.com/images/cmp-enclosure.jpg" type="image/jpeg" width="800" height="600">
                <url>https://example.com/images/cmp-enclosure.jpg</url>
                <media:url>https://example.com/images/cmp-enclosure.jpg</media:url>
                <type>image/jpeg</type>
                <width>800</width>
                <height>600</height>
                <media:title>CMP Overview</media:title>
                <media:description>Compose Multiplatform visual guide</media:description>
            </media:enclosure>
        </item>
    </channel>
</rss>"""
    }
}
