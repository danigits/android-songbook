package igrek.songbook.layout

import android.app.Activity
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import dagger.Lazy
import igrek.songbook.R
import igrek.songbook.activity.ActivityController
import igrek.songbook.admin.antechamber.AdminSongsLayoutContoller
import igrek.songbook.contact.ContactLayoutController
import igrek.songbook.contact.MissingSongLayoutController
import igrek.songbook.contact.PublishSongLayoutController
import igrek.songbook.custom.CustomSongsLayoutController
import igrek.songbook.custom.EditSongLayoutController
import igrek.songbook.custom.editor.ChordsEditorLayoutController
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.info.logger.LoggerFactory
import igrek.songbook.layout.navigation.NavigationMenuController
import igrek.songbook.playlist.PlaylistLayoutController
import igrek.songbook.settings.SettingsLayoutController
import igrek.songbook.songpreview.SongPreviewLayoutController
import igrek.songbook.songselection.favourite.FavouritesLayoutController
import igrek.songbook.songselection.history.OpenHistoryLayoutController
import igrek.songbook.songselection.latest.LatestSongsLayoutController
import igrek.songbook.songselection.search.SongSearchLayoutController
import igrek.songbook.songselection.tree.SongTreeLayoutController
import javax.inject.Inject
import kotlin.reflect.KClass

class LayoutController {

    @Inject
    lateinit var activity: Activity
    @Inject
    lateinit var navigationMenuController: Lazy<NavigationMenuController>
    @Inject
    lateinit var activityController: Lazy<ActivityController>

    @Inject
    lateinit var songTreeLayoutController: Lazy<SongTreeLayoutController>
    @Inject
    lateinit var songSearchLayoutController: Lazy<SongSearchLayoutController>
    @Inject
    lateinit var songPreviewLayoutController: Lazy<SongPreviewLayoutController>
    @Inject
    lateinit var contactLayoutController: Lazy<ContactLayoutController>
    @Inject
    lateinit var settingsLayoutController: Lazy<SettingsLayoutController>
    @Inject
    lateinit var editSongLayoutController: Lazy<EditSongLayoutController>
    @Inject
    lateinit var chordsEditorLayoutController: Lazy<ChordsEditorLayoutController>
    @Inject
    lateinit var customSongsLayoutController: Lazy<CustomSongsLayoutController>
    @Inject
    lateinit var favouritesLayoutController: Lazy<FavouritesLayoutController>
    @Inject
    lateinit var playlistLayoutController: Lazy<PlaylistLayoutController>
    @Inject
    lateinit var latestSongsLayoutController: Lazy<LatestSongsLayoutController>
    @Inject
    lateinit var openHistoryLayoutController: Lazy<OpenHistoryLayoutController>
    @Inject
    lateinit var missingSongLayoutController: Lazy<MissingSongLayoutController>
    @Inject
    lateinit var publishSongLayoutController: Lazy<PublishSongLayoutController>
    @Inject
    lateinit var adminSongsLayoutContoller: Lazy<AdminSongsLayoutContoller>

    private var mainContentLayout: CoordinatorLayout? = null
    private var currentLayout: MainLayout? = null
    private var layoutHistory: MutableList<MainLayout> = mutableListOf()
    private var registeredLayouts: Map<KClass<out MainLayout>, Lazy<out MainLayout>> = emptyMap()
    private val logger = LoggerFactory.logger

    init {
        DaggerIoc.factoryComponent.inject(this)
    }

    fun init() {
        activity.setContentView(R.layout.main_layout)
        mainContentLayout = activity.findViewById(R.id.main_content)
        navigationMenuController.get().init()
        registerLayouts()
    }

    private fun registerLayouts() {
        registeredLayouts = mapOf(
                SongTreeLayoutController::class to songTreeLayoutController,
                SongSearchLayoutController::class to songSearchLayoutController,
                SongPreviewLayoutController::class to songPreviewLayoutController,
                ContactLayoutController::class to contactLayoutController,
                SettingsLayoutController::class to settingsLayoutController,
                EditSongLayoutController::class to editSongLayoutController,
                ChordsEditorLayoutController::class to chordsEditorLayoutController,
                CustomSongsLayoutController::class to customSongsLayoutController,
                FavouritesLayoutController::class to favouritesLayoutController,
                PlaylistLayoutController::class to playlistLayoutController,
                LatestSongsLayoutController::class to latestSongsLayoutController,
                OpenHistoryLayoutController::class to openHistoryLayoutController,
                MissingSongLayoutController::class to missingSongLayoutController,
                PublishSongLayoutController::class to publishSongLayoutController,
                AdminSongsLayoutContoller::class to adminSongsLayoutContoller
        )
    }

    fun showLayout(layoutClass: KClass<out MainLayout>, disableReturn: Boolean = false) {
        val lazyLayout = registeredLayouts[layoutClass]
                ?: throw IllegalArgumentException("${layoutClass.simpleName} class not registered as layout")
        val layoutController = lazyLayout.get()

        if (disableReturn) {
            // remove current layout from history
            if (currentLayout in layoutHistory) {
                layoutHistory.remove(currentLayout)
            }
        }

        layoutController.let {
            if (it in layoutHistory) {
                layoutHistory.remove(it)
            }
            layoutHistory.add(it)
        }

        logger.debug("Showing layout ${layoutClass.simpleName} [${layoutHistory.size} in history]")
        showMainLayout(layoutController)
    }

    private fun showMainLayout(mainLayout: MainLayout) {
        currentLayout?.onLayoutExit()

        currentLayout = mainLayout

        // replace main content with brand new inflated layout
        val layoutResource = mainLayout.getLayoutResourceId()
        mainContentLayout?.removeAllViews()
        val inflater = activity.layoutInflater
        val layoutView = inflater.inflate(layoutResource, null)
        layoutView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mainContentLayout?.addView(layoutView)

        mainLayout.showLayout(layoutView)
    }

    fun showPreviousLayoutOrQuit() {
        // remove current layout from last place
        try {
            val last = layoutHistory.last()
            if (last == currentLayout) {
                layoutHistory = layoutHistory.dropLast(1).toMutableList()
            }
        } catch (e: NoSuchElementException) {
        }

        if (layoutHistory.isEmpty()) {
            activityController.get().quit()
            return
        }

        val previousLayout = layoutHistory.last()
        logger.debug("Showing previous layout ${previousLayout::class.simpleName} [${layoutHistory.size} in history]")
        showMainLayout(previousLayout)
    }

    fun isState(compareLayoutClass: KClass<out MainLayout>): Boolean {
        return compareLayoutClass.isInstance(currentLayout)
    }

    fun onBackClicked() {
        if (navigationMenuController.get().isDrawerShown()) {
            navigationMenuController.get().navDrawerHide()
            return
        }
        currentLayout!!.onBackClicked()
    }

}
