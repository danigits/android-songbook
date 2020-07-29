package igrek.songbook.room

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import igrek.songbook.R
import igrek.songbook.info.UiInfoService
import igrek.songbook.info.errorcheck.SafeClickListener
import igrek.songbook.inject.LazyExtractor
import igrek.songbook.inject.LazyInject
import igrek.songbook.inject.appFactory
import igrek.songbook.layout.InflatedLayout
import igrek.songbook.layout.contextmenu.ContextMenuBuilder
import igrek.songbook.layout.dialog.InputDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RoomListLayoutController(
        bluetoothService: LazyInject<BluetoothService> = appFactory.bluetoothService,
        uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
        roomLobby: LazyInject<RoomLobby> = appFactory.roomLobby,
) : InflatedLayout(
        _layoutResourceId = R.layout.screen_rooms_list
) {
    private val bluetoothService by LazyExtractor(bluetoothService)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val roomLobby by LazyExtractor(roomLobby)

    private var joinRoomListView: JoinRoomListView? = null
    private var myNameEditText: EditText? = null

    override fun showLayout(layout: View) {
        super.showLayout(layout)

        layout.findViewById<ImageButton>(R.id.moreActionsButton)?.setOnClickListener(SafeClickListener {
            showMoreActions()
        })

        myNameEditText = layout.findViewById<EditText>(R.id.myNameEditText)?.also {
            it.setText(bluetoothService.deviceName())
        }

        layout.findViewById<Button>(R.id.hostNewRoomButton)?.setOnClickListener {
            hostRoom()
        }

        joinRoomListView = layout.findViewById<JoinRoomListView>(R.id.itemsListView)?.also {
            it.onClickCallback = { room ->
                joinRoom(room)
            }
        }

        layout.findViewById<Button>(R.id.scanRoomsButtton)?.setOnClickListener {
            scanRooms()
        }
    }

    private fun joinRoom(room: Room) {
        GlobalScope.launch {
            uiInfoService.showInfo("Joining room on ${room.name}...")
            val username = myNameEditText?.text?.toString().orEmpty()
            roomLobby.joinRoom(username, room).await().fold(onSuccess = {
                uiInfoService.showInfo("Joined to room")
                GlobalScope.launch(Dispatchers.Main) {
                    layoutController.showLayout(RoomLobbyLayoutController::class)
                }
            }, onFailure = { e ->
                logger.error(e)
                uiInfoService.showInfoIndefinite(R.string.error_communication_breakdown, e.message.orEmpty())
            })
        }
    }

    private fun hostRoom() {
        InputDialogBuilder().input(R.string.screen_share_set_room_password, null) { password ->
            GlobalScope.launch {
                val username = myNameEditText?.text?.toString().orEmpty()
                roomLobby.hostRoom(username, password).await().fold(onSuccess = {
                    uiInfoService.showInfo("room created")
                    layoutController.showLayout(RoomLobbyLayoutController::class)
                }, onFailure = { e ->
                    logger.error(e)
                    uiInfoService.showInfoIndefinite(R.string.error_communication_breakdown, e.message.orEmpty())
                })
            }
        }
    }

    private fun showMoreActions() {
        ContextMenuBuilder().showContextMenu(mutableListOf(
                ContextMenuBuilder.Action(R.string.screen_share_scan_rooms) {
                    scanRooms()
                },
        ))
    }

    private fun scanRooms() {
        joinRoomListView?.items = emptyList()
        uiInfoService.showInfoIndefinite(R.string.screen_share_scanning_devices)

        GlobalScope.launch(Dispatchers.Main) {
            bluetoothService.scanRoomsAsync().await().fold(onSuccess = { roomCh ->
                for (room in roomCh) {
                    joinRoomListView?.add(room)
                }
                uiInfoService.showInfo("scanning completed")
            }, onFailure = { e ->
                logger.error(e)
                uiInfoService.showInfoIndefinite(R.string.error_communication_breakdown, e.message.orEmpty())
            })
        }
    }

}