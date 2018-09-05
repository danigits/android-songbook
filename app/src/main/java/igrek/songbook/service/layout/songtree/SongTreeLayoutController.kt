package igrek.songbook.service.layout.songtree

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import igrek.songbook.R
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.domain.exception.NoParentItemException
import igrek.songbook.domain.songsdb.SongsDb
import igrek.songbook.service.layout.SongSelectionLayoutController
import igrek.songbook.service.songtree.SongTreeItem

class SongTreeLayoutController : SongSelectionLayoutController() {

    private var toolbarTitle: TextView? = null
    private var goBackButton: ImageButton? = null

    init {
        DaggerIoc.getFactoryComponent().inject(this)
    }

    fun showLayout(layout: View) {
        initSongSelectionLayout(layout)

        val navMenuButton = layout.findViewById<ImageButton>(R.id.navMenuButton)
        navMenuButton.setOnClickListener { _ -> navigationMenuController.navDrawerShow() }

        goBackButton = layout.findViewById(R.id.goBackButton)
        goBackButton!!.setOnClickListener { _ -> onBackClicked() }

        toolbarTitle = layout.findViewById(R.id.toolbarTitle)

        songTreeWalker.goToAllCategories()
        itemsListView!!.init(activity, this)
        updateSongItemsList()

        restoreScrollPosition(songTreeWalker.currentCategory)
    }

    fun onBackClicked() {
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
    }

    private fun setTitle(title: String?) {
        actionBar!!.title = title
        toolbarTitle!!.text = title
    }

    private fun goUp() {
        try {
            songTreeWalker.goUp()
            updateSongItemsList()
            //restoreScrollPosition(songTreeWalker.currentPath)
        } catch (e: NoParentItemException) {
            activityController.get().quit()
        }
    }

    override fun getSongItems(songsDb: SongsDb): List<SongTreeItem> {
        return if (!songTreeWalker.isCategorySelected) {
            // all categories list
            songsDb.categories
                    .map { category -> SongTreeItem.category(category) }
        } else {
            // selected category
            songTreeWalker.currentCategory.songs!!
                    .map { song -> SongTreeItem.song(song) }
        }
    }
}
