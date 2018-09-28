package app.dav.universalsoundboard.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.graphics.BitmapFactory
import android.support.v4.media.session.MediaControllerCompat
import app.dav.davandroidlibrary.models.TableObject
import app.dav.universalsoundboard.models.Category
import app.dav.universalsoundboard.models.PlayingSound
import app.dav.universalsoundboard.models.Sound
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

object FileManager{
    const val appId = 1                 // Dev: 8, Prod: 1
    const val soundFileTableId = 6      // Dev: 11, Prod: 6
    const val imageFileTableId = 7      // Dev: 15, Prod: 7
    const val categoryTableId = 8       // Dev: 16, Prod: 8
    const val soundTableId = 5          // Dev: 17, Prod: 5
    const val playingSoundTableId = 9   // Dev: 18, Prod: 9

    const val soundTableNamePropertyName = "name"
    const val soundTableFavouritePropertyName = "favourite"
    const val soundTableSoundUuidPropertyName = "sound_uuid"
    const val soundTableImageUuidPropertyName = "image_uuid"
    const val soundTableCategoryUuidPropertyName = "category_uuid"

    const val categoryTableNamePropertyName = "name"
    const val categoryTableIconPropertyName = "icon"

    const val playingSoundTableSoundIdsPropertyName = "sound_ids"
    const val playingSoundTableCurrentPropertyName = "current"
    const val playingSoundTableRepetitionsPropertyName = "repetitions"
    const val playingSoundTableRandomlyPropertyName = "randomly"
    const val playingSoundTableVolumePropertyName = "volume"

    val itemViewHolder: ItemViewHolder = ItemViewHolder(title = "All Sounds")

    suspend fun showCategory(category: Category){
        itemViewHolder.currentCategory = category
        itemViewHolder.setShowCategoryIcons(category.uuid != Category.allSoundsCategory.uuid)
        itemViewHolder.setTitle(category.name)
        itemViewHolder.loadSounds()
    }

    // Sound functions
    suspend fun addSound(uuid: UUID?, name: String, categoryUuid: UUID?, audioFile: File){
        // Generate a new uuid if necessary
        val newUuid: UUID = if(uuid == null) UUID.randomUUID() else uuid

        // Create a uuid for the sound file
        val soundFileUuid = UUID.randomUUID()

        val categoryUuidString = if(categoryUuid == null) "" else categoryUuid.toString()

        // Copy the sound file
        DatabaseOperations.createSoundFile(soundFileUuid, audioFile)

        DatabaseOperations.createSound(newUuid, name, soundFileUuid.toString(), categoryUuidString)
        itemViewHolder.loadSounds()
    }

    suspend fun getAllSounds() : ArrayList<Sound>{
        val tableObjects = DatabaseOperations.getAllSounds()
        val sounds = ArrayList<Sound>()

        for(obj in tableObjects){
            val sound = convertTableObjectToSound(obj) ?: continue
            sounds.add(sound)
        }

        return sounds
    }

    suspend fun getSoundsOfCategory(categoryUuid: UUID) : ArrayList<Sound>{
        val sounds = ArrayList<Sound>()

        for(sound in getAllSounds()){
            if(sound.category?.uuid == categoryUuid){
                sounds.add(sound)
            }
        }

        return sounds
    }

    suspend fun getSound(uuid: UUID) : Sound?{
        val soundTableObject = DatabaseOperations.getObject(uuid) ?: return null
        return convertTableObjectToSound(soundTableObject)
    }

    suspend fun updateImageOfSound(soundUuid: UUID, imageFile: File){
        val soundTableObject = DatabaseOperations.getObject(soundUuid)
        if(soundTableObject == null || soundTableObject.tableId != soundTableId) return

        val imageUuidString = soundTableObject.getPropertyValue(soundTableImageUuidPropertyName)
        var imageUuid = UUID.randomUUID()

        if(imageUuidString != null){
            imageUuid = UUID.fromString(imageUuidString)

            // Update the existing imageFile
            DatabaseOperations.updateImageFile(imageUuid, imageFile)
        }else{
            // Create a new imageFile
            DatabaseOperations.createImageFile(imageUuid, imageFile)
            DatabaseOperations.updateSound(soundUuid, null, null, null, imageUuid.toString(), null)
        }

        itemViewHolder.loadSounds()
    }

    suspend fun renameSound(uuid: UUID, newName: String){
        DatabaseOperations.updateSound(uuid, newName, null, null, null, null)
        itemViewHolder.loadSounds()
    }

    suspend fun deleteSound(uuid: UUID){
        DatabaseOperations.deleteSound(uuid)
        itemViewHolder.loadSounds()
    }
    // End Sound functions

    // Category functions
    suspend fun getAllCategories() : ArrayList<Category>{
        val categories = ArrayList<Category>()

        for(obj in DatabaseOperations.getAllCategories()){
            val category = convertTableObjectToCategory(obj) ?: continue
            categories.add(category)
        }

        return categories
    }

