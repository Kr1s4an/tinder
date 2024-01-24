package com.volasoftware.tinder.repository;

import com.volasoftware.tinder.model.FriendDetails;
import com.volasoftware.tinder.model.User;
import com.volasoftware.tinder.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, java.lang.Long> {
    Optional<User> findOneByEmail(String email);

    List<User> findByType(UserType userType);

    @Query(value = "SELECT " +
            "round(3959 * ACOS( COS( RADIANS(:lat) ) " +
            "                     * COS( RADIANS( friends.lat ) )" +
            "                     * COS( RADIANS( friends.lon ) " +
            "                            - RADIANS(:lon) " +
            "                          ) " +
            "                     + SIN( RADIANS(:lat) )" +
            "                     * SIN( RADIANS( friends.lat ) ) " +
            "                 ), 2) as distanceInKm, " +
            "         u.first_name as firstName, " +
            "         u.last_name as lastName, " +
            "         u.age as age" +
            "       " +
            "FROM " +
            "(SELECT " +
            "l.LONGITUDE as lon," +
            "l.LATITUDE as lat, " +
            "f.FRIEND_ID as fr " +
            "FROM friend f " +
            "inner join location l on l.USER_ID = f.FRIEND_ID " +
            "where f.USER_ID = :id) friends " +
            "join user u on friends.fr = u.ID " +
            "order by distanceInKm ASC", nativeQuery = true)
    List<FriendDetails> findUserFriendsSortedByLocation(@Param("id") Long id,
                                                        @Param("lat") Double lat,
                                                        @Param("lon") Double lon);
    Optional<User> findFriendById(Long friendId);
}
