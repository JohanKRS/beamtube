package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoLinksTest {
    @Test
    fun readsTheCommonLinkShapes() {
        assertEquals(VideoLink("dQw4w9WgXcQ"), VideoLinks.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(VideoLink("dQw4w9WgXcQ"), VideoLinks.parse("https://m.youtube.com/watch?feature=share&v=dQw4w9WgXcQ"))
        assertEquals(VideoLink("dQw4w9WgXcQ"), VideoLinks.parse("https://youtu.be/dQw4w9WgXcQ?si=abcdEFGH"))
        assertEquals(VideoLink("dQw4w9WgXcQ"), VideoLinks.parse("https://www.youtube.com/live/dQw4w9WgXcQ?si=x"))
        assertEquals(VideoLink("dQw4w9WgXcQ"), VideoLinks.parse("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals(VideoLink("dQw4w9WgXcQ"), VideoLinks.parse("http://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RD"))
    }

    @Test
    fun keepsTheStartTime() {
        assertEquals(VideoLink("dQw4w9WgXcQ", 42), VideoLinks.parse("https://youtu.be/dQw4w9WgXcQ?t=42"))
        assertEquals(VideoLink("dQw4w9WgXcQ", 90), VideoLinks.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1m30s"))
        assertEquals(VideoLink("dQw4w9WgXcQ", 3723), VideoLinks.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1h2m3s"))
    }

    @Test
    fun findsTheLinkInsideSharedText() {
        val shared = "Olha isso: https://youtu.be/dQw4w9WgXcQ?si=abc. Muito bom!"
        assertEquals(VideoLink("dQw4w9WgXcQ"), VideoLinks.parse(shared))
    }

    @Test
    fun ignoresWhatIsNotAVideo() {
        assertNull(VideoLinks.parse("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(VideoLinks.parse("https://www.youtube.com/@canal"))
        assertNull(VideoLinks.parse("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(VideoLinks.parse("só texto"))
        assertNull(VideoLinks.parse(null))
    }
}
