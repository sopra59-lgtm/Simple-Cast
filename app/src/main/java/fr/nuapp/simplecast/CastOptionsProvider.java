package fr.nuapp.simplecast;
import android.content.Context;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import java.util.List;
public final class CastOptionsProvider implements OptionsProvider {
    public static final String NAMESPACE = "urn:x-cast:fr.nuapp.simplecast";
    @Override public CastOptions getCastOptions(Context context) {
        String id = BuildConfig.CAST_RECEIVER_ID;
        if (id == null || id.isEmpty()) id = "CC1AD845"; // Discovery only; default receiver cannot display custom frames.
        return new CastOptions.Builder().setReceiverApplicationId(id).build();
    }
    @Override public List<SessionProvider> getAdditionalSessionProviders(Context context) { return null; }
}
