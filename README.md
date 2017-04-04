# About
MapsForgeFX is the Android implementation of MapsForge (https://github.com/mapsforge/mapsforge) ported to JavaFX
in mid 2013. The port was done by Michael Watzko for the Car2X research project lead by Stefan Kaufman which was
developed at IT-Designers GmbH in Esslingen, Germany.

Some minor changes have been applied to the pom.xml and general structure for publication purpose in early 2017.

See the [MapsForgeFX_demo](https://github.com/mwatzko/mapsforgefx_demo) for an example usage.

# Getting started
```java
import de.itd.maps.mapsforge.MapsforgeMap;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {
    
    public static void main(String args[]) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        MapsforgeMap map = new MapsforgeMap();

        map.setPrefSize(1024, 768);
        map.loadMap(new File("/tmp/baden-wuerttemberg.map"));
        map.getMapView().set(48.718299, 9.363812);
        map.updateMapLater(false);

        primaryStage.setScene(new Scene(map));
        primaryStage.show();
    }
}
```

## Interesting methods in the 'MapsForgeMap' class
 * [loadMap(file: File)](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#loadMap-java.io.File-)
 * [hasLoadedMap(): boolean](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#hasLoadedMap--)
 * [updateMap(fast: boolean)](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#updateMap-boolean-)
 * [getMapView(): MapView](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#getMapView--)
 * [setPriorityMouseDrag(priority: int)](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#setPriorityMouseDrag-int-)
 * [getPriorityMouseDrag():int](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#getPriorityMouseDrag--)
 * [addMapItem(item: Node)](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#addMapItem-de.itd.maps.mapsforge.MapItem-)
 * [addMapItem(layer: Integer, item: Node)](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#addMapItem-java.lang.Integer-de.itd.maps.mapsforge.MapItem-)
 * [getGeoPoint(x: double, y: double): GeoPoint](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#getGeoPoint-double-double-)
 * [getMapContextMenu()](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#getMapContextMenu--)
 * [setPaused(paused: boolean)](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#setPaused-boolean-)
 * [isPaused(): boolean](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#isPaused--)
 * [destroy()](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#destroy--)
 * [isDestroyed()](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#isDesytroyed--)
 * [getLiveRenderRule(): LiveRenderRule](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#getLiveRenderRule--)
 
 * [useFileTileCacheProperty(): BooleanProperty](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#useFileTileCacheProperty--)
 * [fileTileCacheTileCount(): ReadOnlyIntegerProperty](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#fileTileCacheTileCount--)
 * [fileTileCacheCapacity(): IntegerProperty](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#fileTileCacheCapacity--)
 * [memoryTileCacheTileCount(): ReadOnlyIntegerProperty](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#memoryTileCacheTileCount--)
 * [memoryTileCacheCapacity(): IntegerProperty](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#memoryTileCacheCapacity--)
 * [clearMemoryTileCache()](https://mwatzko.github.io/mapsforgefx/apidocs/de/itd/maps/mapsforge/MapsforgeMap.html#clearMemoryTileCache--)

## JavaDoc
The JavaDoc is located [here](https://mwatzko.github.io/mapsforgefx/apidocs).

# Dependencies
The source code of the modules 'mapsforge-map' and 'mapsforge-core' from the time of porting is included
at 'src/java/mapsforge'. This is for the uncertain case the the maven artifacts (version 0.3.1-MSM-0.3) are no longer
available through the boundlessgeo.com maven repository,


# License
This project is licensed under the [LGPL v3](COPYING.LESSER).
