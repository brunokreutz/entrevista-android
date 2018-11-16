package com.example.administrador.starwarswiki;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> implements Filterable {
    private List<StarWarsCharacter> mDataset;
    private List<StarWarsCharacter> characterListFiltered;
    private List<Favorite> favoriteList;
    private CharacterViewModel viewModel;

    public RecyclerViewAdapter(CharacterViewModel viewModel) {
        this.viewModel = viewModel;
        mDataset = viewModel.getStarWarsCharactersList().getValue();
        characterListFiltered = mDataset;
        favoriteList = viewModel.getFavoritelist().getValue();
    }

    void setStarWarsCharacters(List<StarWarsCharacter> starWarsCharacters){
        mDataset = starWarsCharacters;
        notifyDataSetChanged();
    }

    void setFavoriteList(List<Favorite> favoriteList){
        this.favoriteList = favoriteList;
        notifyDataSetChanged();
    }

    // Provide a reference to the views for each data item
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public TextView textViewName;
        public TextView textViewGender;
        public TextView textViewHeight;
        public TextView textViewMass;
        public ToggleButton favoriteButton;

        public MyViewHolder(View v, TextView textViewName, TextView textViewGender, TextView textViewHeight, TextView textViewMass,  ToggleButton favoriteButton) {
            super(v);
            mView = v;
            this.textViewName = textViewName;
            this.textViewGender = textViewGender;
            this.textViewHeight = textViewHeight;
            this.textViewMass = textViewMass;
            this.favoriteButton = favoriteButton;
        }
    }
     // Create new views (invoked by the layout manager)
    @Override
    public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item, parent, false);

        TextView textViewName = v.findViewById(R.id.name);
        TextView textViewGender = v.findViewById(R.id.gender);
        TextView textViewHeight = v.findViewById(R.id.height);
        TextView textViewMass = v.findViewById(R.id.mass);
        ToggleButton favbtn = (ToggleButton) v.findViewById(R.id.favorite_button);

        MyViewHolder vh = new MyViewHolder(v,textViewName, textViewGender, textViewHeight, textViewMass, favbtn);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
    // - replace the contents of the view with that element
        if (characterListFiltered != null) {
            viewHolderDataBinder(holder, position, characterListFiltered);
        }else if(mDataset != null){
           viewHolderDataBinder(holder, position, mDataset);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (characterListFiltered != null)
            return characterListFiltered.size();
        else if(mDataset != null)
            return mDataset.size();
        else
            return 0;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    characterListFiltered = mDataset;
                } else {
                    List<StarWarsCharacter> filteredList = new ArrayList<>();
                    for (StarWarsCharacter row : mDataset) {

                        // name match condition.
                        if (row.getName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }

                    characterListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = characterListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                characterListFiltered = (ArrayList<StarWarsCharacter>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    private void viewHolderDataBinder(MyViewHolder holder, int position, List<StarWarsCharacter> dataList){
        holder.textViewName.setText(dataList.get(position).getName());
        holder.textViewGender.setText(dataList.get(position).getGender());
        holder.textViewHeight.setText(dataList.get(position).getHeight());
        holder.textViewMass.setText(dataList.get(position).getMass());
        
        if (favoriteList != null && isInFavorite(favoriteList, dataList.get(position).getId())) {
            holder.favoriteButton.setChecked(true);
            Log.d(">>>>>>>>>", "tem favorito");
        }else{
            holder.favoriteButton.setChecked(false);
        }

        holder.favoriteButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    Log.d(">>>>>>>>>", "inserindo favorito");
                    viewModel.insertFavorite(dataList.get(position).getId());
                } else {
                    // The toggle is disabled
                    viewModel.removeFavorite(dataList.get(position).getId());
                }
            }
        });
    }

    public static boolean isInFavorite(List<Favorite> favlist, int id) {
        for (Favorite row : favlist) {
            // name match condition.
            Log.d("==================================", row.toString());
            if (row.getStarWarsCharacterId() == id) {
                return true;
            }
        }
        return false;
    }

}

