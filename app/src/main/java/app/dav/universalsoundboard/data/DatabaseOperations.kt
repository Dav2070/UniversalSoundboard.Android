package app.dav.universalsoundboard.data

import app.dav.davandroidlibrary.Dav
import app.dav.davandroidlibrary.data.Property
import app.dav.davandroidlibrary.data.TableObject
import java.io.File
import java.util.*

object DatabaseOperations {
    // General methods
    suspend fun getObject(uuid: UUID) : TableObject?{
        return Dav.Database.getTableObject(uuid).await()
    }
    // End General methods

    // Sound methods
    fun createSound(uuid: UUID, name: String, soundUuid: String, categoryUuid: String){
        // Create the properties of the table object
        val nameProperty = Property()
        nameProperty.name = FileManager.soundTableNamePropertyName
        nameProperty.value = name

        val properties = arrayListOf(nameProperty)

        if(!soundUuid.isEmpty()){
            val soundFileProperty = Property()
            soundFileProperty.name = FileManager.soundTableSoundUuidPropertyName
            soundFileProperty.value = soundUuid
            properties.add(soundFileProperty)
        }

        if(!categoryUuid.isEmpty()){
            val categoryProperty = Property()
            categoryProperty.name = FileManager.soundTableCategoryUuidPropertyName
            categoryProperty.value = categoryUuid
            properties.add(categoryProperty)
        }

        TableObject(uuid, FileManager.soundTableId, properties)
    }

    suspend fun getAllSounds() : ArrayList<TableObject>{
        return Dav.Database.getAllTableObjects(FileManager.soundTableId, false).await()
    }

    suspend fun updateSound(uuid: UUID, name: String?, favourite: String?, soundUuid: String?, imageUuid: String?, categoryUuid: String?){
        // Get the sound table object
        val soundTableObject = Dav.Database.getTableObject(uuid).await()

        if(soundTableObject == null) return
        if(soundTableObject.tableId != FileManager.soundTableId) return

        if(!name.isNullOrEmpty()) soundTableObject.setPropertyValue(FileManager.soundTableNamePropertyName, name!!)
        if(!favourite.isNullOrEmpty()) soundTableObject.setPropertyValue(FileManager.soundTableFavouritePropertyName, favourite!!)
        if(!soundUuid.isNullOrEmpty()) soundTableObject.setPropertyValue(FileManager.soundTableSoundUuidPropertyName, soundUuid!!)
        if(!imageUuid.isNullOrEmpty()) soundTableObject.setPropertyValue(FileManager.soundTableImageUuidPropertyName, imageUuid!!)
        if(!categoryUuid.isNullOrEmpty()) soundTableObject.setPropertyValue(FileManager.soundTableCategoryUuidPropertyName, categoryUuid!!)
    }
    // End Sound methods

    // SoundFile methods
    fun createSoundFile(uuid: UUID, audioFile: File){
        TableObject(uuid, FileManager.soundFileTableId, audioFile)
    }

    suspend fun getAllSoundFiles() : ArrayList<TableObject>{
        return Dav.Database.getAllTableObjects(FileManager.soundFileTableId, false).await()
    }
    // End SoundFile methods

    // ImageFile methods
    fun createImageFile(uuid: UUID, imageFile: File){
        TableObject(uuid, FileManager.imageFileTableId, imageFile)
    }

    suspend fun updateImageFile(uuid: UUID, imageFile: File){
        val imageFileTableObject = Dav.Database.getTableObject(uuid).await()

        if(imageFileTableObject == null) return
        if(imageFileTableObject.tableId != FileManager.imageFileTableId) return

        imageFileTableObject.setFile(imageFile)
    }
    // End ImageFile methods

    // Category methods
    fun createCategory(uuid: UUID, name: String, icon: String){
        val nameProperty = Property()
        nameProperty.name = FileManager.categoryTableNamePropertyName
        nameProperty.value = name

        val iconProperty = Property()
        iconProperty.name = FileManager.categoryTableIconPropertyName
        iconProperty.value = icon

        val properties = arrayListOf(nameProperty, iconProperty)
        TableObject(uuid, FileManager.categoryTableId, properties)
    }

    suspend fun getAllCategories() : ArrayList<TableObject>{
        return Dav.Database.getAllTableObjects(FileManager.categoryTableId, false).await()
    }
    // End Category methods
}