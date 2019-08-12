package igrek.songbook.playlist.list

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ListView
import igrek.songbook.info.logger.LoggerFactory.logger
import igrek.songbook.layout.list.ListItemClickListener
import igrek.songbook.songselection.ListScrollPosition

class PlaylistListView : ListView, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private var adapter: PlaylistListItemAdapter? = null
    var scrollHandler: TreeListScrollHandler? = null
        private set
    val reorder = TreeListReorder(this)
    private var onClickListener: ListItemClickListener<PlaylistListItem>? = null
    /** view index -> view height  */
    private val itemHeights = SparseArray<Int>()

    val currentScrollPosition: ListScrollPosition
        get() {
            var yOffset = 0
            if (childCount > 0) {
                yOffset = -getChildAt(0).top
            }
            return ListScrollPosition(firstVisiblePosition, yOffset)
        }

    var items: List<PlaylistListItem>?
        get() = adapter!!.dataSource
        private set(items) {
            adapter!!.dataSource = items
            adapter!!.notifyDataSetChanged()
            invalidate()
            calculateViewHeights()
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun init(context: Context, onClickListener: ListItemClickListener<PlaylistListItem>) {
        this.onClickListener = onClickListener
        onItemClickListener = this
        onItemLongClickListener = this
        choiceMode = CHOICE_MODE_SINGLE

        scrollHandler = TreeListScrollHandler(this, context)
        setOnScrollListener(scrollHandler)

        adapter = PlaylistListItemAdapter(context, null, onClickListener, this)
        setAdapter(adapter)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val item = adapter!!.getItem(position)
        if (onClickListener != null)
            onClickListener!!.onItemClick(item!!)
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        val item = adapter!!.getItem(position)
        if (onClickListener != null)
            onClickListener!!.onItemLongClick(item!!)
        return true
    }

    /**
     * @param position of element to scroll
     */
    private fun scrollTo(position: Int) {
        setSelection(position)
        invalidate()
    }

    fun scrollToBeginning() {
        scrollTo(0)
    }

    fun restoreScrollPosition(scrollPosition: ListScrollPosition?) {
        if (scrollPosition != null) {
            // scroll to first position
            setSelection(scrollPosition.firstVisiblePosition)
            // and move a little by y offset
            smoothScrollBy(scrollPosition.yOffsetPx, 50)
            invalidate()
        }
    }


    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.source == 777) { // from moveButton
            if (ev.action == MotionEvent.ACTION_MOVE)
                return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> if (reorder.isDragging) {
                reorder.setLastTouchY(event.y)
                reorder.handleItemDragging()
                return false
            }
            MotionEvent.ACTION_UP -> {
                reorder.itemDraggingStopped()
            }
            MotionEvent.ACTION_CANCEL -> {
                reorder.itemDraggingStopped()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun invalidate() {
        super.invalidate()
        if (reorder != null && reorder.isDragging) {
            reorder.setDraggedItemView()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        reorder.dispatchDraw(canvas)
    }

    fun setItemsAndSelected(items: List<PlaylistListItem>) {
        this.items = items
    }

    private fun calculateViewHeights() {
        // WARNING: for a moment - there's invalidated item heights map
        val observer = this.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                itemHeights.clear()
                this@PlaylistListView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                // now view width should be available at last
                val viewWidth = this@PlaylistListView.width
                if (viewWidth == 0)
                    logger.warn("List view width == 0")

                val measureSpecW = View.MeasureSpec.makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY)
                for (i in 0 until adapter!!.count) {
                    val itemView = adapter!!.getView(i, null, this@PlaylistListView)
                    itemView.measure(measureSpecW, View.MeasureSpec.UNSPECIFIED)
                    itemHeights.put(i, itemView.measuredHeight)
                }

            }
        })
    }

    fun getItemHeight(position: Int): Int {
        val h = itemHeights.get(position)
        if (h == null) {
            logger.warn("Item View ($position) = null")
            return 0
        }

        return h
    }

    fun putItemHeight(position: Int?, height: Int?) {
        itemHeights.put(position!!, height)
    }

    fun getItemView(position: Int): View? {
        return adapter!!.getStoredView(position)
    }

    public override fun computeVerticalScrollOffset(): Int {
        return super.computeVerticalScrollOffset()
    }

    public override fun computeVerticalScrollExtent(): Int {
        return super.computeVerticalScrollExtent()
    }

    public override fun computeVerticalScrollRange(): Int {
        return super.computeVerticalScrollRange()
    }

    fun scrollToBottom() {
        scrollHandler!!.scrollToBottom()
    }

    fun scrollToPosition(y: Int) {
        scrollHandler!!.scrollToPosition(y)
    }

    fun scrollToItem(itemIndex: Int) {
        scrollHandler!!.scrollToItem(itemIndex)
    }
}
