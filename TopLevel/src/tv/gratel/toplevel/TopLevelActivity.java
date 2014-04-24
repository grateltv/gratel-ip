package tv.gratel.toplevel;

import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Contacts.Data;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;  
import android.widget.Button;  
import android.widget.EditText;
import android.widget.GridView;  
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
 


public class TopLevelActivity extends Activity {
	
public ArrayList<String> arrNumbers = new ArrayList<String>();
public ArrayList<String> arrNames = new ArrayList<String>();
public ArrayList<String> arrBoxes = new ArrayList<String>();
public ArrayList<String> arrButNumbers = new ArrayList<String>();
public ArrayList<String> arrButNames = new ArrayList<String>();


private static final String TAG = "TopLevelActivity";

public final static String PARAM_PINTENT = "pendingIntent";
public final static int STATUS_REFRESH = 100;
final int TASK1_CODE = 1;
public static String OUR_OTDELENIE = "1";
public static String XML_PATH = "smb://192.168.100.254/shara/gtv.xml";
public static String XML_PATH2 = "smb://192.168.100.254/shara/gtv_b.xml";
public static final String APP_PREFERENCES = "mysettings"; 
public static long lastMod = -1, lastMod2 = -1;
SharedPreferences mSettings;
public static String isRing;
    
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.e(TAG, "=============================================================");        
    Log.e(TAG, "======================== ON CREATE ==========================");        
    Log.e(TAG, "=============================================================");        
    requestWindowFeature(Window.FEATURE_NO_TITLE); //скрываем заголовок
    setContentView(R.layout.main);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
    WindowManager.LayoutParams.FLAG_FULLSCREEN); //убираем title-bar
    mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
	restoreSettings();
	checkContacts();
	
    GridView gridview = (GridView) findViewById(R.id.gridview);  
	gridview.setAdapter(new ButtonAdapter(this));  
	 
	
	gridview.setOnKeyListener(new android.view.View.OnKeyListener() {
		@Override
        public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
            if (arg2.getAction() == KeyEvent.ACTION_DOWN) {
            	
            	Toast.makeText(getBaseContext(),"+++key:"+arg1, Toast.LENGTH_SHORT).show();
                if (arg1 == KeyEvent.KEYCODE_DPAD_CENTER || arg1 == KeyEvent.KEYCODE_MENU) {
                	getPreferences();
                	startService();
                    return true;
                }
            }
            return false;
        }
    });
	
	
	startService();
	loadContacts();
	
}

public final String POPUP_LOGIN_TITLE="Настройки";
public final String POPUP_LOGIN_TEXT="Введите номер отделения и пути к XML-файлам";
//public final String OTDELENIE_HINT=OUR_OTDELENIE;
//public final String XML_HINT=XML_PATH;

private void getPreferences(){
	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    alert.setTitle(POPUP_LOGIN_TITLE);
    alert.setMessage(POPUP_LOGIN_TEXT);
    // Set an EditText view to get user input 
    final EditText ourotdelenie = new EditText(this);
    //ourotdelenie.setHint(OTDELENIE_HINT);
    ourotdelenie.setText(OUR_OTDELENIE);
    final EditText xmlpath1 = new EditText(this);
    final EditText xmlpath2 = new EditText(this);
    //xmlpath.setHint(XML_HINT);
    xmlpath1.setText(XML_PATH);
    xmlpath2.setText(XML_PATH2);
    LinearLayout layout = new LinearLayout(getApplicationContext());
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.addView(ourotdelenie);
    layout.addView(xmlpath1);
    layout.addView(xmlpath2);
    alert.setView(layout);

    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int whichButton) {
    	   Editable value = ourotdelenie.getText();
    	   Editable value1 = xmlpath1.getText();
    	   Editable value2 = xmlpath2.getText();
  		  // Получили значение введенных данных!
  		  
  		  OUR_OTDELENIE = value.toString();
  		  XML_PATH = value1.toString();
  		  XML_PATH2 = value2.toString();
  		  // Сохраним настройки
  		  Editor editor = mSettings.edit();
  		  editor.putString("XML_PATH2", XML_PATH2);
  		  editor.putString("XML_PATH", XML_PATH);
  		  editor.putString("OUR_OTDELENIE", OUR_OTDELENIE);
  			
  		  editor.commit();
  		  Log.i(TAG, "..getPreferences commit...");
  		  startService();
      }
    });

    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        // Canceled.
      }
    });
    alert.show();
}
	
	

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  super.onActivityResult(requestCode, resultCode, data);
  Log.d(TAG, "requestCode = " + requestCode + ", resultCode = " + resultCode);

  // Ловим сообщения об обновлении контактов
  if (resultCode == STATUS_REFRESH) {
	loadContacts();
	saveSettings();
	/*switch (requestCode) {
    case TASK1_CODE:
      tvTask1.setText("Task1 start");
      break;
    case TASK2_CODE:
      tvTask2.setText("Task2 start");
      break;
    case TASK3_CODE:
      tvTask3.setText("Task3 start");
      break;
    }*/
	  
  }

}

