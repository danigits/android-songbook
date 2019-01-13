package igrek.songbook.custom

import android.os.Handler
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import dagger.Lazy
import igrek.songbook.R
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.layout.LayoutState
import igrek.songbook.layout.MainLayout
import igrek.songbook.model.songsdb.SongsDb
import igrek.songbook.songselection.ListScrollPosition
import igrek.songbook.songselection.SongSearchItem
import igrek.songbook.songselection.SongSelectionLayoutController
import igrek.songbook.songselection.SongTreeItem
import javax.inject.Inject

class CustomSongsLayoutController : SongSelectionLayoutController(), MainLayout {

    @Inject
    lateinit var customSongService: Lazy<CustomSongService>

    private var storedScroll: ListScrollPosition? = null
    private var emptyListLabel: TextView? = null

    init {
        DaggerIoc.getFactoryComponent().inject(this)
    }

    override fun showLayout(layout: View) {
        initSongSelectionLayout(layout)

        val addCustomSongButton: ImageButton = layout.findViewById(R.id.addCustomSongButton)
        addCustomSongButton.setOnClickListener { addCustomSong() }

        emptyListLabel = layout.findViewById(R.id.emptyListLabel)

        itemsListView!!.init(activity, this)
        updateSongItemsList()

        songsRepository.dbChangeSubject.subscribe {
            if (layoutController.isState(layoutState))
                updateSongItemsList()
        }
    }

    private fun addCustomSong() {
        customSongService.get().showAddSongScreen()
    }

    override fun getLayoutState(): LayoutState {
        return LayoutState.CUSTOM_SONGS
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.custom_songs
    }

    override fun updateSongItemsList() {
        super.updateSongItemsList()
        // restore Scroll Position
        if (storedScroll != null) {
            Handler().post { itemsListView?.restoreScrollPosition(storedScroll) }
        }

        if (itemsListView!!.count == 0) {
            emptyListLabel!!.visibility = View.VISIBLE
        } else {
            emptyListLabel!!.visibility = View.GONE
        }
    }

    override fun getSongItems(songsDb: SongsDb): MutableList<SongTreeItem> {
        // filter songs
        val songsSequence = songsDb.getCustomSongs()
                .asSequence()
                .map { song -> SongSearchItem.song(song) }
        return songsSequence.toMutableList()
    }

    override fun onBackClicked() {
        layoutController.showSongTree()
    }

    override fun onSongItemClick(item: SongTreeItem) {
        // store Scroll Position
        storedScroll = itemsListView?.currentScrollPosition
        if (item.isSong) {
            openSongPreview(item)
        }
    }

    override fun onLayoutExit() {}
}
