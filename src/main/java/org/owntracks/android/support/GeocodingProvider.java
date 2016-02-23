package org.owntracks.android.support;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import org.owntracks.android.messages.MessageLocation;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class GeocodingProvider {
    private static final String TAG = "GeocodingProvider";
    private static Context context;
    private static final Double RUN_FIRST = 1d;
    private static final Double RUN_SECOND = 2d;

    public static void resolve(MessageLocation m) {
        MessageLocationResolverTask.execute(m, RUN_FIRST);
    }

    private static class MessageLocationResolverTask extends ResolverTask {
        public static void execute(MessageLocation m, double run) {
            (new MessageLocationResolverTask(m)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, m.getLatitude(), m.getLongitude(), run);
        }

        final WeakReference<MessageLocation> message;
        public MessageLocationResolverTask(MessageLocation m) {
            this.message = new WeakReference<MessageLocation>(m);
        }

        @Override
        protected void onPostExecute(String result) {
            // Retry once if request timed out or we didn't get a result for some temporary reason
            if(result == null && run.equals(RUN_FIRST)) {
                MessageLocationResolverTask.execute(message.get(), RUN_SECOND);
                return;
            }


            MessageLocation m = message.get();
            if(m!=null)
                m.setGeocoder(result);
        }
    }


    private abstract static class ResolverTask extends AsyncTask<Double, Void, String> {
        protected Double lat;
        protected Double lon;
        protected Double run;

        protected abstract void onPostExecute(String result);


        @Override
        protected String doInBackground(Double... params) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            lat = params[0];
            lon = params[1];
            run = params[2];

            if(!geocoder.isPresent()) {
               Log.e(TAG, "geocoder is not present");
                return null;
            }

            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(lat, lon, 1);
                if ((addresses != null) && (addresses.size() > 0)) {
                    StringBuffer g = new StringBuffer();
                    if (addresses.get(0).getAddressLine(0) != null)
                        g.append(addresses.get(0).getAddressLine(0)).append(", ");
                    if (addresses.get(0).getLocality() != null)
                        g.append(addresses.get(0).getLocality());
                    else if (addresses.get(0).getCountryName() != null)
                        g.append(addresses.get(0).getCountryName());
                    return g.toString();
                } else {
                    return "not available";
                }
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static void initialize(Context c){
        context = c;
    }



}
