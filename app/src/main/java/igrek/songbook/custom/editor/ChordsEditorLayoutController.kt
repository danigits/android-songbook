package igrek.songbook.custom.editor

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dagger.Lazy
import igrek.songbook.R
import igrek.songbook.chords.converter.ChordsConverter
import igrek.songbook.chords.detector.ChordsDetector
import igrek.songbook.custom.EditSongLayoutController
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.info.UiInfoService
import igrek.songbook.info.UiResourceService
import igrek.songbook.info.errorcheck.SafeClickListener
import igrek.songbook.layout.LayoutController
import igrek.songbook.layout.MainLayout
import igrek.songbook.layout.contextmenu.ContextMenuBuilder
import igrek.songbook.layout.dialog.ConfirmDialogBuilder
import igrek.songbook.layout.navigation.NavigationMenuController
import igrek.songbook.settings.chordsnotation.ChordsNotation
import igrek.songbook.system.SoftKeyboardService
import javax.inject.Inject


class ChordsEditorLayoutController : MainLayout {

    @Inject
    lateinit var layoutController: LayoutController
    @Inject
    lateinit var uiInfoService: UiInfoService
    @Inject
    lateinit var uiResourceService: UiResourceService
    @Inject
    lateinit var activity: AppCompatActivity
    @Inject
    lateinit var navigationMenuController: NavigationMenuController
    @Inject
    lateinit var editSongLayoutController: Lazy<EditSongLayoutController>
    @Inject
    lateinit var softKeyboardService: SoftKeyboardService
    @Inject
    lateinit var contextMenuBuilder: ContextMenuBuilder

    private var contentEdit: EditText? = null
    private var clipboardChords: String? = null
    private var layout: View? = null
    private var chordsNotation: ChordsNotation? = null
    private var history = LyricsEditorHistory()

    init {
        DaggerIoc.factoryComponent.inject(this)
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.screen_chords_editor
    }

    override fun showLayout(layout: View) {
        this.layout = layout

        // Toolbar
        val toolbar1 = layout.findViewById<Toolbar>(R.id.toolbar1)
        activity.setSupportActionBar(toolbar1)
        val actionBar = activity.supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setDisplayShowHomeEnabled(false)
        }
        // navigation menu button
        val navMenuButton = layout.findViewById<ImageButton>(R.id.navMenuButton)
        navMenuButton.setOnClickListener { navigationMenuController.navDrawerShow() }

        val goBackButton = layout.findViewById<ImageButton>(R.id.goBackButton)
        goBackButton.setOnClickListener(SafeClickListener {
            onBackClicked()
        })

        val tooltipEditChordsLyricsInfo = layout.findViewById<ImageButton>(R.id.tooltipEditChordsLyricsInfo)
        tooltipEditChordsLyricsInfo.setOnClickListener {
            uiInfoService.showTooltip(R.string.tooltip_edit_chords_lyrics)
        }

        buttonOnClick(R.id.addChordButton) { onAddChordClick() }
        buttonOnClick(R.id.addChordSplitterButton) { addChordSplitter() }
        buttonOnClick(R.id.copyChordButton) { onCopyChordClick() }
        buttonOnClick(R.id.pasteChordButton) { onPasteChordClick() }
        buttonOnClick(R.id.detectChordsButton) { wrapHistoryContext { detectChords() } }
        buttonOnClick(R.id.undoChordsButton) { undoChange() }
        buttonOnClick(R.id.transformChordsButton) { showTransformMenu() }
        buttonOnClick(R.id.moveLeftButton) { moveCursor(-1) }
        buttonOnClick(R.id.moveRightButton) { moveCursor(+1) }
        buttonOnClick(R.id.validateChordsButton) { validateChords() }
        buttonOnClick(R.id.reformatTrimButton) { reformatAndTrim() }

