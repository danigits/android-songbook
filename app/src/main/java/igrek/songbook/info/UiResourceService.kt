package igrek.songbook.info

import android.app.Activity
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import igrek.songbook.dagger.DaggerIoc
import javax.inject.Inject

class UiResourceService {

    @Inject
    lateinit var activity: Activity

    init {
        DaggerIoc.factoryComponent.inject(this)
    }

    fun resString(resourceId: Int): String {
        return activity.resources.getString(resourceId)
    }

    fun resString(resourceId: Int, vararg args: Any?): String {
        val message = resString(resourceId)
        return if (args.isNotEmpty()) {
            String.format(message, *args)
        } else {
            message
        }
    }

    @ColorInt
    fun getColor(@ColorRes resourceId: Int): Int {
        return ContextCompat.getColor(activity, resourceId)
    }

}
