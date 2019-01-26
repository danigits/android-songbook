package igrek.songbook.songpreview.quickmenu;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

import dagger.Lazy;
import igrek.songbook.R;
import igrek.songbook.dagger.DaggerIoc;
import igrek.songbook.info.UiResourceService;
import igrek.songbook.chords.transpose.ChordsTransposerManager;

public class QuickMenuTranspose {
	
	@Inject
	Lazy<ChordsTransposerManager> chordsTransposerManager;
	@Inject
	UiResourceService uiResourceService;
	
	private boolean visible = false;
	private View quickMenuView;
	private TextView transposedByLabel;
	
	public QuickMenuTranspose() {
		DaggerIoc.getFactoryComponent().inject(this);
	}
	
	public void setQuickMenuView(View quickMenuView) {
		this.quickMenuView = quickMenuView;
		
		transposedByLabel = quickMenuView.findViewById(R.id.transposedByLabel);
		
		Button transposeM5Button = quickMenuView.findViewById(R.id.transposeM5Button);
		transposeM5Button.setOnClickListener(v -> chordsTransposerManager.get()
				.onTransposeEvent(-5));
		
		Button transposeM1Button = quickMenuView.findViewById(R.id.transposeM1Button);
		transposeM1Button.setOnClickListener(v -> chordsTransposerManager.get()
				.onTransposeEvent(-1));
		
		Button transpose0Button = quickMenuView.findViewById(R.id.transpose0Button);
		transpose0Button.setOnClickListener(v -> chordsTransposerManager.get()
				.onTransposeResetEvent());
		
		Button transposeP1Button = quickMenuView.findViewById(R.id.transposeP1Button);
		transposeP1Button.setOnClickListener(v -> chordsTransposerManager.get()
				.onTransposeEvent(+1));
		
		Button transposeP5Button = quickMenuView.findViewById(R.id.transposeP5Button);
		transposeP5Button.setOnClickListener(v -> chordsTransposerManager.get()
				.onTransposeEvent(+5));
		
		updateTranspositionText();
	}
	
	private void updateTranspositionText() {
		String semitonesDisplayName = chordsTransposerManager.get().getTransposedByDisplayName();
		String transposedByText = uiResourceService.resString(R.string.transposed_by_semitones, semitonesDisplayName);
		transposedByLabel.setText(transposedByText);
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
		if (visible) {
			quickMenuView.setVisibility(View.VISIBLE);
			updateTranspositionText();
		} else {
			quickMenuView.setVisibility(View.GONE);
		}
	}
	
	public void onTransposedEvent() {
		if (visible) {
			updateTranspositionText();
		}
	}
	
	/**
	 * @return is feature active - has impact on song preview (panel may be hidden)
	 */
	public boolean isFeatureActive() {
		return chordsTransposerManager.get().isTransposed();
	}
	
}
