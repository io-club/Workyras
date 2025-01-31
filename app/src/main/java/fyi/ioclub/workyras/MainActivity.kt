/*
 * Copyright (C) 2025 IO Club
 *
 * This file is part of Workyras.
 *
 * Workyras is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Workyras is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with Workyras.  If not, see <https://www.gnu.org/licenses/>.
 */

package fyi.ioclub.workyras

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHost
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import fyi.ioclub.workyras.data.db.MainDatabase
import fyi.ioclub.workyras.databinding.ActivityMainBinding
import fyi.ioclub.workyras.ui.analysis.AnalysisViewModel
import fyi.ioclub.workyras.ui.common.StringResource
import fyi.ioclub.workyras.ui.common.Timestamp
import fyi.ioclub.workyras.ui.worktagbin.WorkTagBinViewModel
import fyi.ioclub.workyras.ui.worktags.WorkTagsViewModel
import fyi.ioclub.workyras.utils.runQuietly
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val binding get() = _binding
    private lateinit var _binding: ActivityMainBinding

    private val navController get() = _navController
    private lateinit var _navController: NavController

    private val bottomNavMore get() = _bottomNavMore
    private lateinit var _bottomNavMore: MenuItem

    private var isBottomNavViewItemSelectSilent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        for (it in arrayOf(
            MainDatabase,
            StringResource,
            Timestamp,
        )) it.tryLateInit { applicationContext }

        _binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
            _navController =
                (supportFragmentManager.findFragmentById(navHostFragmentActivityMain.id) as NavHost).navController
        }

        setUpNavDrawer()
        setUpBottomNav()

        setUpViewModels()
    }

    private fun setUpViewModels() {
        viewModels<AnalysisViewModel>().value

        val lazyWorkTagsViewModel = viewModels<WorkTagsViewModel>()
        val lazyWorkTagBinViewModel = viewModels<WorkTagBinViewModel>()
        for ((lvm1, lvm2) in arrayOf(
            lazyWorkTagsViewModel to lazyWorkTagBinViewModel,
            lazyWorkTagBinViewModel to lazyWorkTagsViewModel,
        )) lvm1.value.tryLateInit { lvm2 }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpNavDrawer() = with(binding) {

        drawerLayout.addDrawerListener(object : SimpleDrawerListener() {

            @IdRes
            private var lastDestinationIdInBottomNavView: Int = -1

            private var isSliding = false

            override fun onDrawerOpened(drawerView: View) {
                navController.currentDestination?.id?.let {
                    if (it in bottomNavView) lastDestinationIdInBottomNavView = it
                }
                with(bottomNavView) {
                    silentSelectItem(R.id.bottom_nav_more)
                    resetBottomNavMoreTitle()
                }
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val isSlidedOut = slideOffset > 0
                if (isSlidedOut == isSliding) return
                isSliding = isSlidedOut
                drawerView.let { if (isSlidedOut) onDrawerOpened(it) else onDrawerClosed(it) }
            }

            override fun onDrawerClosed(drawerView: View) {
                navController.currentDestination?.id?.let {
                    if (it in bottomNavView) bottomNavView.silentSelectItem(it)
                    else updateBottomNavMoreTitle()
                }
            }
        })
        // Setup the gesture detector for main screen area
        drawerLayout.setUpOnGestureListener()
        GestureDetector(this@MainActivity, MainOnGestureListener).let {
            drawerLayout.setOnTouchListener { _, ev ->
                it.onTouchEvent(ev)
                false
            }
        }

        // Set up the navigation view
        with(navViewDrawer) {
            setupWithNavController(navController)
            setNavigationItemSelectedListener { item ->
                navController.navigate(item.itemId)
                drawerLayout.closeDrawer(DRAWER_LAYOUT_GRAVITY)
                true
            }
        }
    }

    private fun BottomNavigationView.silentSelectItem(itemId: Int) =
        runQuietly(::isBottomNavViewItemSelectSilent::set) { selectedItemId = itemId }

    object MainOnGestureListener : GestureDetector.SimpleOnGestureListener() {

        var onSwipeRight: (() -> Unit)? = null
        var onSwipeLeft: (() -> Unit)? = null
        private var onSwipeDown: (() -> Unit)? = null
        private var onSwipeUp: (() -> Unit)? = null

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            e1 ?: return super.onFling(null, e2, velocityX, velocityY)
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (abs(diffX) > abs(diffY)) {
                // Horizontal swipe
                if (diffX > 0) onSwipeRight else onSwipeLeft
            } else {
                // Vertical swipe (up/down)
                if (diffY > 0) onSwipeDown else onSwipeUp
            }?.invoke()
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        @SuppressLint("ClickableViewAccessibility")
        fun enableGestureOnView(context: Context, view: View) {
            GestureDetector(context, this).let {
                view.setOnTouchListener { _, ev ->
                    it.onTouchEvent(ev)
                    false
                }
            }
        }
    }

    private fun setUpBottomNav() = with(binding) {
        with(bottomNavView) {
            _bottomNavMore = menu.findItem(R.id.bottom_nav_more)

            setupWithNavController(navController)

            var isToSetPadding = true
            viewTreeObserver.addOnPreDrawListener {
                if (isToSetPadding) {
                    with(navHostFragmentActivityMain) {
                        setPadding(
                            paddingLeft,
                            paddingTop,
                            paddingRight,
                            bottomNavView.height,
                        )
                    }
                    isToSetPadding = false
                }
                true
            }
            setOnItemSelectedListener { item ->
                if (isBottomNavViewItemSelectSilent) return@setOnItemSelectedListener true
                val itemId = item.itemId
                !(itemId == R.id.bottom_nav_more).also {
                    if (it) drawerLayout.openDrawer(DRAWER_LAYOUT_GRAVITY)
                    else with(navController) {
                        if (currentDestination?.id != itemId) navigate(itemId)
                    }
                }
            }
        }
    }

    fun resumeNavDrawerFragment() = with(binding.bottomNavView) {
        silentSelectItem(R.id.bottom_nav_more)
        updateBottomNavMoreTitle()
    }

    private fun resetBottomNavMoreTitle() = bottomNavMore.setTitle(R.string.bottom_nav_more_title)
    private fun updateBottomNavMoreTitle() =
        navController.currentDestination
            ?.id.let(fragmentInNavDrawerToTitleMap::get)
            ?.let(bottomNavMore::setTitle)

    companion object {

        private val fragmentsInBottomNavView = setOf(
            R.id.bottom_nav_work,
            R.id.bottom_nav_analysis,
            R.id.bottom_nav_work_tags,
        )

        operator fun BottomNavigationView.contains(obj: Any?) =
            (if (obj is NavDestination) obj.id else obj) in fragmentsInBottomNavView

        private val fragmentInNavDrawerToTitleMap = mapOf(
            R.id.nav_drawer_work_tag_bin to R.string.nav_drawer_work_tag_bin_title,
            R.id.nav_drawer_save to R.string.nav_drawer_save_title,
            R.id.nav_drawer_settings to R.string.nav_drawer_settings_title,
            R.id.nav_drawer_about to R.string.nav_drawer_about_title,
        )

        const val DRAWER_LAYOUT_GRAVITY = GravityCompat.START

        fun DrawerLayout.setUpOnGestureListener() {
            MainOnGestureListener.onSwipeRight = { openDrawer(DRAWER_LAYOUT_GRAVITY) }
            MainOnGestureListener.onSwipeLeft = { closeDrawer(DRAWER_LAYOUT_GRAVITY) }
        }
    }
}

fun Fragment.enableGestureOnView(view: View) =
    MainActivity.MainOnGestureListener.enableGestureOnView(requireContext(), view)
