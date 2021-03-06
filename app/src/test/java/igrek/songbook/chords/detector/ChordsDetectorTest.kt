package igrek.songbook.chords.detector

import igrek.songbook.settings.chordsnotation.ChordsNotation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ChordsDetectorTest {

    @Test
    fun test_recognize_major() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.recognizeSingleChord("C")).isEqualTo(Chord(0, false, ""))
        assertThat(detector.recognizeSingleChord("C#")).isEqualTo(Chord(1, false, ""))
        assertThat(detector.recognizeSingleChord("D")).isEqualTo(Chord(2, false, ""))

        assertThat(detector.recognizeSingleChord("Csus4")).isEqualTo(Chord(0, false, "sus4"))
        assertThat(detector.recognizeSingleChord("C#sus4")).isEqualTo(Chord(1, false, "sus4"))
        assertThat(detector.recognizeSingleChord("Dsus4")).isEqualTo(Chord(2, false, "sus4"))
    }

    @Test
    fun test_recognize_minor() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.recognizeSingleChord("c")).isEqualTo(Chord(0, true, ""))
        assertThat(detector.recognizeSingleChord("c#")).isEqualTo(Chord(1, true, ""))
        assertThat(detector.recognizeSingleChord("d")).isEqualTo(Chord(2, true, ""))

        assertThat(detector.recognizeSingleChord("csus4")).isEqualTo(Chord(0, true, "sus4"))
        assertThat(detector.recognizeSingleChord("c#sus4")).isEqualTo(Chord(1, true, "sus4"))
        assertThat(detector.recognizeSingleChord("dsus4")).isEqualTo(Chord(2, true, "sus4"))

        assertThat(detector.recognizeSingleChord("dis")).isEqualTo(Chord(3, true, ""))
        assertThat(detector.recognizeSingleChord("es")).isEqualTo(Chord(3, true, ""))
        assertThat(detector.recognizeSingleChord("dupa")).isNull()
    }

    @Test
    fun test_detect_fmaj7() {
        val detector = ChordsDetector(ChordsNotation.ENGLISH)
        assertThat(detector.recognizeSingleChord("Fmaj7")).isEqualTo(Chord(5, false, "maj7"))
        assertThat(detector.detectAndMarkChords("Fmaj7")).isEqualTo("[Fmaj7]")
        assertThat(detector.isWordAChord("Fmaj7")).isTrue()
    }

    @Test
    fun test_mark_chords() {
        val detector = ChordsDetector(ChordsNotation.ENGLISH)
        assertThat(detector.detectAndMarkChords("Fm")).isEqualTo("[Fm]")
    }

    @Test
    fun test_c_plus() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.recognizeSingleChord("C+")).isEqualTo(Chord(0, false, "+"))
        assertThat(detector.isWordAChord("C+")).isTrue()
    }

    @Test
    fun test_c_minus() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.recognizeSingleChord("C-")).isEqualTo(Chord(0, false, "-"))
        assertThat(detector.isWordAChord("C-")).isTrue()
    }

    @Test
    fun test_detect_parentheses_Ch() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.isWordAChord("C")).isTrue()
        assertThat(detector.isWordAChord("C-h")).isTrue()
        assertThat(detector.isWordAChord("C-h)")).isTrue()
        assertThat(detector.isWordAChord("(C-h)")).isTrue()
        assertThat(detector.isWordAChord("D(C-h)")).isTrue()
    }

    @Test
    fun test_not_a_chord() {
        var detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.isWordAChord("Dupa")).isFalse()
        assertThat(detector.isWordAChord("Cmaj7blahblahblah")).isFalse()
        assertThat(detector.isWordAChord("Dsus888")).isFalse()
        assertThat(detector.isWordAChord(" ")).isFalse()
        assertThat(detector.isWordAChord("")).isFalse()
        detector = ChordsDetector(ChordsNotation.ENGLISH)
        assertThat(detector.recognizeSingleChord("Dmupa")).isNull()
    }

    @Test
    fun test_suffix() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.isWordAChord("Fmaj7")).isTrue()
        assertThat(detector.isWordAChord("G#maj7-F")).isTrue()
    }

    @Test
    fun test_minor_chords() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.isWordAChord("cmaj7")).isTrue()
    }

    @Test
    fun test_cis() {
        val detector = ChordsDetector(ChordsNotation.GERMAN_IS)
        assertThat(detector.isWordAChord("cismaj7")).isTrue()
    }

    @Test
    fun test_all_notations() {
        val detector = ChordsDetector()
        assertThat(detector.isWordAChord("cismaj7")).isTrue()
        assertThat(detector.isWordAChord("Cm")).isTrue()
        assertThat(detector.isWordAChord("C#")).isTrue()
        assertThat(detector.isWordAChord("C#add9")).isTrue()
    }

    @Test
    fun test_detect_long_spaced_chords() {
        val detector = ChordsDetector(ChordsNotation.ENGLISH)
        assertThat(detector.detectAndMarkChords("Am    Fm   G")).isEqualTo("[Am    Fm   G]")
    }

    @Test
    fun test_detect_dashed_chords() {
        val detector = ChordsDetector(ChordsNotation.ENGLISH)
        assertThat(detector.detectAndMarkChords("Am-G-C")).isEqualTo("[Am-G-C]")
    }

    @Test
    fun test_detect_english_minor() {
        val detector = ChordsDetector(ChordsNotation.ENGLISH)
        assertThat(detector.recognizeSingleChord("Dm")).isEqualTo(Chord(2, true, ""))
        assertThat(detector.recognizeSingleChord("Dmmaj7")).isEqualTo(Chord(2, true, "maj7"))
        assertThat(detector.recognizeSingleChord("Dmaj7")).isEqualTo(Chord(2, false, "maj7"))
        assertThat(detector.recognizeSingleChord("D")).isEqualTo(Chord(2, false, ""))
    }

    @Test
    fun test_slashed_chord() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.isWordAChord("C/H")).isTrue()
        assertThat(detector.isWordAChord("C/Y")).isFalse()
        assertThat(detector.isWordAChord("a/G")).isTrue()
    }

    @Test
    fun test_g6add11() {
        val detector = ChordsDetector(ChordsNotation.GERMAN)
        assertThat(detector.recognizeSingleChord("G6add11")).isNotNull
        assertThat(detector.recognizeSingleChord("G6add11")).isEqualTo(Chord(noteIndex = 7, minor = false, suffix = "6add11"))
    }

}
