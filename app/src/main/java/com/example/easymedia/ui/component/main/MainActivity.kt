package com.example.easymedia.ui.component.main

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentManager
import com.example.easymedia.R
import com.example.easymedia.base.BaseActivity
import com.example.easymedia.databinding.ActivityMainBinding
import com.example.easymedia.ui.component.addpost.AddPostFragment
import com.example.easymedia.ui.component.animation.FragmentTransactionAnimation.setSlideAnimations
import com.example.easymedia.ui.component.home.HomeFragment
import com.example.easymedia.ui.component.myprofile.MyProfileFragment
import com.example.easymedia.ui.component.search.SearchFragment

class MainActivity() : BaseActivity<ActivityMainBinding, MainViewModel>() {
    override val viewModel: MainViewModel by viewModels()
    var fragmentCurrent = "HomeFragment"
    var fragmentPre = "HomeFragment"
    var countFragment = 0

    override fun inflateBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        initHomeFragment()
        actionHome()
        actionAdd()
        actionSearch()
        actionMyProfile()
    }

    override fun initData() {
        super.initData()
    }

    fun hideBottomBar() {
        binding.bottomBar.visibility = View.INVISIBLE
    }

    fun showBottomBar() {
        binding.bottomBar.visibility = View.VISIBLE
    }
    fun clearBackStackExceptFirst() {
        fragmentCurrent = "HomeFragment"
        fragmentPre = "HomeFragment"
        countFragment = 0
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 1) {
            val first = fm.getBackStackEntryAt(0)
            fm.popBackStack(first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    fun switchScreenMyProfile() {
        if (fragmentCurrent != "MyProfileFragment") {
            if (fragmentPre != "HomeFragment") fragmentPre = fragmentCurrent
            val myProfileFragment = MyProfileFragment()
            val transactionMyProfileFragment = supportFragmentManager.beginTransaction()
            transactionMyProfileFragment.setSlideAnimations()
            transactionMyProfileFragment.add(R.id.fragment, myProfileFragment)
            transactionMyProfileFragment.addToBackStack(null)
            transactionMyProfileFragment.commit()
        }
    }

    private fun actionAdd() {
        binding.btnAdd.setOnClickListener {
            if (fragmentCurrent != "AddPostFragment") {
                if (fragmentPre != "HomeFragment") fragmentPre = fragmentCurrent
                // ẩn đi BottomBar
                val addPostFragment = AddPostFragment()
                val transactionAddPostFragment = supportFragmentManager.beginTransaction()
                transactionAddPostFragment.setSlideAnimations()
                transactionAddPostFragment.add(R.id.fragment, addPostFragment)
                transactionAddPostFragment.addToBackStack(null)
                transactionAddPostFragment.commit()
                hideBottomBar()
            }
        }
    }

    private fun actionSearch() {
        binding.btnSearch.setOnClickListener {
            if (fragmentCurrent != "SearchFragment") {
                if (fragmentPre != "HomeFragment") fragmentPre = fragmentCurrent
                val searchFragment = SearchFragment()
                val transactionMyProfileFragment = supportFragmentManager.beginTransaction()
                transactionMyProfileFragment.setSlideAnimations()
                transactionMyProfileFragment.add(R.id.fragment, searchFragment)
                transactionMyProfileFragment.addToBackStack(null)
                transactionMyProfileFragment.commit()
            }
        }

    }

    private fun actionHome() {
        binding.btnHome.setOnClickListener {
            if (fragmentCurrent == "AddPostFragment" || fragmentCurrent == "MyProfileFragment" || fragmentCurrent == "SearchFragment") {
                for (i in 0 until countFragment) {
                    supportFragmentManager.popBackStack()
                }
                fragmentCurrent = "HomeFragment"
                fragmentPre = "HomeFragment"
                countFragment = 0
            } else {
                val homeFragment = HomeFragment()
                val transactionHomeFragment = supportFragmentManager.beginTransaction()
                transactionHomeFragment.replace(R.id.fragment, homeFragment)
                transactionHomeFragment.commit()
                fragmentCurrent = "HomeFragment"
                fragmentPre = "HomeFragment"
                countFragment = 0
            }
        }
    }

    private fun actionMyProfile() {
        binding.btnMyProfile.setOnClickListener {
            switchScreenMyProfile()
        }
    }

    private fun initHomeFragment() {
        val homeFragment = HomeFragment()
        val transactionHomeFragment = supportFragmentManager.beginTransaction()
        transactionHomeFragment.add(R.id.fragment, homeFragment)
        transactionHomeFragment.commit()
    }
}
