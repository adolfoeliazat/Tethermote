package com.azi.tethermote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 */
public class TetherRemoteWidget extends AppWidgetProvider {
    public static final String SWITCH_ACTION = "com.azi.tethermote.TetherRemoteWidget.SWITCH_ACTION";
    public static final String SWITCH_STATE = "com.azi.tethermote.TetherRemoteWidget.SWITCH_STATE";
    //private static final String SWITCH_WIDGET_ID = "com.azi.tethermote.TetherRemoteWidget.SWITCH_WIDGET_ID";
    private static final int[] imageIds = new int[]{
            R.drawable.widget_off_sel,
            R.drawable.widget_on_sel,
            R.drawable.widget_bet
    };

    private static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                        final int appWidgetId, final int state) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Construct the RemoteViews object
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tether_remote_widget);

                Intent switchIntent = new Intent(context, TetherRemoteWidget.class);
                switchIntent.setAction(SWITCH_ACTION);
                switchIntent.putExtra(SWITCH_STATE, state == WirelessTools.TETHERING_DISABLED ? WirelessTools.TETHERING_ENABLED : WirelessTools.TETHERING_DISABLED);

                PendingIntent switchPendingIntent = PendingIntent.getBroadcast(context, 0, switchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.imageButton, switchPendingIntent);

                views.setInt(R.id.imageButton, "setImageResource", imageIds[state]);

                // Instruct the widget_off manager to update the widget_off

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        });
    }

    public static void updateWidgets(Context context, int newState) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, TetherRemoteWidget.class));

        for (int appWidgetId : appWidgetIds) {
            TetherRemoteWidget.updateAppWidget(context, manager, appWidgetId, newState);
        }

        SwitchNotification.Check(context, newState != WirelessTools.TETHERING_ENABLED);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(SWITCH_ACTION)) {
            String address = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("remote_device", "");
            if (address.isEmpty()) {
                ShowSettings(context);
            } else {

                final int state = intent.getIntExtra(SWITCH_STATE, WirelessTools.TETHERING_DISABLED);

                if (state == WirelessTools.TETHERING_ENABLED) {
                    Toast.makeText(context, context.getString(R.string.enabling), Toast.LENGTH_SHORT).show();
                } else if (state == WirelessTools.TETHERING_DISABLED) {
                    Toast.makeText(context, context.getString(R.string.disabling), Toast.LENGTH_SHORT).show();
                }

                new AsyncTask<Integer, Void, Integer>() {
                    @Override
                    protected void onPreExecute() {
                        updateWidgets(context, WirelessTools.TETHERING_STATE);
                    }

                    @Override
                    protected Integer doInBackground(Integer... params) {
                        int state = params[0];
                        return WirelessTools.enableRemoteTethering(context, state == WirelessTools.TETHERING_ENABLED);
                    }

                    @Override
                    protected void onPostExecute(Integer newState) {
                        if (newState == WirelessTools.TETHERING_ERROR) {
                            newState = WirelessTools.TETHERING_STATE;
                            AlertDialog alert = new AlertDialog.Builder(context, R.style.AlertTheme)
                                    .setTitle(R.string.bluetooth_error)
                                    .setMessage(R.string.bluetooth_device_not_accessible_notification)
                                    .setCancelable(true)
                                    .create();
                            alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            alert.show();
                        }
                        updateWidgets(context, newState);
                    }
                }.execute(state);
            }
        }
        super.onReceive(context, intent);
    }

    private void ShowSettings(Context context) {
        Intent settingsIntent = new Intent(context, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(settingsIntent);
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, WirelessTools.TETHERING_STATE);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int state = WirelessTools.getRemoteTetherState(context);
                if (state == WirelessTools.TETHERING_ERROR) state = WirelessTools.TETHERING_STATE;
                // There may be multiple widgets active, so update all of them
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, state);
                }
            }
        }).start();
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget_off is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget_off is disabled
    }
}

