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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.icu.text.TimeZoneNames;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import com.landenlabs.all_devtool.receivers.AlarmReceiver;
import com.landenlabs.all_devtool.shortcuts.util.ALogNotification;
import com.landenlabs.all_devtool.shortcuts.util.LLog;
import com.landenlabs.all_devtool.shortcuts.util.Ui;
import com.landenlabs.all_devtool.shortcuts.util.Utils;

import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Display "Clock"  information.
 *
 * @author Dennis Lang
 *
 */
public class ClockFragment extends DevFragment implements View.OnClickListener  {

    private final LLog m_log = LLog.DBG;
    public static String s_name = "Clock";
    SubMenu m_menu;
    Date m_date = new Date();
    TimeZone m_timeZone = TimeZone.getDefault();


    // ---- Alaram ----
    long m_alarmMilli = 0L;
    AlarmManager m_alarmManager;
    private PendingIntent m_pendingIntent;
    private TimePicker m_alarmTimePicker;
    private TextView m_alarmTextView;
    private ToggleButton alarmToggle;
    private ImageView alarmExpander;

    // ---- Clock / Times ----
    private TextView m_clockLocalTv;
    private TextView m_clockGmtTv;
    private TextView m_timePartsTv;
    private TextView m_dayLight;
    private TextView m_localeTv;

    // Timezone Daylight savings
    enum DaylightFilter { NoDS, HasDS, InDS}
    DaylightFilter m_daylightFilter = DaylightFilter.HasDS;

    // Additional times
    private TextView m_clockSysClkUpTm;
    private TextView m_clockSysClkReal;

    private static final SimpleDateFormat s_time12Format = new SimpleDateFormat("MMM-dd hh:mm:ss a");
    private static final SimpleDateFormat s_time12ZoneFormat = new SimpleDateFormat("MM/dd/yyyy  hh:mm:ss a zzz");
    private static final SimpleDateFormat s_time12GmtFormat = new SimpleDateFormat("MM/dd/yyyy  hh:mm:ss a 'GMT'");

    private static final SimpleDateFormat s_time24Format = new SimpleDateFormat("MMM-dd HH:mm:ss");
    private static final SimpleDateFormat s_time24ZoneFormat = new SimpleDateFormat("MM/dd/yyyy  HH:mm:ss zzz");
    private static final SimpleDateFormat s_time24GmtFormat = new SimpleDateFormat("MM/dd/yyyy  HH:mm:ss 'GMT'");

    private static SimpleDateFormat s_timeFormat = s_time12Format;
    private static SimpleDateFormat s_timeZoneFormat = s_time12ZoneFormat;
    private static SimpleDateFormat s_timeGmtFormat = s_time12GmtFormat;

