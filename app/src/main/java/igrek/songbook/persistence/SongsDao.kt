package igrek.songbook.persistence

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.model.songsdb.Song
import igrek.songbook.model.songsdb.SongCategory
import igrek.songbook.model.songsdb.SongCategoryType
import igrek.songbook.model.songsdb.SongStatus


class SongsDao : AbstractSqliteDao() {

    init {
        DaggerIoc.getFactoryComponent().inject(this)
    }

    /*
    SCHEMA:

class Category(models.Model):
    type_id = models.IntegerField(default=3, choices=TYPE_ID_CHOICE)
    name = models.CharField(blank=True, null=True, max_length=512)
    is_custom = models.BooleanField(default=False)

class Song(models.Model):
    title = models.CharField(max_length=512)
    category = models.ForeignKey(Category, on_delete=models.CASCADE)
    file_content = models.TextField(blank=True, null=True)
    version_number = models.IntegerField(default=1)
    create_time = models.DateTimeField(default=datetime.now)
    update_time = models.DateTimeField(default=datetime.now)
    language = models.CharField(blank=True, null=True, max_length=8, choices=LANGUAGE_CHOICES)
    author = models.CharField(blank=True, null=True, max_length=512)
    is_custom = models.BooleanField(default=False)
    custom_category_name = models.CharField(blank=True, null=True, max_length=512)
    filename = models.CharField(blank=True, null=True, max_length=512)
    comment = models.CharField(blank=True, null=True, max_length=512)
    preferred_key = models.CharField(blank=True, null=True, max_length=512)
    is_locked = models.BooleanField(default=False)
    lock_password = models.CharField(blank=True, null=True, max_length=512)
    state = models.IntegerField(default=1, choices=STATE_ID_CHOICE)

class Info(models.Model):
    name = models.CharField(max_length=512)
    value = models.CharField(blank=True, null=True, max_length=512)

     */

    override fun getDatabase(): SQLiteDatabase {
        return localDbService.openSongsDb()
    }

    fun readAllCategories(): List<SongCategory> {
        val entities: MutableList<SongCategory> = mutableListOf()
        try {
            val cursor = sqlQuery("SELECT * FROM songs_category ORDER BY id")

            while (cursor.moveToNext()) {
                entities.add(mapSongCategory(cursor))
            }

            cursor.close()
        } catch (e: IllegalArgumentException) {
            logger.error(e)
        }
        return entities
    }

    private fun mapSongCategory(cursor: Cursor): SongCategory {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val typeId = cursor.getLong(cursor.getColumnIndexOrThrow("type_id"))
        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        val custom = getBooleanColumn(cursor, "is_custom")
        val type = SongCategoryType.parseById(typeId)
        return SongCategory(id, type, name, custom)
    }

    fun readAllSongs(categories: List<SongCategory>): MutableList<Song> {
        val songs: MutableList<Song> = mutableListOf()
        try {
            val cursor = sqlQuery("SELECT * FROM songs_song ORDER BY id")

            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val fileContent = cursor.getString(cursor.getColumnIndexOrThrow("file_content"))
                val versionNumber = cursor.getLong(cursor.getColumnIndexOrThrow("version_number"))
                val createTime = getTimestampColumn(cursor, "create_time")
                val updateTime = getTimestampColumn(cursor, "update_time")
                val custom = getBooleanColumn(cursor, "is_custom")
                val filename = cursor.getString(cursor.getColumnIndexOrThrow("filename"))
                val comment = cursor.getString(cursor.getColumnIndexOrThrow("comment"))
                val preferredKey = cursor.getString(cursor.getColumnIndexOrThrow("preferred_key"))
                val locked = getBooleanColumn(cursor, "is_locked")
                val lockPassword = cursor.getString(cursor.getColumnIndexOrThrow("lock_password"))
                val author = cursor.getString(cursor.getColumnIndexOrThrow("author"))
                val stateId = cursor.getLong(cursor.getColumnIndexOrThrow("state"))
                val categoryId = cursor.getLong(cursor.getColumnIndexOrThrow("category_id"))
                val customCategoryName = cursor.getString(cursor.getColumnIndexOrThrow("custom_category_name"))
                val language = cursor.getString(cursor.getColumnIndexOrThrow("language"))

                val songStatus = SongStatus.parseById(stateId)
                val category = categories.first { category -> category.id == categoryId }

                val song = Song(id, title, category, fileContent, versionNumber, createTime, updateTime, custom, filename, comment, preferredKey, locked, lockPassword, author, songStatus, customCategoryName, language)
                songs.add(song)
            }

            cursor.close()
        } catch (e: IllegalArgumentException) {
            logger.error(e)
        }
        return songs
    }

}
