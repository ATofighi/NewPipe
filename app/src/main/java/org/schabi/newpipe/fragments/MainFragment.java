package org.schabi.newpipe.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import org.schabi.newpipe.BaseFragment;
import org.schabi.newpipe.MainActivity;
import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.fragments.list.channel.ChannelFragment;
import org.schabi.newpipe.local.feed.FeedFragment;
import org.schabi.newpipe.fragments.list.kiosk.KioskFragment;
import org.schabi.newpipe.local.bookmark.BookmarkFragment;
import org.schabi.newpipe.local.history.StatisticsPlaylistFragment;
import org.schabi.newpipe.local.subscription.SubscriptionFragment;
import org.schabi.newpipe.report.ErrorActivity;
import org.schabi.newpipe.report.UserAction;
import org.schabi.newpipe.util.KioskTranslator;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.ServiceHelper;
import org.schabi.newpipe.util.ThemeHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.schabi.newpipe.util.NavigationHelper.MAIN_FRAGMENT_TAG;

public class MainFragment extends BaseFragment implements TabLayout.OnTabSelectedListener {

    public int currentServiceId = -1;
    private ViewPager viewPager;
    private List<String> tabs = new ArrayList<>();
    static PagerAdapter adapter;
    TabLayout tabLayout;
    private SharedPreferences prefs;
    private Bundle savedInstanceStateBundle;

    SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> {
        if(key.equals("saveUsedTabs")||key.equals("service")) {
            mainPageChanged();
        }
    };


    /*//////////////////////////////////////////////////////////////////////////
    // Constants
    //////////////////////////////////////////////////////////////////////////*/

    private static final int FALLBACK_SERVICE_ID = ServiceList.YouTube.getServiceId();
    private static final String FALLBACK_CHANNEL_URL = "https://www.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ";
    private static final String FALLBACK_CHANNEL_NAME = "Music";
    private static final String FALLBACK_KIOSK_ID = "Trending";
    private static final int KIOSK_MENU_OFFSET = 2000;

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        savedInstanceStateBundle = savedInstanceState;
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        currentServiceId = ServiceHelper.getSelectedServiceId(activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(listener);
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        getTabOrder();

        tabLayout = rootView.findViewById(R.id.main_tab_layout);
        viewPager = rootView.findViewById(R.id.pager);

        /*  Nested fragment, use child fragment here to maintain backstack in view pager. */
        adapter = new PagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(adapter.getCount());

        tabLayout.setupWithViewPager(viewPager);
    }

    private void getTabOrder() {
        tabs.clear();

        String save = prefs.getString("saveUsedTabs", "1\n2\n4\n");
        String tabsArray[] = save.trim().split("\n");

        KioskList kl = null;

        try {
            StreamingService service = NewPipe.getService(currentServiceId);
            kl = service.getKioskList();
        } catch (Exception e) {
            ErrorActivity.reportError(activity, e,
                    activity.getClass(),
                    null,
                    ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                            "none", "", R.string.app_ui_crash));
        }

        for(String tabNumber:tabsArray) {
            if(tabNumber.equals("1")) {
                if (kl != null) {
                    for(String ks : kl.getAvailableKiosks()) {
                        tabs.add(tabNumber+"\t"+ks);
                    }
                }
            } else {
                tabs.add(tabNumber);
            }
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu() called with: menu = [" + menu + "], inflater = [" + inflater + "]");
        inflater.inflate(R.menu.main_fragment_menu, menu);

        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                try {
                    NavigationHelper.openSearchFragment(
                            getFragmentManager(),
                            ServiceHelper.getSelectedServiceId(activity),
                            "");
                } catch (Exception e) {
                    ErrorActivity.reportUiError((AppCompatActivity) getActivity(), e);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Tabs
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    public void mainPageChanged() {
        getTabOrder();
        adapter.notifyDataSetChanged();
    }

    private class PagerAdapter extends FragmentPagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            String tabNumber = tabs.get(position);

            if(tabNumber.startsWith("1\t")) {
                String kiosk[] = tabNumber.split("\t");
                if(kiosk.length==2) {
                    KioskFragment fragment = null;
                    try {
                        tabLayout.getTabAt(position).setIcon(KioskTranslator.getKioskIcons(kiosk[1], getContext()));

                        fragment = KioskFragment.getInstance(currentServiceId, kiosk[1]);
                        fragment.useAsFrontPage(true);
                        return fragment;
                    } catch (Exception e) {
                        ErrorActivity.reportError(activity, e,
                                activity.getClass(),
                                null,
                                ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                                        "none", "", R.string.app_ui_crash));
                    }
                }
            } else if(tabNumber.startsWith("6\t")) {
                String channelInfo[] = tabNumber.split("\t");
                if(channelInfo.length==4) {
                    tabLayout.getTabAt(position).setIcon(R.drawable.ic_channel_white_24dp);

                    ChannelFragment fragment = ChannelFragment.getInstance(Integer.parseInt(channelInfo[3]), channelInfo[1], channelInfo[2]);
                    fragment.useAsFrontPage(true);
                    return fragment;
                } else {
                    return new BlankFragment();
                }
            } else {
                    switch (tabNumber) {
                        case "0":
                            tabLayout.getTabAt(position).setIcon(R.drawable.ic_whatshot_white_24dp);

                            return new BlankFragment();
                        case "2":
                            tabLayout.getTabAt(position).setIcon(R.drawable.ic_channel_white_24dp);

                            SubscriptionFragment sfragment = new SubscriptionFragment();
                            sfragment.useAsFrontPage(true);
                            return sfragment;
                        case "3":
                            tabLayout.getTabAt(position).setIcon(R.drawable.ic_rss_feed_white_24dp);

                            FeedFragment ffragment = new FeedFragment();
                            ffragment.useAsFrontPage(true);
                            return ffragment;
                        case "4":
                            tabLayout.getTabAt(position).setIcon(R.drawable.ic_bookmark_white_24dp);

                            BookmarkFragment bFragment = new BookmarkFragment();
                            bFragment.useAsFrontPage(true);
                            return bFragment;
                        case "5":
                            tabLayout.getTabAt(position).setIcon(R.drawable.ic_history_white_24dp);

                            StatisticsPlaylistFragment cFragment = new StatisticsPlaylistFragment();
                            cFragment.useAsFrontPage(true);
                            return cFragment;
                    }
                }

            return new BlankFragment();
            }

        @Override
        public int getItemPosition(Object object) {
            // Causes adapter to reload all Fragments when
            // notifyDataSetChanged is called
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return tabs.size();
        }
    }
}
