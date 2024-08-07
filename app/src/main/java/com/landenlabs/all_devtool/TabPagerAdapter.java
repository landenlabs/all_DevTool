/*
 * Copyright (c) 2020 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Dennis Lang
 * @see http://LanDenLabs.com/
 */

package com.landenlabs.all_devtool;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.landenlabs.all_devtool.shortcuts.util.LLog;
import com.landenlabs.all_devtool.shortcuts.util.Utils;

import java.util.List;

/**
 * Tab page adapter manages page UI fragments.
 * <p/>
 * <ul>
 * <li> Available tab pages
 * <li> Page names
 * <li> Helper to share page (grab screen shot)
 * </ul>
 *
 * @author Dennis Lang
 */
@SuppressWarnings({"UnnecessaryLocalVariable"})
public class TabPagerAdapter extends FragmentPagerAdapter implements ActionBar.TabListener {

    // Logger - set to LLog.DBG to only log in Debug build, use LLog.On for always log.
    private final LLog m_log = LLog.DBG;

    private static final int SHARE_MAX_IMAGE_HEIGHT = 3000;
    private ViewPager m_viewPager;
    private ActionBar m_actionBar;

    public interface Creator {
        DevFragment creator();

        String name();
    }

    private Creator[] m_tabList = new Creator[]{
            // Build
            new Creator() {
                public DevFragment creator() {
                    return BuildFragment.create();
                }

                public String name() {
                    return BuildFragment.s_name;
                }
            },

            // Properties
            new Creator() {
                public DevFragment creator() {
                    return PropFragment.create();
                }

                public String name() {
                    return PropFragment.s_name;
                }
            },

            // Proc
            new Creator() {
                public DevFragment creator() {
                    return ProcFragment.create();
                }

                public String name() {
                    return ProcFragment.s_name;
                }
            },


            // Disk
            new Creator() {
                public DevFragment creator() {
                    return DiskFragment.create();
                }

                public String name() {
                    return DiskFragment.s_name;
                }
            },

            // Netstat
            new Creator() {
                public DevFragment creator() {
                    return NetstatFragment.create();
                }

                public String name() {
                    return NetstatFragment.s_name;
                }
            },

            // Network
            new Creator() {
                public DevFragment creator() {
                    return NetFragment.create();
                }

                public String name() {
                    return NetFragment.s_name;
                }
            },



            // System
            new Creator() {
                public DevFragment creator() {
                    return SystemFragment.create();
                }

                public String name() {
                    return SystemFragment.s_name;
                }
            },

            // Package
            new Creator() {
                public DevFragment creator() {
                    return PackageFragment.create();
                }

                public String name() {
                    return PackageFragment.s_name;
                }
            },

            // File Browser
            new Creator() {
                public DevFragment creator() {
                    return FileBrowserFragment.create();
                }

                public String name() {
                    return FileBrowserFragment.s_name;
                }
            },

            // Console
            new Creator() {
                public DevFragment creator() {
                    return ConsoleFragment.create();
                }

                public String name() {
                    return ConsoleFragment.s_name;
                }
            },

            // Sensor
            new Creator() {
                public DevFragment creator() {
                    return SensorFragment.create();
                }

                public String name() {
                    return SensorFragment.s_name;
                }
            },

            // Theme
            new Creator() {
                public DevFragment creator() {
                    return ThemeFragment.create();
                }

                public String name() {
                    return ThemeFragment.s_name;
                }
            },

            // Light
            new Creator() {
                public DevFragment creator() {
                    return LightFragment.create();
                }

                public String name() {
                    return LightFragment.s_name;
                }
            },

            // Clock
            new Creator() {
                public DevFragment creator() {
                    return ClockFragment.create();
                }

                public String name() {
                    return ClockFragment.s_name;
                }
            },

            // Gps
            new Creator() {
                public DevFragment creator() {
                    return GpsFragment.create();
                }

                public String name() {
                    return GpsFragment.s_name;
                }
            },

            // Screen
            new Creator() {
                public DevFragment creator() {
                    return ScreenFragment.create();
                }

                public String name() {
                    return ScreenFragment.s_name;
                }
            },

            // Text
            new Creator() {
                public DevFragment creator() {
                    return TextFragment.create();
                }

                public String name() {
                    return TextFragment.s_name;
                }
            },

            // Icon Draw
            new Creator() {
                public DevFragment creator() {
                    return IconDrawFragment.create();
                }

                public String name() {
                    return IconDrawFragment.s_name;
                }
            },

            // Icon Attr
            new Creator() {
                public DevFragment creator() {
                    return IconAttrFragment.create();
                }

                public String name() {
                    return IconAttrFragment.s_name;
                }
            },

            // Num Attr
            new Creator() {
                public DevFragment creator() {
                    return NumAttrFragment.create();
                }

                public String name() {
                    return NumAttrFragment.s_name;
                }
            },

            // Num Dime
            new Creator() {
                public DevFragment creator() {
                    return NumDimenFragment.create();
                }

                public String name() {
                    return NumDimenFragment.s_name;
                }
            },
    };

    private  Creator[] removeArrayIdx(Creator[] inArray, int removeIdx) {
        Creator[] outArray = new Creator[inArray.length - 1];
        System.arraycopy(inArray, 0, outArray, 0, removeIdx);
        System.arraycopy(inArray, removeIdx + 1, outArray, removeIdx, inArray.length - removeIdx - 1);
        return outArray;
    }

