package com.instadrop.app.domain

import com.instadrop.app.domain.model.PostKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramUrlTest {

    @Test
    fun extracts_bare_url() {
        assertEquals(
            "https://www.instagram.com/reel/Cabc123",
            InstagramUrl.extract("https://www.instagram.com/reel/Cabc123"),
        )
    }

    @Test
    fun extracts_url_from_surrounding_text() {
        assertEquals(
            "https://www.instagram.com/p/Cxyz789",
            InstagramUrl.extract("Check this out: https://www.instagram.com/p/Cxyz789 so cool!"),
        )
    }

    @Test
    fun strips_tracking_query_and_fragment() {
        assertEquals(
            "https://www.instagram.com/reel/Cabc123",
            InstagramUrl.extract("https://www.instagram.com/reel/Cabc123?igsh=tracking#frag"),
        )
    }

    @Test
    fun trims_trailing_punctuation() {
        assertEquals(
            "https://instagram.com/p/Cabc",
            InstagramUrl.extract("(see https://instagram.com/p/Cabc)."),
        )
    }

    @Test
    fun rejects_non_instagram_text() {
        assertNull(InstagramUrl.extract("https://example.com/p/Cabc"))
        assertNull(InstagramUrl.extract("just some words"))
        assertNull(InstagramUrl.extract(null))
        assertNull(InstagramUrl.extract(""))
    }

    @Test
    fun classifies_by_path() {
        assertEquals(PostKind.REEL, InstagramUrl.classify("https://www.instagram.com/reel/Cabc"))
        assertEquals(PostKind.POST, InstagramUrl.classify("https://www.instagram.com/p/Cabc"))
        assertEquals(PostKind.STORY, InstagramUrl.classify("https://www.instagram.com/stories/user/123"))
        assertEquals(PostKind.PROFILE_PIC, InstagramUrl.classify("https://www.instagram.com/someuser"))
    }
}
