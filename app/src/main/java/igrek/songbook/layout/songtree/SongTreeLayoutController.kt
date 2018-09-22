package igrek.songbook.layout.songtree

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import igrek.songbook.R
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.domain.exception.NoParentItemException
import igrek.songbook.domain.songsdb.SongCategory
import igrek.songbook.domain.songsdb.SongsDb
import igrek.songbook.layout.LayoutState
import igrek.songbook.layout.MainLayout
import igrek.songbook.layout.songselection.SongSelectionLayoutController
import igrek.songbook.layout.songselection.SongTreeItem
import javax.inject.Inject

class SongTreeLayoutController : SongSelectionLayoutController(), MainLayout {

    @Inject
    lateinit var songTreeWalker: SongTreeWalker
    @Inject
    lateinit var scrollPosBuffer: ScrollPosBuffer

    private var toolbarTitle: TextView? = null
    private var goBackButton: ImageButton? = null
    private var searchSongButton: ImageButton? = null

    init {
        DaggerIoc.getFactoryComponent().inject(this)
    }

    override fun showLayout(layout: View) {
        initSongSelectionLayout(layout)

        goBackButton = layout.findViewById(R.id.goBackButton)
        goBackButton?.setOnClickListener { _ -> onBackClicked() }

        searchSongButton = layout.findViewById(R.id.searchSongButton)
        searchSongButton?.setOnClickListener { _ -> goToSearchSong() }

        toolbarTitle = layout.findViewById(R.id.toolbarTitle)

        itemsListView!!.init(activity, this)
        updateSongItemsList()

        songsDbRepository.dbChangeSubject.subscribe { _ ->
            if (layoutController.isState(LayoutState.SONGS_TREE))
                updateSongItemsList()
        }
    }

    override fun getLayoutState(): LayoutState {
        return LayoutState.SONGS_TREE
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.song_tree
    }

    override fun onBackClicked() {
        goUp()
    }

    override fun updateSongItemsList() {
        super.updateSongItemsList()
        if (songTreeWalker.isCategorySelected) {
            goBackButton!!.visibility = View.VISIBLE
            setTitle(songTreeWalker.currentCategory.displayName)
        } else {
            goBackButton!!.visibility = View.INVISIBLE
            setTitle(uiResourceService.resString(R.string.nav_songs_list))
        }
        restoreScrollPosition(songTreeWalker.currentCategory)
    }

    override fun getSongItems(songsDb: SongsDb): List<SongTreeItem> {
        return if (!songTreeWalker.isCategorySelected) {
            // all categories list
            songsDb.getAllUnlockedCategories()
                    .map { category -> SongTreeItem.category(category) }
        } else {
            // selected category
            songTreeWalker.currentCategory.getUnlockedSongs()
                    .map { song -> SongTreeItem.song(song) }
        }
    }

    private fun setTitle(title: String?) {
        actionBar!!.title = title
        toolbarTitle!!.text = title
    }

    private fun goToSearchSong() {
        layoutController.showSongSearch()
    }

    private fun goUp() {
        try {
            songTreeWalker.goUp()
            updateSongItemsList()
        } catch (e: NoParentItemException) {
            activityController.get().quit()
        }
    }

    private fun storeScrollPosition() {
        scrollPosBuffer.storeScrollPosition(songTreeWalker.currentCategory, itemsListView?.currentScrollPosition)
    }

    private fun restoreScrollPosition(category: SongCategory?) {
        if (scrollPosBuffer.hasScrollPositionStored(category)) {
            itemsListView?.restoreScrollPosition(scrollPosBuffer.restoreScrollPosition(category))
        }
    }

    override fun onSongItemClick(item: SongTreeItem) {
        storeScrollPosition()
        if (item.isCategory) {
            songTreeWalker.goToCategory(item.category)
            updateSongItemsList()
            // scroll to beginning
            itemsListView?.scrollToBeginning()
        } else {
            openSongPreview(item)
        }
    }
}
