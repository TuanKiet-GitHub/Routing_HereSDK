package com.example.routing_heresdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolygon;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.GestureState;
import com.here.sdk.mapview.LocationIndicator;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapPolygon;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.routing.AvoidanceOptions;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.OptimizationMode;
import com.here.sdk.routing.PedestrianOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RouteOptions;
import com.here.sdk.routing.RouteTextOptions;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Waypoint;
import android.location.Location;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private PermissionRequest permissionRequest;
    private Context context;
    FusedLocationProviderClient fusedLocationProviderClient;
    Button btnClear , btnRoute ;

    private TextView routeTextView;
    private RoutingEngine routingEngine ; // công cụ định tuyến
    private List<Waypoint> waypoints = new ArrayList<>(); // Danh sách các điểm tham chiếu
    private List<MapMarker> waypointMarkers = new ArrayList<>(); //
    private MapPolyline routePolyline ;
    private android.location.Location location;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_View);
//        routeTextView = findViewById(R.id.map_View);
        context = getApplicationContext();
        btnClear = findViewById(R.id.btnClear);
        btnRoute = findViewById(R.id.btnRouting);

        mapView.onCreate(savedInstanceState);
        try {
            routingEngine = new RoutingEngine();
        } catch (InstantiationErrorException e) {
            e.printStackTrace();
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            Log.e("Log","VÀO");
                getLocation();


        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
            Log.e("Log","KHONG");
        }
//           loadMap();
           setLongPressGestureHandler();
           btnRoute.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   calculateRoute();
               }
           });
           btnClear.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   clearMap(v);
               }
           });

    }

    // Get Location
    private void getLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
            @Override
            public void onComplete(@NonNull Task<android.location.Location> task) {
                Location mlocation = task.getResult();
                location = mlocation;
                loadMap();
                Log.e("Log","Latitude : " + location.getLatitude() + " | Longitude : " + location.getLongitude()  +"");
            }
        });
    }

    // Tính quản dường
    public void calculateRoute()
    {
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.alternatives = 3 ;
        routeOptions.optimizationMode = OptimizationMode.FASTEST;
        CarOptions options = new CarOptions(routeOptions, new RouteTextOptions(), new AvoidanceOptions());
        routingEngine.calculateRoute(
                waypoints,
                options,
                new CalculateRouteCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable  List<Route> list) {
                        if (routingError == null)
                        {
                            Route route = list.get(0);
                            drawRoute(route);
                        }
                        else {
                            // Richard doesn't like error handling
                        }
                    }
                }

        );

    }
    // Clear
    public void clearMap(View view)
    {
        for (MapMarker marker : waypointMarkers)
        {
            mapView.getMapScene().removeMapMarker(marker);
        }
        mapView.getMapScene().removeMapPolyline(routePolyline);
        waypoints.clear();
    }





    @RequiresApi(api = Build.VERSION_CODES.O)
    private void drawRoute(Route route) {
        GeoPolyline routeGeoPolyline ;
        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        } catch (InstantiationErrorException e) {
            return;
        }
        Color fillColor = Color.valueOf(0, 0.56f, 0.54f, 0.63f);
        routePolyline = new MapPolyline(routeGeoPolyline , 20 ,fillColor);

        mapView.getMapScene().addMapPolyline(routePolyline);

        Toast.makeText(context , "Your destination is " + route.getLengthInMeters() + " meters away !!! ", Toast.LENGTH_LONG).show();

    }
    // Load map từ Here SDK
    private void loadMap()
    {
//        GeoCoordinates geoCoordinates = new GeoCoordinates(10.46171 , 105.64354);\
            GeoCoordinates geoCoordinates = new GeoCoordinates(location.getLatitude() , location.getLongitude());
            mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
                @Override
                public void onLoadScene(@Nullable MapError mapError) {
                    if (mapError == null) {
                        double distanceInMeters = 1000;
                        mapView.getCamera().lookAt(
                                //        new GeoCoordinates(10.46384,  105.6441), distanceInMeters);
                                geoCoordinates,distanceInMeters);
                    } else {
                        Log.d("Log", "Loading map failed: mapError: " + mapError.name());
                    }
                }
            });
    }
    // Nhấn giữ
    private void setLongPressGestureHandler()
    {
        mapView.getGestures().setLongPressListener((((gestureState, touchPoint) ->{
            if (gestureState == GestureState.BEGIN)
            {
                MapImage waypointImage = MapImageFactory.fromResource(context.getResources(), R.drawable.location);
                MapMarker waypointMarker = new MapMarker(mapView.viewToGeoCoordinates(touchPoint),waypointImage);
                mapView.getMapScene().addMapMarker(waypointMarker);

                waypointMarkers.add(waypointMarker);
                waypoints.add(new Waypoint(mapView.viewToGeoCoordinates(touchPoint)));

            }
        })));
    }

}