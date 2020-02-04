package uk.co.martinpearman.mapsforgetileprovider;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import org.mapsforge.android.maps.MapsforgeTileProvider;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

	private static final int BUFFER_SIZE=1024;
	private static final String PROW_MAP_DATABASE_NAME="Norfolk Rights of Way.map";
	private static final String TAG=MapsActivity.class.getSimpleName();

	private static boolean extractAsset(Context context, String assetsPath, File destinationFile){
		boolean success=false;
		try {
			InputStream inputStream=context.getAssets().open(assetsPath);
			OutputStream outputStream=new FileOutputStream(destinationFile);
			byte[] buffer=new byte[BUFFER_SIZE];
			int bytesRead=inputStream.read(buffer);
			while(bytesRead!=-1){
				outputStream.write(buffer, 0, bytesRead);
				bytesRead=inputStream.read(buffer);
			}
			outputStream.close();
			inputStream.close();
			success=true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return success;
	}

	private GoogleMap googleMap;
	private File mapDatabaseFile;
	private MapsforgeTileProvider mapsForgeTileProvider=null;
	private TileOverlay prowTileOverlay=null;

	private void addProwTileOverlay(){
		if(prowTileOverlay!=null){
			prowTileOverlay.remove();
		}
		if(mapsForgeTileProvider!=null){
			mapsForgeTileProvider.close();
		}
		MapsforgeTileProvider.MapsforgeTileProviderOptions mapsForgeTileProviderOptions=new MapsforgeTileProvider.MapsforgeTileProviderOptions();


		MapFile mapFile=new MapFile(mapDatabaseFile.getPath());
		mapsForgeTileProviderOptions.setMapDataStore(mapFile);
		try {
			mapsForgeTileProviderOptions.setRenderTheme(new AssetsRenderTheme(MapsActivity.this, "", "prow_render_theme.xml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		mapsForgeTileProviderOptions.setTransparent(true);

		mapsForgeTileProvider=new MapsforgeTileProvider(MapsActivity.this, mapsForgeTileProviderOptions);

		TileOverlayOptions tileOverlayOptions=new TileOverlayOptions();
		tileOverlayOptions.tileProvider(mapsForgeTileProvider);
		tileOverlayOptions.visible(true);

		prowTileOverlay=googleMap.addTileOverlay(tileOverlayOptions);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

		//	extract map database file to internal cache if necessary
		mapDatabaseFile=new File(getCacheDir(), PROW_MAP_DATABASE_NAME);
		if(!mapDatabaseFile.exists()){
			extractAsset(MapsActivity.this, PROW_MAP_DATABASE_NAME, mapDatabaseFile);
		}

		mapFragment.getMapAsync(this);
	}

	@Override
	protected void onDestroy() {
		if(mapsForgeTileProvider!=null){
			mapsForgeTileProvider.close();
		}
		super.onDestroy();
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		this.googleMap = googleMap;

		// Add a marker and move the camera
		LatLng kingsLynnLatLng = new LatLng(52.75, 0.392);
		this.googleMap.addMarker(new MarkerOptions().position(kingsLynnLatLng).title("Sunny Kings Lynn!"));
		this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kingsLynnLatLng, 14));

		addProwTileOverlay();
	}
}