private void startService() {
    Log.v(TAG, "Starting service... ");
    Intent myintent = new Intent();
    PendingIntent pi= createPendingResult(TASK1_CODE, myintent, 0);
    Intent intent = new Intent(TopLevelActivity.this, BackgroundService.class);
    intent.putExtra(XML_PATH, XML_PATH);
    intent.putExtra(XML_PATH2, XML_PATH2);
    intent.putExtra(OUR_OTDELENIE, OUR_OTDELENIE);
    intent.putExtra(PARAM_PINTENT, pi);
    startService(intent);
}

/*
private void stopService() {
	Log.v(TAG, "Stopping service...");
    if(stopService(new Intent(TopLevelActivity.this,
                BackgroundService.class)))
    	Log.v(TAG, "stopService was successful");
    else
    	Log.v(TAG, "stopService was unsuccessful");
}*/

private void restoreSettings(){
	// если ли нужный нам ключ
	if (mSettings.contains("XML_PATH")) 
		XML_PATH = mSettings.getString("XML_PATH", "");
	if (mSettings.contains("XML_PATH2")) 
		XML_PATH2 = mSettings.getString("XML_PATH2", "");
	if (mSettings.contains("OUR_OTDELENIE")) 
		OUR_OTDELENIE = mSettings.getString("OUR_OTDELENIE", "0");
	if (mSettings.contains("LAST_MOD")) 
		lastMod = mSettings.getLong("LAST_MOD", -1);
	if (mSettings.contains("LAST_MOD2")) 
		lastMod2 = mSettings.getLong("LAST_MOD2", -1);
	
	Log.v(TAG, "restoreSettings: lastMod="+lastMod+ "  lastMod2="+lastMod2+" XML_PATH="+XML_PATH);
}

public void saveSettings(){
	Editor editor = mSettings.edit();
	editor.putLong("LAST_MOD", lastMod);
	editor.putLong("LAST_MOD2", lastMod2);
	editor.putString("XML_PATH", XML_PATH);
	editor.putString("XML_PATH2", XML_PATH2);
	editor.putString("OUR_OTDELENIE", OUR_OTDELENIE);
	editor.commit();
	
	Log.v(TAG, "saveSettings: lastMod="+lastMod+ "  lastMod2="+lastMod2);
}

private boolean resumeHasRun = false;

@Override
protected void onResume() {
    super.onResume();
    /*
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
        String strRing = extras.getString("strRing");
        Toast.makeText(getBaseContext(),"---onResume() isRing:"+strRing, Toast.LENGTH_LONG).show();
    }
    else
        Toast.makeText(getBaseContext(),"Extras is null", Toast.LENGTH_LONG).show();
*/
    
    //isRing = this.getIntent().getBooleanExtra("isRinging",false);
    //String str = this.getIntent().getAction();
    //Toast.makeText(getBaseContext(),"---onResume() isRing:"+isRing, Toast.LENGTH_LONG).show();
    
    if (!resumeHasRun) {
        resumeHasRun = true;
        return;
    }
    // Normal case behavior follows
    Log.d(TAG,"+++onResume+++ isRing="+isRing);
    GridView gridview = (GridView) findViewById(R.id.gridview);  
	gridview.invalidateViews();
    //restoreSettings();
    //loadContacts();
}



