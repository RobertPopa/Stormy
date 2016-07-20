package robertpopa.com.stormy.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Locale;

/**
 * Created by RobertP on 20/07/16.
 */
public class StormyUtils {
    private Context mContext;

    public StormyUtils(Context context) {
        mContext = context;
    }

    public boolean checkNetwork() {
        ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
        if ( activeNetworkInfo != null && activeNetworkInfo.isConnected() ) {
            return true;
        } else {
            return false;
        }
    }

    public static String getUserLanguage() {
        return Locale.getDefault().getLanguage();
    }
}
