package app.dav.universalsoundboard.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.dav.universalsoundboard.R
import app.dav.universalsoundboard.databinding.PlayingSoundListItemBinding
import app.dav.universalsoundboard.models.PlayingSound
import app.dav.universalsoundboard.models.PlayingSoundState

class PlayingSoundListAdapter(val context: Context, private val clickListeners: PlayingSoundButtonClickListeners) :
        ListAdapter<PlayingSound, PlayingSoundListAdapter.ViewHolder>(PlayingSoundDiffCallback()) {

    interface PlayingSoundButtonClickListeners{
        fun skipPreviousButtonClicked(playingSound: PlayingSound)
        fun playPauseButtonClicked(playingSound: PlayingSound)
        fun skipNextButtonClicked(playingSound: PlayingSound)
        fun removeButtonClicked(playingSound: PlayingSound)
        fun menuButtonClicked(playingSound: PlayingSound, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PlayingSoundListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item,
                View.OnClickListener { clickListeners.skipPreviousButtonClicked(item) },
                View.OnClickListener { clickListeners.playPauseButtonClicked(item) },
                View.OnClickListener { clickListeners.skipNextButtonClicked(item) },
                View.OnClickListener { clickListeners.removeButtonClicked(item) },
                View.OnClickListener { clickListeners.menuButtonClicked(item, it) })
    }

    inner class ViewHolder(private val binding: PlayingSoundListItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item: PlayingSound,
                 skipPreviousClickListener: View.OnClickListener,
                 playPauseClickListener: View.OnClickListener,
                 skipNextClickListener: View.OnClickListener,
                 removeClickListener: View.OnClickListener,
                 menuClickListener: View.OnClickListener){
            binding.playingSound = item
            binding.skipPreviousClickListener = skipPreviousClickListener
            binding.playPauseClickListener = playPauseClickListener
            binding.skipNextClickListener = skipNextClickListener
            binding.removeClickListener = removeClickListener
            binding.menuClickListener = menuClickListener

            val playPauseButton = binding.root.findViewById<ImageView>(R.id.playing_sound_list_item_play_pause)
            val skipPreviousButton = binding.root.findViewById<ImageButton>(R.id.playing_sound_list_item_skip_previous)
            val skipNextButton = binding.root.findViewById<ImageButton>(R.id.playing_sound_list_item_skip_next)
            val nameTextView = binding.root.findViewById<TextView>(R.id.playing_sound_list_item_name)
            val seekbar = binding.root.findViewById<SeekBar>(R.id.playing_sound_list_item_seekbar)
            val progressbar = binding.root.findViewById<ProgressBar>(R.id.playing_sound_list_item_progressbar)

            item.state.observeForever {
                if(it == PlayingSoundState.Paused){
                    playPauseButton.setImageResource(R.drawable.ic_play_arrow_white)
                    seekbar.visibility = View.VISIBLE
                    progressbar.visibility = View.GONE
                }else if(it == PlayingSoundState.Buffering){
                    playPauseButton.setImageResource(R.drawable.ic_pause_white)

                    // Show the progress bar and set it to indeterminate
                    progressbar.visibility = View.VISIBLE
                    seekbar.visibility = View.GONE
                }else{      // Playing or null
                    playPauseButton.setImageResource(R.drawable.ic_pause_white)
                    seekbar.visibility = View.VISIBLE
                    progressbar.visibility = View.GONE
                }
            }

            item.currentSoundLiveData.observeForever {
                if(it == null) return@observeForever
                try {
                    val currentSound = item.sounds[it]
                    nameTextView.text = currentSound.name

                    if(it == 0){
                        skipPreviousButton.visibility = View.GONE
                    }else{
                        skipPreviousButton.visibility = View.VISIBLE
                    }

                    if(item.sounds.count() > 1 && it == item.sounds.count() - 1){
                        skipNextButton.visibility = View.GONE
                    }else if(item.sounds.count() > 1){
                        skipNextButton.visibility = View.VISIBLE
                    }else{
                        skipNextButton.visibility = View.GONE
                    }
                }catch (e: Exception){}
            }

            item.progress.observeForever {
                it ?: return@observeForever
                seekbar.progress = it
            }

            item.duration.observeForever {
                it ?: return@observeForever
                seekbar.max = it
            }

            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if(fromUser){
                        item.seekTo(context, progress);
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }
    }
}