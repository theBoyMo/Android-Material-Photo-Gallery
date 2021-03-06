package com.example.materialphotogallery.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.materialphotogallery.R;
import com.example.materialphotogallery.common.Constants;
import com.example.materialphotogallery.common.Utils;
import com.example.materialphotogallery.custom.CustomDialogFragment;
import com.example.materialphotogallery.model.PhotoItem;
import com.example.materialphotogallery.ui.fragment.AboutFragment;
import com.example.materialphotogallery.ui.fragment.FavouriteFragment;
import com.example.materialphotogallery.ui.fragment.HomeFragment;
import com.example.materialphotogallery.ui.fragment.ModelFragment;
import com.example.materialphotogallery.ui.fragment.PhotoMapFragment;
import com.example.materialphotogallery.ui.fragment.PlaceholderMapFragment;
import com.example.materialphotogallery.ui.fragment.SettingsFragment;
import com.example.materialphotogallery.ui.fragment.SlideShowFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 *  References:
 *  [1] https://guides.codepath.com/android/Fragment-Navigation-Drawer
 *  [2] http://stackoverflow.com/questions/13472258/handling-actionbar-title-with-the-fragment-back-stack
 *  [3] http://stackoverflow.com/questions/17107005/how-to-clear-fragment-backstack-in-android
 */
