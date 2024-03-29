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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.HardwarePropertiesManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import com.landenlabs.all_devtool.shortcuts.util.ListInfo;
import com.landenlabs.all_devtool.shortcuts.util.SearchList;
import com.landenlabs.all_devtool.shortcuts.util.Ui;
import com.landenlabs.all_devtool.shortcuts.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static android.content.Context.HARDWARE_PROPERTIES_SERVICE;
import static android.os.HardwarePropertiesManager.DEVICE_TEMPERATURE_BATTERY;
import static android.os.HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU;
import static android.os.HardwarePropertiesManager.DEVICE_TEMPERATURE_GPU;
import static android.os.HardwarePropertiesManager.TEMPERATURE_CURRENT;
import static com.landenlabs.all_devtool.shortcuts.util.SysUtils.runShellCmd;

/**
 * Display "Process"  information.
 *
 * @author Dennis Lang
 */
@SuppressWarnings({"Convert2Lambda", "UnnecessaryLocalVariable"})
public class ProcFragment extends DevFragment {

    private final ArrayList<ListInfo> m_list = new ArrayList<>();
    private ExpandableListView m_listView;
    private TextView m_titleTime;
    ImageButton m_search;
    View m_refresh;
    String m_filter;
    SearchList m_searchList = new SearchList();

    private static final SimpleDateFormat m_timeFormat = new SimpleDateFormat("HH:mm:ss zz");

    public static final String s_name = "Proc";

    public ProcFragment() {
    }

    public static DevFragment create() {
        return new ProcFragment();
    }

    // ============================================================================================
    // DevFragment methods

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public List<Bitmap> getBitmaps(int maxHeight) {
        return Utils.getListViewAsBitmaps(m_listView, maxHeight);
    }

    @Override
    public List<String> getListAsCsv() {
        return Utils.getListViewAsCSV(m_listView);
    }


// ============================================================================================
    // Fragment methods

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View rootView = inflater.inflate(R.layout.proc_tab, container, false);
        Ui.<TextView>viewById(rootView, R.id.list_title).setText(R.string.proc_title);
        m_listView = Ui.viewById(rootView, R.id.procListView);

        Ui.viewById(rootView, R.id.list_time_bar).setVisibility(View.VISIBLE);
        m_titleTime = Ui.viewById(rootView, R.id.list_time);

