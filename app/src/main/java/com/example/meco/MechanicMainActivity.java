package com.example.meco;

import androidx.annotation.NonNull;
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

import com.example.meco.Fragments.MechanicHome;
import com.example.meco.Fragments.MechanicMaps;
import com.example.meco.Fragments.MechanicMyCustomers;
import com.example.meco.Fragments.MechanicProfile;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MechanicMainActivity extends AppCompatActivity implements MechanicHome.MechanicHomeListener {


    ArrayList<String> user;
    String userName, userEmail, userPhone, userAccountType, userId;
    private BottomNavigationView bottomNavigationView;

    private boolean locationPermissionGranted=false;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION=1;

    FusedLocationProviderClient fusedLocationProviderClient;
    MechanicHome mechanicHome;
    MechanicMaps mechanicMaps;
    MechanicProfile mechanicProfile;
    MechanicMyCustomers mechanicMyCustomers;
    private int fragmentRequestingForLocation;

    private final int MECHANIC_HOME =0;
    private final int MECHANIC_MAPS =1;
    Timer timer = new Timer();
    FirebaseUser mechanic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_main);


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
        bottomNavigationView=findViewById(R.id.mechanic_bottom_navigation);
        mechanicHome= new MechanicHome();
        mechanicMaps=new MechanicMaps();
        mechanicProfile=new MechanicProfile();
        mechanicMyCustomers=new MechanicMyCustomers();
        mechanic=FirebaseAuth.getInstance().getCurrentUser();


        // start updating mechanic location in database
//        updateMechanicLocationInDatabase();

        // if we don't have location permission then ask for it else open customer home by default
        if(checkLocationPermission()==false)
        {
            fragmentRequestingForLocation=MECHANIC_HOME;
            getLocationPermission(); // requesting for location
        }
        else
        {
            // opening MechanicHome page
            Bundle b=new Bundle();
            b.putString("phone", userPhone);
            mechanicHome.setArguments(b);

            bottomNavigationView.setSelectedItemId(R.id.mechanic_home);
            loadFragment(mechanicHome);
        }

        // working on bottom navigation
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id=item.getItemId();

                if(id==R.id.mechanic_home)
                {
                    Bundle bdl=new Bundle();
                    bdl.putString("phone", userPhone);
                    mechanicHome.setArguments(bdl);

                    loadFragment(mechanicHome);
                }
                else if(id==R.id.mechanic_accepted_requests)
                {
                    loadFragment(mechanicMyCustomers);
                }
                else if(id==R.id.mechanic_map)
                {
                    // if we don't have location permission then ask for it else open maps
                    if(checkLocationPermission()==false)
                    {
                        fragmentRequestingForLocation=MECHANIC_MAPS;
                        getLocationPermission(); // requesting for location
                    }
                    else
                    {
                        // load customer maps
                        loadFragment(mechanicMaps);
                    }

                }
                else if(id==R.id.mechanic_profile)
                {
                    // passing user info to profile page
                    Bundle bdl=new Bundle();
                    bdl.putString("name", userName);
                    bdl.putString("email", userEmail);
                    bdl.putString("phone", userPhone);
                    bdl.putString("accountType", userAccountType);


                    mechanicProfile.setArguments(bdl);

                    loadFragment(mechanicProfile);
                }

                return true;
            }
        });

    }

    private void updateMechanicLocationInDatabase()
    {
        timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("MYTIMER","Time: " +new java.util.Date() );

                // keep finding current location and updating in database
                findCurrLocationAndUpdateInDatabase();
            }
        }, 0, 5000);
    }

    private void findCurrLocationAndUpdateInDatabase()
    {

        // first get the current location of the mechanic
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);
        try{

            Task<Location> locationResult=fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.isSuccessful())
                    {
                        GeoPoint location=new GeoPoint(task.getResult().getLatitude(),task.getResult().getLongitude() );

                        Map<String, Object> mechanicLocationsData=new HashMap<>();
                        mechanicLocationsData.put("location", location);
                        mechanicLocationsData.put("name", userName);
                        mechanicLocationsData.put("phone", userPhone);

                        FirebaseFirestore db=FirebaseFirestore.getInstance();
                        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();

                        if(user!=null)
                        {
                            db.collection("mechanic_locations").document(user.getUid())
                                    .set(mechanicLocationsData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.d("LIVELOCATION", "Mechanic Location Updated with "+location);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("LIVELOCATION", "Could not update mechanic location");
                                        }
                                    });
                        }


                    }
                    else{
                        Log.d("MYTAGS", "Could not fetch current location");
                    }
                }
            });

        }
        catch(SecurityException e){
            Log.d("MYTAGS", e.getMessage());
        }

    }

    private void removeMechanicLocationFromDatabase()
    {
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        db.collection("mechanic_locations").document(mechanic.getUid()).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("MYTAGS", "Stopped updating mechanic location");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MYTAGS", "Could not stop updating mechanic location");
                    }
                });
    }
    public void loadFragment(Fragment fragment)
    {
        FragmentManager fragmentManager=getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mechanic_screens_container, fragment);
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
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if(requestCode==PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                if(fragmentRequestingForLocation== MECHANIC_MAPS)
                    loadFragment(mechanicMaps);
                if(fragmentRequestingForLocation== MECHANIC_HOME)
                    loadFragment(mechanicHome);
            }
        }
        else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMechanicLocationInDatabase();
    }

    @Override
    protected void onPause() {
        timer.cancel();  // we need to stop updating mechanic location in database
        removeMechanicLocationFromDatabase();
        super.onPause();

        // removing customer requests listener
        mechanicHome.getRealTimeDataListener();  // interface listener
    }


    // remove customer requests listener
    @Override
    public void serveListener(ListenerRegistration listenerRegistration) {
        listenerRegistration.remove();
    }


//        logoutBtn=findViewById(R.id.mechanic_logout_btn);
//        auth=FirebaseAuth.getInstance();
//        user=auth.getCurrentUser();
//
//        logoutBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                auth.signOut();
//                Intent i=new Intent(MechanicMainActivity.this, LoginActivity.class);
//                startActivity(i);
//            }
//        });



    }
