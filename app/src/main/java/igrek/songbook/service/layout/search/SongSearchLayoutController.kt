package igrek.songbook.service.layout.search

import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import igrek.songbook.R
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.domain.songsdb.SongsDb
import igrek.songbook.service.layout.LayoutState
import igrek.songbook.service.layout.MainLayout
import igrek.songbook.service.layout.SongSelectionLayoutController
import igrek.songbook.service.songtree.SongTreeFilter
import igrek.songbook.service.songtree.SongTreeItem
import igrek.songbook.service.system.SoftKeyboardService
import igrek.songbook.view.songlist.ListScrollPosition
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SongSearchLayoutController : SongSelectionLayoutController(), MainLayout {

    private var searchFilterEdit: EditText? = null
    private var searchFilterSubject: PublishSubject<String> = PublishSubject.create()
    private var itemNameFilter: String? = null
    private var storedScroll: ListScrollPosition? = null

    @Inject
    lateinit var softKeyboardService: SoftKeyboardService

    init {
        DaggerIoc.getFactoryComponent().inject(this)
    }

    override fun showLayout(layout: View) {
        initSongSelectionLayout(layout)

        searchFilterEdit = layout.findViewById(R.id.searchFilterEdit)
        searchFilterEdit!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchFilterSubject.onNext(s.toString())
            }
        })
        // refresh only after some inactive time
        searchFilterSubject.debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ -> setSongFilter(searchFilterEdit!!.text.toString()) }
        if (isFilterSet()) {
            searchFilterEdit!!.setText(itemNameFilter, TextView.BufferType.EDITABLE)
        }
        searchFilterEdit!!.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                softKeyboardService.hideSoftKeyboard(searchFilterEdit)
        }
        searchFilterEdit!!.requestFocus()
        Handler().post { softKeyboardService.showSoftKeyboard(searchFilterEdit) }

        searchFilterEdit!!.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchFilterEdit?.clearFocus()
                softKeyboardService.hideSoftKeyboard(searchFilterEdit)
                return@setOnEditorActionListener true
            }
            false
        }

        val searchFilterClearButton: ImageButton = layout.findViewById(R.id.searchFilterClearButton)
        searchFilterClearButton.setOnClickListener { _ -> onClearFilterClicked() }


        itemsListView!!.init(activity, this)
        updateSongItemsList()

        songsDbRepository.dbChangeSubject.subscribe { _ ->
            if (layoutController.isState(LayoutState.SONGS_TREE))
                updateSongItemsList()
        }
    }

    override fun getLayoutState(): LayoutState {
        return LayoutState.SEARCH_SONG
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.song_search
    }

    override fun updateSongItemsList() {
        super.updateSongItemsList()
        // restore Scroll Position
        if (storedScroll != null) {
            Handler().post { itemsListView?.restoreScrollPosition(storedScroll) }
        }
    }

    private fun setSongFilter(itemNameFilter: String?) {
        this.itemNameFilter = itemNameFilter
        if (itemNameFilter == null)
            searchFilterEdit?.setText("", TextView.BufferType.EDITABLE)
        // reset scroll
        storedScroll = null
        updateSongItemsList()
    }

    override fun getSongItems(songsDb: SongsDb): List<SongTreeItem> {
        // no filter
        if (!isFilterSet()) {
            return songsDb.getAllUnlockedSongs()
                    .map { song -> SongTreeItem.song(song) }
        } else {
            val songNameFilter = SongTreeFilter(itemNameFilter)
            return songsDb.getAllUnlockedSongs()
                    .map { song -> SongTreeItem.song(song) }
                    .filter { item -> songNameFilter.matchesNameFilter(item) }
        }
    }

    private fun isFilterSet(): Boolean {
        return itemNameFilter != null && !itemNameFilter!!.isEmpty()
    }

    override fun onBackClicked() {
        if (isFilterSet()) {
            setSongFilter(null)
        } else {
            layoutController.showSongTree()
        }
    }

    private fun onClearFilterClicked() {
        if (isFilterSet()) {
            setSongFilter(null)
        } else {
            softKeyboardService.hideSoftKeyboard(searchFilterEdit)
        }
    }

    override fun onSongItemClick(item: SongTreeItem) {
        // store Scroll Position
        storedScroll = itemsListView?.currentScrollPosition
        if (item.isSong) {
            openSongPreview(item)
        }
    }

}