        contentEdit = layout.findViewById(R.id.songContentEdit)
        contentEdit?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (start == 0 && count == s?.length) {
                    return // skip in order not to save undo / transforming operations again
                }
                history.save(contentEdit!!)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        softKeyboardService.showSoftKeyboard(contentEdit)
    }

    private fun buttonOnClick(@IdRes buttonId: Int, onclickAction: () -> Unit) {
        val chordsNotationButton = layout?.findViewById<Button>(buttonId)
        chordsNotationButton?.setOnClickListener(SafeClickListener {
            onclickAction()
        })
    }

    private fun showTransformMenu() {
        val actions = listOf(
                ContextMenuBuilder.Action(R.string.chords_editor_move_chords_to_right) {
                    wrapHistoryContext { moveChordsAboveToRight() }
                },
                ContextMenuBuilder.Action(R.string.chords_editor_fis_to_sharp) {
                    wrapHistoryContext { chordsFisTofSharp() }
                },
                ContextMenuBuilder.Action(R.string.chords_editor_convert_from_notation) {
                    wrapHistoryContext { convertFromOtherNotationDialog() }
                }
        )
        contextMenuBuilder.showContextMenu(R.string.edit_song_transform_chords, actions)
    }

    private fun convertFromOtherNotationDialog() {
        val actions = ChordsNotation.values().map { notation ->
            ContextMenuBuilder.Action(notation.displayNameResId) {
                convertFromNotation(notation)
            }
        }
        ContextMenuBuilder().showContextMenu(R.string.chords_editor_convert_from_notation, actions)
    }

    private fun convertFromNotation(fromNotation: ChordsNotation) {
        val converter = ChordsConverter(fromNotation, chordsNotation ?: ChordsNotation.default)
        val converted = converter.convertLyrics(contentEdit!!.text.toString())
        contentEdit?.setText(converted)
    }

    private fun wrapHistoryContext(action: () -> Unit) {
        history.save(contentEdit!!)
        action.invoke()
        restoreSelectionFromHistory()
    }

    private fun validateChords() {
        val errorMessage = quietValidate()
        if (errorMessage == null) {
            uiInfoService.showToast(R.string.chords_are_valid)
        } else {
            val placeholder = uiResourceService.resString(R.string.chords_invalid)
            uiInfoService.showToast(placeholder.format(errorMessage))
        }
    }

    private fun quietValidate(): String? {
        val text = contentEdit!!.text.toString()
        return try {
            validateChordsBrackets(text)
            validateChordsNotation(text)
            null
        } catch (e: ChordsValidationError) {
            var errorMessage = e.errorMessage
            if (errorMessage.isNullOrEmpty())
                errorMessage = uiResourceService.resString(e.messageResId!!)
            errorMessage
        }
    }

    private fun validateChordsBrackets(text: String) {
        var inBracket = false
        for (char in text) {
            when (char) {
                '[' -> {
                    if (inBracket)
                        throw ChordsValidationError(R.string.chords_invalid_missing_closing_bracket)
                    inBracket = true
                }
                ']' -> {
                    if (!inBracket)
                        throw ChordsValidationError(R.string.chords_invalid_missing_opening_bracket)
                    inBracket = false
                }
            }
        }
        if (inBracket)
            throw ChordsValidationError(R.string.chords_invalid_missing_closing_bracket)
    }

    private fun validateChordsNotation(text: String) {
        val detector = ChordsDetector(chordsNotation)
        text.replace(Regex("""\[((.|\n)+?)]""")) { matchResult ->
            validateChordsGroup(matchResult.groupValues[1], detector)
            ""
        }
    }

    private fun validateChordsGroup(chordsGroup: String, detector: ChordsDetector) {
        val chords = chordsGroup.split(" ", "\n", "(", ")")
        chords.forEach { chord ->
            if (chord.isNotEmpty() && !detector.isWordAChord(chord)) {
                val placeholder = uiResourceService.resString(R.string.chords_unknown_chord)
                val errorMessage = placeholder.format(chord)
                throw ChordsValidationError(errorMessage)
            }
        }
    }

    private fun chordsFisTofSharp() {
        transformChords { chord ->
            chord.replace(Regex("""(\w)is"""), "$1#")
        }
    }

    private fun moveChordsAboveToRight() {
        reformatAndTrim()
        transformLyrics { lyrics ->
            var c = "\n" + lyrics + "\n"
            c = c.replace(Regex("""\n\[(.+)]\n(\w.+)\n"""), "\n$2 [$1]\n")
            c.drop(1).dropLast(1)
        }
    }

    private fun reformatAndTrim() {
        transformLines { line ->
            line.trim()
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replace("\t", " ")
                    .replace("\u00A0", " ")
                    .replace(Regex("""\[+"""), "[")
                    .replace(Regex("""]+"""), "]")
                    .replace(Regex("""\[ +"""), "[")
                    .replace(Regex(""" +]"""), "]")
                    .replace(Regex("""] ?\["""), " ") // join adjacent chords
                    .replace(Regex("""\[]"""), "")
                    .replace(Regex(""" +"""), " ") // double+ spaces
        }
        transformLyrics { lyrics ->
            lyrics.replace(Regex("\n\n+"), "\n\n") // max 1 empty line
                    .replace(Regex("^\n+"), "")
                    .replace(Regex("\n+$"), "")
        }
    }

    private fun transformLyrics(transformer: (String) -> String) {
        val text = contentEdit!!.text.toString()
        contentEdit!!.setText(transformer.invoke(text))
    }

    private fun transformLines(transformer: (String) -> String) {
        val text = contentEdit!!.text.toString()
                .lines().joinToString(separator = "\n") { line ->
                    transformer.invoke(line)
                }
        contentEdit!!.setText(text)
    }

    private fun transformChords(transformer: (String) -> String) {
        val text = contentEdit!!.text.toString()
                .replace(Regex("""\[(.*?)]""")) { matchResult ->
                    "[" + transformer.invoke(matchResult.groupValues[1]) + "]"
                }
        contentEdit!!.setText(text)
    }

    private fun detectChords() {
        reformatAndTrim()
        val detector = ChordsDetector(chordsNotation)
        transformLyrics { lyrics ->
            detector.detectAndMarkChords(lyrics)
        }
        val detectedChordsNum = detector.detectedChords.size
        if (detectedChordsNum == 0) {
            // find chords from other notations as well
            val text = contentEdit!!.text.toString()
            val allNotationsDetector = ChordsDetector()
            allNotationsDetector.detectAndMarkChords(text)
            val otherChordsDetected = allNotationsDetector.detectedChords
            if (otherChordsDetected.isNotEmpty()) {
                val message = uiResourceService.resString(R.string.editor_other_chords_detected, otherChordsDetected.joinToString())
                uiInfoService.showToast(message)
            } else {
                uiInfoService.showToast(R.string.no_new_chords_detected)
            }
        } else {
            uiInfoService.showToast(uiResourceService.resString(R.string.new_chords_detected, detectedChordsNum.toString()))
        }
    }

    private fun onCopyChordClick() {
        val edited = contentEdit!!.text.toString()
        val selStart = contentEdit!!.selectionStart
        val selEnd = contentEdit!!.selectionEnd

        var selection = edited.substring(selStart, selEnd).trim()
        if (selection.startsWith("["))
            selection = selection.drop(1)
        if (selection.endsWith("]"))
            selection = selection.dropLast(1)
        clipboardChords = selection.trim()

        if (clipboardChords.isNullOrEmpty()) {
            uiInfoService.showToast(R.string.no_chords_selected)
        } else {
            uiInfoService.showToast(uiResourceService.resString(R.string.chords_copied, clipboardChords))
        }
    }

    private fun onPasteChordClick() {
        history.save(contentEdit!!)
        if (clipboardChords.isNullOrEmpty()) {
            uiInfoService.showToast(R.string.paste_chord_empty)
            return
        }

        var edited = contentEdit!!.text.toString()
        val selStart = contentEdit!!.selectionStart
        var selEnd = contentEdit!!.selectionEnd
        val before = edited.take(selStart)
        val after = edited.drop(selEnd)

        edited = "$before[$clipboardChords]$after"
        selEnd = selStart + 2 + clipboardChords!!.length

        setContentWithSelection(edited, selStart, selEnd)
    }

    private fun addChordSplitter() {
        history.save(contentEdit!!)
        val edited = contentEdit!!.text.toString()
        val selStart = contentEdit!!.selectionStart
        val before = edited.take(selStart)
        // count previous opening and closing brackets
        val opening = before.count { c -> c == '[' }
        val closing = before.count { c -> c == ']' }

        if (opening > closing) {
            onAddSequenceClick("]")
        } else {
            onAddSequenceClick("[")
        }
    }

    private fun onAddSequenceClick(s: String) {
        var edited = contentEdit!!.text.toString()
        var selStart = contentEdit!!.selectionStart
        var selEnd = contentEdit!!.selectionEnd
        val before = edited.take(selStart)
        val after = edited.drop(selEnd)

        edited = "$before$s$after"
        selStart += s.length
        selEnd = selStart

        setContentWithSelection(edited, selStart, selEnd)
    }

    private fun onAddChordClick() {
        history.save(contentEdit!!)

        var edited = contentEdit!!.text.toString()
        var selStart = contentEdit!!.selectionStart
        var selEnd = contentEdit!!.selectionEnd
        val before = edited.take(selStart)
        val after = edited.drop(selEnd)

        // if there's nonempty selection
        if (selStart < selEnd) {
            val selected = edited.substring(selStart, selEnd)
            edited = "$before[$selected]$after"
            selStart++
            selEnd++
        } else { // just single cursor
            // clicked twice accidentaly
            if (before.endsWith("[") && after.startsWith("]")) {
                return
            }
            // if it's the end of line AND there is no space before
            if ((after.isEmpty() || after.startsWith("\n")) && before.isNotEmpty() && !before.endsWith(" ") && !before.endsWith("\n")) {
                // insert missing space
                edited = "$before []$after"
                selStart += 2
            } else {
                edited = "$before[]$after"
                selStart += 1
            }
            selEnd = selStart
        }

        setContentWithSelection(edited, selStart, selEnd)
    }

    private fun setContentWithSelection(edited: String, selStart: Int, selEnd: Int) {
        contentEdit?.setText(edited)
        contentEdit?.setSelection(selStart, selEnd)
        contentEdit?.requestFocus()
    }

    private fun restoreSelectionFromHistory() {
        val lastSelection = history.peekLastSelection()
        if (lastSelection != null) {
            var selStart = lastSelection.first
            var selEnd = lastSelection.second
            val maxLength = contentEdit!!.text.length
            if (selStart > maxLength)
                selStart = maxLength
            if (selEnd > maxLength)
                selEnd = maxLength
            contentEdit?.setSelection(selStart, selEnd)
            contentEdit?.requestFocus()
        }
    }

    private fun moveCursor(delta: Int) {
        val edited = contentEdit!!.text.toString()
        var selStart = contentEdit!!.selectionStart

        selStart += delta
        if (selStart < 0)
            selStart = 0
        if (selStart > edited.length)
            selStart = edited.length

        contentEdit!!.setSelection(selStart, selStart)
        contentEdit!!.requestFocus()
    }

    private fun undoChange() {
        if (history.isEmpty()) {
            uiInfoService.showToast(R.string.no_undo_changes)
            return
        }
        history.revertLast(contentEdit!!)
    }

    override fun onBackClicked() {
        val err = quietValidate()
        if (err != null) {
            val message = uiResourceService.resString(R.string.editor_onexit_validation_failed, err)
            ConfirmDialogBuilder().confirmAction(message) {
                returnNewContent()
            }
            return
        }
        returnNewContent()
    }

    fun setContent(content: String, chordsNotation: ChordsNotation?) {
        this.chordsNotation = chordsNotation
        val length = content.length
        setContentWithSelection(content, length, length)
        history.reset(contentEdit!!)
        softKeyboardService.showSoftKeyboard(contentEdit)
    }

    private fun returnNewContent() {
        var content = contentEdit?.text.toString()
        if (chordsNotation != null) {
            val converter = ChordsConverter(chordsNotation!!, ChordsNotation.default)
            content = converter.convertLyrics(content)
        }
        layoutController.showPreviousLayoutOrQuit()
        editSongLayoutController.get().setSongContent(content)
    }

    override fun onLayoutExit() {
        softKeyboardService.hideSoftKeyboard()
    }
}
