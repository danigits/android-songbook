package igrek.songbook.layout.songimport;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import javax.inject.Inject;

import dagger.Lazy;
import igrek.songbook.R;
import igrek.songbook.dagger.DaggerIoc;
import igrek.songbook.info.UiInfoService;
import igrek.songbook.info.UiResourceService;
import igrek.songbook.info.errorcheck.SafeClickListener;
import igrek.songbook.info.logger.Logger;
import igrek.songbook.info.logger.LoggerFactory;
import igrek.songbook.layout.LayoutController;
import igrek.songbook.layout.LayoutState;
import igrek.songbook.layout.MainLayout;
import igrek.songbook.layout.navigation.NavigationMenuController;

public class ImportSongLayoutController implements MainLayout {
	
	@Inject
	LayoutController layoutController;
	@Inject
	UiInfoService uiInfoService;
	@Inject
	UiResourceService uiResourceService;
	@Inject
	AppCompatActivity activity;
	@Inject
	NavigationMenuController navigationMenuController;
	@Inject
	Lazy<SongImportService> songImportService;
	
	private String songTitle;
	private String songContent;
	
	private Logger logger = LoggerFactory.getLogger();
	private EditText songTitleEdit;
	private EditText songContentEdit;
	
	public ImportSongLayoutController() {
		DaggerIoc.getFactoryComponent().inject(this);
	}
	
	@Override
	public void showLayout(View layout) {
		// Toolbar
		Toolbar toolbar1 = layout.findViewById(R.id.toolbar1);
		activity.setSupportActionBar(toolbar1);
		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setDisplayShowHomeEnabled(false);
		}
		// navigation menu button
		ImageButton navMenuButton = layout.findViewById(R.id.navMenuButton);
		navMenuButton.setOnClickListener((v) -> navigationMenuController.navDrawerShow());
		
		songTitleEdit = layout.findViewById(R.id.songTitleEdit);
		songContentEdit = layout.findViewById(R.id.songContentEdit);
		Button saveSongButton = layout.findViewById(R.id.saveSongButton);
		saveSongButton.setOnClickListener(new SafeClickListener() {
			@Override
			public void onClick() {
				saveSong();
			}
		});
		
		songTitleEdit.setText(songTitle);
		songContentEdit.setText(songContent);
	}
	
	public void setImportedSong(String title, String content) {
		this.songTitle = title;
		this.songContent = content;
	}
	
	
	private void saveSong() {
		songTitle = songTitleEdit.getText().toString();
		songContent = songContentEdit.getText().toString();
		songImportService.get().importSong(songTitle, songContent);
	}
	
	@Override
	public LayoutState getLayoutState() {
		return LayoutState.IMPORT_SONG;
	}
	
	@Override
	public int getLayoutResourceId() {
		return R.layout.import_song;
	}
	
	@Override
	public void onBackClicked() {
		layoutController.showLastSongSelectionLayout();
	}
	
	@Override
	public void onLayoutExit() {
	}
	
}