package com.prestigio.launcher.mdm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.Collator;
import java.util.*;

/**
 * Created by yanis on 25.3.14.
 */
public class AllAppsActivity extends Activity {

    public static final int DEFAULT_APPLICATIONS_NUMBER = 42;
    private ArrayList<ComponentName> dataComponents = new ArrayList<ComponentName>(DEFAULT_APPLICATIONS_NUMBER);
    private HashMap<String, String> xmlApps = new HashMap<String, String>();
    private HashMap<Object, CharSequence> mLabelCache;
    private IconCache mIconCache;
    private List<ResolveInfo> apps = null;
    private volatile int mWidth = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_apps_activity);



        String app_list = getIntent().getStringExtra("app_list");
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            boolean tagFoundInfo = false;
            boolean tagFoundCommunication = false;
            xpp.setInput( this.getAssets().open("apps.xml"), null );
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {
                    //System.out.println("Start document");
                } else if(eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName() != null && xpp.getName().equals("info")){
                        tagFoundInfo = true;
                        tagFoundCommunication = false;
                    } else
                    if (xpp.getName() != null && xpp.getName().equals("communication")){
                        tagFoundInfo = false;
                        tagFoundCommunication = true;
                    } else
                    if (xpp.getName() != null && xpp.getName().equals("application")){
                        if ( (app_list.equals("info") && tagFoundInfo) || (app_list.equals("communication") && tagFoundCommunication)) {
                            xmlApps.put(xpp.getAttributeValue(null, "className"), xpp.getAttributeValue(null, "packageName"));
                        }
                    }
                }
                eventType = xpp.next();
            }

        } catch (XmlPullParserException ignored) {
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        }

        final PackageManager packageManager = getPackageManager();

        mIconCache = new IconCache(this);

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        apps = packageManager.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA | PackageManager.GET_CONFIGURATIONS | PackageManager.PERMISSION_GRANTED);

        ArrayList<ResolveInfo> filtered_apps = new ArrayList<ResolveInfo>();
        for (int i=0; i < apps.size(); i++) {
            String value = xmlApps.get(apps.get(i).activityInfo.name);
            if (value != null && value.equals(apps.get(i).activityInfo.applicationInfo.packageName) ) {
                filtered_apps.add(apps.get(i));
            }
        }
        apps = filtered_apps;


        mLabelCache = new HashMap<Object, CharSequence>();
        Collections.sort(apps, new ShortcutNameComparator(packageManager, mLabelCache));

        for (int i=0; i < apps.size(); i++) {
            add(packageManager, apps.get(i), mIconCache, mLabelCache);
        }

        final GridView gridView = (GridView) findViewById(R.id.gridview);
        final GridAdapter gridadapter = new GridAdapter(this);


        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(dataComponents.get(position));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        gridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mWidth = gridView.getWidth();
                gridView.setColumnWidth(mWidth / 4 - 4);
                gridView.setAdapter(gridadapter);
                gridView.invalidate();

                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                    gridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                else
                    gridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void add(PackageManager packageManager, ResolveInfo info, IconCache mIconCache, HashMap<Object, CharSequence> mLabelCache) {
        ComponentName componentName = new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
        if (findActivity(dataComponents, componentName)) {
            return;
        }
        dataComponents.add(componentName);
        //added.add(info);
    }

    private static boolean findActivity(ArrayList<ComponentName> apps, ComponentName component) {
        final int N = apps.size();
        for (int i=0; i<N; i++) {
            final ComponentName info = apps.get(i);
            if (info.equals(component)) {
                //Log.d("MdmLauncher", info.getPackageName() + ", " + component.getPackageName());
                return true;
            }
        }
        return false;
    }


    public class GridAdapter extends BaseAdapter {

        class ViewHolder {
            RelativeLayout relative;
            TextView textView;
            ImageView imageView;
            int imageWidth = 0;
            int imageHeight = 0;
            int width = 0;
            int height = 0;
        }

        private Context context;

        public GridAdapter(Context c) {
            context = c;
        }

        public int getCount() {
            return dataComponents.size();
        }

        public Object getItem(int position) {
            return dataComponents.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                holder = new ViewHolder();
                holder.relative = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.app_icon, parent, false);
                holder.textView = (TextView) holder.relative.findViewById(R.id.text_view);
                holder.imageView = (ImageView) holder.relative.findViewById(R.id.image_view);
                //holder.textView.setLayoutParams(new GridView.LayoutParams(50, 50));
                //iview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                //holder.textView.setPadding(2,2,2,2);

                view = holder.relative;
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            ApplicationInfo appInfo = new ApplicationInfo();
            appInfo.componentName = dataComponents.get(position);
            mIconCache.getTitleAndIcon(appInfo, apps.get(position), mLabelCache);

            holder.textView.setText(appInfo.title);

            if ((holder.imageWidth == 0 || holder.imageHeight == 0) && mWidth > 0) {
                holder.width = mWidth / 4;
                holder.height = holder.width;

                holder.textView.measure(0, 0);
                holder.imageHeight = holder.height - 33;//holder.textView.getMeasuredHeight();
                holder.imageWidth = holder.width;
            }

            if (mWidth == 0){
                holder.textView.invalidate();
            } else {
                //long tm = System.currentTimeMillis();
                holder.relative.setMinimumWidth(holder.width);
                holder.relative.setMinimumHeight(holder.height);

                holder.imageView.setImageBitmap(appInfo.iconBitmap);

                /*RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(holder.imageWidth, holder.imageHeight);
                holder.imageView.setLayoutParams(parms);*/

                holder.imageView.getLayoutParams().width = holder.imageWidth;
                holder.imageView.getLayoutParams().height = holder.imageHeight;

                //holder.textView.measure(View.MeasureSpec.makeMeasureSpec(holder.imageWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(holder.imageHeight, View.MeasureSpec.EXACTLY));
                //Bitmap scaledBitmap = Bitmap.createScaledBitmap(appInfo.iconBitmap, holder.imageWidth, holder.imageHeight, true);
                //holder.textView.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(scaledBitmap), null, null);
                //Log.d("MdmLauncher", "init item: " + (System.currentTimeMillis() - tm));


                /*holder.imageView.setMaxWidth(100);
                holder.imageView.setMaxHeight(100);*/
            }

            return view;
        }
    }

    public static class ShortcutNameComparator implements Comparator<ResolveInfo> {

        static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
            if (info.activityInfo != null) {
                return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            } else {
                return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
            }
        }

        private Collator mCollator;
        private PackageManager mPackageManager;
        private HashMap<Object, CharSequence> mLabelCache;
        ShortcutNameComparator(PackageManager pm) {
            mPackageManager = pm;
            mLabelCache = new HashMap<Object, CharSequence>();
            mCollator = Collator.getInstance();
        }
        ShortcutNameComparator(PackageManager pm, HashMap<Object, CharSequence> labelCache) {
            mPackageManager = pm;
            mLabelCache = labelCache;
            mCollator = Collator.getInstance();
        }
        public final int compare(ResolveInfo a, ResolveInfo b) {
            CharSequence labelA, labelB;
            ComponentName keyA = getComponentNameFromResolveInfo(a);
            ComponentName keyB = getComponentNameFromResolveInfo(b);
            if (mLabelCache.containsKey(keyA)) {
                labelA = mLabelCache.get(keyA);
            } else {
                labelA = a.loadLabel(mPackageManager).toString();

                mLabelCache.put(keyA, labelA);
            }
            if (mLabelCache.containsKey(keyB)) {
                labelB = mLabelCache.get(keyB);
            } else {
                labelB = b.loadLabel(mPackageManager).toString();

                mLabelCache.put(keyB, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    };
}