    suspend fun getCategory(uuid: UUID) : Category?{
        val category = DatabaseOperations.getObject(uuid)
        return if(category != null) convertTableObjectToCategory(category) else null
    }

    suspend fun addCategory(uuid: UUID?, name: String, icon: String) : Category?{
        // Generate a new uuid if necessary
        val newUuid: UUID = if(uuid == null) UUID.randomUUID() else uuid

        // Check if an object with the uuid already exists
        if(DatabaseOperations.getObject(newUuid) != null) return null

        DatabaseOperations.createCategory(newUuid, name, icon)
        itemViewHolder.loadCategories()
        val category = Category(newUuid, name, icon)
        return category
    }

    suspend fun updateCategory(uuid: UUID, name: String, icon: String){
        DatabaseOperations.updateCategory(uuid, name, icon)
        itemViewHolder.setTitle(name)
        itemViewHolder.loadCategories()
    }

    suspend fun deleteCategory(uuid: UUID){
        DatabaseOperations.deleteCategory(uuid)
        itemViewHolder.loadCategories()
    }
    // End Category functions

    // PlayingSound functions
    suspend fun addPlayingSound(uuid: UUID?, sounds: ArrayList<Sound>, current: Int, repetitions: Int, randomly: Boolean, volume: Double) : PlayingSound?{
        val newUuid: UUID = if(uuid == null) UUID.randomUUID() else uuid

        // Check if an object with the uuid already exists
        if(DatabaseOperations.getObject(newUuid) != null) return null

        // TODO: Check if playing sounds should be saved

        var newVolume = volume
        if(newVolume >= 1) newVolume = 1.0
        if(newVolume <= 0) newVolume = 0.0

        val soundIds = ArrayList<String>()
        for(sound in sounds)
            soundIds.add(sound.uuid.toString())

        DatabaseOperations.createPlayingSound(newUuid, soundIds, current, repetitions, randomly, volume)
        itemViewHolder.loadPlayingSounds()
        return PlayingSound(newUuid, current, sounds, repetitions, randomly, volume)
    }

    suspend fun getAllPlayingSounds() : ArrayList<PlayingSound>{
        val playingSounds = ArrayList<PlayingSound>()

        for(obj in DatabaseOperations.getAllPlayingSounds()){
            val playingSound = convertTableObjectToPlayingSound(obj) ?: continue
            playingSounds.add(playingSound)
        }

        return playingSounds
    }

    suspend fun deletePlayingSound(uuid: UUID){
        DatabaseOperations.deletePlayingSound(uuid)
        itemViewHolder.loadPlayingSounds()
    }
    // End PlayingSound functions

    private suspend fun convertTableObjectToSound(tableObject: TableObject) : Sound?{
        if(tableObject.tableId != FileManager.soundTableId) return null

        // Get name
        val name = tableObject.getPropertyValue(soundTableNamePropertyName) ?: ""

        // Get favourite
        var favourite = false
        val favouriteString = tableObject.getPropertyValue(soundTableFavouritePropertyName)
        favourite = if(favouriteString != null) favouriteString.toBoolean() else false

        val sound = Sound(tableObject.uuid, name, null, favourite, FileManager.getAudioFileOfSound(tableObject.uuid), null)

        // Get category
        val categoryUuidString = tableObject.getPropertyValue(soundTableCategoryUuidPropertyName)
        if(categoryUuidString != null){
            val categoryUuid = UUID.fromString(categoryUuidString)
            val category = getCategory(categoryUuid)
            if(category != null) sound.category = category
        }

        // Get image
        val imageUuidString = tableObject.getPropertyValue(soundTableImageUuidPropertyName)
        if(imageUuidString != null){
            val imageFileTableObject = getImageFileTableObject(tableObject.uuid)

            if(imageFileTableObject != null){
                if(imageFileTableObject.isFile && imageFileTableObject.file != null){
                    sound.image = BitmapFactory.decodeFile(imageFileTableObject.file?.path)
                }
            }
        }

        return sound
    }

    private fun convertTableObjectToCategory(tableObject: TableObject) : Category? {
        if(tableObject.tableId != FileManager.categoryTableId) return null

        // Get name
        val name = tableObject.getPropertyValue(categoryTableNamePropertyName) ?: ""

        // Get icon
        val icon = tableObject.getPropertyValue(categoryTableIconPropertyName) ?: Category.Icons.HOME

        return Category(tableObject.uuid, name, icon)
    }

