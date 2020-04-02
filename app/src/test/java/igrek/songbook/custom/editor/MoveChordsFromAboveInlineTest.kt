package igrek.songbook.custom.editor


import android.widget.EditText
import igrek.songbook.dagger.base.BaseDaggerTest
import igrek.songbook.info.UiInfoService
import igrek.songbook.info.UiResourceService
import igrek.songbook.settings.chordsnotation.ChordsNotation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito

class MoveChordsFromAboveInlineTest : BaseDaggerTest() {

    private val transformer = ChordsEditorTransformer(
            contentEdit = Mockito.mock(EditText::class.java),
            history = Mockito.mock(LyricsEditorHistory::class.java),
            chordsNotation = ChordsNotation.GERMAN,
            uiResourceService = Mockito.mock(UiResourceService::class.java),
            uiInfoService = Mockito.mock(UiInfoService::class.java),
    )

    @Test
    fun test_move_single_chords() {
        val lyrics = """
            [a]   [F]
            first line
            [C]    [G]
            second line
            """.trimIndent()
        val transformed = transformer.transformMoveChordsAboveToInline(lyrics)
        val expected = """
            [a]first [F]line
            [C]second [G]line
            """.trimIndent()
        assertThat(transformed).isEqualTo(expected)
        assertThat(transformer.transformMoveChordsAboveToInline(expected)).isEqualTo(expected)
    }

    @Test
    fun test_move_double_chords() {
        val lyrics = """
            [a F]
            first line
            """.trimIndent()
        val transformed = transformer.transformMoveChordsAboveToInline(lyrics)
        val expected = """
            [a F]first line
            """.trimIndent()
        assertThat(transformed).isEqualTo(expected)
        assertThat(transformer.transformMoveChordsAboveToInline(expected)).isEqualTo(expected)
    }

    @Test
    fun test_dont_move_when_already_chords() {
        val lyrics = """
            [a]
            first line [F]
            second line
            third line
            """.trimIndent()
        val transformed = transformer.transformMoveChordsAboveToInline(lyrics)
        assertThat(transformed).isEqualTo(lyrics)
    }

    @Test
    fun test_move_inside_word() {
        val lyrics = """
                [a] [F]
            veryLongWord
            """.trimIndent()
        val transformed = transformer.transformMoveChordsAboveToInline(lyrics)
        val expected = """
            very[a]Long[F]Word
            """.trimIndent()
        assertThat(transformed).isEqualTo(expected)
    }

    @Test
    fun test_chords_longer_than_word() {
        val lyrics = """
              [C]  [a] [F]
            s ort
            """.trimIndent()
        val transformed = transformer.transformMoveChordsAboveToInline(lyrics)
        val expected = """
            s [C]ort  [a]    [F]
            """.trimIndent()
        assertThat(transformed).isEqualTo(expected)
    }

}