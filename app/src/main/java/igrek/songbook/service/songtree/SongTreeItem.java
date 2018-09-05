package igrek.songbook.service.songtree;

import igrek.songbook.domain.song.Song;
import igrek.songbook.domain.song.SongCategory;

public class SongTreeItem {
	
	private Song song;
	private SongCategory category;
	
	private SongTreeItem(Song song, SongCategory category) {
		this.song = song;
		this.category = category;
	}
	
	public static SongTreeItem song(Song song) {
		return new SongTreeItem(song, null);
	}
	
	public static SongTreeItem category(SongCategory category) {
		return new SongTreeItem(null, category);
	}
	
	public String getSimpleName() {
		if (isCategory()) {
			return category.getName();
		} else {
			return song.getTitle();
		}
	}
	
	public boolean isCategory() {
		return !isSong();
	}
	
	public boolean isSong() {
		return song != null;
	}
	
	public Song getSong() {
		return song;
	}
	
	public SongCategory getCategory() {
		return category;
	}
	
	@Override
	public String toString() {
		if (isCategory()) {
			return "[" + category.getName() + "]";
		} else {
			return song.getCategoryName() + " - " + song.getTitle();
		}
	}
}