    private suspend fun convertTableObjectToPlayingSound(tableObject: TableObject) : PlayingSound?{
        if(tableObject.tableId != FileManager.playingSoundTableId) return null

        // Get the sounds
        val soundIds = tableObject.getPropertyValue(FileManager.playingSoundTableSoundIdsPropertyName)
        val sounds = ArrayList<Sound>()

        if(soundIds != null){
            for (uuidString in soundIds.split(',')){
                val uuid = UUID.fromString(uuidString)
                val sound = getSound(uuid)
                if(sound != null) sounds.add(sound)
            }
        }

        if(sounds.count() == 0){
            // Delete the PlayingSound
            // TODO DeletePlayingSound()
            // return null
        }

        // Get current
        val currentString = tableObject.getPropertyValue(FileManager.playingSoundTableCurrentPropertyName)
        val current = currentString?.toIntOrNull() ?: 0

        // Get volume
        val volumeString = tableObject.getPropertyValue(FileManager.playingSoundTableVolumePropertyName)
        val volume = volumeString?.toDoubleOrNull() ?: 1.0

        // Get repetitions
        val repetitionsString = tableObject.getPropertyValue(FileManager.playingSoundTableRepetitionsPropertyName)
        val repetitions = repetitionsString?.toIntOrNull() ?: 1

        // Get randomly
        val randomlyString = tableObject.getPropertyValue(FileManager.playingSoundTableRandomlyPropertyName)
        val randomly = randomlyString?.toBoolean() ?: false

        return PlayingSound(tableObject.uuid, current, sounds, repetitions, randomly, volume)
    }

    fun getDavDataPath(filesDir: String) : File{
        val path = filesDir + "/dav"
        val dir = File(path)
        if(!dir.exists()){
            dir.mkdir()
        }
        return dir
    }

    suspend fun getAudioFileOfSound(uuid: UUID) : File?{
        val soundFileTableObject = getSoundFileTableObject(uuid) ?: return null
        return soundFileTableObject.file
    }

    private suspend fun getSoundFileTableObject(soundUuid: UUID) : TableObject?{
        val soundTableObject = DatabaseOperations.getObject(soundUuid) ?: return null
        val soundFileUuidString = soundTableObject.getPropertyValue(soundTableSoundUuidPropertyName) ?: return null
        val soundFileUuid = UUID.fromString(soundFileUuidString)
        return DatabaseOperations.getObject(soundFileUuid)
    }

    private suspend fun getImageFileTableObject(soundUuid: UUID) : TableObject?{
        val soundTableObject = DatabaseOperations.getObject(soundUuid) ?: return null
        val imageFileUuidString = soundTableObject.getPropertyValue(soundTableImageUuidPropertyName) ?: return null
        val imageFileUuid = UUID.fromString(imageFileUuidString)
        return DatabaseOperations.getObject(imageFileUuid)
    }
}

class ItemViewHolder(){
    constructor(title: String) : this() {
        titleData.value = title
        showCategoryIconsData.value = false
        soundsData.value = ArrayList<Sound>()
        categoriesData.value = ArrayList<Category>()
        playingSoundsData.value = ArrayList<PlayingSound>()
    }

    var currentCategory: Category = Category.allSoundsCategory
    private val titleData = MutableLiveData<String>()
    val title: LiveData<String>
        get() =  titleData
    private val showCategoryIconsData = MutableLiveData<Boolean>()
    val showCategoryIcons: LiveData<Boolean>
        get() = showCategoryIconsData
    private val showPlayAllIconData = MutableLiveData<Boolean>()
    val showPlayAllIcon: LiveData<Boolean>
        get() = showPlayAllIconData
    private val soundsData = MutableLiveData<ArrayList<Sound>>()
    val sounds: LiveData<ArrayList<Sound>>
        get() = soundsData
    private val categoriesData = MutableLiveData<ArrayList<Category>>()
    val categories: LiveData<ArrayList<Category>>
        get() = categoriesData
    private val playingSoundsData = MutableLiveData<ArrayList<PlayingSound>>()
    val playingSounds: LiveData<ArrayList<PlayingSound>>
        get() = playingSoundsData
    lateinit var mediaController: MediaControllerCompat

    fun setTitle(value: String){
        titleData.value = value
    }

    fun setShowCategoryIcons(showCategoryIcons: Boolean){
        showCategoryIconsData.value = showCategoryIcons
    }

    fun setShowPlayAllIcon(showPlayAllIcon: Boolean){
        showPlayAllIconData.value = showPlayAllIcon
    }

    suspend fun loadSounds(){
        soundsData.value = if(currentCategory.uuid == Category.allSoundsCategory.uuid){
            // Get all sounds
            FileManager.getAllSounds()
        }else{
            // Get the sounds of the selected category
            FileManager.getSoundsOfCategory(currentCategory.uuid)
        }
    }

    suspend fun loadCategories(){
        val categories = FileManager.getAllCategories()
        categories.add(0, Category.allSoundsCategory)

        categoriesData.value = categories
    }

    suspend fun loadPlayingSounds(){
        playingSoundsData.value = FileManager.getAllPlayingSounds()
    }
}