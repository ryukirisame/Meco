package com.example.meco;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.meco.Fragments.CustomerHome;
import com.example.meco.Fragments.CustomerMaps;
import com.example.meco.Fragments.CustomerProfile;
import com.example.meco.Fragments.CustomerRequestAccepted;
import com.example.meco.models.CUSTOMER_LOCATION;
import com.example.meco.models.CUSTOMER_REQUEST_ACCEPTED_DATA;
import com.example.meco.models.MechanicsNearby;
import com.example.meco.models.OngoingService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CustomerMainActivity extends AppCompatActivity implements CustomerHome.CustomerHomeListener {


    ArrayList<String> user;
    String userName, userEmail, userPhone, userAccountType, userId;
    private BottomNavigationView bottomNavigationView;


    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION=1;

    private int fragmentRequestingForLocation;

    private final int CUSTOMER_HOME=0;
    private final int CUSTOMER_MAPS=1;

    CustomerHome customerHome;
    CustomerMaps customerMaps;
    CustomerProfile customerProfile;
    CustomerRequestAccepted customerRequestAccepted;

    FirebaseFirestore db=FirebaseFirestore.getInstance();
    FirebaseAuth auth=FirebaseAuth.getInstance();
    ListenerRegistration listenerRegistration;
    Timer timer=new Timer();
    public static final int NOTIFICATION_ID=100;

    FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);

        // getting user data passed from login activity
        Bundle bdl=getIntent().getExtras();
        if(bdl!=null)
        {

            user=bdl.getStringArrayList("userData");
            userName=user.get(0);
            userEmail=user.get(1);
            userPhone=user.get(2);
            userAccountType=user.get(3);
            userId=user.get(4);

        }


        // variables initialization
        bottomNavigationView=findViewById(R.id.customer_bottom_navigation);
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(CustomerMainActivity.this);


        // creating fragments
        customerHome=new CustomerHome();
        customerMaps=new CustomerMaps();
        customerProfile=new CustomerProfile();
        customerRequestAccepted=new CustomerRequestAccepted();

        // we need to check whether a request by customer has been accepted or not, if it has been
        // then we need to open CustomerRequestAccepted fragment otherwise we need to show CustomerHome fragment
        showRelevantScreen();

       // click listener on bottom navigation
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id=item.getItemId();

                if(id==R.id.customer_home)
                {
                    // if there are no ongoing service then simply go to customerHome page
                    if(CUSTOMER_REQUEST_ACCEPTED_DATA.CUSTOMER_REQUEST_ACCEPTED==false)
                    {
                        Bundle b=new Bundle();
                        b.putString("customerId", userId);
                        customerHome.setArguments(b);
                        loadFragment(customerHome);
                    }

                    // else go to CustomerRequestAccepted Page
                    else{


                        Bundle bdl=new Bundle();
                        bdl.putString("mechanic_name",CUSTOMER_REQUEST_ACCEPTED_DATA.ongoingService.getMechanic_name());
                        bdl.putString("mechanic_id", CUSTOMER_REQUEST_ACCEPTED_DATA.ongoingService.getMechanic_id());
                        bdl.putString("mechanic_phone", CUSTOMER_REQUEST_ACCEPTED_DATA.ongoingService.getMechanic_phone());
                        customerRequestAccepted.setArguments(bdl);

                        // finally load the fragment
                        loadFragment(customerRequestAccepted);
                    }


                }
                else if(id==R.id.customer_map)
                {
                    // if we don't have location permission then ask for it else open maps
                    if(checkLocationPermission()==false)
                    {
                        fragmentRequestingForLocation=CUSTOMER_MAPS;
                        getLocationPermission(); // requesting for location
                    }
                    else
                    {
                        Log.d("MYTAGS", "fetching data from customerHome about nearby mechanics");

                        // if we dont have an ongoing request then only we need to fetch nearby mechanics
                        if(CUSTOMER_REQUEST_ACCEPTED_DATA.CUSTOMER_REQUEST_ACCEPTED==false)
                        {
                            //fetch data from customerHome about nearby mechanics (interface)
                            customerHome.fetchMechanicsNearbyData();
                            Log.d("MYTAGS", "done fetching data from customerHome about nearby mechanics");
                        }


                        // load customer maps
                        loadFragment(customerMaps);
                    }

                }
                else if(id==R.id.customer_profile)
                {
                    // passing user info to profile page
                    Bundle bdl=new Bundle();
                    bdl.putString("name", userName);
                    bdl.putString("email", userEmail);
                    bdl.putString("phone", userPhone);
                    bdl.putString("accountType", userAccountType);


                    customerProfile.setArguments(bdl);

                    loadFragment(customerProfile);
                }

                return true;
            }
        });

    }

    private void showRelevantScreen()
    {
        // we need to check whether a request by customer has been accepted or not, if it has been
        // then we need to open CustomerRequestAccepted fragment otherwise we need to show CustomerHome fragment

        db.collection("ongoing_services").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if(task.isSuccessful())
                {

                    DocumentSnapshot document= task.getResult();

                    if(document.exists())
                    {
                        OngoingService onService=document.toObject(OngoingService.class);

//                        Toast.makeText(CustomerMainActivity.this, ""+onService.getMechanic_name(), Toast.LENGTH_SHORT).show();
//

                        // if a valid mechanic_id is assigned that means we need to open CustomerRequestAccepted fragment

                        if(!onService.getMechanic_id().equals(""))
                        {
                            // set CUSTOMER_REQUEST_ACCEPTED FLAG to true to indicate that we have an ongoing service
                            // and then store the respective mechanic info from ongoing_service collection
                            CUSTOMER_REQUEST_ACCEPTED_DATA.CUSTOMER_REQUEST_ACCEPTED=true;
                            CUSTOMER_REQUEST_ACCEPTED_DATA.ongoingService=onService;


                            // information that we need to pass to CustomerRequestAccepted fragment
//                            Bundle bdl=new Bundle();
//                            bdl.putString("mechanic_name",onService.getMechanic_name());
//                            bdl.putString("mechanic_id", onService.getMechanic_id());
//                            bdl.putString("mechanic_phone", onService.getMechanic_phone());
//                            customerRequestAccepted.setArguments(bdl);


                            // finally load the fragment
                            bottomNavigationView.setSelectedItemId(R.id.customer_home);
//                            addFragment(customerRequestAccepted);

//                            // keep updating customer location
//                            updateCustomerLocation();



                        }
                        // if a valid mechanic_id is not assigned that means we need to open CustomerHome fragment
                        else{
                            // if we don't have location permission then ask for it else open customer home by default
                            if(checkLocationPermission()==false)
                            {
                                fragmentRequestingForLocation=CUSTOMER_HOME;
                                getLocationPermission(); // requesting for location
                            }

                            else
                            {
                                // opening CustomerHome page

                                Bundle bundle=new Bundle();
                                bundle.putString("customerId", userId);
                                customerHome.setArguments(bundle);
                                bottomNavigationView.setSelectedItemId(R.id.customer_home);
//                                loadFragment(customerHome);
                            }
                        }

                    }
                }
                else{
                    Log.d("MYTAGS", "Could not fetch ongoing services of: "+userId);
                }

            }
        });
    }

    // we need to keep listening if a mechanic has accepted any request or not, if someone has, then we need to show customerrequestaccepted page
    private void listenToOngoingServices()
    {
        // removing previous listener, if any
        if(listenerRegistration!=null)
        {
            listenerRegistration.remove();
        }

        // since we could send the request, so we need to keep listening in the ongoing_services collection to know if the mechanic has accepted the request or not
            listenerRegistration = db.collection("ongoing_services").document(auth.getCurrentUser().getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error!=null)
                {
                    Log.w("MYTAGS", "listening to ongoing services of "+auth
                            .getCurrentUser().getUid()+" failed");
                    return ;
                }
                else{
                    // when the mechanic accepts the request then display customer request accepted screen
                    if(value!=null && value.exists())
                    {

                        OngoingService ongoingService=value.toObject(OngoingService.class);

                        // if a valid mechanic_id is assigned that means a mechanic has accepted the request
                        if(!ongoingService.getMechanic_id().equals(""))
                        {
                            Log.d("MYTAGS", "Ongoing services data changed "+ongoingService.getMechanic_id());

                            // set CUSTOMER_REQUEST_ACCEPTED FLAG to true to indicate that we have an ongoing service
                            // and then store the respective mechanic info from ongoing_service collection
                            CUSTOMER_REQUEST_ACCEPTED_DATA.CUSTOMER_REQUEST_ACCEPTED=true;
                            CUSTOMER_REQUEST_ACCEPTED_DATA.ongoingService=ongoingService;


                            // if we are on home page, then show customer request accepted screen
                            if(bottomNavigationView.getSelectedItemId()==R.id.customer_home)
                            {
                                // passing required data to CustomerRequestAccepted page
                                Bundle bdl=new Bundle();
                                bdl.putString("mechanic_name",ongoingService.getMechanic_name());
                                bdl.putString("mechanic_id", ongoingService.getMechanic_id());
                                bdl.putString("mechanic_phone", ongoingService.getMechanic_phone());
                                customerRequestAccepted.setArguments(bdl);

                                loadFragment(customerRequestAccepted);
                            }

                        }
                    }
                }
            }
        });
    }



    public void updateCustomerLocation()
    {
        timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(CustomerMainActivity.this);
                try{

                    Task<Location> locationResult=fusedLocationProviderClient.getLastLocation();
                    locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if(task.isSuccessful())
                            {

                                CUSTOMER_LOCATION.setLocation(task.getResult().getLatitude(), task.getResult().getLongitude());

                                Log.d("LIVELOCATION", "Updated current location to: "+CUSTOMER_LOCATION.curLat+" "+CUSTOMER_LOCATION.curLong);

                                // if we have an ongoing service, then keep updating location in database.
                                // update customer location in accepted_customers collection
                                if(CUSTOMER_REQUEST_ACCEPTED_DATA.CUSTOMER_REQUEST_ACCEPTED)
                                {
                                    updateCustomerLocationInDatabase();
                                }
                                else{
//                                    Toast.makeText(CustomerMainActivity.this, "NO ONGOING SERVICE", Toast.LENGTH_SHORT).show();
                                    Log.d("LIVELOCATION", "No ongoing services, so not updating customer location in database");
                                }

                            }
                            else{
                                Log.d("MYTAGS", "Could not update current location");
                            }
                        }
                    });

                }
                catch(SecurityException e){
                    Log.d("MYTAGS", e.getMessage());
                }

            }
        }, 0, 10000);


    }

    private void updateCustomerLocationInDatabase()
    {
        GeoPoint customerLocation=new GeoPoint(CUSTOMER_LOCATION.curLat, CUSTOMER_LOCATION.curLong);

            db.collection("accepted_customers").document(CUSTOMER_REQUEST_ACCEPTED_DATA.ongoingService.getMechanic_id())
                    .collection("customers").document(auth.getCurrentUser().getUid()).update("customer_location",customerLocation)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
//                            Toast.makeText(CustomerMainActivity.this, "Customer location updated", Toast.LENGTH_SHORT).show();
                            Log.d("LIVELOCATION", "Updated Customer Location in database with: "+customerLocation.getLatitude()+" "+customerLocation.getLongitude());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("LIVELOCATION", "Could not update customer location");

                        }
                    });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // keep tracking customer location
        updateCustomerLocation();

        // keep listening to ongoing-services in real-time
        listenToOngoingServices();


    }

    @Override
    protected void onPause() {
        super.onPause();

        // removing ongoing_services real time listener
        listenerRegistration.remove();

        // removing timer of updating customer location
        timer.cancel();
    }


    public void loadFragment(Fragment fragment)
    {
        FragmentManager fragmentManager=getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.customer_screens_container, fragment);
//        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();

    }

    public void addFragment(Fragment fragment)
    {
        FragmentManager fragmentManager=getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.customer_screens_container, fragment);
//        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder=new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        builder.show();
    }

    // The code below deals with location permission at run time
    private boolean checkLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
        {
            Log.d("MYTAGS", "Location permission is already granted");
            return true;
        }
        else return false;

    }
    private void getLocationPermission()
    {
            Log.d("MYTAGS", "Requesting for permission");
            ActivityCompat.requestPermissions(CustomerMainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if(requestCode==PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                if(fragmentRequestingForLocation==CUSTOMER_MAPS)
                    loadFragment(customerMaps);
                if(fragmentRequestingForLocation==CUSTOMER_HOME)
                    loadFragment(customerHome);
            }
        }
        else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }


    }


    @Override
    public void serveMechanicsNearbyData(ArrayList<MechanicsNearby> mechanicsNearbyData) {
            customerMaps.setMechanicsNearbyData(mechanicsNearbyData);
    }
}