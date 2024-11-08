package com.example.soundnotificationspeed;

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ru.dgis.sdk.Context
import ru.dgis.sdk.DGis
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.map.BearingSource
import ru.dgis.sdk.map.CameraPosition
import ru.dgis.sdk.map.GraphicsPreset
import ru.dgis.sdk.map.MapView
import ru.dgis.sdk.map.MyLocationControllerSettings
import ru.dgis.sdk.map.MyLocationMapObjectSource
import ru.dgis.sdk.map.StyleZoom
import ru.dgis.sdk.map.Zoom
import ru.dgis.sdk.navigation.NavigationManager
import ru.dgis.sdk.navigation.RouteBuildOptions
import ru.dgis.sdk.navigation.SimulationConstantSpeed
import ru.dgis.sdk.navigation.SimulationSpeedMode
import ru.dgis.sdk.navigation.SpeedRange
import ru.dgis.sdk.navigation.SpeedRangeToStyleZoom
import ru.dgis.sdk.positioning.DefaultLocationSource
import ru.dgis.sdk.positioning.registerPlatformLocationSource
import ru.dgis.sdk.routing.CarRouteSearchOptions
import ru.dgis.sdk.routing.RouteDistance
import ru.dgis.sdk.routing.RouteSearchOptions
import ru.dgis.sdk.routing.RouteSearchPoint
import ru.dgis.sdk.routing.RouteSearchType
import ru.dgis.sdk.routing.TrafficRouter

class MainActivity : ComponentActivity() {
    private lateinit var sdkContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sdkContext = DGis.initialize(
            this
        )
        registerPlatformLocationSource(sdkContext, DefaultLocationSource(
            applicationContext
        ))

        val navigationManager = NavigationManager(sdkContext)
        val trafficRouter = TrafficRouter(sdkContext)

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                MapTest(sdkContext = sdkContext, navigationManager = navigationManager, trafficRouter = trafficRouter)
            }
        }
    }
}

@Composable
fun BubbleMapView(sdkContext: Context, navigationManager: NavigationManager) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            MapView(context).apply {
                this.getMapAsync { map ->
                    val mapSource = MyLocationMapObjectSource(
                        sdkContext,
                        MyLocationControllerSettings(
                            bearingSource = BearingSource.AUTO,
                        ),
                    )
                    map.addSource(mapSource)

                    navigationManager.mapManager.addMap(map)
                    map.camera.setBehaviour(
                        behaviour = navigationManager.mapFollowController.cameraBehaviour
                    )
                    map.camera.position = CameraPosition(point = GeoPoint(latitude = 55.759909, longitude = 37.618806), zoom = Zoom(value = 17f))
                    map.graphicsPreset = GraphicsPreset.IMMERSIVE

                }
            }
        },
    )
}

@Composable
fun MapTest(sdkContext: Context, navigationManager: NavigationManager, trafficRouter: TrafficRouter) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            BubbleMapView(sdkContext = sdkContext, navigationManager = navigationManager)
        }
        Row {
            Button(onClick = {
                val routeSearchOptions = RouteSearchOptions(
                    CarRouteSearchOptions(
                        avoidTollRoads = true,
                        avoidUnpavedRoads = false,
                        routeSearchType = RouteSearchType.JAM
                    )
                )
                val startSearchPoint = RouteSearchPoint(
                    coordinates = GeoPoint(latitude = 24.742325877, longitude = 46.6453437446)
                )
                val finishSearchPoint = RouteSearchPoint(
                    coordinates = GeoPoint(latitude = 24.755246334971346, longitude = 46.63962439633906)
                )

                val routeBuildOptions = RouteBuildOptions(
                    finishPoint = finishSearchPoint,
                    routeSearchOptions = routeSearchOptions,
                )

                val routesFuture = trafficRouter.findRoute(startSearchPoint, finishSearchPoint, routeSearchOptions)
                routesFuture.onResult {
                    navigationManager.mapFollowController.setFollow(true)
                    navigationManager.zoomFollowSettings.speedRangeToStyleZoomSequence = listOf(
                        SpeedRangeToStyleZoom(
                            range = SpeedRange(
                                minSpeed = 0.0,
                                maxSpeed = 5.0
                            ),
                            minDistanceToManeuver = RouteDistance(millimeters = 0),
                            maxDistanceToManeuver = RouteDistance(millimeters = 0),
                            styleZoom = StyleZoom(value = 16.5f)
                        )
                    )

                    navigationManager.simulationSettings.speedMode = SimulationSpeedMode(
                        speed = SimulationConstantSpeed(22.2)
                    )

                    navigationManager.startSimulation(
                        routeBuildOptions,
                        trafficRoute = it.first()
                    )
                }
            }) {
                Text(text = "Start Navigation")
            }
        }
    }
}
