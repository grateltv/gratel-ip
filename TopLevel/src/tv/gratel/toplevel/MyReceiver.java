package tv.gratel.toplevel;

import tv.gratel.toplevel.TopLevelActivity;
import android.content.BroadcastReceiver;  
import android.content.Context;  
import android.content.Intent;  
import android.net.ConnectivityManager;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
  
public class MyReceiver extends BroadcastReceiver {  
  
    @Override  
    public void onReceive(Context context, Intent intent) {  
        Intent i = new Intent(context, TopLevelActivity.class);  
    	//i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	//i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("isRinging", "1");
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | 
        	    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        //while (!isOnline(context)) {
        	
        	/*Handler handler = new Handler(); 
            handler.postDelayed(new Runnable() { 
                 public void run() { 
                      int i=1;
                      i+=1;
                 } 
            }, 2000); */
        	
        //}
        Log.v("MyReceiver", "..... in onReceive() intent.getAction(): "+intent.getAction());
        
        /*
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            Log.v("MyReceiver", "-----in onReceive() event.getKeyCode():"+event.getKeyCode());
            if (KeyEvent.KEYCODE_VOLUME_DOWN == event.getKeyCode()) {
                // Handle key press.
            }
        }
        */
        Log.v("MyReceiver", "..... in onReceive() intent.getStringExtra(TelephonyManager.EXTRA_STATE): "+intent.getStringExtra(TelephonyManager.EXTRA_STATE));
        Toast.makeText(context,""+intent.getStringExtra(TelephonyManager.EXTRA_STATE), Toast.LENGTH_LONG).show();
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {

                // Phone number 
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                Log.v("MyReceiver", "..... incomingNumber: "+incomingNumber);
                Toast.makeText(context,"["+intent.getStringExtra(TelephonyManager.EXTRA_STATE)+"] входящий:"+incomingNumber, Toast.LENGTH_LONG).show();
                //i.putExtra("strRing", "1");
                TopLevelActivity.isRing = incomingNumber;
                context.startActivity(i);  


                // Ringing state
                // This code will execute when the phone has an incoming call
        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
        //} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {

                 // This code will execute when the call is answered or disconnected
	            //Toast.makeText(context,"["+intent.getStringExtra(TelephonyManager.EXTRA_STATE)+"] входящий: "+TopLevelActivity.isRing, Toast.LENGTH_LONG).show();
	            context.startActivity(i);
	            TopLevelActivity.isRing = null;
        }
        else{
        	TopLevelActivity.isRing = null;
        }
        //context.startActivity(i);  
    }  
    
    public boolean isOnline(Context context) {
        boolean var = false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if ( cm.getActiveNetworkInfo() != null ) {
            var = true;
        }
        return var;
    } 
}     