    TabPagerAdapter(FragmentManager fm, ViewPager viewPager, ActionBar actionBar) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        if (Build.VERSION.SDK_INT >= 29) {
            // https://developer.android.com/about/versions/10/privacy/changes
            for (int idx = 0; idx < m_tabList.length; idx++) {
                if (m_tabList[idx].name().equals(NetstatFragment.s_name)) {
                    m_tabList = removeArrayIdx(m_tabList, idx);
                    break;
                }
            }
        }

        m_viewPager = viewPager;
        m_actionBar = actionBar;
        m_viewPager.setAdapter(this);

        // Keep all fragments cached.
        //   m_viewPager.setOffscreenPageLimit(this.getCount());

        if (m_actionBar != null) {
            m_actionBar.setHomeButtonEnabled(false);
            m_actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            // Adding Tabs
            for (int tabIdx = 0; tabIdx < this.getCount(); tabIdx++) {
                m_actionBar.addTab(m_actionBar.newTab()
                        .setText(this.getTabName(tabIdx)).setTabListener(this));
            }
        }

        /*
         * On swiping the viewpager sets respective tab selected.
         */
        if (m_actionBar != null) {
            // TODO - need to remove this listener in detach
            m_viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            // m_viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageSelected(int position) {
                    if (m_actionBar != null) {
                        m_actionBar.setSelectedNavigationItem(position);
                    }
                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {
                }

                @Override
                public void onPageScrollStateChanged(int arg0) {
                }
            });

            m_actionBar.setSelectedNavigationItem(0);
        }
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        super.restoreState(state, loader);
    }

    // ========================================================================
    // Implement FragmentPagerAdapter

    @Override
    public Fragment getItem(int tabIdx) {

        DevFragment devFragment;

        devFragment = DevFragment.getFragmentByName(getFragName(tabIdx));
        if (null != devFragment)
            return devFragment;

        devFragment = m_tabList[tabIdx].creator();
        return devFragment;
    }

    @Override
    public int getCount() {
        return m_tabList.length;
    }

    // ========================================================================
    // Override ActionBar.TabListener

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        int tabIdx = tab.getPosition();
        m_viewPager.setCurrentItem(tabIdx);
        getFragment(tabIdx).onSelected();
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        int tabIdx = tab.getPosition();
        getFragment(tabIdx).onSelected();
    }

    // ========================================================================
    // Implement TabsPagerAdapter

    private DevFragment getFragment(int index) {
        return (DevFragment) getItem(index);
    }

    private String getTabName(int tabIdx) {
        String name = m_tabList[tabIdx].name();
        return name;
    }

    private String getFragName(int tabIdx) {
        return getTabName(tabIdx);
    }

    /***
     * Find tab index for matching page by name.
     *
     * @param defIdx  - default to return on no match.
     *
     * @return tab index to matching fragName, else defIdx.
     */
    public int findFragPos(String fragName, int defIdx) {

        for (int tabIdx = 0; tabIdx != m_tabList.length; tabIdx++) {
            String name = m_tabList[tabIdx].name();
            if (name.equals(fragName)) {
                return tabIdx;
            }
        }

        return defIdx;   // No match
    }


    public int getCurrentTabIdx() {
        int tabIdx = m_viewPager.getCurrentItem();
        return tabIdx;
    }

    public void setCurrentTabIdx(int tabIdx) {
        m_viewPager.setCurrentItem(tabIdx);
    }

    /**
     * Execute sharing for current page.
     */
    @SuppressLint("DefaultLocale")
    public void sharePage() {
        try {
            int tabIdx = m_viewPager.getCurrentItem();
            String fragName = getFragName(tabIdx);
            String imageName = fragName.toLowerCase() + ".png";

            DevFragment devFrag = getFragment(tabIdx);
            try {
                if (null != devFrag) {
                    List<String> shareCSV = devFrag.getListAsCsv();
                    List<Bitmap> shareImages = devFrag.getBitmaps(SHARE_MAX_IMAGE_HEIGHT);
                    // if (null != shareImages && shareImages.size() != 0) {
                        Utils.shareList(devFrag.getContext(), shareImages, shareCSV, fragName, imageName,
                                GlobalInfo.s_globalInfo.shareActionProvider);
                    /*} else {
                        Toast.makeText(devFrag.getActivity(),
                                "Unable to share\nFailed to grab screen", Toast.LENGTH_LONG).show();
                    }*/
                } else {
                    Toast.makeText(GlobalInfo.s_globalInfo.mainFragActivity,
                            "Unable to share\nSwitch screens\nand try again", Toast.LENGTH_LONG).show();
                }

            } catch (Exception ex) {
                m_log.e("share failed - " + ex.getMessage());
                Toast.makeText(GlobalInfo.s_globalInfo.mainFragActivity,
                        "Unable to share\nSwitch screens\nand try again", Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            m_log.e("share failed - " + ex.getMessage());
        }
    }

    public static void sharePage(String mediaPath) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        final String IMAGE_TYPE = "image/png";
        shareIntent.setType(IMAGE_TYPE);
        Uri uri = Uri.parse(mediaPath);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        GlobalInfo.s_globalInfo.shareActionProvider.setShareIntent(shareIntent);
    }
}