    static {
        s_timeGmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    // ---- Timer ----
    private static final int REFRESH_MSEC = 1000;
    private Handler m_handler = new Handler();
    private Runnable m_updateElapsedTimeTask = new Runnable() {
        public void run() {
            updateClock();
            m_handler.postDelayed(this, REFRESH_MSEC);   // Re-execute after xxx ms.
        }
    };

    public static DevFragment create() {
        return new ClockFragment();
    }

    AlarmManager getAlarmMgr() {
        return (AlarmManager)getActivitySafe().getSystemService(Context.ALARM_SERVICE);
    }

    // ============================================================================================
    // DevFragment methods

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public List<Bitmap> getBitmaps(int maxHeight) {
        List<Bitmap> bitmapList = new ArrayList<>();
        bitmapList.add(Utils.grabScreen(getActivitySafe()));
        return bitmapList;
    }

    @Override
    public List<String> getListAsCsv() {
        return null;
    }

    @Override
    public void onSelected() {
        GlobalInfo.s_globalInfo.mainFragActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        GlobalInfo.s_globalInfo.mainFragActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // ============================================================================================
    // Fragment methods

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            m_log.e("onViewCreated savedInstanceState");

        setHasOptionsMenu(true);

        View m_rootView = inflater.inflate(R.layout.clock_tab, container, false);

        m_alarmTimePicker =  Ui.viewById(m_rootView, R.id.alarmTimePicker);
        m_alarmTextView = Ui.viewById(m_rootView, R.id.alarmText);
        m_clockLocalTv = Ui.viewById(m_rootView, R.id.clockLocal);
        m_clockGmtTv = Ui.viewById(m_rootView, R.id.clockGmt);
        m_timePartsTv = Ui.viewById(m_rootView, R.id.timeParts);
        LinearLayout m_timeTopList = Ui.viewById(m_rootView, R.id.timeTopList);

        Ui.viewById(m_rootView, R.id.timezone_hasds).setOnClickListener(this);
        Ui.viewById(m_rootView, R.id.timezone_inds).setOnClickListener(this);
        Ui.viewById(m_rootView, R.id.timezone_nods).setOnClickListener(this);
        Ui.viewById(m_rootView, R.id.showTzMapBtn).setOnClickListener(this);

        // ---- Additional times ----
        m_clockSysClkUpTm = new TextView(m_timeTopList.getContext());
        m_timeTopList.addView(m_clockSysClkUpTm);
        m_clockSysClkReal = new TextView(m_timeTopList.getContext());
        m_timeTopList.addView(m_clockSysClkReal);
        m_localeTv = new TextView(m_timeTopList.getContext());
        m_timeTopList.addView(m_localeTv);

        LinearLayout m_timeBotList = Ui.viewById(m_rootView, R.id.timeBotList);
        m_dayLight = new TextView(m_timeBotList.getContext());
        m_timeBotList.addView(m_dayLight);

        // ---- Alaram ----
        alarmToggle = Ui.viewById(m_rootView, R.id.alarmToggle);
        alarmToggle.setOnClickListener(this);
        alarmExpander = Ui.viewById(m_rootView, R.id.alarmExpand);
        alarmExpander.setOnClickListener(this);

        m_alarmTimePicker.setVisibility(alarmExpander.getTag() != null ? View.VISIBLE : View.GONE);
        m_alarmManager = getAlarmMgr();

        return m_rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        m_handler.removeCallbacks(m_updateElapsedTimeTask);
        m_handler.postDelayed(m_updateElapsedTimeTask, 0);
        updateAlarm();
    }

    @Override
    public void onPause() {
        super.onPause();
        m_handler.removeCallbacks(m_updateElapsedTimeTask);
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.gps_clock_12:
                s_timeFormat = s_time12Format;
                s_timeZoneFormat = s_time12ZoneFormat;
                s_timeGmtFormat = s_time12GmtFormat;
                break;
            case R.id.gps_clock_24:
                s_timeFormat = s_time24Format;
                s_timeZoneFormat = s_time24ZoneFormat;
                s_timeGmtFormat = s_time24GmtFormat;
                break;

            default:
                break;
        }

        item.setChecked(true);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        m_menu = menu.addSubMenu("Clock Format");
        inflater.inflate(R.menu.clock_menu, m_menu);
        m_menu.findItem(R.id.gps_clock_12).setChecked(true);
    }



    // ============================================================================================
    // OnClick
    @Override
    public void onClick(View view) {

        int id = view.getId();
        switch (id) {
            case R.id.showTzMapBtn:
                showTimezoneMap();
                break;
            case R.id.timezone_hasds:
                m_daylightFilter = DaylightFilter.HasDS;
                updateClock();
                break;
            case R.id.timezone_inds:
                m_daylightFilter = DaylightFilter.InDS;
                updateClock();
                break;
            case R.id.timezone_nods:
                m_daylightFilter = DaylightFilter.NoDS;
                updateClock();
                break;

            case R.id.alarmToggle:
                if (alarmToggle.isChecked() && alarmToggle.getTag() == null) {
                    toggleAlaramExpand();
                }
                updateAlarm();
                break;
            case R.id.alarmExpand:
                toggleAlaramExpand();
                updateAlarm();
                break;
        }
    }

    // ============================================================================================
    // Clock methods

    private static final long MIN_MILLI = 60 * 1000;
    private static final long HOUR_MILLI = MIN_MILLI * 60;

    private String getTzOffsetStr(TimeZone tz) {
        long tzOffMilli = tz.getRawOffset();
        long hours = tzOffMilli / HOUR_MILLI;
        long mins = Math.abs(tzOffMilli % HOUR_MILLI) / MIN_MILLI;

        return String.format("    GMT%+2d:%02d",
                hours, mins);
    }

    private String getTzDetailStr(TimeZone tz) {

        return String.format("%13s %s %s %s\n",
                getTzOffsetStr(tz),
                tz.getID(),
                tz.useDaylightTime() ? "HasDS" : "",
                tz.inDaylightTime(m_date) ? "InDs" : "");
    }

    private String getTzOffsetStr(TimeZone tz, SimpleDateFormat dateFormat) {
        dateFormat.setTimeZone(tz);
        return String.format("%13s %s %s\n",
                getTzOffsetStr(tz),
                dateFormat.format(m_date),
                tz.getID());
    }

