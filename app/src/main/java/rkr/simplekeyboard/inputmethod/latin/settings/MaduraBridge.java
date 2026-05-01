package rkr.simplekeyboard.inputmethod.latin.settings;

import android.content.Context;
import android.content.SharedPreferences;
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;
import java.util.HashMap;
import java.util.Map;

/**
 * MaduraBridge - Jembatan komunikasi antara Flutter dan Native Android.
 */
public class MaduraBridge {
    private final Context mContext;
    private final SharedPreferences mPrefs;

    public MaduraBridge(Context context) {
        mContext = context;
        mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context);
    }

    // Mendapatkan statistik penggunaan untuk ditampilkan di UI Canggih Flutter
    public Map<String, Object> getMaduraStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_words", mPrefs.getInt("madura_total_words", 0));
        stats.put("corrected_words", mPrefs.getInt("madura_corrected_count", 0));
        stats.put("accuracy", 98.5); // Contoh statis
        return stats;
    }

    // Update pengaturan dari Flutter
    public void updateSetting(String key, Object value) {
        SharedPreferences.Editor editor = mPrefs.edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
        editor.apply();
    }
}
