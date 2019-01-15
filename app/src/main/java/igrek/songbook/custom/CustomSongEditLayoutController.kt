package igrek.songbook.custom

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import dagger.Lazy
import igrek.songbook.R
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.info.UiInfoService
import igrek.songbook.info.errorcheck.SafeClickListener
import igrek.songbook.layout.LayoutController
import igrek.songbook.layout.LayoutState
import igrek.songbook.layout.MainLayout
import igrek.songbook.layout.navigation.NavigationMenuController
import igrek.songbook.persistence.songsdb.Song
import igrek.songbook.system.SoftKeyboardService
import javax.inject.Inject

class CustomSongEditLayoutController : MainLayout {

    @Inject
    lateinit var layoutController: LayoutController
    @Inject
    lateinit var uiInfoService: UiInfoService
    @Inject
    lateinit var activity: AppCompatActivity
    @Inject
    lateinit var navigationMenuController: NavigationMenuController
    @Inject
    lateinit var customSongService: Lazy<CustomSongService>
    @Inject
    lateinit var softKeyboardService: SoftKeyboardService
    @Inject
    lateinit var songImportFileChooser: Lazy<SongImportFileChooser>

    private var currentSong: Song? = null
    private var songTitle: String? = null
    private var songContent: String? = null
    private var customCategoryName: String? = null

    private var songTitleEdit: EditText? = null
    private var songContentEdit: EditText? = null
    private var customCategoryNameEdit: EditText? = null

    init {
        DaggerIoc.getFactoryComponent().inject(this)
    }

    override fun showLayout(layout: View) {
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

        songTitleEdit = layout.findViewById(R.id.songTitleEdit)
        customCategoryNameEdit = layout.findViewById(R.id.customCategoryNameEdit)
        songContentEdit = layout.findViewById(R.id.songContentEdit)
        val saveSongButton = layout.findViewById<Button>(R.id.saveSongButton)
        saveSongButton.setOnClickListener(object : SafeClickListener() {
            override fun onClick() {
                saveSong()
            }
        })

        val removeSongButton = layout.findViewById<Button>(R.id.removeSongButton)
        removeSongButton.setOnClickListener(object : SafeClickListener() {
            override fun onClick() {
                removeSong()
            }
        })

        val importFromFileButotn = layout.findViewById<Button>(R.id.importFromFileButotn)
        importFromFileButotn.setOnClickListener(object : SafeClickListener() {
            override fun onClick() {
                importContentFromFile()
            }
        })

        val addChordButton = layout.findViewById<Button>(R.id.addChordButton)
        addChordButton.setOnClickListener(object : SafeClickListener() {
            override fun onClick() {
                onAddChordClick()
            }
        })

        songTitleEdit!!.setText(songTitle)
        songContentEdit!!.setText(songContent)
        customCategoryNameEdit!!.setText(customCategoryName)
    }


    private fun onAddChordClick() {
        var edited = songContentEdit!!.text.toString()
        var selStart = songContentEdit!!.selectionStart
        var selEnd = songContentEdit!!.selectionEnd

        val before = edited.substring(0, selStart)
        val after = edited.substring(selEnd)

        // if there's nonempty selection
        if (selStart < selEnd) {

            val selected = edited.substring(selStart, selEnd)
            edited = "$before[$selected]$after"
            selStart++
            selEnd++

        } else { // just single cursor

            // if it's the end of line AND there is no space before
            if ((after.isEmpty() || after.startsWith("\n")) && !before.isEmpty() && !before.endsWith(" ")) {
                // insert missing space
                edited = "$before []$after"
                selStart += 2
            } else {
                edited = "$before[]$after"
                selStart += 1
            }
            selEnd = selStart

        }

        songContentEdit!!.setText(edited)
        songContentEdit!!.setSelection(selStart, selEnd)
        songContentEdit!!.requestFocus()
    }

    private fun importContentFromFile() {
        songImportFileChooser.get().showFileChooser()
    }

    fun setCurrentSong(song: Song?) {
        this.currentSong = song
        if (currentSong == null) {
            songTitle = null
            songContent = null
            customCategoryName = null
        } else {
            songTitle = currentSong!!.title
            songContent = currentSong!!.content
            customCategoryName = currentSong!!.customCategoryName
        }
    }

    private fun saveSong() {
        songTitle = songTitleEdit!!.text.toString()
        songContent = songContentEdit!!.text.toString()
        customCategoryName = customCategoryNameEdit!!.text.toString()

        if (songTitle!!.isEmpty() || songContent!!.isEmpty()) {
            uiInfoService.showInfo(R.string.fill_in_all_fields)
            return
        }

        if (customCategoryName!!.isEmpty())
            customCategoryName = null

        if (currentSong == null) {
            // add
            currentSong = customSongService.get()
                    .addCustomSong(songTitle!!, customCategoryName, songContent)
        } else {
            // update
            customSongService.get()
                    .updateSong(currentSong!!, songTitle!!, customCategoryName, songContent)
        }
        uiInfoService.showInfo(R.string.edit_song_has_been_saved)
        layoutController.showCustomSongs()
    }

    private fun removeSong() {
        if (currentSong == null) {
            // just cancel
            uiInfoService.showInfo(R.string.edit_song_has_been_removed)
        } else {
            // remove song from database
            customSongService.get().removeSong(currentSong!!)
        }
        layoutController.showCustomSongs()
    }

    override fun getLayoutState(): LayoutState {
        return LayoutState.EDIT_SONG
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.edit_song
    }

    override fun onBackClicked() {
        layoutController.showCustomSongs()
    }

    override fun onLayoutExit() {
        softKeyboardService.hideSoftKeyboard()
    }

    fun setSongFromFile(filename: String, content: String) {
        songTitle = songTitleEdit!!.text.toString()
        songContent = songContentEdit!!.text.toString()
        customCategoryName = customCategoryNameEdit!!.text.toString()

        if (songTitle!!.isEmpty()) {
            songTitle = filename
            songTitleEdit!!.setText(songTitle)
        }

        songContent = content
        songContentEdit!!.setText(songContent)
    }
}