    /**
     * Update clock on screen
     */
    private void updateClock() {
        m_date.setTime(System.currentTimeMillis());
        // m_timeZone = TimeZone.getDefault();

        m_timeZone = Calendar.getInstance().getTimeZone();

        s_timeZoneFormat.setTimeZone(m_timeZone);
        String localTmStr = s_timeZoneFormat.format(m_date);
    //    localTmStr += "\n  zzz=" + new SimpleDateFormat("zzz", Locale.getDefault()).format(m_date);
        localTmStr += "\n   " + new SimpleDateFormat("zzzz", Locale.getDefault()).format(m_date);
    //    localTmStr += "\n  ZZZ=" + new SimpleDateFormat("ZZZ").format(m_date);
        localTmStr += "\n   " + new SimpleDateFormat("ZZZZ", Locale.getDefault()).format(m_date);
     //   localTmStr += "\n Build Ver=" + Build.VERSION.SDK_INT;
        if (Build.VERSION.SDK_INT >= 24) {
            String tzId = m_timeZone.getID();
            Locale us = Locale.US;
            TimeZoneNames tzNames = TimeZoneNames.getInstance(us);
            String tzName = tzNames.getDisplayName(tzId, TimeZoneNames.NameType.SHORT_STANDARD, m_date.getTime());
            Log.d("ZZ_", "tzName=" + tzName);
        }


        m_clockLocalTv.setText(localTmStr);
        String gmtTmStr = s_timeGmtFormat.format(m_date);
        m_clockGmtTv.setText(gmtTmStr);

        long currDay = TimeUnit.DAYS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        m_timePartsTv.setText(String.format("Days since Jan 1 1970: %d", currDay));

        Locale ourLocale = Locale.getDefault();
        StringBuilder tzStr1 = new StringBuilder();
        StringBuilder tzStr2 = new StringBuilder();

        tzStr1.append(String.format("Locale %s\n", ourLocale.getDisplayName()));

        tzStr1.append(getTzDetailStr(m_timeZone));
        tzStr1.append("Daylight Savings:\n");

        // tzStr.append((m_timeZone.useDaylightTime() ? "Has" : "No") + " daylight savings\n");
        String ds_short = m_timeZone.getDisplayName(false, TimeZone.SHORT, ourLocale);
        tzStr1.append(String.format("    %s=%s\n", ds_short,m_timeZone.getDisplayName(false, TimeZone.LONG, ourLocale)));
        if (m_timeZone.useDaylightTime()) {
            String std_short = m_timeZone.getDisplayName(true, TimeZone.SHORT, ourLocale);
            tzStr1.append(String.format("    %s=%s\n", std_short, m_timeZone.getDisplayName(true, TimeZone.LONG, ourLocale)));

            // ----
            // DateTimeZone zone1 = DateTimeZone.forID("Europe/London");
            // DateTimeZone zone2 = DateTimeZone.forID("America/New_York");

            DateTimeZone zone = DateTimeZone.forTimeZone(m_timeZone);
            org.joda.time.format.DateTimeFormatter format = org.joda.time.format.DateTimeFormat.mediumDateTime();

            long current = System.currentTimeMillis();
            for (int i=0; i < 4; i++)
            {
                long next = zone.nextTransition(current);
                if (current == next)
                {
                    break;
                }

                tzStr1.append(String.format("    %s %s\n", zone.isStandardOffset(next-3600000) ? std_short : ds_short,
                        format.print(next)));
                current = next;
            }
        //    m_localeTv.setText(tzStr1.toString());


            String[] ids = TimeZone.getAvailableIDs();
            if (ids != null && ids.length > 0) {
                switch (m_daylightFilter) {
                    case NoDS:
                        tzStr2.append("TimeZones (no Daylight savings):\n");
                        break;
                    case HasDS:
                        tzStr2.append("TimeZone (Has Daylight savings):\n");
                        break;
                    case InDS:
                        tzStr2.append("TimeZone (In Daylight savings):\n");
                        break;
                }

                SparseIntArray zones = new SparseIntArray();
                for (int tzIdx = 0; tzIdx < ids.length; tzIdx++) {
                    TimeZone tz = TimeZone.getTimeZone(ids[tzIdx]);
                    boolean addTz = false;
                    switch (m_daylightFilter) {
                        case NoDS:
                            addTz = !tz.useDaylightTime();
                            break;
                        case HasDS:
                            addTz = tz.useDaylightTime() && !tz.inDaylightTime(m_date);
                            break;
                        case InDS:
                            addTz = tz.inDaylightTime(m_date);
                            break;
                    }
                    if (addTz) {
                        zones.put(tz.getRawOffset(), tzIdx);
                    }
                }

                for (int idx = 0; idx != zones.size(); idx++) {
                    TimeZone tz = TimeZone.getTimeZone(ids[zones.valueAt(idx)]);
                    tzStr2.append(getTzOffsetStr(tz, s_timeFormat));
                }
            }

        }

        m_localeTv.setText(tzStr1.toString());
        m_dayLight.setText(tzStr2.toString());

        m_clockSysClkUpTm.setText("uptimeMillis:"+ formatInterval(SystemClock.uptimeMillis()));
        m_clockSysClkReal.setText("elapsedRealtime:" + formatInterval(SystemClock.elapsedRealtime()));
    }

