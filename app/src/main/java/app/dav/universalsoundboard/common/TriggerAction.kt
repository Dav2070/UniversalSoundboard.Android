package app.dav.universalsoundboard.common

import app.dav.davandroidlibrary.common.ITriggerAction
import app.dav.davandroidlibrary.models.TableObject
import app.dav.universalsoundboard.data.FileManager
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch

class TriggerAction : ITriggerAction {
    override fun updateAllOfTable(tableId: Int) {
        updateView(tableId, false)
    }

    override fun updateTableObject(tableObject: TableObject, fileDownloaded: Boolean) {
        updateView(tableObject.tableId, fileDownloaded)
    }

    override fun deleteTableObject(tableObject: TableObject) {
        updateView(tableObject.tableId, false)
    }

    private fun updateView(tableId: Int, fileDownloaded: Boolean){
        if(tableId == FileManager.imageFileTableId ||
                (tableId == FileManager.soundFileTableId && !fileDownloaded) ||
                tableId == FileManager.soundTableId){
            // Update the sounds
            GlobalScope.launch(Dispatchers.Main) { FileManager.itemViewHolder.loadSounds() }
        }else if(tableId == FileManager.categoryTableId){
            // Update the categories
            GlobalScope.launch(Dispatchers.Main) { FileManager.itemViewHolder.loadCategories() }
        }else if(tableId == FileManager.playingSoundTableId){
            // Update the playing sounds
            GlobalScope.launch(Dispatchers.Main) { FileManager.itemViewHolder.loadPlayingSounds() }
        }
    }
}