        m_search = Ui.viewById(rootView, R.id.list_search);
        m_search.setVisibility(View.VISIBLE);
        m_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_titleTime.setText("");
                m_titleTime.setHint("enter search text");
                InputMethodManager imm = getServiceSafe(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(m_titleTime, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        m_titleTime.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView edView, int actionId, KeyEvent event)
            {
                if(actionId == EditorInfo.IME_ACTION_DONE)
                {
                    InputMethodManager imm = getServiceSafe(Context.INPUT_METHOD_SERVICE);
                    // imm.showSoftInput(m_titleTime, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    imm.toggleSoftInput(0, 0);

                    Toast.makeText(getContext(), "Searching...", Toast.LENGTH_SHORT).show();
                    m_filter = edView.getText().toString();
                    m_searchList.search(m_list, m_filter);
                    // updateList();
                    if (m_searchList.matchCnt == 0) {
                        Toast.makeText(getContext(), "No match", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), String.format("%d matches", m_searchList.matchCnt), Toast.LENGTH_SHORT).show();
                        // m_listView.scrollTo(m_filterGroup, 0);
                        m_listView.setSelectedChild(m_searchList.groupIdx, m_searchList.childIdx, true);
                    }
                    return true; // consume.
                }
                return false; // pass on to other listeners.
            }
        });

        final ToggleButton expandTb = Ui.viewById(rootView, R.id.list_collapse_tb);
        expandTb.setVisibility(View.VISIBLE);
        expandTb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandTb.isChecked())
                    expandAll();
                else
                    collapseAll();
            }
        });

        m_refresh = Ui.viewById(rootView, R.id.list_refresh);
        m_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateList();
                m_listView.invalidateViews();
            }
        });


        return rootView;
    }

    // Coming into forground - update list.
    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }

    // ============================================================================================
    // Internal methods

    /**
     * Populate list with 'Build' parameters.
     */
    @SuppressWarnings({"ConstantConditions", "ConstantIfStatement"})
    private void updateList() {
        // Time today = new Time(Time.getCurrentTimezone());
        // today.setToNow();
        // today.format(" %H:%M:%S")
        Date dt = new Date();
        m_titleTime.setText(m_timeFormat.format(dt));

        boolean firstTime = m_list.isEmpty();

        if (m_list.isEmpty()) {
            if (true) {
                addString("BOARD", Build.BOARD);
                addString("BOOTLOADER", Build.BOOTLOADER);
                addString("BRAND", Build.BRAND);
                addString("CPU_ABI", Build.CPU_ABI);
                addString("CPU_ABI2", Build.CPU_ABI2);
                addString("OS.ARCH", System.getProperty("os.arch"));
                if (Build.VERSION.SDK_INT >= 21) {
                    // addString("SUPPORTED_ABIS", Arrays.toString(Build.SUPPORTED_ABIS));
                    if (Build.SUPPORTED_32_BIT_ABIS != null && Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                        addString("32_BIT_ABIS", Arrays.toString(Build.SUPPORTED_32_BIT_ABIS));
                    } else {
                        addString("64_BIT_ABIS", Arrays.toString(Build.SUPPORTED_64_BIT_ABIS));
                    }
                }

                addString("DEVICE", Build.DEVICE);
                addString("MANUFACTURER", Build.MANUFACTURER);
                addString("MODEL", Build.MODEL);
                addString("PRODUCT", Build.PRODUCT);
            }

            /*
                HardwarePropertiesManager hwpm = (HardwarePropertiesManager) getSystemService(this.HARDWARE_PROPERTIES_SERVICE);
                CpuUsageInfo cpuUsage[] = hwpm.getCpuUsages();

                PowerManager pm = getServiceSafe(Context.POWER_SERVICE);
                pm.isSustainedPerformanceModeSupported()
                activity.getWindow().setSustainedPerformanceMode(true)
             */
            if (true) {
                if (Build.VERSION.SDK_INT >= 24) {
                    HardwarePropertiesManager hwpm = getServiceSafe(HARDWARE_PROPERTIES_SERVICE);

                    if (Build.VERSION.SDK_INT >= 24) {
                        try {
                            Map<String, String> temperatureItems = new HashMap<>();
                            float[] tcpu = hwpm.getDeviceTemperatures(DEVICE_TEMPERATURE_CPU,
                                    TEMPERATURE_CURRENT);
                            float[] tgpu = hwpm.getDeviceTemperatures(DEVICE_TEMPERATURE_GPU,
                                    TEMPERATURE_CURRENT);
                            float[] tbat = hwpm.getDeviceTemperatures(DEVICE_TEMPERATURE_BATTERY,
                                    TEMPERATURE_CURRENT);
                            temperatureItems.put(" CPU", String.format("%.1f", tcpu[0]));
                            temperatureItems.put(" GPU", String.format("%.1f", tgpu[0]));
                            temperatureItems.put(" Battery", String.format("%.1f", tbat[0]));
                            addMap("Temperaures", temperatureItems);
                        } catch (Throwable tr) {
                            Log.e("Hardware properties", "access failed", tr);
                        }
                    }
                }
            }
            if (true) {
                ArrayList<ArrayList<String>> cpuInfoLists = readFile("/proc/cpuinfo", "",": ", 2);
                for (ArrayList<String> cpuInfoList : cpuInfoLists) {
                    Map<String, String> cpuItems = new HashMap<>();
                    for (String line : cpuInfoList) {
                        String[] vals = line.split(": ");
                        cpuItems.put(vals[0], vals[1]);
                    }
                    addMap("CPU " + cpuInfoList.get(0), cpuItems);
                }
            }

            if (true) {
                ArrayList<String> listInfo = readFile("/proc/100/stat", " ", 2);
                for (String line : listInfo) {
                    String[] vals = line.split(" ");
                    int rowCnt = 0;
                    for (String val : vals) {
                        addString(String.format("100/stat %2d", rowCnt++), val);
                    }
                }
            }

            if (true) {
                ArrayList<String> memList = getPkgMemInfo("com.wsiscroll.android.weather");
                if (memList != null && memList.size() > 0) {
                    int rowCnt = 0;
                    for (String line : memList) {
                        addString(String.format("pkgMem %3d", rowCnt++), line);
                    }
                }
            }

            if (false) {
                // Need android.permission.DUMP which is only allowed to system apps.
                ArrayList<String> netList = runShellCmd(
                        new String[] {"dumpsys", "netpolicy"});
                ArrayList<String> pkgList = runShellCmd(
                        new String[] {"dumpsys", "package"});

                addString("netpolicy ", " " + netList.size());
                addString("package ", " " + pkgList.size());
            }
        }

        if (firstTime ||
                !(m_listView.getExpandableListAdapter() instanceof  BaseExpandableListAdapter)) {
            final BuildArrayAdapter adapter = new BuildArrayAdapter(getActivitySafe());
            m_listView.setAdapter(adapter);

            int count = adapter.getGroupCount();
            for (int position = 0; position < count; position++)
                m_listView.expandGroup(position);
        }

        // m_listView.invalidate();
        if (m_listView.getExpandableListAdapter() instanceof BaseExpandableListAdapter ) {
            ((BaseExpandableListAdapter) m_listView.getExpandableListAdapter())
                    .notifyDataSetChanged();
        }
    }


    private void addString(String name, String value) {
        if (!TextUtils.isEmpty(value))
            m_list.add(new ListInfo(name, value.trim()));
    }

    @SuppressWarnings("SameParameterValue")
    void addMap(String name, Map<String, String> map) {
        m_list.add(new ListInfo(name, map));
    }

    @SuppressWarnings("SameParameterValue")
    private static ArrayList<ArrayList<String>> readFile(String filename, String sectionPat, String splitPat, int  minSplitCnt) {
        ArrayList<ArrayList<String>> listOfList = new ArrayList<>();
        ArrayList<String> list = new ArrayList<>();
        try {
            Scanner scan = new Scanner(new File(filename));
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] vals = line.split(splitPat);
                if (vals.length >= minSplitCnt) {
                    list.add(line);
                    // map.put(vals[0].trim(), vals[1].trim());
                }
                if (line.equals(sectionPat)) {
                    listOfList.add(list);
                    list = new ArrayList<>();
                }
            }

            if (list.size() != 0) {
                listOfList.add(list);
            }
        } catch (Exception e) {
            Log.e("getCpuInfoMap",Log.getStackTraceString(e));
        }
        return listOfList;
    }

    @SuppressWarnings("SameParameterValue")
    private static ArrayList<String> readFile(String filename,  String splitPat, int  minSplitCnt) {
        ArrayList<String> list = new ArrayList<>();
        try {
            Scanner scan = new Scanner(new File(filename));
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] vals = line.split(splitPat);
                if (vals.length >= minSplitCnt) {
                    list.add(line);
                    // map.put(vals[0].trim(), vals[1].trim());
                }

            }

        } catch (Exception e) {
            Log.e("getCpuInfoMap",Log.getStackTraceString(e));
        }
        return list;
    }

    @SuppressWarnings("SameParameterValue")
    private static ArrayList<String> getPkgMemInfo(String packageName) {
        ArrayList<String> list = runShellCmd(
                new String[] {"dumpsys", "meminfo", packageName});
        return list;
    }

    private void collapseAll() {
        int count = m_listView.getAdapter().getCount(); // m_list.size();
        for (int position = 0; position < count; position++) {
            // m_log.d("Collapse " + position);
            m_listView.collapseGroup(position);
        }
    }

    private void expandAll() {
        int count = m_listView.getAdapter().getCount(); // m_list.size();
        for (int position = 0; position < count; position++)
            m_listView.expandGroup(position);
    }

    // =============================================================================================

    // final static int EXPANDED_LAYOUT = R.layout.build_list_row;
    private final static int SUMMARY_LAYOUT = R.layout.build_list_row;

    /**
     * ExpandableLis UI 'data model' class
     */
    private class BuildArrayAdapter extends BaseExpandableListAdapter
            implements View.OnClickListener {
        private final LayoutInflater m_inflater;

        BuildArrayAdapter(Context context) {
            m_inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * Generated expanded detail view object.
         */
        @Override
        public View getChildView(final int groupPosition,
                 final int childPosition, boolean isLastChild, View convertView,
                 ViewGroup parent) {

            ListInfo listInfo = m_list.get(groupPosition);

            View expandView; // = convertView;
            // if (null == expandView) {
            expandView = m_inflater.inflate(SUMMARY_LAYOUT, parent, false);
            // }

            String key = (String) listInfo.valueListStr().keySet().toArray()[childPosition];
            String val = listInfo.valueListStr().get(key);

            TextView textView = Ui.viewById(expandView, R.id.buildField);
            textView.setText(key);
            textView.setPadding(40, 0, 0, 0);

            textView = Ui.viewById(expandView, R.id.buildValue);
            textView.setText(val);

            String text = key + val;

            if (!TextUtils.isEmpty(m_filter) && (m_filter.equals("*")
                    || text.matches(m_filter)
                    || Utils.containsIgnoreCase(text, m_filter))  ) {
                expandView.setBackgroundColor(0x80ffff00);
            } else {
                if ((groupPosition & 1) == 1)
                    expandView.setBackgroundColor(0);
                else
                    expandView.setBackgroundColor(0x80d0ffe0);
            }

            return expandView;
        }

        @Override
        public int getGroupCount() {
            return m_list.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return m_list.get(groupPosition).getCount();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        /**
         * Generate summary (row) presentation view object.
         */
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            ListInfo listInfo = m_list.get(groupPosition);

            View summaryView = convertView;
            if (null == summaryView) {
                summaryView = m_inflater.inflate(SUMMARY_LAYOUT, parent, false);
            }

            TextView textView = Ui.viewById(summaryView, R.id.buildField);
            textView.setText(listInfo.fieldStr());
            textView.setPadding(10, 0, 0, 0);

            textView = Ui.viewById(summaryView, R.id.buildValue);
            textView.setText(listInfo.valueStr());

            String text = listInfo.fieldStr();
            if (listInfo.valueStr() != null) {
                text += listInfo.valueStr();
            }


            if (!TextUtils.isEmpty(m_filter) && (m_filter.equals("*")
                    || text.matches(m_filter)
                    || Utils.containsIgnoreCase(text, m_filter))  ) {
                summaryView.setBackgroundColor(0x80ffff00);
            } else {
                if ((groupPosition & 1) == 1)
                    summaryView.setBackgroundColor(0);
                else
                    summaryView.setBackgroundColor(0x80d0ffe0);
            }

            summaryView.setTag(groupPosition);
            summaryView.setOnClickListener(this);
            return summaryView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public void onClick(View view) {
            int grpPos = (Integer) view.getTag();

            if (m_listView.isGroupExpanded(grpPos))
                m_listView.collapseGroup(grpPos);
            else
                m_listView.expandGroup(grpPos);
        }
    }
}