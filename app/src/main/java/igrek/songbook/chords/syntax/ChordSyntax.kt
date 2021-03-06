package igrek.songbook.chords.syntax

val longestChordComparator = Comparator { lhs: String, rhs: String ->
    if (rhs.length != lhs.length) {
        rhs.length - lhs.length
    } else {
        lhs.compareTo(rhs)
    }
}

// these delimiters split chords irrespectively (at any time)
val chordsPrimaryDelimiters = setOf(" ", ",", "\n").toTypedArray()
// these delimiters may split chords but may be a part of chords as well
val singleChordsDelimiters = setOf("(", ")", "-", "/").toTypedArray()
val chordsAllDelimiters = chordsPrimaryDelimiters + singleChordsDelimiters

val chordsGroupRegex = Regex("""\[((.|\n)+?)]""")
val singleChordsSplitRegex = Regex("""(.*?)([ ,\n/()\-])""")

val chordSuffixes = setOf(
        "+",
        "-",
        "-5",
        "-dur",
        "-moll",
        "0",
        "11",
        "11b9",
        "13",
        "13#11",
        "13b9",
        "2",
        "4",
        "4-3",
        "5",
        "5+",
        "6",
        "6+",
        "6-",
        "6-4",
        "6add9",
        "6add11",
        "7",
        "7#5",
        "7#9",
        "7(#5,#9)",
        "7(#5,b9)",
        "7(b5,#9)",
        "7(b5,b9)",
        "7+",
        "7/5+",
        "7/5-",
        "7b5",
        "7b9",
        "7sus2",
        "7sus4",
        "9",
        "9#5",
        "9b5",
        "9sus4",
        "add11",
        "add2",
        "add9",
        "aug",
        "b",
        "dim",
        "dim7",
        "m",
        "m+",
        "m11",
        "m13",
        "m5+",
        "m6",
        "m6+",
        "m6add9",
        "m7",
        "M7",
        "m7#5",
        "m7+",
        "m7/5-",
        "m7b5",
        "m9",
        "madd2",
        "madd9",
        "maj11",
        "maj13",
        "maj13#11",
        "maj7",
        "maj7#5",
        "maj7b5",
        "maj9",
        "maj9#11",
        "mmaj7",
        "mmaj9",
        "o",
        "sus2",
        "sus2sus4",
        "sus4"
)