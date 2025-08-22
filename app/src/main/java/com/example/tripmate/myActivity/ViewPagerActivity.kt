package com.example.tripmate.myActivity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.tripmate.R
import com.example.tripmate.myAdapter.ViewPagerAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ViewPagerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_pager)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)

        val items = listOf(
            OnboardingItem(
                R.drawable.firefly_goa_beach,
                "Unwind by the Sea",
                "Feel the golden sand under your feet and watch the sun set over the Arabian Sea in Goa."
            ),
            OnboardingItem(
                R.drawable.firefly_mountain_snow,
                "Awaken to the Himalayas",
                "Breathe in the crisp mountain air as the first rays of sunlight kiss the majestic snow peaks."
            ),
            OnboardingItem(
                R.drawable.firefly_varanasi_ghat,
                "Witness Timeless Traditions",
                "Experience the spiritual heart of India, where prayers meet the Ganges at the sacred Varanasi Ghats."
            )
        )

        val adapter = ViewPagerAdapter(items)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        btnNext.setOnClickListener {
            if (viewPager.currentItem < items.size - 1) {
                viewPager.currentItem += 1
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
