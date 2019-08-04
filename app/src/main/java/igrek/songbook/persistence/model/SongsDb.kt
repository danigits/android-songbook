package igrek.songbook.persistence.model

import igrek.songbook.system.cache.FinderById
import igrek.songbook.system.cache.SimpleCache

data class SongsDb(
        val versionNumber: Long,
        val categories: List<Category>,
        val songs: List<Song>
) {
    val songFinder = FinderById(songs) { e -> e.id }
    val categoryFinder = FinderById(categories) { e -> e.id }

    private var customSongsCache: SimpleCache<List<Song>> =
            SimpleCache {
                songs.filter { s -> s.custom }
            }

    fun getCustomSongs(): List<Song> {
        return customSongsCache.get()
    }
}