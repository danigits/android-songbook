package igrek.songbook.chords.lyrics

import igrek.songbook.chords.lyrics.model.LyricsFragment
import igrek.songbook.chords.lyrics.model.LyricsLine
import igrek.songbook.chords.lyrics.model.LyricsTextType
import igrek.songbook.chords.lyrics.wrapper.DoubleLineWrapper

class ChordsAboveArranger(
        screenWRelative: Float,
        private val lengthMapper: TypefaceLengthMapper
) {
    private val doubleLineWrapper = DoubleLineWrapper(
            screenWRelative = screenWRelative,
            lengthMapper = lengthMapper,
    )

    fun arrangeLine(line: LyricsLine): List<LyricsLine> {
        calculateXPositions(line.fragments)

        val chords = LyricsLine(filterFragments(line.fragments, LyricsTextType.CHORDS))
        val texts = LyricsLine(filterFragments(line.fragments, LyricsTextType.REGULAR_TEXT))
        preventChordsOverlapping(chords.fragments, texts.fragments)

        val lines = doubleLineWrapper.wrapDoubleLine(chords = chords, texts = texts)

        return lines.onEach(this::postProcessLine)
    }

    private fun postProcessLine(line: LyricsLine): LyricsLine {
        // cleanup blank fragments
        val fragments = line.fragments
                .onEach { fragment -> fragment.text = fragment.text.trimEnd() }
                .filter { fragment -> fragment.text.isNotBlank() }

        return LyricsLine(fragments)
    }

    private fun calculateXPositions(fragments: List<LyricsFragment>) {
        var x = 0f
        fragments.forEach { fragment ->
            fragment.x = x
            if (fragment.type == LyricsTextType.REGULAR_TEXT) {
                x += fragment.width
            }
        }
    }

    private fun preventChordsOverlapping(chords: List<LyricsFragment>, texts: List<LyricsFragment>) {
        chords.forEachIndexed { index, chord ->
            chords.getOrNull(index - 1)
                    ?.let {
                        val spaceWidth = lengthMapper.charWidth(chord.type, ' ')
                        val overlappedBy = it.x + it.width - chord.x
                        if (overlappedBy > 0) {
                            texts.filter { f -> f.x > chord.x }.forEach { f -> f.x += overlappedBy }
                            chord.x += overlappedBy + spaceWidth
                        }
                    }
        }
    }

}