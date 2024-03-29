/*
 * Copyright (c) 2023 Dennis Lang (LanDen Labs) landenlabs@gmail.com
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

package com.landenlabs.all_devtool.shortcuts.util;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import com.landenlabs.all_devtool.DevToolActivity;
import com.landenlabs.all_devtool.GlobalInfo;
import com.landenlabs.all_devtool.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

// import com.google.android.gms.maps.model.LatLng;

public class Utils {
    static class LatLng {
        double latitude;
        double longitude;
    }

    // Logger - set to LLog.DBG to only log in Debug build, use LLog.On for always log.
    private static final LLog s_log = LLog.DBG;

    // =============================================================================================
    // Theme

    public final static int sNoThemeIdx = -1;
    public final static int sTheme_00  = 0; //android:style/Theme 
    public final static int sTheme_01  = 1; //android:style/Theme.Black
    public final static int sTheme_02  = 2; //android:style/Theme.WithActionBa
    public final static int sTheme_03  = 3; //android:style/Theme.Translucent
    public final static int sTheme_04  = 4; //android:style/Theme.Wallpaper
    public final static int sTheme_05  = 5; //android:style/Theme.Holo.Light
    public final static int sTheme_06  = 6; //android:style/Theme.Holo
    public final static int sTheme_07  = 7; //android:style/Theme.Holo.Light.DarkActionBar
    public final static int sTheme_08  = 8; //android:style/Theme.Holo.Dialog
    public final static int sTheme_09  = 9; //android:style/Theme.Dialog
    public final static int sTheme_10  = 10; //android:style/Theme.Panel
    public final static int sTheme_11  = 11; //android:style/Theme.Material
    public final static int sTheme_12  = 12; //android:style/Theme.Material.Light
    public final static int sTheme_13  = 13; //android:style/Theme.Material.Light.DarkActionBar
    public final static int sTheme_14  = 14;
    public final static int sTheme_15  = 15;
    public final static int sTheme_16  = 16;
    public final static int sTheme_17  = 17;


    private static int sThemeIdx = sNoThemeIdx;

    public static int getThemeIdx() {
        return (sThemeIdx == sNoThemeIdx) ? sTheme_06 : sThemeIdx;
    }

    /**
     * Set the theme of the Activity, and restart it by creating a new Activity
     * of the same type.
     */
    public static void changeToTheme(Activity activity, int themeIdx, String themeName) {
        GlobalInfo.s_globalInfo.themeName = themeName;
        if (getThemeIdx() != themeIdx) {
            sThemeIdx = themeIdx;
            // assert(R.style.Theme_10 == R.style.Theme_00 + 10);
            GlobalInfo.s_globalInfo.mainFragActivity.recreate();
        }
    }

    /**
     * Set the theme of the activity, according to the configuration.
     */
    public static void onActivityCreateSetTheme(Activity activity) {
        if (sThemeIdx != sNoThemeIdx) {
            activity.setTheme(GlobalInfo.getThemeResId(sThemeIdx));  // R.style.Theme_00 + sThemeId);
            GlobalInfo.grabThemeSetings(activity);
        }
    }


    // =============================================================================================
    // Misc

    public static <E> E last(E[] array) {
        return (array == null || array.length == 0) ? null : array[array.length-1];
    }

    public static boolean containsIgnoreCase(String src, String what) {
        final int length = what.length();
        if (length == 0)
            return true; // Empty string is contained

        final char firstLo = Character.toLowerCase(what.charAt(0));
        final char firstUp = Character.toUpperCase(what.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            // Quick check before calling the more expensive regionMatches() method:
            final char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp)
                continue;

            if (src.regionMatches(true, i, what, 0, length))
                return true;
        }

        return false;
    }

    // =============================================================================================
    // File System

    public static class DirSizeCount  {
        public long size = 0;
        public long count = 0;

        DirSizeCount add(DirSizeCount rhs) {
            DirSizeCount lhs = new DirSizeCount();
            lhs.count = this.count + rhs.count;
            lhs.size = this.size + rhs.size;
            return lhs;
        }
    }

    public static DirSizeCount getDirectorySize(File  dir) {

        DirSizeCount dirSizeCount = null;
        if (dir != null)
        try {
            File[] files = dir.listFiles();

            if (files != null) {
                dirSizeCount = new DirSizeCount();
                for (File file : files) {
                    try {
                        if (file.isFile()) {
                            dirSizeCount.size += file.length();
                            dirSizeCount.count++;
                        } else if (file.isDirectory()) {
                            dirSizeCount = dirSizeCount.add(getDirectorySize(file));
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception ignore) {
        }

        return dirSizeCount;
    }

    /**
     * Delete all files in directory tree.
     */
    public static List<String> deleteFiles(File dirFile) {
        File[] files = dirFile.listFiles();
        List<String> deletedFiles = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                try {
                    if (file.isFile()) {
                        try {
                            if (file.delete()) {
                                deletedFiles.add(file.getName());
                            }
                        } catch (Exception ex) {
                            Log.d("deleteFiles", ex.getMessage());
                        }
                    } else if (file.isDirectory()) {
                        deletedFiles.addAll(deleteFiles(file));
                    }
                } catch (Exception ignore) {
                }
            }
        }

        return deletedFiles;
    }

    private static <typ> String joinPath(typ path1, typ path2) {
        return new File(path1.toString(), path2.toString()).toString();
    }

    // =============================================================================================
    // Display

    /**
     * @return Convert dp to px, return px
     */
    public static float dpToPx(int dp) {
        return (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * @return Convert px to dp, return dp
     */
    public static float pxToDp(int px) {
        return (px / Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * @return DisplayMetrics
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager winMgr = SysUtils.getServiceSafe(context, Context.WINDOW_SERVICE);
        winMgr.getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    // =============================================================================================
    // Math - earth

    private static final float EARTH_RADIUS_KM = 6371.009f; // kilometers
    /**
     * Spherical trigonometry laws allow calculating distance between 2 points on a sphere. The
     * shortest distance between points A and B on Earth surface (assumed its has spherical form) is
     * determined by formula:<br>
     * <code>
     * d = arccos {sin(Φa)*sin(Φb) + cos(Φa)*cos(Φb)*cos(Λa - Λb)}
     * </code><br>
     * where <code>Φa</code> and <code>Φb</code> - latitudes,<br>
     * <code>Λa</code>, <code>Λb</code> - longitudes of appropriate points,<br>
     * <code>d</code> - distance between the points measured in radians with the length of big arc
     * of Earth globe. <br>
     * Distance between points measured in kilometers can be calculated with formula: <br>
     * <code>
     * L = d*R
     * </code><br>
     * where <code>R</code> = 6371 km - the average radius of Earth globe.<br>
     * In order to calculate distance between points located in different hemispheres
     * (northern-southern or eastern-western hemispheres) the appropriate values of
     * latitude/longitude should have different signs (+/-).
     *
     * @param g1Position
     *            first LatLng object
     * @param g2Position
     *            second LatLng object
     * @return distance in kilometers between provided positions.
     */
    public static double kilometersBetweenLatLng(LatLng g1Position, LatLng g2Position) {
        return EARTH_RADIUS_KM
                * Math.acos(Math.sin(Math.toRadians(g1Position.latitude)) * Math.sin(Math.toRadians(g2Position.latitude))
                + Math.cos(Math.toRadians(g1Position.latitude)) * Math.cos(Math.toRadians(g2Position.latitude))
                * Math.cos(Math.toRadians(g2Position.longitude - g1Position.longitude)));
    }

    public static double kilometersBetweenLocations(Location g1Position, Location g2Position) {
        return EARTH_RADIUS_KM
                * Math.acos(Math.sin(Math.toRadians(g1Position.getLatitude())) * Math.sin(Math.toRadians(g2Position.getLatitude()))
                + Math.cos(Math.toRadians(g1Position.getLatitude())) * Math.cos(Math.toRadians(g2Position.getLatitude()))
                * Math.cos(Math.toRadians(g2Position.getLongitude() - g1Position.getLongitude())));
    }

    // =============================================================================================
    // Colors

    /**
     * Blend two colors
     *
     * @param color1 24bit colors rgb
     * @param color2 24bit colors rgb
     * @param ratio  Percent of color1 to use.
     * @return 24bit color = c1 * r + c2 * (1-r)
     */
    public static int blend(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    /**
     * Blend two colors using their alpha
     *
     * @param color1 32bit colors argb
     * @param color2 32bit colors argb
     * @return 24bit color
     */
    public static int blend(int color1, int color2) {
        final float aTot = Color.alpha(color1) + Color.alpha(color2);
        final float ratio = Color.alpha(color1) / aTot;
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    // =============================================================================================
    // Bitmap

    /**
     * Helper to get screen shot of View object.
     */
    private static Bitmap getBitmap(View view) {
        Bitmap screenBitmap =
                Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenBitmap);
        view.draw(canvas);
        return screenBitmap;
    }

    public static Bitmap grabScreen(Activity activity) {
        View view = activity.getWindow().getDecorView().getRootView();
        Bitmap screenBitmap = getBitmap(view);
        if (screenBitmap != null && screenBitmap.isRecycled()) {
            return null;
        }

        return screenBitmap;
    }


    /**
     * Render all items from ListView into CSV text.
     *
     * @param listview  input listView to hold rows of CSV text
     * @return List filled with CSV text.
     */
    public static List<String> getListViewAsCSV(ListView listview) {
        ListAdapter adapter = listview.getAdapter();
        int itemscount = adapter.getCount();
        List<String> csvList = new ArrayList<>();

        // Render each row into its own bitmap, compute total size.
        for (int row = 0; row < itemscount; row++) {
            View childView = adapter.getView(row, null, listview);
            String rowText = getTextCsv(childView, "", csvList);
            if (rowText != null && rowText.length() > 1) {
                csvList.add(rowText.replaceAll("^,", ""));
            }
        }

        return csvList;
    }

    public static String getTextCsv(View view, String rowText, List<String> csvList) {
        Character sep = ',';
        if (view instanceof TextView) {
            rowText += sep + ((TextView) view).getText().toString();
        } else if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                rowText += sep + imageView.getContentDescription().toString();
            } else {
                rowText += sep + "Image";
            }
        } else if (view instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout)view;
            if (linearLayout.getOrientation() == LinearLayout.VERTICAL) {
                csvList.add(rowText);
                ViewGroup viewGroup = (ViewGroup)view;
                for (int idx = 0; idx < viewGroup.getChildCount(); idx++) {
                    rowText = "";
                    csvList.add(getTextCsv(viewGroup.getChildAt(idx), rowText, csvList));
                }
                rowText = "";
            } else {
                getTextCsv(view, rowText, csvList);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup)view;
            for (int idx = 0; idx < viewGroup.getChildCount(); idx++) {
                rowText = getTextCsv(viewGroup.getChildAt(idx), rowText, csvList);
            }
        }

        return rowText;
    }
    /**
     * Render all items from ListView into bitmaps, such that no bitmap is larger than maxHeight.
     * <li> <a href="http://stackoverflow.com/questions/12742343/android-get-screenshot-of-all-listview-items">
     * Code from - get screenshot of all listview items </a>
     *
     * @param listview  input listView to render into bitmaps
     * @param maxHeight max height of output bitmaps
     * @return List of bitmaps.
     */
    public static List<Bitmap> getListViewAsBitmaps(ListView listview, int maxHeight) {

        ListAdapter adapter = listview.getAdapter();
        int itemscount = adapter.getCount();
        int allitemsheight = 0;
        List<Bitmap> itemBms = new ArrayList<>();

        // Render each row into its own bitmap, compute total size.
        for (int row = 0; row < itemscount; row++) {

            View childView = adapter.getView(row, null, listview);
            childView.measure(MeasureSpec.makeMeasureSpec(listview.getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
            int childHeight = childView.getMeasuredHeight();
            if (childHeight < maxHeight) {
                itemBms.add(getBitmap(childView));
                allitemsheight += childHeight;
            }
        }

        List<Bitmap> outBigBms = new ArrayList<>();
        int outHeight = Math.min(maxHeight, allitemsheight);
        Bitmap bigBitmap = null;
        Canvas bigcanvas = null;
        Paint paint = new Paint();
        int iHeight = 0;

        for (int idx = 0; idx < itemBms.size(); idx++) {
            Bitmap bmp = itemBms.get(idx);
            if (!isBitmapValid(bmp)) {
                s_log.e("invalid bitmap");
                continue;
            }

            if (iHeight + bmp.getHeight() >= outHeight) {
                outBigBms.add(bigBitmap);
                bigBitmap = null;
                allitemsheight -= iHeight;
                iHeight = 0;
            }

            if (bigBitmap == null) {
                outHeight = Math.min(maxHeight, allitemsheight);
                // TODO - handle outOfMemory exception.
                bigBitmap = Bitmap.createBitmap(listview.getMeasuredWidth(), outHeight, Bitmap.Config.ARGB_8888);
                bigcanvas = new Canvas(bigBitmap);
            }

            bigcanvas.drawBitmap(bmp, 0, iHeight, paint);
            iHeight += bmp.getHeight();

            bmp.recycle();
        }

        return outBigBms;
    }

    /**
     * Render table rows into bitmaps, group multiple rows into single image until maxHeight is
     * exceeded.
     *
     * @return List of bitmaps.
     */
    public static List<Bitmap> getTableLayoutAsBitmaps(TableLayout tableLayout, int maxHeight) {

        int itemscount = tableLayout.getChildCount();
        int allitemsheight = 0;
        List<Bitmap> itemBms = new ArrayList<>();

        // Render each row into its own bitmap, compute total size.
        for (int row = 0; row < itemscount; row++) {

            View childView = tableLayout.getChildAt(row);
            childView.measure(MeasureSpec.makeMeasureSpec(tableLayout.getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
            int childHeight = childView.getMeasuredHeight();
            if (childHeight < maxHeight) {
                itemBms.add(getBitmap(childView));
                allitemsheight += childHeight;
            }

        }

        // If only two pages, try and divide into two even pages
        // Dont' try exactly 50% per page because row items may not
        // fit evenly, so give pages 60% of total height.
        if (allitemsheight / maxHeight == 2)
            maxHeight = (int) (allitemsheight * 0.6);

        List<Bitmap> outBigBms = new ArrayList<>();
        int outHeight = Math.min(maxHeight, allitemsheight);
        Bitmap bigBitmap = null;
        Canvas bigcanvas = null;
        Paint paint = new Paint();
        int iHeight = 0;

        for (int idx = 0; idx < itemBms.size(); idx++) {
            Bitmap bmp = itemBms.get(idx);
            if (!isBitmapValid(bmp)) {
                s_log.e("invalid bitmap");
                continue;
            }

            if (iHeight + bmp.getHeight() >= outHeight) {
                outBigBms.add(bigBitmap);
                bigBitmap = null;
                allitemsheight -= iHeight;
                iHeight = 0;
            }

            if (bigBitmap == null) {
                outHeight = Math.min(maxHeight, allitemsheight);
                bigBitmap = Bitmap.createBitmap(tableLayout.getMeasuredWidth(), outHeight, Bitmap.Config.ARGB_8888);
                bigcanvas = new Canvas(bigBitmap);
            }

            bigcanvas.drawBitmap(bmp, 0, iHeight, paint);
            iHeight += bmp.getHeight();

            bmp.recycle();
        }

        if (bigBitmap != null)
            outBigBms.add(bigBitmap);

        // tableLayout.restoreHierarchyState(stateArray);
        return outBigBms;
    }


    /**
     * Save bitmap to local filesystem
     *
     * @param bitmap   Bitmap to save
     * @param baseName Base filename used to save image, ex: "screenshot.png"
     * @return full filename path
     */
    private static Uri getUriForBitmap(Context context, Bitmap bitmap, String baseName) {

        try {
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            // context.grantUriPermission(mContext.getPackageName(), uri,
            //         Intent.FLAG_GRANT_WRITE_URI_PERMISSION + Intent.FLAG_GRANT_READ_URI_PERMISSION);
            OutputStream ostream = context.getContentResolver().openOutputStream(uri);

            // Jpeg format about 20x faster to export then PNG and smaller image.
            bitmap.compress(CompressFormat.JPEG, 90, ostream);
            ostream.close();
            return uri;
        } catch (Exception ex) {
            s_log.e("Save bitmap failed " , ex.getMessage());
        }
        return null;
    }


    /**
     * Share screen capture
     */
    public static void shareScreen(FragmentActivity activity, String what, ShareActionProvider shareActionProvider) {
        Bitmap screenBitmap = Utils.grabScreen(activity);
        List<Bitmap> bitmapList = new ArrayList<>();
        bitmapList.add(screenBitmap);
        shareList(activity, bitmapList, null, what, "screenshot.png", shareActionProvider);
        GoogleAnalyticsHelper.event(activity, "share", "screen", activity.getClass().getName());
    }

    public static void shareScreen(View view, String what, ShareActionProvider shareActionProvider) {
        Bitmap screenBitmap = getBitmap(view);
        List<Bitmap> bitmapList = new ArrayList<>();
        bitmapList.add(screenBitmap);
        shareList(view.getContext(), bitmapList, null, what, "screenshot.png", shareActionProvider);
    }

    public static String getScreenImagePath(View view, Activity activity) {
        Bitmap viewBitmap = getBitmap(view);
        return Images.Media.insertImage(activity.getContentResolver(), viewBitmap, "view.png", null);
    }

    private static boolean isBitmapValid(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled() && bitmap.getHeight() * bitmap.getWidth() > 0;
    }

    public static void shareList(
            Context context,
            List<Bitmap> shareImages,
            List<String> shareCsv,
            String what, String imageName,
            ShareActionProvider shareActionProvider) {

        final String IMAGE_TYPE = "image/png";
        final String TEXT_TYPE = "text/plain";
        Intent shareIntent;

        if (shareImages != null && shareImages.size() > 0) {
            int imgCnt = shareImages.size();
            shareIntent = new Intent(imgCnt == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType(IMAGE_TYPE);
            if (imgCnt == 1) {
                Bitmap bitmap = shareImages.get(0);
                if (!isBitmapValid(bitmap))
                    return;

                /*
                    // String screenImgFilename = Images.Media.insertImage(getContentResolver(), bitmap, imageName, null);
                    String screenImgFilename = Utils.saveBitmap(context, bitmap, imageName);

                    Uri uri = Uri.fromFile(new File(screenImgFilename));
                */
                Uri uri = Utils.getUriForBitmap(context, bitmap, imageName);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                bitmap.recycle();
            } else {
                ArrayList<Uri> uris = new ArrayList<>();
                for (int bmIdx = 0; bmIdx != shareImages.size(); bmIdx++) {
                    Bitmap bitmap = shareImages.get(bmIdx);
                    if (isBitmapValid(bitmap)) {
                        /*
                        String screenImgFilename = Utils.saveBitmap(context, bitmap, String.valueOf(bmIdx) + imageName);
                        Uri uri = Uri.fromFile(new File(screenImgFilename));
                        */
                        Uri uri = Utils.getUriForBitmap(context, bitmap, bmIdx + imageName);
                        uris.add(uri);
                        bitmap.recycle();
                    } else
                        s_log.e("invalid bitmap");
                }
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            }
        } else {
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(TEXT_TYPE);
        }

        String shareBody = String.format("%s v%s\n%s\n%s\n",
                GlobalInfo.s_globalInfo.appName,
                GlobalInfo.s_globalInfo.version,
                context.getString(R.string.websiteLanDenLabs),
                what);

        if (shareCsv != null && shareCsv.size() > 0) {
            shareBody += TextUtils.join("\n", shareCsv);
        }
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, GlobalInfo.s_globalInfo.appName + " " + what);
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

    	/*
        if (IMAGE_TYPE.equals(shareIntent.getMimeType())) {
        	shareActionProvider.setHistoryFileName(SHARE_IMAGE_HISTORY_FILE_NAME);
        } else if (TEXT_TYPE.equals(shareIntent.getMimeType())) {
        	shareActionProvider.setHistoryFileName(SHARE_TEXT_HISTORY_FILE_NAME);
        }
        */
        //	if (shareActionProvider != null) {
        //		shareActionProvider.setShareIntent(shareIntent);
        //	} else {
        GlobalInfo.s_globalInfo.mainFragActivity.startActivity(Intent.createChooser(shareIntent, "Share"));
        //	}

        GoogleAnalyticsHelper.event(GlobalInfo.s_globalInfo.mainFragActivity, "", "share-screen", imageName);
    }

    /*
     * http://www.truiton.com/2013/03/android-take-screenshot-programmatically-and-send-email/
     *
     * Need permission -
     * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
     */
    public static void sendMail(Activity activity, String path) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[]{"receiver@website.com"});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Truiton Test Mail");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                "This is an autogenerated mail from Truiton's InAppMail app");
        emailIntent.setType("image/png");
        Uri myUri = Uri.parse("file://" + path);
        emailIntent.putExtra(Intent.EXTRA_STREAM, myUri);
        activity.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        GoogleAnalyticsHelper.event(GlobalInfo.s_globalInfo.mainFragActivity, "", "share-email", activity.getClass().getName());
    }

    public static final int CLOCK_NOTIFICATION_ID = 1;

    public static void showAlarmNotification(Context context, int id, String msg) {
        s_log.i("Preparing to send notification...: " + msg);
        NotificationManager notificationManager = SysUtils.getServiceSafe(context, Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, DevToolActivity.class), FLAG_IMMUTABLE);

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.dev_tool_ic);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, "Alarm")
                        .setContentTitle("DevTool Alarm")
                        .setSmallIcon(R.drawable.dev_tool)
                        .setLargeIcon(largeIcon)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                        .setContentText(msg);

        notificationBuilder.setContentIntent(contentIntent);
        notificationManager.notify(id, notificationBuilder.build());

        s_log.i("Notification sent.");
    }

    public static void cancelNotification(Context context, int id) {
        s_log.i("Cancel notification");
        NotificationManager notificationManager = SysUtils.getServiceSafe(context, Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    // =============================================================================================
    // Web

    /**
     * Load asset file.
     *
     * @param inFile Asset file name.
     * @return String with asset contents.
     */
    public static String LoadData(Context context, String inFile) {
        String tContents = "";

        try {
            InputStream stream = context.getAssets().open(inFile);

            int size = stream.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (IOException ignore) {
            // Handle exceptions here
        }

        return tContents;
    }
}
