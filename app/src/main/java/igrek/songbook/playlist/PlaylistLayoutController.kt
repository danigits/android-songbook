package igrek.songbook.playlist

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import igrek.songbook.R
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.info.UiInfoService
import igrek.songbook.info.UiResourceService
import igrek.songbook.layout.InflatedLayout
import igrek.songbook.layout.contextmenu.ContextMenuBuilder
import igrek.songbook.layout.dialog.ConfirmDialogBuilder
import igrek.songbook.layout.dialog.InputDialogBuilder
import igrek.songbook.layout.list.ListItemClickListener
import igrek.songbook.persistence.general.model.SongIdentifier
import igrek.songbook.persistence.general.model.SongNamespace
import igrek.songbook.persistence.repository.SongsRepository
import igrek.songbook.persistence.user.playlist.Playlist
import igrek.songbook.playlist.list.PlaylistListItem
import igrek.songbook.playlist.list.PlaylistListView
import igrek.songbook.songpreview.SongOpener
import igrek.songbook.songselection.ListScrollPosition
import igrek.songbook.songselection.contextmenu.SongContextMenuBuilder
import igrek.songbook.songselection.tree.NoParentItemException
import igrek.songbook.util.ListMover
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import javax.inject.Inject


class PlaylistLayoutController : InflatedLayout(
        _layoutResourceId = R.layout.screen_playlists
), ListItemClickListener<PlaylistListItem> {

    @Inject
    lateinit var songsRepository: SongsRepository
    @Inject
    lateinit var uiResourceService: UiResourceService
    @Inject
    lateinit var songContextMenuBuilder: SongContextMenuBuilder
    @Inject
    lateinit var contextMenuBuilder: ContextMenuBuilder
    @Inject
    lateinit var uiInfoService: UiInfoService
    @Inject
    lateinit var songOpener: SongOpener

    private var itemsListView: PlaylistListView? = null
    private var addPlaylistButton: ImageButton? = null
    private var emptyListLabel: TextView? = null
    private var playlistTitleLabel: TextView? = null
    private var goBackButton: ImageButton? = null

    private var playlist: Playlist? = null

    private var storedScroll: ListScrollPosition? = null
    private var subscriptions = mutableListOf<Disposable>()

    init {
        DaggerIoc.factoryComponent.inject(this)
    }

    override fun showLayout(layout: View) {
        super.showLayout(layout)

        itemsListView = layout.findViewById(R.id.playlistListView)

        addPlaylistButton = layout.findViewById(R.id.addPlaylistButton)
        addPlaylistButton?.setOnClickListener { addPlaylist() }

        playlistTitleLabel = layout.findViewById(R.id.playlistTitleLabel)
        emptyListLabel = layout.findViewById(R.id.emptyListLabel)

        goBackButton = layout.findViewById(R.id.goBackButton)
        goBackButton?.setOnClickListener { goUp() }

        itemsListView!!.init(activity, this, ::itemMoved)
        updateItemsList()

        subscriptions.forEach { s -> s.dispose() }
        subscriptions.clear()
        subscriptions.add(songsRepository.dbChangeSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (isLayoutVisible())
                        updateItemsList()
                })
        subscriptions.add(songsRepository.playlistDao.playlistDbSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (isLayoutVisible())
                        updateItemsList()
                })
    }

    private fun addPlaylist() {
        InputDialogBuilder().input(R.string.new_playlist_name, null) { name ->
            val playlist = Playlist(0, name)
            songsRepository.playlistDao.savePlaylist(playlist)
        }
    }

    private fun updateItemsList() {
        val items = if (playlist == null) {
            songsRepository.playlistDao.playlistDb.playlists
                    .map { p -> PlaylistListItem(playlist = p) }
                    .toMutableList()
        } else {
            playlist!!.songs
                    .mapNotNull { s ->
                        val namespace = when {
                            s.custom -> SongNamespace.Custom
                            else -> SongNamespace.Public
                        }
                        val id = SongIdentifier(s.songId, namespace)
                        val song = songsRepository.allSongsRepo.songFinder.find(id)
                        when {
                            song != null -> PlaylistListItem(song = song)
                            else -> null
                        }
                    }
                    .toMutableList()
        }

        itemsListView!!.items = items

        if (storedScroll != null) {
            Handler(Looper.getMainLooper()).post {
                itemsListView?.restoreScrollPosition(storedScroll)
            }
        }

        val playlistsTitle = uiResourceService.resString(R.string.nav_playlists)
        playlistTitleLabel?.text = when (playlist) {
            null -> playlistsTitle
            else -> "$playlistsTitle: ${playlist?.name}"
        }

        emptyListLabel?.text = when (playlist) {
            null -> uiResourceService.resString(R.string.empty_playlists)
            else -> uiResourceService.resString(R.string.empty_playlist_songs)
        }
        emptyListLabel?.visibility = when (itemsListView!!.count) {
            0 -> View.VISIBLE
            else -> View.GONE
        }

        addPlaylistButton?.visibility = when (playlist) {
            null -> View.VISIBLE
            else -> View.GONE
        }

        goBackButton?.visibility = when (playlist) {
            null -> View.GONE
            else -> View.VISIBLE
        }
    }

    override fun onBackClicked() {
        goUp()
    }

    private fun goUp() {
        try {
            if (playlist == null)
                throw NoParentItemException()

            playlist = null
            updateItemsList()
        } catch (e: NoParentItemException) {
            layoutController.showPreviousLayoutOrQuit()
        }
    }

    override fun onItemClick(item: PlaylistListItem) {
        storedScroll = itemsListView?.currentScrollPosition
        if (item.song != null) {
            songOpener.openSongPreview(item.song)
        } else if (item.playlist != null) {
            playlist = item.playlist
            updateItemsList()
        }
    }

    override fun onItemLongClick(item: PlaylistListItem) {
        onMoreActions(item)
    }

    override fun onMoreActions(item: PlaylistListItem) {
        if (item.song != null) {
            songContextMenuBuilder.showSongActions(item.song)
        } else if (item.playlist != null) {
            showPlaylistActions(item.playlist)
        }
    }

    private fun showPlaylistActions(playlist: Playlist) {
        val actions = mutableListOf(
                ContextMenuBuilder.Action(R.string.rename_playlist) {
                    renamePlaylist(playlist)
                },
                ContextMenuBuilder.Action(R.string.remove_playlist) {
                    ConfirmDialogBuilder().confirmAction(R.string.confirm_remove_playlist) {
                        songsRepository.playlistDao.removePlaylist(playlist)
                        uiInfoService.showInfo(R.string.playlist_removed)
                    }
                }
        )

        contextMenuBuilder.showContextMenu(R.string.choose_playlist, actions)
    }

    private fun renamePlaylist(playlist: Playlist) {
        InputDialogBuilder().input("Edit playlist name", playlist.name) { name ->
            playlist.name = name
            songsRepository.playlistDao.savePlaylist(playlist)
        }
    }

    @Synchronized
    fun itemMoved(position: Int, step: Int): List<PlaylistListItem>? {
        ListMover(playlist!!.songs).move(position, step)
        val items = playlist!!.songs
                .mapNotNull { s ->
                    val namespace = when {
                        s.custom -> SongNamespace.Custom
                        else -> SongNamespace.Public
                    }
                    val id = SongIdentifier(s.songId, namespace)
                    val song = songsRepository.allSongsRepo.songFinder.find(id)
                    when {
                        song != null -> PlaylistListItem(song = song)
                        else -> null
                    }
                }
                .toMutableList()
        itemsListView!!.items = items
        return items
    }
}
