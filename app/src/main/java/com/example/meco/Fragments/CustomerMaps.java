package com.example.meco.Fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.meco.R;
import com.example.meco.models.AcceptedCustomers;
import com.example.meco.models.MechanicsNearby;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class CustomerMaps extends Fragment{

    private GoogleMap gMap;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private  boolean locationPermissionGranted;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION=1;
    private Location lastKnownLocation;
    private LatLng mechanicLocation;
    private String mechanicName;
    private String mechanicId;
    private boolean arguments=false;
    private boolean liveLocation;
    private final LatLng defaultLocation=new LatLng(-33.8523341, 151.2106085);

    ArrayList<MechanicsNearby> mechanicsNearbyData;
    FirebaseAuth auth;
    FirebaseFirestore db;
    ListenerRegistration firebaseMechanicLocationListener;
    Marker mechanicLiveLocationMarker;

    public void setMechanicsNearbyData(ArrayList<MechanicsNearby> mechanicsNearbyData)
    {
        this.mechanicsNearbyData=mechanicsNearbyData;
    }


    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
//            LatLng sydney = new LatLng(-34, 151);
//            googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//            googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


            gMap=googleMap;


            // if arguments were passed and we dont need to show live location that means we simply
            // need to locate the mechanic on the map. this is done from the nearby mechanics page (home page of customer view)
            if(arguments==true && liveLocation==false)
            {
                showMechanicLocation();
            }

            // if arguments were passed and we need to show live location that means we need to
            // show live location of the assigned mechanic on the map. this is done from the customer request accepted screen locate button
            if(arguments==true && liveLocation==true)
            {
                showMechanicLiveLocation();
            }

            // show customer live location
            showCustomerLocation();

        }
    };

    private void showMechanicLocation()
    {
        // if arguments were passed that means we need to show specific mechanic location and highlight him

            gMap.addMarker(new MarkerOptions()
                    .position(mechanicLocation)
                    .title(mechanicName));
            gMap.moveCamera(CameraUpdateFactory.newLatLng(mechanicLocation));
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mechanicLocation, 15f));

    }

    private void showMechanicLiveLocation()
    {
        // removing previous listener, if any
        if(firebaseMechanicLocationListener!=null)
        {
            firebaseMechanicLocationListener.remove();
        }

        firebaseMechanicLocationListener=db.collection("mechanic_locations").document(mechanicId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error!=null)
                        {
                            Log.d("LIVELOCATION", "could not listen to mechanic live location from customer maps page");
                        }
                        else if(value!=null && value.exists()){
                            MechanicsNearby mechanic=value.toObject(MechanicsNearby.class);

                            LatLng mechanicLocation=new LatLng(mechanic.getLocation().getLatitude(), mechanic.getLocation().getLongitude());

                            // removing previous marker, if any
                            if(mechanicLiveLocationMarker!=null)
                            {
                                mechanicLiveLocationMarker.remove();
                            }

                            mechanicLiveLocationMarker=gMap.addMarker(new MarkerOptions()
                                    .position(mechanicLocation)
                                    .title(mechanicName));

                        }
                    }
                });

    }

    // shows user current location
    private void showCustomerLocation()
    {

        try{

            Task<Location> locationResult=fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    // successfully fetched current user location
                    if(task.isSuccessful())
                    {
                        gMap.setMyLocationEnabled(true);
                        gMap.getUiSettings().setMyLocationButtonEnabled(true);
                        gMap.getUiSettings().setRotateGesturesEnabled(true);
                        gMap.getUiSettings().setCompassEnabled(true);
                        

//                        gMap.setBuildingsEnabled(false);

                        // represents the current location of the user
                        lastKnownLocation=task.getResult();

                        if(lastKnownLocation!=null)
                        {
                            // add a circle of 1Km around the current location
                            gMap.addCircle(new CircleOptions()
                                    .center(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))
                                    .radius(1500)
                                    .fillColor(Color.TRANSPARENT)
                                    .strokeColor(R.color.faint_grey)
                                    .strokeWidth(5.0f));


                            // users current location
                            gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())));
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), 16f));

                        }

                        // marking all the nearby mechanics on the map
                        // showing nearby mechanics only when we don't want to locate a specific mechanic
                        if(mechanicsNearbyData!=null && arguments==false)
                        {
                            for(MechanicsNearby mechanicNearby:mechanicsNearbyData)
                            {
                                gMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(mechanicNearby.getLocation().getLatitude(), mechanicNearby.getLocation().getLongitude()))
                                        .title(mechanicNearby.getName()));
                            }
                        }


                    }
                    else{
                        Log.d("EXCEPTION:", "Current location is null. Using defaults");
                        Log.e("EXCEPTION:", task.getException().toString());
                        gMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation));
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 16f));
                        gMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                }
            });
        }
        catch(SecurityException e){
            Log.e("EXCEPTION", e.getMessage(), e);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if(gMap!=null)
        {

            // if arguments were passed and we need to show live location that means we need to
            // show live location of the customer on the map. this is done from the my customers page
            if(arguments==true && liveLocation==true)
            {
                showMechanicLiveLocation();
            }

        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(firebaseMechanicLocationListener!=null)
        {
            firebaseMechanicLocationListener.remove();
            Log.d("LIVELOCATION", "Stopped listening to customers live location from mechanic maps page");
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view=inflater.inflate(R.layout.fragment_customer_maps, container, false);
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(getContext());
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();

        Bundle bdl=getArguments();
        if(bdl!=null)
        {
            arguments=true;
            double lat=bdl.getDouble("lat", defaultLocation.latitude);
            double lng=bdl.getDouble("lng", defaultLocation.longitude);
            mechanicLocation=new LatLng(lat, lng);
            mechanicName=bdl.getString("mechanic_name","");
            liveLocation=bdl.getBoolean("live_location", false);
            mechanicId=bdl.getString("mechanic_id");
        }


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }
}