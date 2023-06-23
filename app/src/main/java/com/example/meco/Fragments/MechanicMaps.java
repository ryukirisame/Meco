package com.example.meco.Fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.meco.R;
import com.example.meco.models.AcceptedCustomers;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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

import java.util.Timer;
import java.util.TimerTask;

public class MechanicMaps extends Fragment {

    private GoogleMap gMap;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private  boolean locationPermissionGranted;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION=1;
    private Location lastKnownLocation;
    private final LatLng defaultLocation=new LatLng(-33.8523341, 151.2106085);
    String customerName;
    String customerId;
    LatLng customerLocation;
    boolean arguments=false;
    boolean liveLocation;
    Timer timer;
    FirebaseFirestore db;
    FirebaseAuth auth;
    ListenerRegistration firebaseCustomerLocationListener;
    Marker customerLiveLocationMarker;
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
            // need to locate the customer on the map. this is done from customer requests page
            if(arguments==true && liveLocation==false)
            {
                showCustomerLocation();
            }

            // if arguments were passed and we need to show live location that means we need to
            // show live location of the customer on the map. this is done from the my customers page
            if(arguments==true && liveLocation==true)
            {
                showCustomerLiveLocation();
            }

            // showing live location of the mechanic
            showMechanicLocation();

        }
    };


    private void showCustomerLiveLocation()
    {

        // removing previous listener, if any
        if(firebaseCustomerLocationListener!=null)
        {
            firebaseCustomerLocationListener.remove();
        }

        firebaseCustomerLocationListener=db.collection("accepted_customers").document(auth.getCurrentUser().getUid()).collection("customers").document(customerId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error!=null)
                        {
                            Log.d("LIVELOCATION", "could not listen to customers live location from mechanic maps page");

                        }
                        else{
//                            Toast.makeText(getActivity(), "Customer location updated", Toast.LENGTH_SHORT).show();

                            AcceptedCustomers customer=value.toObject(AcceptedCustomers.class);

                            LatLng customerLocation= new LatLng(customer.getCustomer_location().getLatitude(), customer.getCustomer_location().getLongitude());

                            // removing previous marker
                            if(customerLiveLocationMarker!=null)
                            {
                                customerLiveLocationMarker.remove();
                            }

                            customerLiveLocationMarker=gMap.addMarker(new MarkerOptions()
                                    .position(customerLocation)
                                    .title(customerName));
//                                            gMap.moveCamera(CameraUpdateFactory.newLatLng(customerLocation));
//                                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLocation, 17f));



                            Log.d("LIVELOCATION","Customer live location updated to: " +customerLocation.latitude+" "+customerLocation.longitude);
                        }
                    }
                });



    }

    private void showCustomerLocation()
    {
        // if arguments were passed that means we need to show specific customer location and highlight him

            gMap.addMarker(new MarkerOptions()
                    .position(customerLocation)
                    .title(customerName));
            gMap.moveCamera(CameraUpdateFactory.newLatLng(customerLocation));
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLocation, 16f));

    }

    private void showMechanicLocation()
    {
        try{

            Task<Location> locationResult=fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.isSuccessful())
                    {
                        gMap.setMyLocationEnabled(true);
                        gMap.getUiSettings().setMyLocationButtonEnabled(true);
                        gMap.getUiSettings().setRotateGesturesEnabled(true);
                        gMap.getUiSettings().setCompassEnabled(true);


//                        gMap.setBuildingsEnabled(false);

                        lastKnownLocation=task.getResult();
                        if(lastKnownLocation!=null)
                        {
                            // mechanics current location
                            gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())));
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), 17f));

                        }


                    }
                    else{
                        Log.d("EXCEPTION:", "Current location is null. Using defaults");
                        Log.e("EXCEPTION:", task.getException().toString());
                        gMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation));
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 18f));
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
                showCustomerLiveLocation();
            }

        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(firebaseCustomerLocationListener!=null)
        {
            firebaseCustomerLocationListener.remove();
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

        db=FirebaseFirestore.getInstance();
        auth=FirebaseAuth.getInstance();

        Bundle bdl=getArguments();
        if(bdl!=null)
        {
            arguments=true;
            double lat=bdl.getDouble("lat", defaultLocation.latitude);
            double lng=bdl.getDouble("lng", defaultLocation.longitude);
            customerLocation=new LatLng(lat, lng);
            customerName=bdl.getString("customer_name","");
            customerId=bdl.getString("customer_id","");
            liveLocation=bdl.getBoolean("live_location");
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