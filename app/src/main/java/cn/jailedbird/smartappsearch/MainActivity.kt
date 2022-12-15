package cn.jailedbird.smartappsearch

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Window
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import cn.jailedbird.smartappsearch.adapter.AppListAdapter
import cn.jailedbird.smartappsearch.data.AppDao
import cn.jailedbird.smartappsearch.databinding.ActivityMainBinding
import cn.jailedbird.smartappsearch.dialog.AppSettingsPopWindow
import cn.jailedbird.smartappsearch.model.ConfigModel
import cn.jailedbird.smartappsearch.utils.setDebouncedClick
import cn.jailedbird.smartappsearch.utils.showKeyboard
import cn.jailedbird.smartappsearch.utils.toPx
import cn.jailedbird.smartappsearch.utils.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val config = ConfigModel()

    @Inject
    lateinit var appDao: AppDao
    private val adapter = AppListAdapter()

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

    /** Dynamic broadcast for apk install and uninstall
     * [StackOverflow](https://stackoverflow.com/questions/7470314/receiving-package-install-and-uninstall-events)*/
    private val apkChangeListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refreshAppDatabase()
        }
    }

    private val apkChangeIntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        addDataScheme("package")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(apkChangeListener, apkChangeIntentFilter)
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
    }

    private fun initEvent() {
        binding.ivMore.setDebouncedClick {
            AppSettingsPopWindow.open(this, it, listener)
        }
    }

    private fun initObserver() {
        lifecycleScope.launch {
            viewModel.list.collectLatest {
                adapter.submitList(it)
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
        if (config.popImeWhenStart) {
            activity.showKeyboard()
        }
        window.decorView.background = GradientDrawable().apply {
            cornerRadius = 8.toPx()
            shape = GradientDrawable.RECTANGLE
            color = ColorStateList.valueOf(Color.WHITE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(apkChangeListener)
    }
}