@Override
protected void onPause() {
	// TODO Auto-generated method stub
	super.onPause();

	saveSettings();
}

public void checkContacts(){
	int count = 0;
	try{
		ContentResolver cr = getContentResolver();
	    Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, "display_name");
		   
	    String[] projection = new String[] {ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, 
	     		ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, 
	     		ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME };
	    count = cur.getCount();
	}catch(Exception ex) {ex.printStackTrace();}
	
    Log.e(TAG,"::checkContacts: Contacts count="+count);
    if (count == 0) {
    	getPreferences();
    	//startService();
    	lastMod=-1;
    	lastMod2=-1;
    	saveSettings();
    	Log.e(TAG,"::checkContacts: need for load");
    }
}

public void loadContacts(){
	Log.e(TAG, ":: loadContacts() ::");
	ContentResolver cr = getContentResolver();
    Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, "display_name");
	   
    String[] projection = new String[] {ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, 
     		ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, 
     		ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME };
    
    if (cur.getCount() > 0) {
    	arrNumbers.clear();
		arrNames.clear();
		arrBoxes.clear();
        while (cur.moveToNext()) {
              String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
              String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
              if (Integer.parseInt(cur.getString( cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                 Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{id}, null);
                 while (pCur.moveToNext()) {
                     String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                     String phoneType = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                     if(Integer.parseInt(phoneType) == ContactsContract.CommonDataKinds.Phone.TYPE_MAIN){
                    	 arrNumbers.add(phoneNo);
                    	 //Log.i("--loadContacts", "Name: " + name + ", Phone No: " + phoneNo + " phoneType: "+phoneType);
                     }
                 }
                 pCur.close();
                  
                 String where = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?"; 
                 String[] whereParameters = new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, id};

                 //Request
                 Cursor contacts = getContentResolver().query( ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, 
                		 ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
                 String Name="", BoxName="";
                 while (contacts.moveToNext())
         		 { 
         				BoxName=contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
         				Log.e(TAG, " -- BoxName:"+BoxName);
         				//Name=contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
         				try{
         					String wrapperStr = contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
         					//Log.i(TAG, " -- wrapperStr:"+wrapperStr);
         					if (wrapperStr!=null){
		         				
			         				//Map <Integer,String> myMap = new HashMap<Integer,String>();
			         				Gson gson = new Gson();
			         				MapWrapper wrapper = gson.fromJson(wrapperStr, MapWrapper.class);
			         				HashMap<Integer, String> MyMap = wrapper.getMyMap();
			         				
			         				Iterator<java.util.Map.Entry<Integer, String>> iterator = MyMap.entrySet().iterator();
			         				Name="";
			         				while (iterator.hasNext()) {
			         				    Map.Entry<Integer,String> pairs = (Map.Entry<Integer,String>)iterator.next();
			         				    String value =  pairs.getValue();
			         				    Integer Key = pairs.getKey();
			         				    Log.e(TAG, " -"+Key+": --->"+value);
			         				    Name += value+"<br>";
			         				    
				         				}
         					}
         				}catch(Exception ex) {ex.printStackTrace();}
         				
         				
        				arrNames.add(Name);
        				arrBoxes.add(BoxName);
        				
         			    Log.d("...", " Name: "+Name+" BoxName:"+BoxName);
         			    
         		 }
                 
                 contacts.close();
            }
        }
    }
    GridView gridview = (GridView) findViewById(R.id.gridview);  
	gridview.invalidateViews();
	
}
/*
public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);  
}

public static String padLeft(String s, int n) {
   return String.format("%1$" + n + "s", s);  
}
*/
public  int getContactIDFromNumber(String contactNumber)
{
    contactNumber = Uri.encode(contactNumber);
    int phoneContactID = 0;
    //String displayname="";
    Cursor contactLookupCursor = getContentResolver().query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,Uri.encode(contactNumber)),new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup._ID}, null, null, null);
    
    //Log.e(TAG, "+++ getContactIDFromNumber n: "+contactLookupCursor.getCount());    
    if (contactLookupCursor.getCount()>1){
    	Log.e(TAG, "+++ getContactIDFromNumber ОБНАРУЖЕНА НЕОДНОЗНАЧНОСТЬ В НОМЕРАХ. Повторение номера "+contactNumber);
    	Toast.makeText(getBaseContext(),"ОБНАРУЖЕНА НЕОДНОЗНАЧНОСТЬ В НОМЕРАХ. Повторение номера "+contactNumber, Toast.LENGTH_LONG).show();
    	return 0;
    }
    	
    while(contactLookupCursor.moveToNext()){
        phoneContactID = contactLookupCursor.getInt(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup._ID));
        Log.e(TAG, "+++ getContactIDFromNumber id: "+phoneContactID); 
        //displayname = contactLookupCursor.getString(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
    }
    contactLookupCursor.close();
    //Log.e(TAG, "+++ updateContact id="+phoneContactID+" displayname="+displayname);    
    return phoneContactID;
}


