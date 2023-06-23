package com.example.meco.Adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meco.Fragments.MechanicMaps;
import com.example.meco.R;
import com.example.meco.models.CustomerRequests;
import com.example.meco.models.OngoingService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CustomerRequestsAdapter extends RecyclerView.Adapter<CustomerRequestsAdapter.ViewHolder> {

    Context context;
    String current_mechanic_id;
    String current_mechanic_name;
    String mechanic_phone;
    ArrayList<CustomerRequests> customerRequestsData=new ArrayList<>();

    public CustomerRequestsAdapter(Context context, ArrayList<CustomerRequests> customerRequestsData, String mechanic_phone) {
        this.context = context;
        this.customerRequestsData = customerRequestsData;
        this.mechanic_phone=mechanic_phone;
        this.current_mechanic_id=FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.current_mechanic_name=FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.customer_requests_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

//        Log.d("MYTAGS", "onBindViewHolder called "+ customerRequestsData.get(position).getCustomer_name() + " "+customerRequestsData.get(position).getCustomer_distance()+ " "+customerRequestsData.get(position).getCustomer_message());


        holder.customerName.setText(customerRequestsData.get(position).getCustomer_name());
        holder.customerDistance.setText(customerRequestsData.get(position).getCustomer_distance()+" KM");
        holder.customerMessage.setText(customerRequestsData.get(position).getCustomer_message());



        // when mechanic clicks on decline button, then delete the customer request
        holder.declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseFirestore db=FirebaseFirestore.getInstance();
                // removing the request
                db.collection("requests").document(current_mechanic_id).collection("customers").document(customerRequestsData.get(position).getCustomer_id()).delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d("MYTAGS", "Request accepted and removed from the request list");

                            }
                        })  .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("MYTAGS", "Could not remove request");
                            }
                        });

            }
        });


        final String[] customer_phone = {""};

        // when mechanic clicks on accept button
        holder.acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseFirestore db=FirebaseFirestore.getInstance();

                // first check if the customer has already been assigned a mechanic in ongoing_services
                db.collection("ongoing_services").document(customerRequestsData.get(position).getCustomer_id()).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                   if(documentSnapshot.exists())
                                   {
                                       OngoingService ongoingService =documentSnapshot.toObject(OngoingService.class);

                                       // if a mechanic has not been assigned then we can accept the request otherwise not
                                       if(ongoingService.getMechanic_id().equals(""))
                                       {
                                           // showing confirmation
                                           Toast.makeText(context, "Request Accepted", Toast.LENGTH_SHORT).show();
                                           Log.d("MYTAGS", current_mechanic_name+" accepted the request "+ customerRequestsData.get(position).getCustomer_id());
                                           Map<String, Object> mechanicData=new HashMap<>();
                                           mechanicData.put("mechanic_id", current_mechanic_id);
                                           mechanicData.put("mechanic_name", current_mechanic_name);
                                           mechanicData.put("mechanic_phone", mechanic_phone);


                                           // updating in ongoing service
                                           db.collection("ongoing_services").document(customerRequestsData.get(position).getCustomer_id()).set(mechanicData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                               @Override
                                               public void onComplete(@NonNull Task<Void> task) {


                                                    if(task.isSuccessful())
                                                    {
                                                        // get customers phone number from users collection and then add the customer to mycustomers (accepted_requests) list
                                                        db.collection("users").document(customerRequestsData.get(position).getCustomer_id()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if(task.isSuccessful())
                                                                {
                                                                    // customer phone is fetched from users collection
                                                                    DocumentSnapshot doc= task.getResult();

                                                                    if(doc.exists())
                                                                    {
                                                                        customer_phone[0] =doc.getString("phone");
                                                                    }

                                                                    // now we need to add it to accepted_customers collection (mycustomers page) and remove from requests

                                                                    // adding to accepted_customers collection
                                                                    Map<String, Object> customerData=new HashMap<>();
                                                                    customerData.put("customer_id", customerRequestsData.get(position).getCustomer_id());
                                                                    customerData.put("customer_name", customerRequestsData.get(position).getCustomer_name());
                                                                    customerData.put("customer_location", customerRequestsData.get(position).getCustomer_location());
                                                                    customerData.put("customer_message", customerRequestsData.get(position).getCustomer_message());
                                                                    customerData.put("customer_phone", customer_phone[0]);
                                                                    db.collection("accepted_customers").document(current_mechanic_id).collection("customers")
                                                                            .document(customerRequestsData.get(position).getCustomer_id()).set(customerData)
                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void unused) {
                                                                                    Log.d("MYTAGS", "Customer added to mycustomer list");
                                                                                }
                                                                            }).addOnFailureListener(new OnFailureListener() {
                                                                                @Override
                                                                                public void onFailure(@NonNull Exception e) {
                                                                                    Log.w("MYTAGS", "Could not add customer to mycustomer list");
                                                                                }
                                                                            });

                                                                    // removing the request
                                                                    db.collection("requests").document(current_mechanic_id).collection("customers").document(customerRequestsData.get(position).getCustomer_id()).delete()
                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void unused) {
                                                                                    Log.d("MYTAGS", "Request accepted and removed from the request list");

                                                                                }
                                                                            })  .addOnFailureListener(new OnFailureListener() {
                                                                                @Override
                                                                                public void onFailure(@NonNull Exception e) {
                                                                                    Log.w("MYTAGS", "Could not remove request");
                                                                                }
                                                                            });

                                                                }
                                                                // could not fetch phone number of customer from users collections
                                                                else{
                                                                    // display a message
                                                                    Log.d("MYTAGS", "Could not fetch customers data "+  task.getException());

                                                                }


                                                            }
                                                        });
                                                    }

                                               }
                                           });
                                       }
                                       else{
                                           Toast.makeText(context, "Customer not available anymore.", Toast.LENGTH_SHORT).show();

                                           // now delete the request
                                           db.collection("requests").document(current_mechanic_id).collection("customers").document(customerRequestsData.get(position).getCustomer_id()).delete()
                                                   .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                       @Override
                                                       public void onSuccess(Void unused) {
                                                           Log.d("MYTAGS", "Request removed from the request list (Customer already assigned a mechanic)");

                                                       }
                                                   })  .addOnFailureListener(new OnFailureListener() {
                                                       @Override
                                                       public void onFailure(@NonNull Exception e) {
                                                           Log.w("MYTAGS", "Could not remove request (Customer already assigned a mechanic)");
                                                       }
                                                   });

                                       }
                                   }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });

            }
        });


        // when user presses locate button then he should be redirected to the map fragment with the location of the current customer
        holder.locateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MechanicMaps mechanicMaps=new MechanicMaps();

                // making the maps button selected
                BottomNavigationView bottomNavigationView=((AppCompatActivity)context).findViewById(R.id.mechanic_bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.mechanic_map);

                // sending mechanic location and name to maps
                Bundle bdl=new Bundle();
                bdl.putDouble("lat", customerRequestsData.get(position).getCustomer_location().getLatitude());
                bdl.putDouble("lng", customerRequestsData.get(position).getCustomer_location().getLongitude());
                bdl.putString("customer_name", customerRequestsData.get(position).getCustomer_name());
                bdl.putBoolean("live_location", false);
                mechanicMaps.setArguments(bdl);

                // loading maps
                loadFragment(mechanicMaps);

            }
        });


    }

    public void loadFragment(Fragment fragment)
    {
        FragmentManager fragmentManager=((AppCompatActivity)context).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mechanic_screens_container, fragment);

        fragmentTransaction.commit();

    }

    @Override
    public int getItemCount() {
        return customerRequestsData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView customerName;
        TextView customerDistance;
        TextView acceptBtn;
        TextView declineBtn;
        TextView locateBtn;
        TextView customerMessage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            customerName=itemView.findViewById(R.id.card_customer_name);
            customerDistance=itemView.findViewById(R.id.card_customer_distance);
            acceptBtn=itemView.findViewById(R.id.card_btn_accept);
            declineBtn=itemView.findViewById(R.id.card_btn_decline);
            locateBtn=itemView.findViewById(R.id.card_btn_locate_customer);
            customerMessage=itemView.findViewById(R.id.card_customer_message);

        }
    }
}
