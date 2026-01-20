package com.example.safeqr;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;


public class DashboardActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setItemIconTintList(null);
        bottomNav.setItemTextColor(null);

        if (savedInstanceState == null) {
            loadFragment(new ScanFragment());
            bottomNav.setSelectedItemId(R.id.nav_scan);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;

            int id = item.getItemId();
            if (id == R.id.nav_history) selected = new HistoryFragment();
            else if (id == R.id.nav_scan) selected = new ScanFragment();
            else if (id == R.id.nav_profile) selected = new ProfileFragment();
            else if (id == R.id.nav_help) selected = new HelpFragment();

            return loadFragment(selected);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment == null) return false;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        return true;
    }
}
