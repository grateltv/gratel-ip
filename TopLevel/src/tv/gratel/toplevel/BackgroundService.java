package tv.gratel.toplevel;

// BackgroundService.java
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

//import tv.gratel.toplevel.TopLevelActivity.MapWrapper;

import com.google.gson.Gson;

public class BackgroundService extends Service
{
	private String xmlPath="", xmlPath2="";
	
	private static final String TAG = "GratelMedService";
	private static final int TIME_OUT_SECONDS = 30;
	private NotificationManager notificationMgr;
    private ThreadGroup myThreads = new ThreadGroup("ServiceWorker");
    private String OUR_OTDELENIE="1";
    private int isServiceRunning=0;
    private long currentDate=0; //, lastMod = -1, lastMod2 = -1;

    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "in onCreate() starting service");
        notificationMgr =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        displayNotificationMessage("GratelMedService is running");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "onStartCommand");
        OUR_OTDELENIE = intent.getStringExtra(TopLevelActivity.OUR_OTDELENIE);
        xmlPath = intent.getStringExtra(TopLevelActivity.XML_PATH);
        xmlPath2 = intent.getStringExtra(TopLevelActivity.XML_PATH2);
        //lastMod = -1;
        if (isServiceRunning==0){
	        Log.v(TAG, "onStartCommand-OUR_OTDELENIE:"+OUR_OTDELENIE);
	        Log.v(TAG, "onStartCommand-xmlPath:"+xmlPath);
	        Log.v(TAG, "onStartCommand-xmlPath2:"+xmlPath2);
	        PendingIntent pi = intent.getParcelableExtra(TopLevelActivity.PARAM_PINTENT);
	        Log.v(TAG, "onStartCommand-pi:"+pi);
	        new Thread(myThreads, new ServiceWorker(1, pi), "GratelMedService").start();
	        isServiceRunning=1;
        }
        return START_NOT_STICKY;
    }

    class ServiceWorker implements Runnable
    {
    	final String TAG2 = "ServiceWorker:" + Thread.currentThread().getId();
        final String LOG_TAG = "ServiceWorker:XML";
    	private int counter = -1;
		PendingIntent pi;
		
    	public ServiceWorker(int counter, PendingIntent pi) {
			this.counter = counter;
			this.pi = pi;
		}
		
		XmlPullParser prepareXpp(String strPath, int type) throws XmlPullParserException {
			/*
			 * type = 1:  XML - Пациенты
			 * type = 2:  XML - Устройства
			 */
		    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		    XmlPullParser xpp = factory.newPullParser();
		    try{
			    final String TAG2 = "ServiceWorker:" + Thread.currentThread().getId();
			    if(strPath.startsWith("smb:")){
				        jcifs.Config.setProperty("jcifs.smb.client.responseTimeout","500");
				        jcifs.Config.setProperty("jcifs.smb.client.soTimeout","500");
				    	//////////////shara/////////////////////////////////        
				    	//NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("",username, password);
				    	NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, null, null);
				    	SmbFile f = new SmbFile(strPath,auth);
				    	if(f.exists()){
			    			currentDate=f.getDate();
			    			
			    			long testMod=TopLevelActivity.lastMod2;
			    			if(type==1) testMod=TopLevelActivity.lastMod;
			    			Log.i(TAG2, ""+type+") "+f+" currentDate: "+currentDate+" lastDate: "+testMod);	
			    			if(currentDate != testMod){	
			    				SmbFileInputStream instream = new SmbFileInputStream(f);
			        			xpp.setInput(instream,null);
				        		//Log.i(TAG2, "reading from XML-file:"+f.getName());	
				        		if(type==1){
				        			TopLevelActivity.lastMod=currentDate;
				        			TopLevelActivity.lastMod2=-1;
				        		}
				        		else
				        			TopLevelActivity.lastMod2=currentDate;
				        		
				    			Log.i(TAG2, "lastMod: "+TopLevelActivity.lastMod+" lastMod2: "+TopLevelActivity.lastMod2);	
				    		}
			    			else{
			    				xpp = null;
			    				Log.i(TAG2, "XML-file is up to date - nothink for load...");
			    			}
					    }else{
					    	xpp = null;
			        		Log.e(TAG2, "XML-file does not exist:"+f.getName());	
					    }
			    }
		    	else{
			    	File f = new File(strPath);
			    	if(f.exists()){
		    			currentDate=f.lastModified();
		    			Log.i(TAG2, "=== File: "+f+" currentDate: "+currentDate+" lastMod: "+TopLevelActivity.lastMod);	
		    			long testMod=TopLevelActivity.lastMod2;
		    			if(type==1) testMod=TopLevelActivity.lastMod;
		    			if(currentDate != testMod){	
		    				FileInputStream instream = new FileInputStream(f);
		        			xpp.setInput(instream,null);
			        		//Log.i(TAG2, "reading from XML-file:"+f.getName());	
			        		if(type==1){
			        			TopLevelActivity.lastMod=currentDate;
			        			TopLevelActivity.lastMod2=-1;
			        		}
			        		else
			        			TopLevelActivity.lastMod2=currentDate;
			    			Log.i(TAG2, "lastMod: "+TopLevelActivity.lastMod+" lastMod2: "+TopLevelActivity.lastMod2);	
			    		}
		    			else{
		    				xpp = null;
		    				Log.i(TAG2, "XML-file is up to date - nothink for load...");
		    			}
				    }else{
				    	xpp = null;
		        		Log.e(TAG2, "XML-file does not exist:"+f.getName());	
				    }

					
				}	 
		    }catch(Exception ex) {ex.printStackTrace();}
			    
			   	
		    return xpp;
		}

		
		
		public void run() {
	        
	        try {
                // do background processing here...
	        	int j=0;
            	while (true){
	        	//for (j = 1; j<=50; j++) {
	  		        Log.v(TAG2, "j = " + j++);
	  		        loadXMLFile(j); //кнопки 
	  		        for (int k=0; k<2; k++){
	  		        	loadXMLFile2(k); //пациенты
	  		            TimeUnit.SECONDS.sleep(TIME_OUT_SECONDS);
	  		        }
	  		          
	  		        	  
  		            TimeUnit.SECONDS.sleep(TIME_OUT_SECONDS);
  		         
	  		          
  		        }//for-wile
  		        //stopSelf();
  		        //Log.v(TAG2, "...stopSelf...");
	        }catch(Exception ex) {ex.printStackTrace();}
			
        }
		
		public void loadXMLFile(int j){
          try {
	        	String tmp = "";
	        	XmlPullParser xpp1 = prepareXpp(xmlPath2, 1); //железо
	        	//очистим контакты
	            if (xpp1==null)
	            	return;
	            Log.e(TAG2, ""+j+") loadXMLFile-1 : "+xmlPath2+" xpp1="+xpp1);	
	            //removeAllContacts();
	            	
	           String name="", boxname="";
	           String Loudspeaker="";
	           ArrayList<String> Buttons = new ArrayList<String>();
	           ArrayList<String> Phones = new ArrayList<String>();
	           int Otdelenie=0;
	           String curBox="";
	           while (xpp1!=null && xpp1.getEventType() != XmlPullParser.END_DOCUMENT) {
		            switch (xpp1.getEventType()) {
		              // начало документа
		              case XmlPullParser.START_DOCUMENT:
		            	removeAllContacts();
		                //Log.d(LOG_TAG, "START_DOCUMENT");
		                break;
		              
		              // начало тэга
		              case XmlPullParser.START_TAG:
		                //Log.d(LOG_TAG, "START_TAG: name = " + xpp.getName()+ ", depth = " + xpp.getDepth() + ", attrCount = " + xpp.getAttributeCount()+ " Otd:"+Otdelenie);
		                tmp = "";
		                for (int i = 0; i < xpp1.getAttributeCount(); i++) {
	  		                  tmp = tmp + xpp1.getAttributeName(i) + " = " + xpp1.getAttributeValue(i) + ", ";
	  		                  
	  		                 
	  		                	if(xpp1.getName().equalsIgnoreCase("Otdelenie") && xpp1.getAttributeName(i).equalsIgnoreCase("ID"))
		  		                	  Otdelenie= Integer.parseInt(xpp1.getAttributeValue(i));
	  		                	
	  		                	
	  		                	if (Otdelenie==Integer.parseInt(OUR_OTDELENIE)){
	  		                		if(xpp1.getName().equalsIgnoreCase("Box") && xpp1.getAttributeName(i).equalsIgnoreCase("ID")){
			  		                	  curBox = xpp1.getAttributeValue(i);
			  		                }
	  		                		else if(xpp1.getName().equalsIgnoreCase("Box") && xpp1.getAttributeName(i).equalsIgnoreCase("boxname")){
			  		                	  boxname = xpp1.getAttributeValue(i)+"\n";
			  		                }
	  		                		else if(xpp1.getName().equalsIgnoreCase("Loudspeaker") && xpp1.getAttributeName(i).equalsIgnoreCase("number")){
	  		                			  Loudspeaker = xpp1.getAttributeValue(i);
			  		                }
	  		                		else if(xpp1.getName().equalsIgnoreCase("Button") && xpp1.getAttributeName(i).equalsIgnoreCase("number")){
			  		                	  Buttons.add(xpp1.getAttributeValue(i));
			  		                }
	  		                		else if(xpp1.getName().equalsIgnoreCase("Phone") && xpp1.getAttributeName(i).equalsIgnoreCase("number")){
			  		                	  Phones.add(xpp1.getAttributeValue(i));
			  		                }
	  		                	}
	  		                		
		                }
		                  
		                if (!TextUtils.isEmpty(tmp)){
	  		                  //Log.d(LOG_TAG, "Attributes: " + tmp);
	  		                  //Log.e(LOG_TAG, "------ name: " + name+ " phone: "+phone);
		                }
		                break;
		                
		              // конец тэга
		              case XmlPullParser.END_TAG:
		                //Log.d(LOG_TAG, "END_TAG: name = " + xpp1.getName()+" Otdelenie="+OUR_OTDELENIE+" curBox="+curBox);
		              
		                if( Otdelenie==Integer.parseInt(OUR_OTDELENIE) && xpp1.getName().equalsIgnoreCase("Box") ){
		                	ArrayList<String> Patients = new ArrayList<String>(); //getPatientsFromXML(strforxpp2, OUR_OTDELENIE, curBox);
		                	name="";
		                	
		                	for(int i=0; i<Patients.size(); i++)
		                		name += Patients.get(i)+", ";
		                	
		                	createContact(name, boxname, Loudspeaker, Buttons, Phones);
		                	curBox="";
		                	name="";
		                	boxname="";
		                	Phones.clear();
		                	Buttons.clear();
		                }
		                
		                if(xpp1.getName().equalsIgnoreCase("Otdelenie"))
		            	  Otdelenie=0;
		                
		                break;
		              // содержимое тэга
		              case XmlPullParser.TEXT:
		                //Log.d(LOG_TAG, "text = " + xpp.getText());
		                break;

		              default:
		                break;
		             }//switch
		             // следующий элемент
		             xpp1.next();
	            }//while
	            
	            //Log.d(LOG_TAG, "END_DOCUMENT");
	            pi.send(TopLevelActivity.STATUS_REFRESH);
	            
	            //Intent intent = new Intent().putExtra(MainActivity.PARAM_RESULT, time * 100);
	            //pi.send(BackgroundService.this, TopLevelActivity.STATUS_REFRESH, intent);
	            //TopLevelActivity.this.loadContacts();

	          } catch (XmlPullParserException e) {
	            e.printStackTrace();
	          } catch (Exception e) {
	            e.printStackTrace();
	          }  		        	  
		}
		
		public void loadXMLFile2(int k){
          try {
	           String tmp = "";
	           XmlPullParser xpp = prepareXpp(xmlPath,2); //пациенты
	           if(xpp==null)
	        	   return;
	           
	           Log.e(TAG2, ""+k+") loadXMLFile-2 : "+xmlPath);	
	           String name="", boxname="";
	           ArrayList<String> namelist = new ArrayList<String>();
	           int phone=0;
	           int Otdelenie=0;
	           while (xpp!=null && xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
		            switch (xpp.getEventType()) {
		              // начало документа
		              case XmlPullParser.START_DOCUMENT:
		                //Log.d(LOG_TAG, "START_DOCUMENT");
		                break;
		              
		              // начало тэга
		              case XmlPullParser.START_TAG:
		                //Log.d(LOG_TAG, "START_TAG: name = " + xpp.getName()+ ", depth = " + xpp.getDepth() + ", attrCount = " + xpp.getAttributeCount()+ " Otd:"+Otdelenie);
		                tmp = "";
		                for (int i = 0; i < xpp.getAttributeCount(); i++) {
	  		                  tmp = tmp + xpp.getAttributeName(i) + " = " + xpp.getAttributeValue(i) + ", ";
	  		                  
	  		                  if(xpp.getName().equalsIgnoreCase("Otdelenie") && xpp.getAttributeName(i).equalsIgnoreCase("ID"))
	  		                	  Otdelenie= Integer.parseInt(xpp.getAttributeValue(i));
	  		                  
		                	  if (Otdelenie==Integer.parseInt(OUR_OTDELENIE)){
		                		  if(xpp.getName().equalsIgnoreCase("Box") && xpp.getAttributeName(i).equalsIgnoreCase("ID")){
		                			  name="";
		  		                	  phone = Otdelenie*1000 + Integer.parseInt(xpp.getAttributeValue(i));
		  		                	  //boxname = "Бокс "+xpp.getAttributeValue(i)+"\n";
		  		                  }
		  		                  else if(xpp.getName().equalsIgnoreCase("Patient") && xpp.getAttributeName(i).equalsIgnoreCase("fam"))
		  		                	  name = name + " " + xpp.getAttributeValue(i);
		  		                  else if(xpp.getName().equalsIgnoreCase("Patient") && xpp.getAttributeName(i).equalsIgnoreCase("name"))
		  		                	  name = name + " " + xpp.getAttributeValue(i).substring(0, 1)+".";
		  		                  else if(xpp.getName().equalsIgnoreCase("Patient") && xpp.getAttributeName(i).equalsIgnoreCase("otch")){
		  		                	  name = name + xpp.getAttributeValue(i).substring(0, 1)+". ";
		  		                	  namelist.add(name);
		  		                	  name="";
		  		                  }
		                	  }
		                }
		                  
		                if (!TextUtils.isEmpty(tmp)){
	  		                  //Log.d(LOG_TAG, "Attributes: " + tmp);
	  		                  //Log.e(LOG_TAG, "------ name: " + name+ " phone: "+phone);
		                }
		                break;
		                
		              // конец тэга
		              case XmlPullParser.END_TAG:
		                //Log.d(LOG_TAG, "END_TAG: name = " + xpp.getName());
		              
		                if(Otdelenie==Integer.parseInt(OUR_OTDELENIE) && xpp.getName().equalsIgnoreCase("Box")){
		                	updateContact(""+phone, namelist);
		                	namelist.clear();
		                	name="";
		                	phone=0;
		                }
		                
		                if(xpp.getName().equalsIgnoreCase("Otdelenie"))
		            	  Otdelenie=0;
		                
		                break;
		              // содержимое тэга
		              case XmlPullParser.TEXT:
		                //Log.d(LOG_TAG, "text = " + xpp.getText());
		                break;

		              default:
		                break;
		             }//switch
		             // следующий элемент
		             xpp.next();
	            }//while
	            
	            //Log.d(LOG_TAG, "END_DOCUMENT");
	            pi.send(TopLevelActivity.STATUS_REFRESH);
	            
	            //Intent intent = new Intent().putExtra(MainActivity.PARAM_RESULT, time * 100);
	            //pi.send(BackgroundService.this, TopLevelActivity.STATUS_REFRESH, intent);
	            //TopLevelActivity.this.loadContacts();

	          } catch (XmlPullParserException e) {
	            e.printStackTrace();
	          } catch (Exception e) {
	            e.printStackTrace();
	          }  		        	  
		}

		
	    private void createContact(String name, String boxname, String Loudspeaker, ArrayList Buttons, ArrayList Phones ) {
	        ContentResolver cr = getContentResolver();
	        Log.i(TAG2, "createContact box: "+boxname+"  Loudspeaker="+Loudspeaker+" name="+name+"==="); 
 
	        //Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
	        /* 
	        if (cur.getCount() > 0) {
	            while (cur.moveToNext()) {
	                String existName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	                if (existName.contains(name)) {
	                    Log.i(TAG2, "!!! The contact name: " + name + " already exists"); 
	                    return;                 
	                }
	            }
	        }*/
	         
	        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
	        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
	                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "accountname@gmail.com")
	                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, "com.google")
	                .build());
	      
	        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
	                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
	                /*.withValue(ContactsContract.Data.MIMETYPE,
	                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
	                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, name)*/
	                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
	                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, boxname)
	                /*.withValue(ContactsContract.Data.MIMETYPE,
	                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
	                .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, Loudspeaker)*/
	                .build());
	        //Громкоговорящие устройства
	        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
	                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
	                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)	                
	                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, Loudspeaker)
	                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)
	                .build());
	        //Кнопки
	        for(int i=0; i<Buttons.size(); i++){
	        	ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
		                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
		                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)	                
		                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, Buttons.get(i))
		                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
		                .build());
	        }
	        //Трубки
	        for(int i=0; i<Phones.size(); i++){
	        	ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
		                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
		                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)	                
		                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, Phones.get(i))
		                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
		                .build());
	        }
	 
	         
	        try {
	            cr.applyBatch(ContactsContract.AUTHORITY, ops);
	        } catch (RemoteException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        } catch (OperationApplicationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        //cur.close();
	        
	 
	        //Log.i(TAG2, "... OK "); 
	    }
		
		public void removeAllContacts(){
			try{
				Log.e(TAG2, "... Remove All contacts... ");
				
	            
				Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, "display_name");
				while (phones.moveToNext())
				{
					String Name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
					deleteContact(Name);
				}				
				phones.close();
				//Log.e(TAG2, "...OK ");
			}catch(Exception ex) {ex.printStackTrace();}
		}
		
		 private void deleteContact(String name) {
			 //Log.e(TAG2, "... Remove contact "+name);
			 
			 ContentResolver cr = getContentResolver();
		        String where = ContactsContract.Data.DISPLAY_NAME + " = ? ";
		        String[] params = new String[] {name};
		     
		        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		        ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
		                .withSelection(where, params)
		                .build());
		        try {
		            cr.applyBatch(ContactsContract.AUTHORITY, ops);
		        } catch (RemoteException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        } catch (OperationApplicationException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        }
		        
		    }
		 
		 private void updateContact(String phone, ArrayList<String> namelist) {
			 Log.e(TAG2, "+++ updateContact phone="+phone+" len="+namelist.size());
			 if (namelist.size()==0) return;
			 try{
		        int id=getContactIDFromNumber(phone);
		        
		        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(); 
		        	
		        Map <Integer,String> myMap = new HashMap<Integer,String>();
		        
		        for(int i=0; i<namelist.size(); i++){
		        	Log.d(TAG2, ""+i+")  name: "+namelist.get(i));
		        	myMap.put(i, namelist.get(i));
		        }
		        Gson gson = new Gson();
		        MapWrapper wrapper = new MapWrapper();
		        wrapper.setMyMap(myMap);
		        String serializedMap = gson.toJson(wrapper);
		        Log.d(TAG2, "==========serializedMap.len="+serializedMap.length()+" |"+serializedMap+"|");	
		        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
	        			.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?", 
        				new String[]{String.valueOf(id), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
	        			.withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, serializedMap).build() );
    			
		        //.withValue(ContactsContract.CommonDataKinds.Note.NOTE, serializedMap).build() );
		        
		        /*
		        	switch (i){
		        		case 0:{
		        			ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
				        			.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?", 
			        				new String[]{String.valueOf(id), ContactsContract.Data.CONTENT_TYPE})
				        			.withValue(ContactsContract.Data.DATA1, namelist.get(i)).build() );
		        			//.withValue(ContactsContract.CommonDataKinds.Note.DATA1, namelist.get(i)).build() );
		        			//builder.withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, namelist.get(i));
		        			//break;
		        		}
		        		case 1:{
		        			ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
				        			.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?", 
					        				new String[]{String.valueOf(id), ContactsContract.Data.CONTENT_TYPE})
						        			.withValue(ContactsContract.Data.DATA2, namelist.get(i)).build() );

			        				//.withValue(ContactsContract.CommonDataKinds.Note..StructuredName.DATA3, namelist.get(i)).build() );
		        			//builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DATA3, namelist.get(i));
		        			//break;
		        		}
		        		case 2:{
		        			ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
				        			.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?", 
					        				new String[]{String.valueOf(id), ContactsContract.Data.CONTENT_TYPE})
						        			.withValue(ContactsContract.Data.DATA3, namelist.get(i)).build() );

			        				//.withValue(ContactsContract.CommonDataKinds.StructuredName.DATA4, namelist.get(i)).build() );
		        			//builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DATA4, namelist.get(i));
		        			//break;
		        		}
		        		case 5:{
		        			//builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DATA5, namelist.get(i));
		        			//break;
		        		}
		        		case 6:{
		        			//builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DATA6, namelist.get(i));
		        			//break;
		        		}
		        		case 7:{
		        			//builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DATA7, namelist.get(i));
		        			//break;
		        		}
		        	}
		        		*/
		        		
		        	
		        //}
		        
		        ContentResolver cr = getContentResolver();
		        try {
		        	//Log.e(TAG2, "---applyBatch start");
		            cr.applyBatch(ContactsContract.AUTHORITY, ops);
		            //Log.e(TAG2, "---applyBatch ok");
		        } catch (RemoteException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        } catch (OperationApplicationException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        }
		        
			 }catch(Exception ex) {ex.printStackTrace();}
		 
		 }
		
		 public int getContactIDFromNumber(String contactNumber)
		 {
		     contactNumber = Uri.encode(contactNumber);
		     int phoneContactID = 0;
		     String displayname="";
		     Cursor contactLookupCursor = getContentResolver().query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,Uri.encode(contactNumber)),new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup._ID}, null, null, null);
	         while(contactLookupCursor.moveToNext()){
	             phoneContactID = contactLookupCursor.getInt(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup._ID));
	             displayname = contactLookupCursor.getString(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
	         }
	         contactLookupCursor.close();
	         Log.i(TAG2, "-get contact contactNumber:"+contactNumber+" id="+phoneContactID+" displayname="+displayname);    
		     return phoneContactID;
		 }
		
    }//END CLASS


public class MapWrapper {
	  private HashMap<Integer, String> myMap;
	  public void MapWrapper(){
		  myMap = new HashMap();
	  }
	  // getter and setter for 'myMap'
	  public void setMyMap(Map<Integer, String> mp){
		  myMap = (HashMap<Integer, String>) mp;
	  }
	  
	  public HashMap getMyMap(){
		  return myMap;
	  }
	  
	}
    
    @Override
    public void onDestroy()
    {
        Log.v(TAG, "in onDestroy(). Interrupting threads and cancelling notifications");
        myThreads.interrupt();
        notificationMgr.cancelAll();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "in onBind()");
        return null;
    }

    private void displayNotificationMessage(String message)
    {
        Notification notification = new Notification(R.drawable.gtv_med, 
                message, System.currentTimeMillis());
        
        notification.flags = Notification.FLAG_NO_CLEAR;

        PendingIntent contentIntent = 
                PendingIntent.getActivity(this, 0, new Intent(this, TopLevelActivity.class), 0);

        notification.setLatestEventInfo(this, TAG, message, contentIntent);

        notificationMgr.notify(0, notification);
    }
}
