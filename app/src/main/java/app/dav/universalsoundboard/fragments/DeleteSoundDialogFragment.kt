package app.dav.universalsoundboard.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.support.v4.app.DialogFragment
import android.os.Bundle
import app.dav.universalsoundboard.R
import app.dav.universalsoundboard.data.FileManager
import app.dav.universalsoundboard.models.Sound
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch

class DeleteSoundDialogFragment : DialogFragment() {
    var sound: Sound? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return createDialog()
    }

    private fun createDialog() : AlertDialog{
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.delete_sound_dialog_title, sound?.name))
                .setMessage(R.string.delete_sound_dialog_message)
                .setPositiveButton(R.string.delete_sound_dialog_positive_button_text) { dialog, which ->
                    val s = sound
                    if(s != null){
                        GlobalScope.launch(Dispatchers.Main) {
                            FileManager.deleteSound(s.uuid)
                        }
                    }
                }
                .setNegativeButton(R.string.dialog_negative_button, null)
                .show()
    }
}