package igrek.songbook.activity

import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import igrek.songbook.R
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.service.activity.ActivityController
import igrek.songbook.service.activity.AppInitializer
import igrek.songbook.service.activity.OptionSelectDispatcher
import igrek.songbook.service.activity.SystemKeyDispatcher
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appInitializer: AppInitializer
    @Inject
    lateinit var activityController: ActivityController
    @Inject
    lateinit var optionSelectDispatcher: OptionSelectDispatcher
    @Inject
    lateinit var systemKeyDispatcher: SystemKeyDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dagger Container init
        DaggerIoc.init(this)
        DaggerIoc.getFactoryComponent().inject(this)
        appInitializer.init()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activityController.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityController.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        activityController.onStart()
    }

    override fun onStop() {
        super.onStop()
        activityController.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return optionSelectDispatcher.optionsSelect(item.itemId) || super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (systemKeyDispatcher.onKeyBack())
                return true
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (systemKeyDispatcher.onKeyMenu())
                return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }
}
