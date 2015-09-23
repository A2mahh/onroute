package com.prestigio.launcher.mdm;

import android.app.ListActivity;
import android.app.ListFragment;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created by yanis on 28.3.14.
 */
public class LanguagePreferenceFragment extends ListFragment {

    public static class LocaleInfo implements Comparable<LocaleInfo> {
        static final Collator sCollator = Collator.getInstance();

        String label;
        Locale locale;

        public LocaleInfo(String label, Locale locale) {
            this.label = label;
            this.locale = locale;
        }

        public String getLabel() {
            return label;
        }

        public Locale getLocale() {
            return locale;
        }

        @Override
        public String toString() {
            return this.label;
        }

        @Override
        public int compareTo(LocaleInfo another) {
            return sCollator.compare(this.label, another.label);
        }
    }

    /*@Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }*/

    private static String getDisplayName(Locale l, String[] specialLocaleCodes, String[] specialLocaleNames) {
        String code = l.toString();

        for (int i = 0; i < specialLocaleCodes.length; i++) {
            if (specialLocaleCodes[i].equals(code)) {
                return specialLocaleNames[i];
            }
        }

        return l.getDisplayName(l);
    }

    private static String toTitleCase(String s) {
        if (s.length() == 0) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private ArrayAdapter<LocaleInfo> constructAdapter(Context context, int layoutId, int fieldId/*, String cur_language*/) {
        final Resources resources = context.getResources();

        final String[] locales = Resources.getSystem().getAssets().getLocales();

        /*final String[] specialLocaleCodes = resources
                .getStringArray(R.array.special_locale_codes);
        final String[] specialLocaleNames = resources
                .getStringArray(R.array.special_locale_names);*/

        Arrays.sort(locales);
        final int origSize = locales.length;
        final LocaleInfo[] preprocess = new LocaleInfo[origSize];
        int finalSize = 0;
        int curr = 0;

        for (int i = 0; i < origSize; i++) {
            final String s = locales[i];

            final int len = s.length();
            if (len == 5) {
                String language = s.substring(0, 2);
                String country = s.substring(3, 5);

                final Locale l = new Locale(language, country);

                if (finalSize == 0) {

                    preprocess[finalSize++] = new LocaleInfo(toTitleCase(l.getDisplayLanguage(l)), l);
                } else {
                    if (preprocess[finalSize - 1].locale.getLanguage().equals(
                            language)) {

                        preprocess[finalSize - 1].label = toTitleCase(getDisplayName(
                                preprocess[finalSize - 1].locale,
                                new String[]{}, new String[]{}));

                        preprocess[finalSize++] = new LocaleInfo(
                                toTitleCase(getDisplayName(l,
                                        new String[]{}, new String[]{})),
                                l);
                    } else {
                        String displayName;
                        if (s.equals("zz_ZZ")) {
                            displayName = "Pseudo...";
                        } else {
                            // displayName = conf.locale.getDisplayName(l);
                            displayName = toTitleCase(l.getDisplayLanguage(l));
                        }
                        preprocess[finalSize++] = new LocaleInfo(displayName, l);
                    }
                }
            }
        }

        final LocaleInfo[] localeInfos = new LocaleInfo[finalSize];

        for (int i = 0; i < finalSize; i++) {
            localeInfos[i] = preprocess[i];
        }

        Arrays.sort(localeInfos);

        /*for (int i = 0; i < localeInfos.length; i++) {
            if (localeInfos[i].getLocale().getLanguage().equals(cur_language) && curr == 0)
                curr = i;
        }*/

        return new ArrayAdapter<LocaleInfo>(context, android.R.layout.simple_list_item_1, localeInfos);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayAdapter<LocaleInfo> adapter = constructAdapter(getActivity(), 0, 0);

        setListAdapter(adapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                try {
                    updateLocale(((LocaleInfo) parent.getItemAtPosition(position)).getLocale());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                getActivity().onBackPressed();
            }
        };

        getListView().setOnItemClickListener(itemListener);
    }

    public void updateLocale(Locale locale)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, RemoteException {

        if (locale == null)
            return;
        Class amnClass = Class.forName("android.app.ActivityManagerNative");
        Object amn = null;
        Configuration config = null;
        // //android.app.ActivityManagerNative.getDefault().getConfiguration();

        // amn = ActivityManagerNative.getDefault();
        Method methodGetDefault = amnClass.getMethod("getDefault");
        methodGetDefault.setAccessible(true);
        amn = methodGetDefault.invoke(amnClass);

        // config = amn.getConfiguration();
        Method methodGetConfiguration = amnClass.getMethod("getConfiguration");
        methodGetConfiguration.setAccessible(true);
        config = (Configuration) methodGetConfiguration.invoke(amn);

        // config.userSetLocale = true;
        Class configClass = config.getClass();
        Field f = configClass.getField("userSetLocale");
        f.setBoolean(config, true);

        // set the locale to the new value
        config.locale = locale;

        // Locale.setDefault(locale);

        // amn.updateConfiguration(config);
        Method methodUpdateConfiguration = amnClass.getMethod("updateConfiguration", Configuration.class);
        methodUpdateConfiguration.setAccessible(true);
        methodUpdateConfiguration.invoke(amn, config);

        BackupManager.dataChanged("com.android.providers.settings");
    }


}
