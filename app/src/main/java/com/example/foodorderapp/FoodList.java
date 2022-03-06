package com.example.foodorderapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.foodorderapp.Common.Interface.ItemClickListener;
import com.example.foodorderapp.Model.Category;
import com.example.foodorderapp.Model.Food;
import com.example.foodorderapp.Model.Request;
import com.example.foodorderapp.ViewHolder.FoodViewHolder;
import com.example.foodorderapp.ViewHolder.OrderViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class FoodList extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference foodList;

    String categoryId;

    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    // Search Functionality
    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        // Firebase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Food");
        recyclerView = (RecyclerView) findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Get Intent here
        if (getIntent() != null) {
            categoryId = getIntent().getStringExtra("CategoryID");
        }
        if (categoryId != null && !categoryId.isEmpty()) {
            loadListFood(categoryId);
        }

        // Search
        materialSearchBar = (MaterialSearchBar) findViewById(R.id.searchBar);
        materialSearchBar.setHint("Enter your want-to food");
//        materialSearchBar.setSpeechMode(false); Don't need this because we already define it at XML
        loadSuggest(); // Write function to load Suggest from Firebase

        materialSearchBar.setLastSuggestions(suggestList);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // When user type their text, we will change the suggest list
                List<String> suggest = new ArrayList<>();
                for (String search: suggestList) {
                    if (search.toLowerCase().contains(materialSearchBar.getText().toLowerCase())) {
                        suggest.add(search);
                    }
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean b) {
                // When SearchBar is closed, restore the original suggestion list
                if (!b) {
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence charSequence) {
                // When Search finishes, show result of Search Adapter
                startSearch(charSequence);
            }

            @Override
            public void onButtonClicked(int i) {

            }
        });
    }

    private void startSearch(CharSequence charSequence) {
        FirebaseRecyclerOptions<Food> options = new FirebaseRecyclerOptions.Builder<Food>().setQuery(foodList.orderByChild("name").equalTo(charSequence.toString()), Food.class).build();
        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.food_item,parent, false);
                return new FoodViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder holder, int position, @NonNull Food model) {
                holder.food_name.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage())
                        .into(holder.food_image);
                final Food local = model;
                holder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        // Start new activity to show the food's information
                        Intent foodDetail = new Intent(FoodList.this, FoodDetail.class);
                        // Send FoodID to the new activity
                        foodDetail.putExtra("foodID", searchAdapter.getRef(position).getKey());
                        startActivity(foodDetail);
                    }
                });
            }
        };
        searchAdapter.startListening();
        recyclerView.setAdapter(searchAdapter);
    }
    private void loadSuggest() {
        foodList.orderByChild("menuID").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Food item = postSnapshot.getValue(Food.class);
                            suggestList.add(item.getName()); // Add name of food to the suggestList
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadListFood(String categoryId) {
        FirebaseRecyclerOptions<Food> options = new FirebaseRecyclerOptions.Builder<Food>().setQuery(foodList.orderByChild("menuID").equalTo(categoryId), Food.class).build();

        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.food_item,parent, false);
                return new FoodViewHolder(view);

            }

            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder holder, int position, @NonNull Food model) {
                holder.food_name.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage())
                        .into(holder.food_image);
                final Food local = model;
                holder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        // Start new activity to show the food's information
                        Intent foodDetail = new Intent(FoodList.this, FoodDetail.class);
                        // Send FoodID to the new activity
                        foodDetail.putExtra("foodID", adapter.getRef(position).getKey());
                        startActivity(foodDetail);
                    }
                });
            }
        };
        // Set Adapter
        adapter.startListening();
        recyclerView.setAdapter(adapter);

    }
}