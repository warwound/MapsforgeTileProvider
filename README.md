# MapsforgeTileProvider
MapsforgeTileProvider extends the android GoogleMap TileProvider class, rendering tiles from a Mapsforge map database file.

MapsforgeTileProviderExample shows typical usage:

Norfolk Rights of Way.map is my Mapsforge map database and contains the paths of public rights of way in the county of Norfolk, UK.
The public rights of way data was obtained from https://data.gov.uk/dataset/8f98c821-f955-49f7-93e5-94f5d53fd030/norfolk-public-rights-of-way and is licenced under the Ordnance Survey OpenData Licence.

prow_render_theme.xml is my custom render theme, containing only render rules for my public rights of way.

MapsActivity displays a GoogleMap on which i've added a MapsforgeTileProvider.
The MapsforgeTileProvider renders transparent tiles from the map database and overlays them on the GoogleMap.

![device-2020-02-02-161452](https://user-images.githubusercontent.com/3781446/73755895-a6410b80-475e-11ea-9acd-1042cb49d365.png)
