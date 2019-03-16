package app.dav.universalsoundboard

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import app.dav.davandroidlibrary.Dav
import app.dav.davandroidlibrary.common.ProjectInterface
import app.dav.davandroidlibrary.models.DavUser
import app.dav.universalsoundboard.adapters.CategoryListAdapter
import app.dav.universalsoundboard.adapters.PlayingSoundListAdapter
import app.dav.universalsoundboard.common.GeneralMethods
import app.dav.universalsoundboard.common.LocalDataSettings
import app.dav.universalsoundboard.common.RetrieveConstants
import app.dav.universalsoundboard.common.TriggerAction
import app.dav.universalsoundboard.data.FileManager
import app.dav.universalsoundboard.data.FileManager.SHOW_CATEGORIES_OF_SOUNDS_KEY
import app.dav.universalsoundboard.data.FileManager.SHOW_PLAYING_SOUNDS_KEY
import app.dav.universalsoundboard.data.FileManager.SHOW_SOUND_TABS_KEY
import app.dav.universalsoundboard.fragments.*
import app.dav.universalsoundboard.models.Category
import app.dav.universalsoundboard.models.PlayingSound
import app.dav.universalsoundboard.services.MediaPlaybackService
import app.dav.universalsoundboard.services.NOTIFICATION_ID
import app.dav.universalsoundboard.viewmodels.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.gordonwong.materialsheetfab.MaterialSheetFab
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.category_list.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_sound.*
import kotlinx.android.synthetic.main.playing_sound_bottom_sheet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

const val REQUEST_AUDIO_FILE_GET = 1

