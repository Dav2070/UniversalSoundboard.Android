package app.dav.universalsoundboard.models

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.ComponentName
import android.content.Context
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import app.dav.universalsoundboard.data.FileManager
import app.dav.universalsoundboard.services.*
import app.dav.universalsoundboard.utilities.Utils
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import java.util.*

private const val updateSeekbarInterval = 250L

class PlayingSound(val uuid: UUID,
                   var currentSound: Int,
                   var sounds: ArrayList<Sound>,
                   var repetitions: Int,
                   var randomly: Boolean,
                   var volume: Double) {

    var mediaBrowser: MediaBrowserCompat? = null
    var mediaController: MediaControllerCompat? = null
    var timerIsOn = false
    val timer: Runnable
    val timerHandler = Handler()

    // LiveData for data binding
    private val progressData = MutableLiveData<Int>()
    val progress: LiveData<Int>
        get() = progressData
    private val durationData = MutableLiveData<Int>()
    val duration: LiveData<Int>
        get() = durationData
    private val isPlayingData = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean>
        get() = isPlayingData
    private val currentSoundData = MutableLiveData<Int>()
    val currentSoundLiveData: LiveData<Int>
        get() = currentSoundData

    constructor(context: Context,
                uuid: UUID,
                currentSound: Int,
                sounds: ArrayList<Sound>,
                repetitions: Int,
                randomly: Boolean,
                volume: Double) : this(uuid, currentSound, sounds, repetitions, randomly, volume){

        initMediaConnection(context, null, 0)
    }

    init {
        currentSoundData.value = currentSound

        timer = object : Runnable{
            override fun run() {
                if(timerIsOn){
                    val p = progressData.value
                    if(p != null){
                        progressData.value = p + updateSeekbarInterval.toInt()
                    }

                    if(progressData.value ?: 0 > durationData.value ?: 0){
                        timerHandler.removeCallbacks(this)
                    }else{
                        timerHandler.postDelayed(this, updateSeekbarInterval)
                    }
                }
            }
        }

        timerHandler.post(timer)
    }

    private fun initMediaConnection(context: Context, action: MediaAction?, position: Int){
        if(mediaBrowser == null){
            mediaBrowser = MediaBrowserCompat(context,
                    ComponentName(context, MediaPlaybackService::class.java),
                    object : MediaBrowserCompat.ConnectionCallback(){
                        override fun onConnected() {
                            super.onConnected()
                            val browser = mediaBrowser ?: return

                            mediaController = MediaControllerCompat(context, browser.sessionToken)

                            mediaController?.registerCallback(object : MediaControllerCompat.Callback() {
                                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                                    super.onMetadataChanged(metadata)

                                    val uuid = Utils.getUuidFromString(metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)) ?: return
                                    if(uuid != this@PlayingSound.uuid) return

                                    // Get the new values of the PlayingSound
                                    GlobalScope.launch(Dispatchers.Main) {
                                        val newPlayingSound = FileManager.getPlayingSound(uuid) ?: return@launch
                                        currentSound = newPlayingSound.currentSound
                                        sounds = newPlayingSound.sounds
                                        repetitions = newPlayingSound.repetitions
                                        randomly = newPlayingSound.randomly
                                        volume = newPlayingSound.volume

                                        currentSoundData.value = newPlayingSound.currentSound
                                    }
                                }

                                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                                    super.onPlaybackStateChanged(state)

                                    val uuid = Utils.getUuidFromString(state?.extras?.getString(BUNDLE_UUID_KEY)) ?: return
                                    if(uuid != this@PlayingSound.uuid) return

                                    val playbackState = state?.playbackState as PlaybackState

                                    progressData.value = playbackState.extras?.getInt(BUNDLE_POSITION_KEY) ?: 0
                                    durationData.value = playbackState.extras?.getInt(BUNDLE_DURATION_KEY) ?: 0

                                    if(playbackState.state == PlaybackStateCompat.STATE_PLAYING){
                                        isPlayingData.value = true
                                        timerIsOn = true
                                        timerHandler.post(timer)
                                    }else if(playbackState.state == PlaybackStateCompat.STATE_PAUSED ||
                                            playbackState.state == PlaybackStateCompat.STATE_STOPPED){
                                        isPlayingData.value = false
                                        timerIsOn = false
                                    }
                                }
                            })

                            browser.subscribe(browser.root, object : MediaBrowserCompat.SubscriptionCallback(){
                                override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                                    super.onChildrenLoaded(parentId, children)
                                }
                            })

                            when(action){
                                MediaAction.PlayPause -> playOrPause(context)
                                MediaAction.Stop -> stop(context)
                                MediaAction.SkipPrevious -> skipPrevious(context)
                                MediaAction.SkipNext -> skipNext(context)
                                MediaAction.Seek -> seekTo(context, position)
                            }
                        }

                        override fun onConnectionFailed() {
                            super.onConnectionFailed()
                        }

                        override fun onConnectionSuspended() {
                            super.onConnectionSuspended()
                        }
                    }, null)

            mediaBrowser?.connect()
        }
    }

    fun playOrPause(context: Context){
        if(mediaController == null){
            initMediaConnection(context, MediaAction.PlayPause, 0)
        }else{
            val bundle = Bundle()
            bundle.putString(BUNDLE_UUID_KEY, uuid.toString())

            // Connection was already established; continue playing the sound
            if(isPlayingData.value == true){
                mediaController?.transportControls?.sendCustomAction(CUSTOM_ACTION_PAUSE, bundle)
                isPlayingData.value = false
            }else{
                mediaController?.transportControls?.sendCustomAction(CUSTOM_ACTION_PLAY, bundle)
                isPlayingData.value = true
            }
        }
    }

    fun stop(context: Context){
        if(mediaController == null){
            initMediaConnection(context, MediaAction.Stop, 0)
        }else{
            val bundle = Bundle()
            bundle.putString(BUNDLE_UUID_KEY, uuid.toString())
            mediaController?.transportControls?.sendCustomAction(CUSTOM_ACTION_STOP, bundle)
        }
    }

    fun skipPrevious(context: Context){
        if(mediaController == null){
            initMediaConnection(context, MediaAction.SkipPrevious, 0)
        }else{
            val bundle = Bundle()
            bundle.putString(BUNDLE_UUID_KEY, uuid.toString())
            mediaController?.transportControls?.sendCustomAction(CUSTOM_ACTION_PREVIOUS, bundle)
        }
    }

    fun skipNext(context: Context){
        if(mediaController == null){
            initMediaConnection(context, MediaAction.SkipNext, 0)
        }else{
            val bundle = Bundle()
            bundle.putString(BUNDLE_UUID_KEY, uuid.toString())
            mediaController?.transportControls?.sendCustomAction(CUSTOM_ACTION_NEXT, bundle)
        }
    }

    fun seekTo(context: Context, position: Int){
        if(mediaController == null){
            initMediaConnection(context, MediaAction.Seek, position)
        }else{
            val bundle = Bundle()
            bundle.putString(BUNDLE_UUID_KEY, uuid.toString())
            bundle.putInt(BUNDLE_POSITION_KEY, position)
            mediaController?.transportControls?.sendCustomAction(CUSTOM_ACTION_SEEK, bundle)
            progressData.value = position
        }
    }
}

enum class MediaAction{
    PlayPause(),
    Stop(),
    SkipPrevious(),
    SkipNext(),
    Seek()
}