public class MapWrapper {
	  private HashMap<Integer, String> myMap;
	 /* public void MapWrapper(){
		  myMap = new HashMap();
	  }*/
	  // getter and setter for 'myMap'
	  public void setMyMap(Map<Integer, String> mp){
		  myMap = (HashMap<Integer, String>) mp;
	  }
	  
	  public HashMap<Integer, String> getMyMap(){
		  return myMap;
	  }
	  
	  
	}



public class ButtonAdapter extends BaseAdapter {
	 private Context mContext;

	 // Gets the context so it can be used later
	 public ButtonAdapter(Context c) {
	  mContext = c;
	 }

	 // Total number of things contained within the adapter
	 public int getCount() {
	  return arrNumbers.size(); //filesnames.length;
	 }

	  // Require for structure, not really used in my code.
	 public Object getItem(int position) {
	  return null;
	 }

	 // Require for structure, not really used in my code. Can
	 // be used to get the id of an item in the adapter for 
	 // manual control. 
	 public long getItemId(int position) {
	  return position;
	 }
	 /////////////////////////////////////////////////////////////////////
	
	 /////////////////////////////////////////////////////////////////////
	 private String isRingButtonOrPhone(String number, int mode){
		 //mode=1 - кнопка
		 //mode=2 - трубка
		 try{
				 String retPhone=null;
				 String callerType=null;
				 int id=getContactIDFromNumber(number);
				 ContentResolver cr = getContentResolver();
				 Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
						 			ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{""+id}, null);
		         
				 while (pCur.moveToNext()) {
		             String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
		             String phoneType = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
		             
		             if(Integer.parseInt(phoneType) == ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)
		            	 retPhone=phoneNo;
		             else if(number.equals(phoneNo)) 
		            	 callerType=phoneType; // тип звонящего
		             
		         }
		         pCur.close();
		         
		         if(callerType==null)
		        	 return null;
		         
		         if( mode==1 && Integer.parseInt(callerType) == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE ) //это кнопка звонит
		        	 return retPhone;
		         else if( mode==2 && Integer.parseInt(callerType) == ContactsContract.CommonDataKinds.Phone.TYPE_WORK ) //это трубка звонит
		        	 return retPhone;
		         else if(Integer.parseInt(callerType) == ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)
		        	 return retPhone;
		         
		 }catch(Exception ex) {ex.printStackTrace();}
		 
