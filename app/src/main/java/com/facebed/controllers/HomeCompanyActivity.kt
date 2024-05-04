package com.facebed.controllers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.facebed.R
import com.facebed.databinding.HomeCompanyActivityBinding
import com.facebed.fragments.EventsCompanyFragment
import com.facebed.fragments.HomeCompanyFragment
import com.facebed.fragments.ProfileCompanyFragment
import com.google.android.material.navigation.NavigationBarView

class HomeCompanyActivity : AppCompatActivity() {
    private lateinit var homeCompanyActivityBinding: HomeCompanyActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_company_activity)

        homeCompanyActivityBinding = HomeCompanyActivityBinding.inflate(layoutInflater)
        setContentView(homeCompanyActivityBinding.root)
        homeCompanyActivityBinding.bottomNavigationCompany.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_UNLABELED
        replaceFragment(HomeCompanyFragment())

        homeCompanyActivityBinding.bottomNavigationCompany.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> replaceFragment(HomeCompanyFragment())
                R.id.navigation_events -> replaceFragment(EventsCompanyFragment())
                R.id.navigation_profile -> replaceFragment(ProfileCompanyFragment())
                else -> {}
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        fragmentTransaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}