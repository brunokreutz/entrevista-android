package com.example.administrador.starwarswiki;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface StarWarsCharacterDao {
    @Insert(onConflict = REPLACE)
    void save(StarWarsCharacter starWarsCharacter);
    @Query("SELECT * FROM starwarscharacter WHERE name = :characterName")
    LiveData<StarWarsCharacter> load(String characterName);
    @Query("SELECT * FROM starwarscharacter")
    LiveData<List<StarWarsCharacter>> getAllCharacters();
    //@Query("SELECT * FROM starwarscharacter WHERE id BETWEEN :v1 and :v2")
    //LiveData<List<StarWarsCharacter>> getCharactersRange(int v1, int v2);
    //@Query("SELECT count(1) FROM starwarscharacter WHERE id = :characterId ")
    //int checkExist(int characterId);
}