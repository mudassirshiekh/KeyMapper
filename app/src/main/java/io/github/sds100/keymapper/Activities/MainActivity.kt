package io.github.sds100.keymapper.Activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import io.github.sds100.keymapper.Adapters.KeymapAdapter
import io.github.sds100.keymapper.BuildConfig
import io.github.sds100.keymapper.Interfaces.OnDeleteMenuItemClickListener
import io.github.sds100.keymapper.Interfaces.OnItemClickListener
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.KeymapAdapterModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Selection.SelectableActionMode
import io.github.sds100.keymapper.Selection.SelectionCallback
import io.github.sds100.keymapper.Selection.SelectionEvent
import io.github.sds100.keymapper.Selection.SelectionProvider
import io.github.sds100.keymapper.Services.MyAccessibilityService
import io.github.sds100.keymapper.Services.MyIMEService
import io.github.sds100.keymapper.Utils.ActionUtils
import io.github.sds100.keymapper.Utils.NotificationUtils
import io.github.sds100.keymapper.ViewModels.KeyMapListViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.append
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class MainActivity : AppCompatActivity(), SelectionCallback, OnDeleteMenuItemClickListener,
        OnItemClickListener<KeymapAdapterModel> {

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                Intent.ACTION_INPUT_METHOD_CHANGED -> updateActionDescriptions()
            }
        }
    }

    private val mKeymapAdapter: KeymapAdapter = KeymapAdapter(this)

    private lateinit var mViewModel: KeyMapListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (defaultSharedPreferences.getBoolean(getString(R.string.key_pref_show_notification), true)) {
            NotificationUtils.showIMEPickerNotification(this)
        } else {
            NotificationUtils.hideImePickerNotification(this)
        }

        /*if the app is a debug build then enable the accessibility service in settings
        / automatically so I don't have to! :)*/
        if (BuildConfig.DEBUG) {
            MyAccessibilityService.enableServiceInSettings()
        }

        mViewModel = ViewModelProviders.of(this).get(KeyMapListViewModel::class.java)

        mViewModel.keyMapList.observe(this, Observer { keyMapList ->
            populateKeymapsAsync(keyMapList)

            updateAccessibilityServiceKeymapCache(keyMapList)
        })

        //start NewKeymapActivity when the fab is pressed
        fabNewKeyMap.setOnClickListener {
            val intent = Intent(this, NewKeymapActivity::class.java)
            startActivity(intent)
        }

        accessibilityServiceStatusLayout.setOnFixClickListener(View.OnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            startActivity(intent)
        })

        imeServiceStatusLayout.setOnFixClickListener(View.OnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            startActivity(intent)
        })

        mKeymapAdapter.iSelectionProvider.subscribeToSelectionEvents(this)

        //recyclerview stuff
        recyclerViewKeyMaps.layoutManager = LinearLayoutManager(this)
        recyclerViewKeyMaps.adapter = mKeymapAdapter

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
        registerReceiver(mBroadcastReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()

        if (MyAccessibilityService.isServiceEnabled(this)) {
            accessibilityServiceStatusLayout.changeToServiceEnabledState()
        } else {
            accessibilityServiceStatusLayout.changeToServiceDisabledState()
        }

        if (MyIMEService.isServiceEnabled(this)) {
            imeServiceStatusLayout.changeToServiceEnabledState()
        } else {
            imeServiceStatusLayout.changeToServiceDisabledState()
        }

        updateActionDescriptions()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState!!.putBundle(
                SelectionProvider.KEY_SELECTION_PROVIDER_STATE,
                mKeymapAdapter.iSelectionProvider.saveInstanceState())

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState!!.containsKey(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)) {
            val selectionProviderState =
                    savedInstanceState.getBundle(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)!!

            mKeymapAdapter.iSelectionProvider.restoreInstanceState(selectionProviderState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDeleteMenuButtonClick() {
        mViewModel.deleteKeyMapsById(*mKeymapAdapter.iSelectionProvider.selectedItemIds)
    }

    override fun onSelectionEvent(id: Long?, event: SelectionEvent) {
        if (event == SelectionEvent.START) {
            val actionMode = SelectableActionMode(this, mKeymapAdapter.iSelectionProvider, this)
            startSupportActionMode(actionMode)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setStatusBarColor(R.color.actionModeStatusBar)
            }

        } else if (event == SelectionEvent.STOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setStatusBarColor(R.color.colorPrimaryDark)
            }
        }
    }

    override fun onItemClick(item: KeymapAdapterModel) {
        val intent = Intent(this, EditKeymapActivity::class.java)
        intent.putExtra(EditKeymapActivity.EXTRA_KEYMAP_ID, item.id)

        startActivity(intent)
    }

    private fun updateAccessibilityServiceKeymapCache(keyMapList: List<KeyMap>) {
        val intent = Intent(MyAccessibilityService.ACTION_UPDATE_KEYMAP_CACHE)
        val jsonString = Gson().toJson(keyMapList)

        intent.putExtra(MyAccessibilityService.EXTRA_KEYMAP_CACHE_JSON, jsonString)

        sendBroadcast(intent)
    }

    private fun populateKeymapsAsync(keyMapList: List<KeyMap>) {
        doAsync {
            val adapterModels = mutableListOf<KeymapAdapterModel>()

            keyMapList.forEach { keyMap ->

                val actionDescription = ActionUtils.getDescription(this@MainActivity, keyMap.action)

                adapterModels.add(KeymapAdapterModel(keyMap, actionDescription))
            }

            mKeymapAdapter.itemList = adapterModels

            uiThread {
                mKeymapAdapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                setCaption()
            }
        }
    }

    private fun updateActionDescriptions() {
        doAsync {
            mKeymapAdapter.itemList.forEach { model ->
                val keyMapId = model.id
                val keyMap = mViewModel.keyMapList.value!!.find { it.id == keyMapId }

                if (keyMap != null) {
                    val actionDescription = ActionUtils.getDescription(this.weakRef.get()!!.baseContext, keyMap.action)
                    model.actionDescription = actionDescription
                }
            }

            uiThread {
                mKeymapAdapter.invalidateBoundViewHolders()
            }
        }
    }

    /**
     * Controls what message is displayed to the user on the home-screen
     */
    private fun setCaption() {
        //tell the user if they haven't created any KeyMaps
        if (mKeymapAdapter.itemCount == 0) {
            val spannableBuilder = SpannableStringBuilder()

            spannableBuilder.append(getString(R.string.shrug), RelativeSizeSpan(2f))
            spannableBuilder.append("\n\n")
            spannableBuilder.append(getString(R.string.no_key_maps))

            textViewCaption.visibility = View.VISIBLE
            textViewCaption.text = spannableBuilder
        } else {
            textViewCaption.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setStatusBarColor(@ColorRes colorId: Int) {
        window.statusBarColor = ContextCompat.getColor(this, colorId)
    }
}