    /**
     * Format time interval
     */
    private static String formatInterval(final long elapsedMillis)
    {
        final long day = TimeUnit.MICROSECONDS.toHours(elapsedMillis) / 24;
        final long hr = TimeUnit.MILLISECONDS.toHours(elapsedMillis) % 24;
        final long min = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60;
        final long sec = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(elapsedMillis) % 1000;
        return String.format("%s %02d:%02d:%02d.%03d", (day == 0 ? "" : String.valueOf(day)), hr, min, sec, ms);
    }

    private void showTimezoneMap() {
        String htmlStr = Utils.LoadData(getContextSafe(), "timezone.html");

        Ui.showWebMessage(getContext(), Ui.HTML_CENTER_BOX, htmlStr);
        // Ui.showWebImage(getContext(), "file:///android_asset/world_timezone_map.png");
    }

    // ============================================================================================
    // Alarm methods

    private void toggleAlaramExpand() {
        alarmExpander.animate().rotationBy(180).setDuration(1000).start();
        alarmExpander.setTag(alarmExpander.getTag() == null ? 1 : null);
    }

    /**
     * Toogle alarm setting UI on/off and start/stop pending intent.
     */
    private void updateAlarm() {
        boolean isOn = alarmToggle.isChecked();
        m_alarmTimePicker.setVisibility(alarmExpander.getTag() != null ? View.VISIBLE : View.GONE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, m_alarmTimePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE, m_alarmTimePicker.getCurrentMinute());
        if (isOn && calendar.getTimeInMillis() - System.currentTimeMillis() < TimeUnit.SECONDS.toMillis(60)) {
            // calendar.add(Calendar.SECOND, 60);
            m_alarmTimePicker.setCurrentHour(calendar.get(Calendar.HOUR));
            m_alarmTimePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
        }

        if (isOn) {
            if (m_pendingIntent == null) {
                Intent myIntent = new Intent(GlobalInfo.s_globalInfo.mainFragActivity, AlarmReceiver.class);
                m_pendingIntent = PendingIntent.getBroadcast(
                        GlobalInfo.s_globalInfo.mainFragActivity, 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                m_alarmMilli = calendar.getTimeInMillis();
                m_alarmManager.set(AlarmManager.RTC, m_alarmMilli, m_pendingIntent);
            }
            s_timeFormat.setTimeZone(m_timeZone);
            String alarmStr = s_timeFormat.format(m_alarmMilli);
            // Utils.showAlarmNotification(this.getActivity(), Utils.CLOCK_NOTIFICATION_ID,  "Alarm " + alarmStr);
            ALogNotification.updateNotification(this.getActivity(), "Alarm " + alarmStr);
            m_alarmTextView.setText(alarmStr);

            m_log.i("Alarm On " + alarmStr);
        } else {
            if (m_pendingIntent != null) {
                m_alarmManager.cancel(m_pendingIntent);
                Utils.cancelNotification(this.getActivity(), Utils.CLOCK_NOTIFICATION_ID);
                m_pendingIntent = null;
            }
            // This will sound the alarm tone
            // this will sound the alarm once, if you wish to
            // raise alarm in loop continuously then use MediaPlayer and setLooping(true)
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (Build.VERSION.SDK_INT >= 28) {
                // RingtoneManager ringMan = new RingtoneManager(getActivity());
                Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), alarmUri);
                if (ringtone != null) {
                    m_log.i("Alarm ringtone stop");
                    ringtone.stop();
                }
            } else {
                MediaPlayer mediaPlayer = MediaPlayer.create(requireContext(), alarmUri);
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    m_log.i("Alarm media stop");
                }
            }

            m_alarmTextView.setText("");
            m_log.i("Alarm Off");
        }
    }

}