package jlp.sim.Modelos;

/**
 * Created by aerocool on 29/07/17.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Autoarranque extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context,  ServicioNotificacion.class);
        context.startService(service);
    }

}