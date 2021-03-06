package com.example.administrador.starwarswiki.data;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;
import android.util.Log;

import com.example.administrador.starwarswiki.data.dao.PendingFavoriteDao;
import com.example.administrador.starwarswiki.data.dao.StarWarsCharacterDao;
import com.example.administrador.starwarswiki.data.model.PendingFavorite;
import com.example.administrador.starwarswiki.data.model.ResponseError;
import com.example.administrador.starwarswiki.data.model.ResponseSuccess;
import com.example.administrador.starwarswiki.network.ApiaryService;
import com.example.administrador.starwarswiki.network.RetrofitConfig;
import com.example.administrador.starwarswiki.network.SwapiService;
import com.example.administrador.starwarswiki.data.model.PeopleList;
import com.example.administrador.starwarswiki.data.model.Planet;
import com.example.administrador.starwarswiki.data.model.Specie;
import com.example.administrador.starwarswiki.data.model.StarWarsCharacter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StarWarsRepository {
    private StarWarsDatabase starWarsDatabase;
    private SwapiService swapiService;
    private ApiaryService apiaryService;
    private LiveData<List<StarWarsCharacter>> starWarsCharacterlist;
    private LiveData<StarWarsCharacter> starWarsCharacterLiveData;
    private MutableLiveData<String> specie;
    private MutableLiveData<String> planet;
    private MutableLiveData<String> favoriteResponseMessage;
    private int count = 1;

    public StarWarsRepository(Application application, RetrofitConfig retrofitConfig) {
        starWarsDatabase = StarWarsDatabase.getDatabase(application);
        swapiService = retrofitConfig.getSwapiService();
        apiaryService = retrofitConfig.getApiaryService();
        starWarsCharacterlist = starWarsDatabase.starWarsCharacterDao().getAllCharacters();
        favoriteResponseMessage = new MutableLiveData<>();
    }

    public StarWarsRepository(Application application, RetrofitConfig retrofitConfig, int id) {
        starWarsDatabase = StarWarsDatabase.getDatabase(application);
        swapiService = retrofitConfig.getSwapiService();
        apiaryService = retrofitConfig.getApiaryService();
        starWarsCharacterLiveData = starWarsDatabase.starWarsCharacterDao().getCharcter(id);
        specie = new MutableLiveData<String>();
        planet = new MutableLiveData<String>();
        favoriteResponseMessage = new MutableLiveData<>();
    }

    public LiveData<List<StarWarsCharacter>> getCharacters(){
        return starWarsCharacterlist;
    }

    public LiveData<StarWarsCharacter> getCharacter(){
        return starWarsCharacterLiveData;
    }

    public MutableLiveData<String> getSpecie() {
        return specie;
    }

    public MutableLiveData<String> getPlanet() {
        return planet;
    }

    public MutableLiveData<String> getFavoriteResponseMessage() {
        return favoriteResponseMessage;
    }

    public void getCharacterById(int id){
        new getCharacterAsyncTask(starWarsDatabase.starWarsCharacterDao()).execute(id);
    }

    public void upsertCharacter(StarWarsCharacter starWarsCharacter) {
        new upsertAsyncTask(starWarsDatabase.starWarsCharacterDao()).execute(starWarsCharacter);
    }

    public void dispatchPendingFavorites(){
        new DispatchPendingFavoritesAsyncTask(starWarsDatabase.pendingFavoriteDao(),apiaryService).execute();
    }

    public void getSpecie(int id){
        Call<Specie> call = swapiService.getSpecies(id);
        //PARALLEL
        call.enqueue(new Callback<Specie>() {
            @Override
            public void onResponse(Call<Specie> call, Response<Specie> response) {
                if(response.isSuccessful()) {
                   Log.d("debug", "fetching specie");
                   specie.setValue(response.body().getName());
                }
            }

            @Override
            public void onFailure(Call<Specie> call, Throwable t) {
                Log.d("ERROR", t.getMessage(), t);
            }
        });
    }

    public void getPlanet(int id){
        Call<Planet> call = swapiService.getPlanet(id);
        //PARALLEL
        call.enqueue(new Callback<Planet>() {
            @Override
            public void onResponse(Call<Planet> call, Response<Planet> response) {
                if(response.isSuccessful()) {
                    Log.d("debug", "fetching specie");
                    planet.setValue(response.body().getName());
                }
            }

            @Override
            public void onFailure(Call<Planet> call, Throwable t) {
                Log.d("ERROR", t.getMessage(), t);
            }
        });
    }

    public void updateFavorite(boolean favorite, int i){
        if (favorite) {
            //half of the requests must have a status=400 header
            //count is used to intercalate requests
            Call<Object> call = null;

            if (count % 2 == 0)
                call = apiaryService.postFavoriteWithHeader(i, "status=400");
            else
                call = apiaryService.postFavorite(i);

            count++;
            ObjectMapper mapper = new ObjectMapper();

            call.enqueue(new Callback<Object>() {

                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.code() == 400) {
                        try {
                            ResponseError responseError = mapper.readValue(response.errorBody().byteStream(), ResponseError.class);
                            favoriteResponseMessage.setValue(responseError.getError_message());
                            PendingFavorite pendingFavorite = new PendingFavorite(i);
                            new insertPendingFavoriteAsyncTask(starWarsDatabase.pendingFavoriteDao()).execute(pendingFavorite);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ResponseSuccess responseSuccess =  mapper.convertValue( response.body(), ResponseSuccess.class);
                        favoriteResponseMessage.setValue(responseSuccess.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    Log.d("error", t.getMessage());

                }
            });
        }
        MyParams params = new MyParams(favorite,i);
        new updateFavoriteAsyncTask(starWarsDatabase.starWarsCharacterDao()).execute(params);
    }

    public void fetchInitialData(){
        Call<PeopleList> call = swapiService.getStarWarsCharacters();
        call.enqueue(new Callback<PeopleList>() {
            @Override
            public void onResponse(Call<PeopleList> call, Response<PeopleList> response) {
                Log.d("debug", "fetching initial data");
                if(response.isSuccessful()) {
                    for (StarWarsCharacter starWarsCharacter : response.body().getResults()) {
                        starWarsCharacter.setId(Integer.parseInt(starWarsCharacter.getUrl().replaceAll("[^\\d]", "")));
                        starWarsCharacter.setFavorite(false);
                        upsertCharacter(starWarsCharacter);
                    }
                    Log.d("debug", "success");
                }
            }

            @Override
            public void onFailure(Call<PeopleList> call, Throwable t) {
                Log.d("ERROR", t.getMessage(), t);
            }
        });
    }

    public void loadNextDataFromApi(int offset) {
        // Send an API request to retrieve appropriate paginated data
        //  --> Send the request including an offset value (i.e `page`) as a query parameter.
        Call<PeopleList> call = swapiService.getStarWarsCharacters(offset);
        call.enqueue(new Callback<PeopleList>() {
            @Override
            public void onResponse(Call<PeopleList> call, Response<PeopleList> response) {
                if (response.isSuccessful()) {
                    for (StarWarsCharacter starWarsCharacter : response.body().getResults()) {
                        starWarsCharacter.setId(Integer.parseInt(starWarsCharacter.getUrl().replaceAll("[^\\d]", "")));
                        upsertCharacter(starWarsCharacter);
                    }
                    Log.d("debug", "loading next page");
                }
            }

            @Override
            public void onFailure(Call<PeopleList> call, Throwable t) {
                Log.d("ERROR", "deu erro",t);
            }
        });
    }

    private static class insertPendingFavoriteAsyncTask extends AsyncTask<PendingFavorite, Void, Void> {
        private PendingFavoriteDao mAsyncTaskDao;
        insertPendingFavoriteAsyncTask(PendingFavoriteDao dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected Void doInBackground(final PendingFavorite... params) {
            mAsyncTaskDao.save(params[0]);
            return null;
        }
    }

    private static class upsertAsyncTask extends AsyncTask<StarWarsCharacter, Void, Void> {
        private StarWarsCharacterDao mAsyncTaskDao;
        upsertAsyncTask(StarWarsCharacterDao dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected Void doInBackground(final StarWarsCharacter... params) {
            long id = mAsyncTaskDao.save(params[0]);
            if (id == -1) {
                params[0].setFavorite(mAsyncTaskDao.load(params[0].getId()).isFavorite());
                mAsyncTaskDao.update(params[0]);
            }
            return null;
        }
    }

    private static class updateFavoriteAsyncTask extends AsyncTask<MyParams, Void , Void> {
        private StarWarsCharacterDao mAsyncTaskDao;
        updateFavoriteAsyncTask(StarWarsCharacterDao dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected Void doInBackground(MyParams... params) {
           mAsyncTaskDao.updateFavorite( params[0].isB(), params[0].getI());
           return null;
        }
    }

    private static class getCharacterAsyncTask extends AsyncTask<Integer, Void , Void> {
        private StarWarsCharacterDao mAsyncTaskDao;
        getCharacterAsyncTask(StarWarsCharacterDao dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected Void doInBackground(Integer... params) {
            mAsyncTaskDao.load(params[0]);
            return null;
        }
    }

    private static class DispatchPendingFavoritesAsyncTask extends AsyncTask<Void, Void , Void> {
        private PendingFavoriteDao mAsyncTaskDao;
        private ApiaryService apiaryService;
        DispatchPendingFavoritesAsyncTask(PendingFavoriteDao dao, ApiaryService apiaryService) {
            mAsyncTaskDao = dao;
            this.apiaryService = apiaryService;

        }
        @Override
        protected Void doInBackground(Void... params) {
            List<PendingFavorite> list = mAsyncTaskDao.selectAllPendingFavorites();
            if(list != null && list.size()>0){
                for (PendingFavorite pendingFavorite : list) {
                    Call<Object> call = apiaryService.postFavorite(pendingFavorite.getId());
                    call.enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> response) {
                            if (response.isSuccessful()) {
                                new deletePendingFavoritesAsyncTask(mAsyncTaskDao).execute(pendingFavorite.getId());
                                Log.d("pendingfavorite", String.valueOf(pendingFavorite.getId()));
                            }
                        }

                        @Override
                        public void onFailure(Call<Object> call, Throwable t) {
                            Log.d("response", t.getMessage());
                        }
                    });
                }
            }
            return null;
        }
    }

    private static class deletePendingFavoritesAsyncTask extends AsyncTask<Integer, Void , Void> {
        private PendingFavoriteDao mAsyncTaskDao;
        deletePendingFavoritesAsyncTask(PendingFavoriteDao dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected Void doInBackground(Integer... params) {
            mAsyncTaskDao.deletePendingFavorite(params[0]);
            Log.d("pendingfavorite", "deleted " + String.valueOf(params[0]));
            return null;
        }
    }


    private class MyParams{
        private boolean b;
        private int i;

        public MyParams(boolean b, int i) {
            this.b = b;
            this.i = i;
        }

        public boolean isB() {
            return b;
        }

        public void setB(boolean b) {
            this.b = b;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }

}