public class MainActivity extends AppCompatActivity implements
        HomeFragment.Contract,
        FavouriteFragment.Contract,
        PhotoMapFragment.Contract{


    // Contract methods
    // - home fragment
    @Override
    public void onHomeItemClick(List<PhotoItem> list, int position) {
        // launch SlideShowFragment
        SlideShowFragment fragment = SlideShowFragment.newInstance(list, position);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        fragment.show(ft, "slide_show");
    }

    // - map fragment
    @Override
    public void onMarkerClick(Marker marker, int totalCount) {
        // set marker image, title and animate popup into view
        populatePopup(marker, totalCount);
        if (!mIsPopupVisible) {
            mIsPopupVisible = true;
            animatePopupIntoView(mMarkerPopup);
        }
    }

    @Override
    public void onMapClick() {
        // animate popup out of view
        hidePopup();

    }
    // END

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
    }

    private static final String[] PERMISSIONS_REQUIRED = {
          WRITE_EXTERNAL_STORAGE
    };
    private static final String IS_FIRST_TIME_IN = "is_first_time_in";
    private static final String PERMISSIONS_TRACK_STATE = "permissions_track_state";
    private static final String MODEL_FRAGMENT = "model_fragment";
    private static final String HOME_FRAGMENT = "home_fragment";
    private static final String FAVOURITE_FRAGMENT = "favourite_fragment";
    private static final String PHOTO_MAP_FRAGMENT = "photo_map_fragment";
    private static final String SETTINGS_FRAGMENT = "settings_fragment";
    private static final String ABOUT_FRAGMENT = "about_fragment";

    private static final String CURRENT_PAGE_TITLE = "current_page_title";
    private static final String FULL_SIZE_PHOTO_PATH = "full_size_photo_path";
    private static final String FAB_VISIBILITY = "fab_visibility";


    private static final int PHOTO_REQUEST_CODE = 100;
    private static final int PERMISSIONS_REQUEST_CODE = 101;

    private SharedPreferences mPrefs;
    private boolean mIsInPermission = false;
    private CoordinatorLayout mLayout;
    private DrawerLayout mDrawer;
    private NavigationView mNavigationView;
    private String mCurrentTitle;
    private String mFullSizePhotoPath;
    private FloatingActionButton mFab;
    private boolean mIsFabVisible = true;
    private View mMarkerPopup;
    private boolean mIsPopupVisible;
    private ImageView mPopupThumb;
    private ImageView mPopupDirections;
    private TextView mPopupCount;
    private TextView mPopupTitle;
    private LatLng mMarkerLatLng;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        initToolbar();
        initFab();
        initPopup();
        setupDrawerLayout();

        // set the initial fragment & title on startup
        if (savedInstanceState == null) {
            displayInitialFragment();
        } else {
            mIsFabVisible = savedInstanceState.getBoolean(FAB_VISIBILITY);
            if (!mIsFabVisible) {
                mFab.setVisibility(View.GONE);
            }
            mFullSizePhotoPath = savedInstanceState.getString(FULL_SIZE_PHOTO_PATH);
            mIsInPermission = savedInstanceState.getBoolean(PERMISSIONS_TRACK_STATE);
            mCurrentTitle = savedInstanceState.getString(CURRENT_PAGE_TITLE);
            setTitle(mCurrentTitle);
        }

        // cache a reference to the model fragment
        Fragment modelFragment = getSupportFragmentManager().findFragmentByTag(MODEL_FRAGMENT);
        if (modelFragment == null) {
            modelFragment = ModelFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(modelFragment, MODEL_FRAGMENT)
                    .commit();
        }
        // request the req'd permissions when the app first launches and permission has not been given
        if (iFirstTimeIn() && !mIsInPermission) {
            mIsInPermission = true;
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE);
        }

    }


    @Override
    public void onEnterAnimationComplete() {
        // ONLY CALLED IN API21+
        super.onEnterAnimationComplete();
        Timber.i("%s: onEnterAnimation() called", Constants.LOG_TAG);
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> list = fm.getFragments();
        if (list.size() == 1) { // home fragment is top of stack/visible
            HomeFragment fragment = (HomeFragment) fm.findFragmentByTag(HOME_FRAGMENT);
            if (fragment != null) {
                fragment.onEnterAnimationComplete();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                Utils.showToast(this, "Clicked about");
                return true;
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_PAGE_TITLE, mCurrentTitle);
        outState.putString(FULL_SIZE_PHOTO_PATH, mFullSizePhotoPath);
        outState.putBoolean(PERMISSIONS_TRACK_STATE, mIsInPermission);
        outState.putBoolean(FAB_VISIBILITY, mIsFabVisible);
    }

    @Override
    public void onBackPressed() {
        // hide marker popup if visible
        hidePopup();

        // update the page title
        FragmentManager fm = getSupportFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count <= 1) {
            finish();
        } else{
            mCurrentTitle = fm.getBackStackEntryAt(count - 2).getName();
        }
        super.onBackPressed();
        setTitle(mCurrentTitle);

        // update nav drawer selection
        switch (mCurrentTitle) {
            case "Photo Gallery":
                mNavigationView.setCheckedItem(R.id.drawer_home);
                mFab.setVisibility(View.VISIBLE);
                mIsFabVisible = true;
                break;
            case "Favourites":
                mNavigationView.setCheckedItem(R.id.drawer_favourite);
                break;
            case "Map":
                mNavigationView.setCheckedItem(R.id.drawer_map);
                break;
            case "Settings":
                mNavigationView.setCheckedItem(R.id.drawer_settings);
                break;
            case "About":
                mNavigationView.setCheckedItem(R.id.drawer_about);
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PHOTO_REQUEST_CODE) {
                // capture title via dialog
                FragmentManager fm = getSupportFragmentManager();
                CustomDialogFragment dialog = CustomDialogFragment.newInstance(mFullSizePhotoPath);
                dialog.show(fm, "dialog_fragment");
            }
        } else if (resultCode == RESULT_CANCELED){
            Utils.showSnackbar(mLayout, "Operation cancelled by user");
        } else {
            Utils.showSnackbar(mLayout, "Error executing operation");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // method called back as a result of requestPermissions() being called
        boolean permissionNotGiven = false;
        mIsInPermission = false;

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (canWriteToExternalStorage()) {
                launchCameraApp();
            } else if (!shouldShowWriteToStorageRational()) {
                permissionNotGiven = true;
            }
        }

        if (permissionNotGiven) {
            Utils.showSnackbar(mLayout, "Use of the camera requires accepting the requested permission");
        }
    }

    // initialize various layout elements
    private void setupDrawerLayout() {
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                selectDrawerItem(item);
                return true;
            }
        });
    }

    private void selectDrawerItem(MenuItem item) {
        // hide marker popup if visible
        hidePopup();

        // select the fragment to instantiate based on the item clicked
        Fragment fragment = null;
        String tag;
        Class fragmentClass;
        switch (item.getItemId()) {
            case R.id.drawer_home:
                fragmentClass = HomeFragment.class;
                tag = HOME_FRAGMENT;
                break;
            case R.id.drawer_favourite:
                fragmentClass = FavouriteFragment.class;
                tag = FAVOURITE_FRAGMENT;
                break;
            case R.id.drawer_map:
                if (Utils.servicesAvailable(this, mLayout) &&
                        Utils.getVersionFromPackageManager(this) >= 2) {
                    fragmentClass = PhotoMapFragment.class;
                } else {
                    fragmentClass = PlaceholderMapFragment.class;
                }
                tag = PHOTO_MAP_FRAGMENT;
                break;
            case R.id.drawer_settings:
                fragmentClass = SettingsFragment.class;
                tag = SETTINGS_FRAGMENT;
                break;
            case R.id.drawer_about:
                fragmentClass = AboutFragment.class;
                tag = ABOUT_FRAGMENT;
                break;
            default:
                fragmentClass = HomeFragment.class;
                tag = HOME_FRAGMENT;
        }
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            Timber.e("%s: Could not instantiate %s fragment, %s",
                    Constants.LOG_TAG, fragmentClass.getName(), e.getMessage());
        }

        // highlight the selected item and update page title
        item.setChecked(true);
        switch (item.getTitle().toString()) {
            case "Photo Gallery":
                mCurrentTitle = getString(R.string.menu_title_home);
                break;
            case "Favourites":
                mCurrentTitle = getString(R.string.menu_title_favourite);
                break;
            case "Map":
                mCurrentTitle = getString(R.string.menu_title_map);
                break;
            case "Settings":
                mCurrentTitle = getString(R.string.menu_title_settings);
                break;
            case "About":
                mCurrentTitle = getString(R.string.menu_title_about);
                break;
            default:
                mCurrentTitle = getString(R.string.menu_title_home);
        }

        // clear the back stack if adding home fragment again
        FragmentManager fm = getSupportFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count > 1 && fragmentClass == HomeFragment.class) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        // replacing the existing fragment
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(mCurrentTitle)
                .commit();

        // hide fab
        if (fragmentClass != HomeFragment.class) {
            mFab.setVisibility(View.GONE);
            mIsFabVisible = false;
        }
        else {
            mFab.setVisibility(View.VISIBLE);
            mIsFabVisible = true;
        }

        setTitle(mCurrentTitle);
        mDrawer.closeDrawers();
    }

    private void initFab() {
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Utils.hasCamera(MainActivity.this)) {
                        takePicture();
                    } else {
                        Utils.showSnackbar(mLayout, "Your device lacks a camera");
                    }
                }
            });
        }
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionbar = getSupportActionBar();
            if (actionbar != null) {
                actionbar.setHomeAsUpIndicator(R.drawable.ic_drawer);
                actionbar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void initPopup() {
        mMarkerPopup = findViewById(R.id.marker_popup);
        mPopupThumb = (ImageView) findViewById(R.id.popup_thumbnail);
        mPopupCount = (TextView) findViewById(R.id.popup_count);
        mPopupTitle = (TextView) findViewById(R.id.popup_title);
        mPopupDirections = (ImageView) findViewById(R.id.popup_directions);

        mMarkerPopup.animate().translationY(300.0f).alpha(0.0f);
        mIsPopupVisible = false;
        // launch google maps to provide the user with directions to the provided coordinates
        mPopupDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMarkerLatLng != null) {
                    Uri uri = Uri.parse("google.navigation:q="
                            + mMarkerLatLng.latitude + "," + mMarkerLatLng.longitude);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            }
        });
    }

    private void displayInitialFragment() {
        mCurrentTitle = getString(R.string.menu_title_home);
        setTitle(mCurrentTitle);
        HomeFragment fragment = HomeFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment, HOME_FRAGMENT)
                .addToBackStack(mCurrentTitle)
                .commit();
    }

    // camera and api 24 permissions
    private boolean iFirstTimeIn() {
        // if no result saved, first time app launched
        boolean firstTimeIn = mPrefs.getBoolean(IS_FIRST_TIME_IN, true);
        if (firstTimeIn) {
            mPrefs.edit().putBoolean(IS_FIRST_TIME_IN, false).apply();
        }
        return firstTimeIn;
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean canWriteToExternalStorage() {
        // check if the req'd permission is held
        return hasPermission(WRITE_EXTERNAL_STORAGE);
    }

    private boolean shouldShowWriteToStorageRational() {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE);
    }

    private void takePicture() {
        if (canWriteToExternalStorage()) {
            // permission given, take the picture
            launchCameraApp();
        }
        else if (!shouldShowWriteToStorageRational()) {
            // permission req'd previously and denied, inform user permission req'd, allowing them
            // the option via a snackbar to provide permission via Device Settings page for app
            showRationalMessage(getString(R.string.snackbar_action_text));
        }
        else {
            // otherwise request permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE);
        }
    }

    private void launchCameraApp() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri filePathUri = Utils.generatePhotoFileUri(this);
        if (filePathUri != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, filePathUri);
            if (Utils.isCameraAppInstalled(this, intent)) {
                startActivityForResult(intent, PHOTO_REQUEST_CODE);
            } else {
                Utils.showSnackbar(mLayout, "No app found suitable to capture photos");
            }
            mFullSizePhotoPath = generateFilePath(filePathUri);
        }
    }

    private void showRationalMessage(String message) {
        //  add action to the snackbar allowing user to amend permissions in app's settings
        Snackbar snackbar = Snackbar
                .make(mLayout, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_action_title, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // launch app settings screen
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                });

        snackbar.show();
    }

    private String generateFilePath(Uri uriPath) {
        String pattern = "/storage";
        int position = uriPath.toString().indexOf(pattern);
        return uriPath.toString().substring(position);
    }

    // map/marker helper methods
    private void animatePopupIntoView(View view) {
        view.animate()
                .translationY(0.0f)
                .alpha(1.0f);
    }

    private void animatePopupOutOfView(View view) {
        view.animate()
                .translationY(300.0f)
                .setDuration(1200)
                .alpha(0.0f);
    }

    private void populatePopup(Marker marker, int totalCount) {
        mPopupCount.setText(String.format(Locale.ENGLISH, "Image %d/%d", Math.round(marker.getZIndex()), totalCount));
        mPopupTitle.setText(marker.getTitle());
        Utils.loadThumbnailWithGlide(this, marker.getSnippet(), mPopupThumb);
        mMarkerLatLng =  marker.getPosition();
    }

    private void hidePopup() {
        if (mIsPopupVisible) {
            mIsPopupVisible = false;
            animatePopupOutOfView(mMarkerPopup);
        }
    }

}
