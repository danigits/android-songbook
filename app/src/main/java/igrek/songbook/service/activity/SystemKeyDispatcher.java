package igrek.songbook.service.activity;

import android.os.Handler;

import javax.inject.Inject;

import igrek.songbook.dagger.DaggerIoc;
import igrek.songbook.service.autoscroll.AutoscrollService;
import igrek.songbook.service.filetree.FileTreeManager;
import igrek.songbook.service.layout.LayoutController;
import igrek.songbook.service.layout.LayoutState;
import igrek.songbook.service.layout.songtree.SongTreeController;
import igrek.songbook.service.window.WindowManagerService;
import igrek.songbook.view.songpreview.quickmenu.QuickMenu;

public class SystemKeyDispatcher {
	
	@Inject
	LayoutController layoutController;
	@Inject
	SongTreeController songTreeController;
	@Inject
	QuickMenu quickMenu;
	@Inject
	WindowManagerService windowManagerService;
	@Inject
	FileTreeManager fileTreeManager;
	@Inject
	AutoscrollService autoscrollService;
	
	public SystemKeyDispatcher() {
		DaggerIoc.getFactoryComponent().inject(this);
	}
	
	public boolean onKeyBack() {
		if (layoutController.isState(LayoutState.SONGS_LIST)) {
			songTreeController.onToolbarBackClickedEvent();
		} else if (layoutController.isState(LayoutState.SONG_PREVIEW)) {
			if (quickMenu.isVisible()) {
				quickMenu.onShowQuickMenuEvent(false);
			} else {
				autoscrollService.onAutoscrollStopEvent();
				
				layoutController.setState(LayoutState.SONGS_LIST);
				layoutController.showSongTree();
				
				windowManagerService.keepScreenOn(false);
				
				new Handler().post(() -> songTreeController.restoreScrollPosition(fileTreeManager
						.getCurrentPath()));
			}
		}
		return true;
	}
	
	public boolean onKeyMenu() {
		return false;
	}
}
