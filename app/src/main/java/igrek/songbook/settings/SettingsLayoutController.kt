package igrek.songbook.settings

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import igrek.songbook.R
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.layout.LayoutController
import igrek.songbook.layout.MainLayout
import javax.inject.Inject

class SettingsLayoutController : MainLayout {

    @Inject
    lateinit var layoutController: LayoutController
    @Inject
    lateinit var activity: AppCompatActivity

    init {
        DaggerIoc.factoryComponent.inject(this)
    }

    override fun showLayout(layout: View) {
        activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_content, SettingsFragment())
                .commit()
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.screen_settings
    }

    override fun onBackClicked() {
        layoutController.showPreviousLayoutOrQuit()
    }

    override fun onLayoutExit() {}
}
