package com.example.meco.Adapters;

import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meco.Fragments.CustomerMaps;
import com.example.meco.R;
import com.example.meco.models.MechanicsNearby;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MechanicsNearbyAdapter extends RecyclerView.Adapter<MechanicsNearbyAdapter.ViewHolder> {

    Context context;

    String customerId;
    GeoPoint customerLocation;
    ArrayList<MechanicsNearby> data=new ArrayList<>();
    private FusedLocationProviderClient fusedLocationProviderClient;

    public MechanicsNearbyAdapter(Context context, ArrayList<MechanicsNearby> data, String customerId, GeoPoint customerLocation)
    {
        this.customerLocation=customerLocation;
        this.customerId=customerId;
        this.context=context;
        this.data=data;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view= LayoutInflater.from(context).inflate(R.layout.nearby_mechanic_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        // showing name
        holder.mechanicName.setText(data.get(position).getName());

        // showing distance
        holder.mechanicDistance.setText(String.format("%.2f KM",data.get(position).getDistance()));

        // request button click listener
        holder.requestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog=new Dialog(context);
                dialog.setContentView(R.layout.request_dialog_box);

                EditText additionalMessage=dialog.findViewById(R.id.request_dialog_edtxt);
                TextView sendRequestBtn=dialog.findViewById(R.id.request_dialog_send_btn);
                TextView cancelRequestBtn=dialog.findViewById(R.id.request_dialog_cancel_btn);

                cancelRequestBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                // when the user clicks on send request button
                sendRequestBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String customerMessage=additionalMessage.getText().toString();
                        String mechanicId=data.get(position).getMechanic_id();

                        Map<String, Object> requestData=new HashMap<>();
                        requestData.put("mechanic_id", mechanicId);
                        requestData.put("customer_id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        requestData.put("customer_name", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                        requestData.put("customer_message", customerMessage);
                        requestData.put("customer_location", customerLocation);


                        FirebaseFirestore db=FirebaseFirestore.getInstance();

                        // sending request data to requests collection
                        db.collection("requests").document(mechanicId).collection("customers").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).set(requestData).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d("MYTAGS", "Request sent to the mechanic");
                                Toast.makeText(context, "Request Sent to "+data.get(position).getName(), Toast.LENGTH_SHORT).show();



                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("MYTAGS", "Could not send request to the mechanic");
                            }
                        });

                        dialog.dismiss();

                    }
                });

                dialog.show();

            }
        });

        // when user presses locate button then he should be redirected to the map fragment with the location of the current mechanic
        holder.locateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                CustomerMaps customerMaps=new CustomerMaps();

                // making the maps button selected
                BottomNavigationView bottomNavigationView=((AppCompatActivity)context).findViewById(R.id.customer_bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.customer_map);

                // sending mechanic location and name to maps
                Bundle bdl=new Bundle();
                bdl.putDouble("lat", data.get(position).getLocation().getLatitude());
                bdl.putDouble("lng", data.get(position).getLocation().getLongitude());
                bdl.putString("mechanic_name", data.get(position).getName());
                bdl.putBoolean("live_location", false);
                customerMaps.setArguments(bdl);

                // loading maps
                loadFragment(customerMaps);

            }
        });

    }


    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView mechanicName;
         TextView mechanicDistance;
        TextView requestBtn;
        TextView locateBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mechanicName= itemView.findViewById(R.id.card_mechanic_name);
            mechanicDistance= itemView.findViewById(R.id.card_mechanic_distance);
            requestBtn=itemView.findViewById(R.id.card_btn_request);
            locateBtn=itemView.findViewById(R.id.card_btn_locate_mechanic);


        }




    }

    public void loadFragment(Fragment fragment)
    {
        FragmentManager fragmentManager=((AppCompatActivity)context).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.customer_screens_container, fragment);

        fragmentTransaction.commit();

    }


    public double calcDistance(double lat1, double lat2, double lon1, double lon2)
    {

        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        return(c * r);
    }


    private void getCustomerLocation()
    {


        // first get the current location of the user
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(context);
        try{

            Task<Location> locationResult=fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                       if(task.isSuccessful())
                       {
                           double curLat=task.getResult().getLatitude();
                           double curLng=task.getResult().getLongitude();



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




}
