package com.naveed.maptest2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener,
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{


	ImageButton findMe,
		findPolice,
		findHospital,
		findFire;
	
	int userIcon = R.drawable.usericon;
	int policeIcon = R.drawable.policeicon;
	int hopsitalIcon = R.drawable.hospitalicon;
	int fireIcon = R.drawable.fireicon;
	
	GoogleMap mMap;
	
	LocationClient mLocationClient;
	
	//user marker
		private Marker userMarker;
		//location manager
		private LocationManager locMan;
		
		private final int MAX_PLACES = 5;
		
		//places of interest
		private Marker[] placeMarkers;
		//marker options
		private MarkerOptions[] places;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if(initMap()){
			mLocationClient = new LocationClient(this, this, this);
			mLocationClient.connect();
			//updatePlaces();
			//Toast.makeText(this, "Map has been loaded!", Toast.LENGTH_SHORT).show();
		}
		else{
			Toast.makeText(this, "Map could not be loaded!", Toast.LENGTH_SHORT).show();
		}
		
		findMe = (ImageButton) findViewById(R.id.btnMe);
		findMe.setOnClickListener(this);
		
		findPolice = (ImageButton) findViewById(R.id.btnPolice);
		findPolice.setOnClickListener(this);
		
		findPolice = (ImageButton) findViewById(R.id.btnHospital);
		findPolice.setOnClickListener(this);
		
		findPolice = (ImageButton) findViewById(R.id.btnFire);
		findPolice.setOnClickListener(this);
		placeMarkers = new Marker[MAX_PLACES];
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private boolean initMap(){
		if(mMap == null){
			MapFragment mainFrag = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
			mMap = mainFrag.getMap();
		}
		return (mMap != null);
	}
	
	private void updatePlaces(){
		
		
		Location lastLoc = mLocationClient.getLastLocation();

		//create LatLng
		LatLng lastLatLng = new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());

		//remove any existing marker
		if(userMarker!=null) userMarker.remove();
		//create and set marker properties
		userMarker = mMap.addMarker(new MarkerOptions()
		.position(lastLatLng)
		.title("You are here")
		.icon(BitmapDescriptorFactory.fromResource(userIcon)));
		//move to location
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 13), 3000, null);
		
		
		
	}

	private class GetPlaces extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... placesURL) {
			//fetch places
			
			//build result as string
			StringBuilder placesBuilder = new StringBuilder();
			//process search parameter string(s)
			for (String placeSearchURL : placesURL) {
				HttpClient placesClient = new DefaultHttpClient();
				try {
					//try to fetch the data
					
					//HTTP Get receives URL string
					HttpGet placesGet = new HttpGet(placeSearchURL);
					//execute GET with Client - return response
					HttpResponse placesResponse = placesClient.execute(placesGet);
					//check response status
					StatusLine placeSearchStatus = placesResponse.getStatusLine();
					//only carry on if response is OK
					if (placeSearchStatus.getStatusCode() == 200) {
						//get response entity
						HttpEntity placesEntity = placesResponse.getEntity();
						//get input stream setup
						InputStream placesContent = placesEntity.getContent();
						//create reader
						InputStreamReader placesInput = new InputStreamReader(placesContent);
						//use buffered reader to process
						BufferedReader placesReader = new BufferedReader(placesInput);
						//read a line at a time, append to string builder
						String lineIn;
						while ((lineIn = placesReader.readLine()) != null) {
							placesBuilder.append(lineIn);
						}
					}
				}
				catch(Exception e){ 
					e.printStackTrace(); 
				}
			}
			return placesBuilder.toString();
		}
		//process data retrieved from doInBackground
		protected void onPostExecute(String result) {
			
			int markerFlag = 0;
			//parse place data returned from Google Places
			//remove existing markers
			if(placeMarkers!=null){
				for(int pm=0; pm<placeMarkers.length; pm++){
					if(placeMarkers[pm]!=null)
						placeMarkers[pm].remove();
				}
			}
			try {
				//parse JSON
				
				//create JSONObject, pass stinrg returned from doInBackground
				JSONObject resultObject = new JSONObject(result);
				//get "results" array
				JSONArray placesArray = resultObject.getJSONArray("results");
				//marker options for each place returned
				places = new MarkerOptions[placesArray.length()];
				//loop through places
				for (int p=0; p<placesArray.length(); p++) {
					//parse each place
					//if any values are missing we won't show the marker
					boolean missingValue=false;
					LatLng placeLL=null;
					String placeName="";
					String vicinity="";
					int currIcon = userIcon;
					
					try{
						//attempt to retrieve place data values
						missingValue=false;
						//get place at this index
						JSONObject placeObject = placesArray.getJSONObject(p);
						//get location section
						JSONObject loc = placeObject.getJSONObject("geometry")
								.getJSONObject("location");
						//read lat lng
						placeLL = new LatLng(Double.valueOf(loc.getString("lat")), 
								Double.valueOf(loc.getString("lng")));	
						
						//get types
						JSONArray types = placeObject.getJSONArray("types");
						//loop through types
						for(int t=0; t<types.length(); t++){
							//what type is it
							String thisType=types.get(t).toString();
							//check for particular types - set icons
							if(thisType.contains("police")){
								currIcon = policeIcon;
								break;
							}
							else if(thisType.contains("hospital")){
								currIcon = hopsitalIcon;
								break;
							}
							else if(thisType.contains("fire_station")){
								currIcon = fireIcon;
								break;
							}
						}
					
						//vicinity
						vicinity = placeObject.getString("vicinity");
						//name
						placeName = placeObject.getString("name");
					}
					catch(JSONException jse){
						Log.d("PLACES", "missing value");
						missingValue=true;
						jse.printStackTrace();
					}
				 
					//if values missing we don't display
					if(missingValue)	places[p]=null;
					else
						places[p]=new MarkerOptions()
					.position(placeLL)
					.title(placeName)
					.icon(BitmapDescriptorFactory.fromResource(currIcon))
					.snippet(vicinity);
			}
			
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			if(places!=null && placeMarkers!=null){
				
				Log.d("place",places.toString());
				for(int p=0; p<places.length && p<placeMarkers.length; p++){
					//will be null if a value was missing
					if(places[p]!=null)
						Log.d("place","places found 2");
						placeMarkers[p]=mMap.addMarker(places[p]);
				}
			}
			
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v.getId() == R.id.btnMe){
			updatePlaces();
		}
		if(v.getId() == R.id.btnPolice){
			Location lastLoc = mLocationClient.getLastLocation();
			double lat = lastLoc.getLatitude();
			double lng = lastLoc.getLongitude();
			
			String placesSearchStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/" +
					"json?location="+lat+","+lng+
					"&radius=2000&sensor=true" +
					"&types=police"+
					"&key=AIzaSyAcyn2epzlgrS4DWvVhMYSukgWkE5WSrDw";//API KEY
			
			new GetPlaces().execute(placesSearchStr);
		}
		if(v.getId() == R.id.btnHospital){
			Location lastLoc = mLocationClient.getLastLocation();
			double lat = lastLoc.getLatitude();
			double lng = lastLoc.getLongitude();
			
			String placesSearchStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/" +
					"json?location="+lat+","+lng+
					"&radius=2000&sensor=true" +
					"&types=hospital"+
					"&key=AIzaSyAcyn2epzlgrS4DWvVhMYSukgWkE5WSrDw";//API KEY
			new GetPlaces().execute(placesSearchStr);
		}
		if(v.getId() == R.id.btnFire){
			
			Location lastLoc = mLocationClient.getLastLocation();
			double lat = lastLoc.getLatitude();
			double lng = lastLoc.getLongitude();
			
			String placesSearchStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/" +
					"json?location="+lat+","+lng+
					"&radius=2000&sensor=true" +
					"&types=fire_station"+
					"&key=AIzaSyAcyn2epzlgrS4DWvVhMYSukgWkE5WSrDw";//API KEY
			
			
			
			new GetPlaces().execute(placesSearchStr);
		}
	}



	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
		LocationRequest request = LocationRequest.create();
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		request.setInterval(30000);
		request.setFastestInterval(0);
		mLocationClient.requestLocationUpdates(request, this);
		
	}
	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
	}
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		String msg = "Location: " + location.getLatitude() + ", " + location.getLongitude();
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

}
