package cn.jailedbird.smartappsearch.ui.main

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import cn.jailedbird.smartappsearch.BuildConfig
import cn.jailedbird.smartappsearch.adapter.AppListTwoTypeAdapter
import cn.jailedbird.smartappsearch.config.Settings
import cn.jailedbird.smartappsearch.databinding.ActivityMainBinding
import cn.jailedbird.smartappsearch.dialog.AppSettingsPopWindow
import cn.jailedbird.smartappsearch.model.AppConfig
import cn.jailedbird.smartappsearch.ui.setting.SettingsActivity
import cn.jailedbird.smartappsearch.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val adapter = AppListTwoTypeAdapter()
    private val viewModel by viewModels<MainViewModel>()

    private val listener = object : AppSettingsPopWindow.Listener {
        override fun refreshApp() {
            viewModel.refreshAppDatabase()
        }

        override fun rate() {
            "rate".toast()
        }

        override fun share() {
            "share".toast()
        }

        override fun clearHistory() {
            "clearHistory".toast()
        }

        override fun settings() {
            SettingsActivity.start(this@MainActivity)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Please use NoActionBar theme
        window.requestFeature(Window.FEATURE_NO_TITLE)
        // prettify Window as Dialog style, Do this when Window is attached
        window.decorView.post {
            initWindowStyle(this)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initEvent()
        initObserver()
        /* quickDebug()*/
    }

    private fun quickDebug() {
        if (BuildConfig.DEBUG) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /**
     * TODO implement dark mode, [google dark mode doc](https://developer.android.com/develop/ui/views/theming/darktheme)
     * */
    private fun initView() {
        binding.search.requestFocus()
        binding.recyclerView.adapter = adapter
        binding.search.addTextChangedListener(onTextChanged = { text, _, _, _ ->
            viewModel.search(text.toString().trim())
        })

        initSearch(binding.search) {
            if (adapter.itemCount > 0) {
                adapter.currentList[0].launch(this@MainActivity)
                lifecycleScope.launch(Dispatchers.IO) {
                    delay(AppConfig.LAUNCH_DELAY_TIME)
                    finish()
                }
            }
        }

    }

    private fun initEvent() {
        binding.ivMore.setDebouncedClick {
            hideKeyboard()
            AppSettingsPopWindow.open(this, it, listener)
        }
    }

    private fun initObserver() {
        lifecycleScope.launch {
            viewModel.list.collectLatest {
                adapter.submitList(it)
                if (it.size == 1 && Settings[Settings.Key.LaunchDirect]) {
                    it[0].launch(this@MainActivity)
                }
            }
        }
    }

    private fun initWindowStyle(activity: Activity) {
        val window = activity.window
        val (width, height) = activity.resources.displayMetrics.let {
            Pair(it.widthPixels, it.heightPixels)
        }
        val widthPercent = 0.8f
        val heightPercent = 0.55f
        window.setLayout((width * widthPercent).toInt(), (height * heightPercent).toInt())
        activity.setFinishOnTouchOutside(true)

        window.decorView.background = GradientDrawable().apply {
            cornerRadius = 8.toPx()
            shape = GradientDrawable.RECTANGLE
            color = ColorStateList.valueOf(Color.WHITE)
        }
    }


    private fun initSearch(editText: EditText, doSearch: () -> Unit) {
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                doSearch.invoke()
                true
            } else {
                false
            }
        }
        editText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                doSearch.invoke()
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings[Settings.Key.ImeAutoPop]) {
            showKeyboard()
        } else { // Avoid ime pop due to other reason
            hideKeyboard()
        }
    }

}