class MainActivity :
        AppCompatActivity(),
        CategoryListAdapter.OnItemClickListener,
        PlayingSoundListAdapter.PlayingSoundButtonClickListeners {
    private lateinit var viewModel: MainViewModel
    private var soundFragment: SoundFragment = SoundFragment.newInstance(1)
    private var settingsFragment: SettingsFragment = SettingsFragment.newInstance()
    private var accountFragment: AccountFragment = AccountFragment.newInstance()
    private var currentFragment: CurrentFragment = CurrentFragment.SoundFragment
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<CoordinatorLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        init()
        initSettingsValues()
    }

    private fun init(){
        FileManager.itemViewHolder.mainActivity = this
        Dav.init(this)
        ProjectInterface.localDataSettings = LocalDataSettings()
        ProjectInterface.generalMethods = GeneralMethods()
        ProjectInterface.retrieveConstants = RetrieveConstants()
        ProjectInterface.triggerAction = TriggerAction()
        FileManager.itemViewHolder.setUser(DavUser())

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.categoryListAdapter = CategoryListAdapter(this)
        viewModel.playingSoundListAdapter = PlayingSoundListAdapter(applicationContext, this)

        startService(Intent(applicationContext, MediaPlaybackService::class.java))

        bottomSheetBehavior = BottomSheetBehavior.from(playing_sound_bottom_sheet)
        val materialSheetFab = MaterialSheetFab(fab, fab_sheet, overlay, R.color.colorSecondary, R.color.colorPrimary)

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        bottom_category_list_settings_item.setOnClickListener {
            drawer_layout.closeDrawers()
            showSettingsFragment()
        }

        bottom_category_list_account_item.setOnClickListener {
            drawer_layout.closeDrawers()
            showAccountFragment()
        }

        fab_sheet_item_new_sound.setOnClickListener{
            materialSheetFab.hideSheet()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "audio/*"
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, REQUEST_AUDIO_FILE_GET)
            }
        }

        fab_sheet_item_new_category.setOnClickListener{
            CategoryDialogFragment().show(supportFragmentManager, "create_category")
            materialSheetFab.hideSheet()
        }

        // Bind the itemViewHolder properties to the UI
        FileManager.itemViewHolder.setTitle(resources.getString(R.string.all_sounds))
        FileManager.itemViewHolder.title.observe(this, Observer<String> { title -> supportActionBar?.title = title })

        Category.allSoundsCategory.name = resources.getString(R.string.all_sounds)
        category_list.layoutManager = LinearLayoutManager(this)
        category_list.adapter = viewModel.categoryListAdapter

        GlobalScope.launch(Dispatchers.Main) { FileManager.itemViewHolder.loadCategories() }
        FileManager.itemViewHolder.categories.observe(this, Observer {
            if(it != null) viewModel.categoryListAdapter?.submitList(it)
            viewModel.categoryListAdapter?.notifyDataSetChanged()
        })

        playing_sound_list.layoutManager = LinearLayoutManager(this)
        playing_sound_list.adapter = viewModel.playingSoundListAdapter

        GlobalScope.launch(Dispatchers.Main) { FileManager.itemViewHolder.loadPlayingSounds() }
        FileManager.itemViewHolder.playingSounds.observe(this, Observer {
            if(it == null) return@Observer

            if(it.count() == 0){
                hideBottomSheet()

                val handler = Handler()
                handler.postDelayed({
                    viewModel.playingSoundListAdapter?.submitList(it)
                    viewModel.playingSoundListAdapter?.notifyDataSetChanged()
                }, 100)
            }else{
                viewModel.playingSoundListAdapter?.submitList(it)
                viewModel.playingSoundListAdapter?.notifyDataSetChanged()

                val handler = Handler()
                handler.postDelayed({
                    showBottomSheet()
                }, 100)
            }
        })

        FileManager.itemViewHolder.user.observe(this, Observer {
            if(it == null || !it.isLoggedIn){
                bottom_category_list_account_item_name.text = getString(R.string.login)
                bottom_category_list_account_item_icon.setImageResource(R.drawable.ic_person)
            }else{
                bottom_category_list_account_item_name.text = it.username

                val avatar = it.avatar
                if(avatar.exists()){
                    val avatarDrawable = RoundedBitmapDrawableFactory.create(resources, avatar.path)
                    avatarDrawable.cornerRadius = 600f
                    bottom_category_list_account_item_icon.setImageDrawable(avatarDrawable)
                }
            }
        })

        FileManager.itemViewHolder.isProgressBarVisible.observe(this, Observer {
            it ?: return@Observer

            if(it){
                background_dim.visibility = View.VISIBLE
                progress_bar.visibility = View.VISIBLE
                fab.isEnabled = false
            }else{
                background_dim.visibility = View.GONE
                progress_bar.visibility = View.GONE
                fab.isEnabled = true
            }
        })

        // Set the correct fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, soundFragment)
        transaction.add(R.id.fragment_container, settingsFragment)
        transaction.add(R.id.fragment_container, accountFragment)
        transaction.hide(settingsFragment)
        transaction.hide(accountFragment)
        transaction.commit()
    }

    private fun initSettingsValues(){
        // showSoundTabs
        val showSoundTabs = FileManager.getBooleanValue(SHOW_SOUND_TABS_KEY, FileManager.showSoundTabsDefault)
        FileManager.itemViewHolder.setShowSoundTabs(showSoundTabs)

        // showPlayingSounds
        val showPlayingSounds = FileManager.getBooleanValue(SHOW_PLAYING_SOUNDS_KEY, FileManager.showPlayingSoundsDefault)
        FileManager.itemViewHolder.setShowPlayingSounds(showPlayingSounds)

        // showCategoriesOfSounds
        val showCategoriesOfSounds = FileManager.getBooleanValue(SHOW_CATEGORIES_OF_SOUNDS_KEY, FileManager.showCategoriesOfSoundsDefault)
        FileManager.itemViewHolder.setShowCategoriesOfSounds(showCategoriesOfSounds)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            if(currentFragment != CurrentFragment.SoundFragment){
                // Show the SoundFragment
                showSoundFragment()
            }else if(FileManager.itemViewHolder.currentCategory.uuid != Category.allSoundsCategory.uuid){
                // App shows a category or the settings; navigate to All Sounds
                GlobalScope.launch(Dispatchers.Main) { FileManager.showCategory(Category.allSoundsCategory) }
            }else{
                // App shows All Sounds, show the home screen
                moveTaskToBack(true)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        FileManager.itemViewHolder.showCategoryIcons.observe(this, Observer {
            val editCategoryItem = toolbar.menu.findItem(R.id.action_edit_category)
            val deleteCategoryItem = toolbar.menu.findItem(R.id.action_delete_category)
            editCategoryItem.isVisible = it == true
            deleteCategoryItem.isVisible = it == true
        })

        FileManager.itemViewHolder.showPlayAllIcon.observe(this, Observer {
            val playAllItem = toolbar.menu.findItem(R.id.action_play_all)
            playAllItem.isVisible = it == true
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_edit_category -> {
                val fragment = CategoryDialogFragment()
                fragment.category = FileManager.itemViewHolder.currentCategory
                fragment.show(supportFragmentManager, "edit_category")
                true
            }
            R.id.action_delete_category -> {
                val fragment = DeleteCategoryDialogFragment()
                fragment.category = FileManager.itemViewHolder.currentCategory
                fragment.show(supportFragmentManager, "delete_category")
                true
            }
            R.id.action_play_all -> {
                val sounds =
                        (if(tablayout.visibility == View.VISIBLE && tablayout.selectedTabPosition == 1)
                            FileManager.itemViewHolder.favouriteSounds.value
                        else
                            FileManager.itemViewHolder.sounds.value) ?: return true

                if(sounds.size == 0) return true
                SoundFragment.playSounds(sounds, this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_AUDIO_FILE_GET && resultCode == Activity.RESULT_OK){
            val fileUri: Uri? = data?.data
            val clipData = data?.clipData

            GlobalScope.launch(Dispatchers.Main) {
                if(fileUri != null){
                    // One file selected
                    viewModel.addSounds(arrayListOf(fileUri), application.contentResolver, cacheDir)
                }else if(clipData != null){
                    // Multiple files selected
                    val fileUris = ArrayList<Uri>()
                    for(i in 0 until clipData.itemCount){
                        fileUris.add(clipData.getItemAt(i).uri)
                    }

                    viewModel.addSounds(fileUris, application.contentResolver, cacheDir)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()

        stopService(Intent(applicationContext, MediaPlaybackService::class.java))

        // Disconnect all MediaBrowser of the Playing Sounds
        val playingSounds = FileManager.itemViewHolder.playingSounds.value ?: return

        for(playingSound in playingSounds){
            playingSound.disconnect()
        }

        // Remove the notification
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val jwt = intent?.data?.getQueryParameter("jwt") ?: return

        // Log the user in
        val user = DavUser()

        GlobalScope.launch(Dispatchers.Main) {
            async { user.login(jwt) }.await()
            FileManager.itemViewHolder.setUser(user)
        }
    }

    override fun onItemClicked(category: Category) {
        drawer_layout.closeDrawers()
        showSoundFragment()

        GlobalScope.launch(Dispatchers.Main) {
            FileManager.showCategory(category)
        }
    }

    override fun skipPreviousButtonClicked(playingSound: PlayingSound) {
        playingSound.skipPrevious(applicationContext)
    }

    override fun playPauseButtonClicked(playingSound: PlayingSound) {
        playingSound.playOrPause(applicationContext)
    }

    override fun skipNextButtonClicked(playingSound: PlayingSound) {
        playingSound.skipNext(applicationContext)
    }

    override fun removeButtonClicked(playingSound: PlayingSound) {
        playingSound.stop(applicationContext)
        GlobalScope.launch(Dispatchers.Main) { FileManager.deletePlayingSound(playingSound.uuid) }
    }

    override fun menuButtonClicked(playingSound: PlayingSound, view: View) {
        val menu = PopupMenu(this, view)
        menu.inflate(R.menu.playing_sound_item_context_menu)
        menu.show()

        menu.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.playing_sound_item_context_menu_repeat -> showSetRepetitionsDialog(playingSound)
            }
            true
        }
    }

    private fun showSetRepetitionsDialog(playingSound: PlayingSound){
        val fragmentManager = supportFragmentManager ?: return
        val fragment = SetRepetitionsDialogFragment()
        fragment.playingSound = playingSound
        fragment.show(fragmentManager, "playing_sound_item_set_repetitions_dialog")
    }

    private fun getCurrentFragment() : Fragment {
        return when(currentFragment){
            CurrentFragment.SoundFragment -> soundFragment
            CurrentFragment.SettingsFragment -> settingsFragment
            CurrentFragment.AccountFragment -> accountFragment
        }
    }

    private fun showSoundFragment(){
        supportFragmentManager
                .beginTransaction()
                .hide(getCurrentFragment())
                .show(soundFragment)
                .commit()
        currentFragment = CurrentFragment.SoundFragment

        // Set the title
        FileManager.itemViewHolder.setTitle(FileManager.itemViewHolder.currentCategory.name)

        // Show the icons
        if(FileManager.itemViewHolder.currentCategory.uuid != Category.allSoundsCategory.uuid){
            // Show the Category icons
            FileManager.itemViewHolder.setShowCategoryIcons(true)
        }
        FileManager.itemViewHolder.setShowPlayAllIcon(FileManager.itemViewHolder.sounds.value?.size ?: 0 > 0)
        showFab()
        showBottomSheet()
    }

    private fun showSettingsFragment(){
        supportFragmentManager
                .beginTransaction()
                .hide(getCurrentFragment())
                .show(settingsFragment)
                .commit()
        currentFragment = CurrentFragment.SettingsFragment

        // Set the title
        FileManager.itemViewHolder.setTitle(getString(R.string.settings))

        // Hide the icons
        FileManager.itemViewHolder.setShowCategoryIcons(false)
        FileManager.itemViewHolder.setShowPlayAllIcon(false)
        hideFab()
        hideBottomSheet()
    }

    private fun showAccountFragment(){
        supportFragmentManager
                .beginTransaction()
                .hide(getCurrentFragment())
                .show(accountFragment)
                .commit()
        currentFragment = CurrentFragment.AccountFragment

        // Set the title
        FileManager.itemViewHolder.setTitle(getString(R.string.account_fragment_title))

        // Hide the icons
        FileManager.itemViewHolder.setShowCategoryIcons(false)
        FileManager.itemViewHolder.setShowPlayAllIcon(false)
        hideFab()
        hideBottomSheet()
    }

    private fun showBottomSheet(){
        if(FileManager.itemViewHolder.playingSounds.value?.count() ?: 0 == 0) return

        if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN){
            // The bottom sheet was hidden
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        val handler = Handler()
        handler.postDelayed({
            bottomSheetBehavior.isHideable = false
        }, 1)
    }

    private fun showFab(){
        val layoutParams = fab.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.anchorId = R.id.playing_sound_bottom_sheet
        fab.layoutParams = layoutParams
        fab.show()
    }

    private fun hideBottomSheet(){
        if(FileManager.itemViewHolder.playingSounds.value?.count() ?: 0 < 0) return

        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun hideFab(){
        val layoutParams = fab.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.anchorId = View.NO_ID
        fab.layoutParams = layoutParams
        fab.hide()
    }
}

enum class CurrentFragment(){
    SoundFragment(),
    SettingsFragment(),
    AccountFragment()
}