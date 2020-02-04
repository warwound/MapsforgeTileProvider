/*
 * Copyright 2020 Martin Pearman
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.TileProvider;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FixedTileSizeDisplayModel;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MapsforgeTileProvider implements TileProvider {

	private static final float MAPSFORGE_TILE_PROVIDER_VERSION=1.1f;

	private static final String TAG=MapsforgeTileProvider.class.getSimpleName();

	private static boolean androidGraphicFactoryCreated=false;

	private final ByteArrayOutputStream byteArrayOutputStream;
	private final boolean cacheLabels;
	private final DatabaseRenderer databaseRenderer;
	private final DisplayModel displayModel;
	private final TileCache inMemoryTileCache;
	private final boolean isTransparent;
	private final boolean labelsOnly;
	private final MapDataStore mapDataStore;
	private final com.google.android.gms.maps.model.Tile noTileTile;
	private final RenderThemeFuture renderThemeFuture;
	private final float textScale;
	private final int tileSize;

	public MapsforgeTileProvider(Context context, MapsforgeTileProviderOptions mapsForgeTileProviderOptions){

		if(!androidGraphicFactoryCreated){
			AndroidGraphicFactory.createInstance((Application) context.getApplicationContext());
			androidGraphicFactoryCreated=true;
		}

		byteArrayOutputStream=new ByteArrayOutputStream(mapsForgeTileProviderOptions.outputStreamCapacity);
		cacheLabels=mapsForgeTileProviderOptions.cacheLabels;
		displayModel = new FixedTileSizeDisplayModel(mapsForgeTileProviderOptions.tileSize);
		inMemoryTileCache = new InMemoryTileCache(mapsForgeTileProviderOptions.tileCacheCapacity);
		isTransparent=mapsForgeTileProviderOptions.isTransparent;
		labelsOnly=mapsForgeTileProviderOptions.labelsOnly;
		this.mapDataStore=mapsForgeTileProviderOptions.mapDataStore;
		noTileTile=mapsForgeTileProviderOptions.noTileTile;
		textScale=mapsForgeTileProviderOptions.textScale;
		tileSize=mapsForgeTileProviderOptions.tileSize;

		renderThemeFuture = new RenderThemeFuture(
				AndroidGraphicFactory.INSTANCE,
				mapsForgeTileProviderOptions.xmlRenderTheme,
				displayModel);
		Thread renderThemeFutureThread = new Thread(renderThemeFuture);
		renderThemeFutureThread.start();

		TileBasedLabelStore tileBasedLabelStore = new TileBasedLabelStore(inMemoryTileCache.getCapacityFirstLevel());

		databaseRenderer = new DatabaseRenderer(
				mapDataStore,
				AndroidGraphicFactory.INSTANCE,
				inMemoryTileCache,
				tileBasedLabelStore,
				mapsForgeTileProviderOptions.renderLabels,
				cacheLabels,
				mapsForgeTileProviderOptions.hillsRenderConfig);

	}

	public void close(){
		try {
			byteArrayOutputStream.close();
		} catch (IOException e) {
			Log.e(TAG, null, e);
		}
		renderThemeFuture.cancel(true);	//	needed or useful?
		inMemoryTileCache.destroy();
		mapDataStore.close();
	}

	@Override
	public synchronized com.google.android.gms.maps.model.Tile getTile(int x, int y, int zoom) {
		com.google.android.gms.maps.model.Tile googleTile;

		final org.mapsforge.core.model.Tile mapsforgeTile = new Tile(x, y, (byte) zoom, tileSize);
		if(mapDataStore.supportsTile(mapsforgeTile)){
			final RendererJob rendererJob = new RendererJob(mapsforgeTile, mapDataStore, renderThemeFuture, displayModel, textScale, isTransparent, labelsOnly);

			try {
				final TileBitmap tileBitmap = databaseRenderer.executeJob(rendererJob);
				inMemoryTileCache.put(rendererJob, tileBitmap);
				tileBitmap.compress(byteArrayOutputStream);
				googleTile=new com.google.android.gms.maps.model.Tile(tileSize, tileSize, byteArrayOutputStream.toByteArray());
				byteArrayOutputStream.reset();
			} catch (IOException e) {
				Log.e(TAG, null, e);
				googleTile=null;
			}
		} else {
			googleTile=noTileTile;
		}

		return googleTile;
	}

	public static class MapsforgeTileProviderOptions{

		private static final boolean CACHE_LABELS=true;
		private static final HillsRenderConfig HILLS_RENDER_CONFIG=null;
		private static final boolean IS_TRANSPARENT=true;
		private static final boolean LABELS_ONLY=false;
		private static final int OUTPUT_STREAM_CAPACITY=(256*1024);
		private static final boolean RENDER_LABELS=true;
		private static final float TEXT_SCALE=1f;
		private static final int TILE_CACHE_CAPACITY=32;
		private static final int TILE_SIZE=768;

		protected boolean cacheLabels=CACHE_LABELS;
		protected HillsRenderConfig hillsRenderConfig=HILLS_RENDER_CONFIG;
		protected boolean isTransparent=IS_TRANSPARENT;
		protected boolean labelsOnly=LABELS_ONLY;
		protected MapDataStore mapDataStore=null;
		protected com.google.android.gms.maps.model.Tile noTileTile=com.google.android.gms.maps.model.TileProvider.NO_TILE;
		protected int outputStreamCapacity=OUTPUT_STREAM_CAPACITY;
		protected boolean renderLabels=RENDER_LABELS;
		protected float textScale=TEXT_SCALE;
		protected int tileCacheCapacity=TILE_CACHE_CAPACITY;
		protected int tileSize=TILE_SIZE;
		protected XmlRenderTheme xmlRenderTheme=InternalRenderTheme.OSMARENDER;

		public void setCacheLabels(boolean cacheLabels){
			this.cacheLabels=cacheLabels;
		}

		public void setLabelsOnly(boolean labelsOnly) {
			this.labelsOnly = labelsOnly;
		}

		public void setMapDataStore(MapDataStore mapDataStore) {
			this.mapDataStore = mapDataStore;
		}

		public void setNoTileTile(com.google.android.gms.maps.model.Tile noTileTile){
			this.noTileTile=noTileTile;
		}

		public void setOutputStreamCapacity(int outputStreamCapacity) {
			this.outputStreamCapacity = outputStreamCapacity;
		}

		public void setRenderLabels(boolean renderLabels) {
			this.renderLabels = renderLabels;
		}

		public void setRenderTheme(XmlRenderTheme xmlRenderTheme){
			this.xmlRenderTheme=xmlRenderTheme;
		}

		public void setTextScale(float textScale) {
			this.textScale = textScale;
		}

		public void setTileCacheCapacity(int tileCacheCapacity) {
			this.tileCacheCapacity = tileCacheCapacity;
		}

		public void setTileSize(int tileSize){
			this.tileSize=tileSize;
		}

		public void setTransparent(boolean transparent) {
			isTransparent = transparent;
		}
	}
}
