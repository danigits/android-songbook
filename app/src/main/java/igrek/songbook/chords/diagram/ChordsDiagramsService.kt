package igrek.songbook.chords.diagram


import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import igrek.songbook.R
import igrek.songbook.chords.converter.ChordsConverter
import igrek.songbook.chords.detector.UniqueChordsFinder
import igrek.songbook.chords.lyrics.model.LyricsModel
import igrek.songbook.info.UiInfoService
import igrek.songbook.info.UiResourceService
import igrek.songbook.inject.LazyExtractor
import igrek.songbook.inject.LazyInject
import igrek.songbook.inject.appFactory
import igrek.songbook.layout.contextmenu.ContextMenuBuilder
import igrek.songbook.settings.chordsnotation.ChordsNotation
import igrek.songbook.settings.chordsnotation.ChordsNotationService
import igrek.songbook.settings.instrument.ChordsInstrument
import igrek.songbook.settings.instrument.ChordsInstrumentService
import igrek.songbook.settings.preferences.PreferencesState
import java.util.*

class ChordsDiagramsService(
        uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
        uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
        contextMenuBuilder: LazyInject<ContextMenuBuilder> = appFactory.contextMenuBuilder,
        activity: LazyInject<Activity> = appFactory.activity,
        chordsInstrumentService: LazyInject<ChordsInstrumentService> = appFactory.chordsInstrumentService,
        chordsNotationService: LazyInject<ChordsNotationService> = appFactory.chordsNotationService,
        preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
) {
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val contextMenuBuilder by LazyExtractor(contextMenuBuilder)
    private val activity by LazyExtractor(activity)
    private val chordsInstrumentService by LazyExtractor(chordsInstrumentService)
    private val chordsNotationService by LazyExtractor(chordsNotationService)
    private val preferencesState by LazyExtractor(preferencesState)

    private var toEnglishConverter = ChordsConverter(ChordsNotation.default, ChordsNotation.ENGLISH)

    private fun findUniqueChords(crdModel: LyricsModel): Set<String> {
        val chordsFinder = UniqueChordsFinder(chordsInstrumentService.instrument)
        return chordsFinder.findUniqueChordsInLyrics(crdModel)
    }

    private fun chordGraphs(chord: String): String {
        val instrument = chordsInstrumentService.instrument
        val diagramBuilder = ChordDiagramBuilder(instrument, preferencesState.chordDiagramStyle)
        val (engChord: String, errors) = toEnglishConverter.convertChordsGroup(chord)
        if (errors.isNotEmpty()) {
            throw RuntimeException("unrecognized chord: $errors")
        }
        val chordDiagramCodes = getChordDiagrams(instrument)
        return chordDiagramCodes[engChord]
                ?.joinToString(separator = "\n\n\n") { diagramBuilder.buildDiagram(it) }
                ?: ""
    }

    private fun getChordDiagrams(instrument: ChordsInstrument): Map<String, List<String>> {
        return when (instrument) {
            ChordsInstrument.GUITAR -> allGuitarChordsDiagrams
            ChordsInstrument.UKULELE -> allUkuleleChordsDiagrams
            ChordsInstrument.MANDOLIN -> allMandolinChordsDiagrams
        }
    }

    fun showLyricsChordsMenu(crdModel: LyricsModel) {
        toEnglishConverter = ChordsConverter(chordsNotationService.chordsNotation, ChordsNotation.ENGLISH)

        val uniqueChords = findUniqueChords(crdModel)
        if (uniqueChords.isEmpty()) {
            uiInfoService.showInfo(R.string.no_chords_recognized_in_song)
            return
        }

        showUniqueChordsMenu(uniqueChords)
    }

    private fun showUniqueChordsMenu(uniqueChords: Set<String>) {
        val actions = uniqueChords.map { chord ->
            ContextMenuBuilder.Action(chord) {
                showChordDefinition(chord, uniqueChords)
            }
        }.toList()

        contextMenuBuilder.showContextMenu(R.string.choose_a_chord, actions)
    }

    private fun showChordDefinition(chord: String, uniqueChords: Set<String>) {
        val message = chordGraphs(chord)
        val instrument = chordsInstrumentService.instrument
        val instrumentName = uiResourceService.resString(instrument.displayNameResId)
        val title = uiResourceService.resString(R.string.chord_diagrams_versions, chord, instrumentName)

        val alertBuilder = AlertDialog.Builder(activity)
        alertBuilder.setTitle(title)
        alertBuilder.setCancelable(true)

        alertBuilder.setPositiveButton(uiResourceService.resString(R.string.action_close)) { _, _ -> }
        alertBuilder.setNeutralButton(uiResourceService.resString(R.string.action_back)) { _, _ ->
            showUniqueChordsMenu(uniqueChords)
        }

        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val diagramView = inflater.inflate(R.layout.component_chord_diagrams, null, false)
        val diagramContent = diagramView.findViewById<TextView>(R.id.chordDiagramContent)
        diagramContent.text = message
        alertBuilder.setView(diagramView)

        alertBuilder.create().show()
    }

    fun chordDiagramStyleEntries(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        for (item in ChordDiagramStyle.values()) {
            val displayName = uiResourceService.resString(item.nameResId)
            map[item.id.toString()] = displayName
        }
        return map
    }

}