		 return null;
        
	 }
	 /////////////////////////////////////////////////
	 /////////////////////////////////////////////////
	 public View getView(int position, 
	                           View convertView, ViewGroup parent) {
		 
	  Button btn;
	  if (convertView == null) {  
		  // if it's not recycled, initialize some attributes
		  btn = new Button(mContext);
	   	  if (arrNumbers.size()<16)
	   		  btn.setLayoutParams(new GridView.LayoutParams(136, 130));
	   	  else if (arrNumbers.size()<21)
	   		  btn.setLayoutParams(new GridView.LayoutParams(136, 87));
	   	  else
	   		  btn.setLayoutParams(new GridView.LayoutParams(136, 70));
	   	  btn.setPadding(1, 1, 1, 1);
	   } 
	   else {
		   btn = (Button) convertView;
	   }
	  //btn.setText(Html.fromHtml("<b>"+(String)arrBoxes.get(position)+"</b>" ));
	  
	  if (isRing!=null) {
		  String RingButton=isRingButtonOrPhone(isRing,1);
		  String RingPhone=isRingButtonOrPhone(isRing,2);
		  ///
		  //Toast.makeText(getBaseContext(),">"+arrNumbers.get(position)+" isRing="+isRing+" RingPhone: "+RingPhone+" RingButton: "+RingButton, Toast.LENGTH_LONG).show();
		  ///
		  if (RingButton!=null && RingButton.equals(arrNumbers.get(position)) && !isRing.equals((String)arrNumbers.get(position))){
			  btn.setText(Html.fromHtml("<font color='#550000' size='+1'><b>"+(String)arrBoxes.get(position)+"</b></font><br>" + (String)arrNames.get(position)));
		      btn.setBackgroundResource(R.drawable.button_red);
		      Toast.makeText(getBaseContext(),"ЭКСТРЕННЫЙ ВЫЗОВ "+arrBoxes.get(position), Toast.LENGTH_LONG).show();
		  }
		  else if (RingPhone!=null && RingPhone.equals(arrNumbers.get(position)) && !isRing.equals((String)arrNumbers.get(position))){
			  btn.setText(Html.fromHtml("<font color='#005500' size='+1'><b>"+(String)arrBoxes.get(position)+"</b></font><br>"+ (String)arrNames.get(position)) );
		      btn.setBackgroundResource(R.drawable.button_green);
		  }
		  else if(RingPhone==null && RingButton==null && isRing.equals((String)arrNumbers.get(position))){
			  btn.setText(Html.fromHtml("<font color='#550000' size='+1'><b>"+(String)arrBoxes.get(position)+"</b></font><br>" + (String)arrNames.get(position) ));
		      btn.setBackgroundResource(R.drawable.button_yellow);
		  }
		  else{
			  btn.setText(Html.fromHtml("<font color='#0000ee'><b>"+(String)arrBoxes.get(position)+"</b></font><br>"+ (String)arrNames.get(position)) );
		  	  btn.setBackgroundResource(R.drawable.button);
		  }
	 }
	  else{
		  btn.setText(Html.fromHtml("<font color='#0000ee'><b>"+(String)arrBoxes.get(position)+"</b></font><br>"+ (String)arrNames.get(position)) );
	  	  btn.setBackgroundResource(R.drawable.button);
	  }
	  btn.setTextColor(Color.parseColor("#555555"));
	  //btn.setTypeface(null, Typeface.BOLD);
	  btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
	  btn.setId(position);
	  btn.setOnClickListener(new MyOnClickListener(position));  
	 
  
	  btn.setOnKeyListener(new android.view.View.OnKeyListener() {
			@Override
	        public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
	            if (arg2.getAction() == KeyEvent.ACTION_DOWN) {
	            	
	            	Toast.makeText(getBaseContext(),"---key:"+arg1, Toast.LENGTH_SHORT).show();
	                if (arg1 == KeyEvent.KEYCODE_DPAD_CENTER || arg1 == KeyEvent.KEYCODE_MENU) {
	                	getPreferences();
	                	startService();
	                    return true;
	                }
	            }
	            return false;
	        }
	    });
	  
	  
	  return btn;
	 }
	 

	    
	}


	class MyOnClickListener implements OnClickListener  
	{  
			 private final int position;  
			  
			 public MyOnClickListener(int position)  
			 {  
				 this.position = position;  
				 
			 }  
			  
			
			 
			 public void onClick(View v)  
			 {  
				 // Preform a function based on the position  
				 //callFunction(this.position);
				 selectFunction(this.position);
			 }  
			 
			 public void selectFunction(int position){
				    String phone = (String)arrNumbers.get(position);    
		  		    Log.d("callFunction","position: " + position+ " phone:"+phone);
		  		    
		  		    HashMap<String, String> MyMap = getCallable(phone);
     				if (MyMap==null || MyMap.size()==1){
     					callFunction(position);
     					return;
     				}
     				Log.d("callFunction","MyMap.size: " + MyMap.size());
     				 
     				Iterator<java.util.Map.Entry<String, String>> iterator = MyMap.entrySet().iterator();
     				ArrayList<String> names = new ArrayList<String>();
     				while (iterator.hasNext()) {
     				    Map.Entry<String,String> pairs = (Map.Entry<String,String>)iterator.next();
     				    String value =  pairs.getValue();
     				    String Key = pairs.getKey();
     				    Log.e(TAG, "+selectFunction+ Key: "+Key+" Value: "+value);
     				    //Spanned str=Html.fromHtml("<font color='#222222'>"+Key+" "+value+" </font>");
     				    String str=Key+" "+value;
   				    	names.add(str);
     				    //Toast.makeText(context,":::"+intent.getStringExtra(TelephonyManager.EXTRA_STATE), Toast.LENGTH_LONG).show();
     				     //Name += value+"\n";
     				}
     				
     				//String names[] ={"A","B","C","D"};
     		        AlertDialog.Builder alertDialog = new AlertDialog.Builder(TopLevelActivity.this);
     		        LayoutInflater inflater = getLayoutInflater();
     		        
     		        View convertView = (View) inflater.inflate(R.layout.custom, null);
     		        
       		        //LayoutInflater inflater2 = getLayoutInflater();
     		        
       		        TextView myMsg = (TextView)inflater.inflate(R.layout.listview_header_row2, null);
	       		     myMsg.setText((String)arrBoxes.get(position));
	       		     
	       		  
     		        alertDialog.setView(convertView);
     		        alertDialog.setCustomTitle(myMsg); 
     		        //alertDialog.setInverseBackgroundForced(true);
     		        ListView lv = (ListView) convertView.findViewById(R.id.listView1);
     		        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(TopLevelActivity.this, android.R.layout.simple_list_item_1,names);
     		        MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(TopLevelActivity.this, names);
     		        //lv.addHeaderView(header);
     		       

     		       //lv.setSelectionAfterHeaderView();
     		        lv.setAdapter(adapter);
     		        
     		       lv.setOnItemClickListener(new OnItemClickListener() {
     		    	   @Override
     		    	   public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
     		    	      Object listItem = adapter.getItemAtPosition(position);
     		    	      String phone = listItem.toString().substring(0, listItem.toString().indexOf(" ")); 
     		    	      Log.e(TAG, "+onItemClick+ position: "+position+" listItem: "+listItem+" phone:"+phone);
	     		  		  	Intent callIntent = new Intent(Intent.ACTION_CALL);          
	     		  		    callIntent.setData(Uri.parse("tel:"+phone));          
	     			        isRing = phone;
	     			        startActivity(callIntent); 
	     			        
     		    	   } 
     		    	});
     		        
     		       //lv.setSelection(0);
     		      lv.setFocusable(true);
    		       
     		      // lv.setItemChecked(1, true);
     		       
     		        alertDialog.show();
     		        
			 }
			
					
			 private HashMap<String, String> getCallable(String phone) {
				 HashMap<String, String> rezultMap = new HashMap<String,String>();
				 try{
					 int id=getContactIDFromNumber(phone);
					 if (id==0) return null;
					 
					 ContentResolver cr = getContentResolver();
					 Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
					 			ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{""+id}, null);
					 ArrayList<String> Patients = getPatients(id);
					 
					 int ii=0;
					 while (pCur.moveToNext()) {
					      String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					      String phoneType = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
					      
					      if(Integer.parseInt(phoneType) == ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)
					    	  rezultMap.put(phoneNo, "Громкоговоритель");
					      else if(Integer.parseInt(phoneType) == ContactsContract.CommonDataKinds.Phone.TYPE_WORK){
					    	  String name="место "+ii;
					    	  //Log.e(TAG, "-getCallable- ii="+ii+" len="+Patients.size());
					    	  if(Patients.size()>ii) 
					    		  	name = Patients.get(ii);
					    	  rezultMap.put(phoneNo, name);
					    	  ii++;
					     	 }
					      
					      
					      
					 }
					 pCur.close();
				  
				 }catch(Exception ex) {ex.printStackTrace();}
				 return rezultMap;
			}

			private ArrayList<String> getPatients(int id){
				 Log.i(TAG, " -getPatients- id: "+id);
				 ArrayList<String> lst = new ArrayList<String>();
				 try{
					 String[] projection = new String[] {ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME };
					 
					 String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?"; 
	                 String[] whereParameters = new String[]{""+id};
	                
	                 //Request
	                 Cursor contacts = getContentResolver().query( ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, null);
					 Log.i(TAG, " -getPatients- n: "+contacts.getCount());
					 int i=0;
	                 while (contacts.moveToNext())
	         		 { 
	                	 Log.e(TAG, " ------ start i:"+i);
	         				try{
	         					String wrapperStr = contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
	         					Log.i(TAG, " -getPatients- wrapperStr:"+wrapperStr);
		         				Gson gson = new Gson();
		         				MapWrapper wrapper = gson.fromJson(wrapperStr, MapWrapper.class);
		         				
		         				if (wrapper!=null){
			         				HashMap<Integer, String> MyMap = wrapper.getMyMap();
			         				
			         				Iterator<java.util.Map.Entry<Integer, String>> iterator = MyMap.entrySet().iterator();
			         				while (iterator.hasNext()) {
			         				    Map.Entry<Integer,String> pairs = (Map.Entry<Integer,String>)iterator.next();
			         				    String value =  pairs.getValue();
			         				    Integer Key = pairs.getKey();
			         				    Log.e(TAG, " -getPatients- Key: "+Key+" Value: "+value);
			         				    lst.add(value);
			         				}
		         				}
	         				}catch(Exception ex) {ex.printStackTrace();}
	         				Log.e(TAG, " ------ end: "+i);
	         				i++;
	         		 }
	                 
	                 contacts.close();
				 } catch (Exception e) {e.printStackTrace();}
				 //Log.d(TAG, "-getPatients- len: "+lst.size());
                 return lst;
			}


			public void callFunction(int position) {
				    String phone = (String)arrNumbers.get(position);    
		  		    Log.d("callFunction","position: " + position+ " phone:"+phone);
		  		  	Intent callIntent = new Intent(Intent.ACTION_CALL);          
		  		    callIntent.setData(Uri.parse("tel:"+phone));          
		  		    //Intent callIntent = new Intent("android.intent.action.CALL_PRIVILEGED", Uri.parse("sip:"+phone));
			        
			          
			        //callIntent.putExtra("line", 1);
			        //callIntent.putExtra("is_video", false);
			        //callIntent.putExtra("is_tel", true);
			        
			        //Toast.makeText(getBaseContext(),"+++ call:"+phone, Toast.LENGTH_SHORT).show();			       
			        isRing = phone;
			        startActivity(callIntent);  
			        
			 }
			 
			
			 
	}  

	
	public class MySimpleArrayAdapter extends ArrayAdapter<String> {
		  private final Context context;
		  private final ArrayList<String> values;

		  public MySimpleArrayAdapter(Context context, ArrayList<String> values) {
		    super(context, R.layout.row_layout, values);
		    this.context = context;
		    this.values = values;
		    
		  }

		  @Override
		  public View getView(int position, View convertView, ViewGroup parent) {
		    LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    View rowView = inflater.inflate(R.layout.row_layout, parent, false);
		    TextView textView = (TextView) rowView.findViewById(R.id.label);
		    //ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		    textView.setText(values.get(position));
		    // change the icon for Windows and iPhone
		    /*
		    String s = values[position];
		    if (s.startsWith("iPhone")) {
		      imageView.setImageResource(R.drawable.no);
		    } else {
		      imageView.setImageResource(R.drawable.ok);
		    }*/

		    return rowView;
		  }
		  
		  
		    
		    
		} 
	
}






